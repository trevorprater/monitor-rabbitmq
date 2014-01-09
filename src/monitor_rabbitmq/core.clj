(ns monitor-rabbitmq.core
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clj-time.core :as tc]
            [clj-time.coerce :as tcoerce]
            [riemann.client :as riemann]))

(def path "/api/queues/")

(def ack "ack_details" )
(def qdeliver "deliver_details")
(def deliver_get "deliver_get_details")
(def deliver_no_ack "deliver_no_ack_details")
(def qget "get_details")
(def get_no_ack "get_no_ack_details")
(def publish "publish_details")
(def redeliver "redeliver_details")

(defn make-Riemann-client
  ([host port] (riemann/tcp-client :host host :port port))
  ([host] (riemann/tcp-client :host host)))

(defn get-stats [url age-of-oldest-sample-in-seconds seconds-between-samples]
  (:body (client/get url
                     {:query-params
                       {"columns"
                         (str "name,"
                              "message_stats." ack ".avg_rate,"
                              "message_stats." qdeliver ".avg_rate,"
                              "message_stats." deliver_get ".avg_rate,"
                              "message_stats." deliver_no_ack ".avg_rate,"
                              "message_stats." qget ".avg_rate,"
                              "message_stats." get_no_ack ".avg_rate,"
                              "message_stats." publish ".avg_rate,"
                              "message_stats." redeliver ".avg_rate,"
                              "backing_queue_status.len")
                        "msg_rates_age" age-of-oldest-sample-in-seconds
                        "msg_rates_incr" seconds-between-samples}} )))

(defn make-queue-monitoring-values [queue-data]
  (let [{name :name {{ack :avg_rate} :ack_details
                     {deliver :avg_rate} :deliver_details
                     {deliver_get :avg_rate} :deliver_get_details
                     {deliver_no_ack :avg_rate} :deliver_no_ack_details
                     {get :avg_rate} :get_details
                     {get_no_ack :avg_rate} :get_no_ack_details
                     {publish :avg_rate} :publish_details
                     {redeliver :avg_rate} :redeliver_details
                     } :message_stats {length :len} :backing_queue_status}  queue-data]
    (list name
          (list (list "ack.rate" ack)
                (list "deliver.rate" deliver)
                (list "deliver_get.rate" deliver_get)
                (list "deliver_no_ack.rate" deliver_no_ack)
                (list "get.rate" get)
                (list "get_no_ack.rate" get_no_ack)
                (list "publish.rate" publish)
                (list "redeliver.rate" redeliver)
                (list "length" length)))))

(defn send-to-Riemann [Riemann-client Riemann-event]
  (let [result  (riemann/send-event Riemann-client Riemann-event)]
    (list result Riemann-event)))

(defn make-Riemann-event [name-value host timestamp state]
  {:time timestamp
   :host host
   :service (first name-value)
   :metric (second name-value)
   :state state
   :tags ["rabbitmq"] } )

(defn convert-monitoring-response-to-Riemann-events
  "filter out nil values and return list of Riemann events"
  [response-for-queue timestamp rabbit-host ]
  (map
    (fn [name-value]
      (make-Riemann-event
        name-value
        (str rabbit-host "." (first response-for-queue))
        timestamp
        "ok"))
    (remove
      (fn [name-value]
        (nil? (nth name-value 1)))
      (nth response-for-queue 1))))

(defn send-events-to-Riemann [Riemann-client Riemann-events]
    (map (fn [Riemann-event] (send-to-Riemann Riemann-client Riemann-event)) Riemann-events))

(defn send-queue-stats [queue-stats Riemann-client display-name-of-rabbit-host]
  (let [timestamp (tcoerce/to-long (tc/now))]
    (doall
    (map (fn [events-for-one-queue]
           (doall (send-events-to-Riemann Riemann-client events-for-one-queue)))
         (map (fn [queue-monitoring-values]
                (convert-monitoring-response-to-Riemann-events
                  queue-monitoring-values
                  timestamp
                  display-name-of-rabbit-host))
              (map make-queue-monitoring-values queue-stats))))))

(defn rmq-url [rabbitmq-host-and-port rabbitmq-user rabbitmq-password]
  (let [query-info {:user rabbitmq-user, :password rabbitmq-password, :host-and-port rabbitmq-host-and-port}]
    (str "http://"
             (:user query-info)
             ":"
             (:password query-info)
             "@"
             (:host-and-port query-info)
             path)))

(defn send-rabbitmq-stats-using-Riemann-client [rabbitmq-host-and-port
                                                rabbitmq-user
                                                rabbitmq-password
                                                age-of-oldest-sample-in-seconds
                                                seconds-between-samples
                                                display-name-of-rabbit-host
                                                Riemann-client]
  (let [url (rmq-url rabbitmq-host-and-port rabbitmq-user rabbitmq-password)
        result (try
                 (send-queue-stats (cheshire/parse-string (get-stats
                                                            url
                                                            age-of-oldest-sample-in-seconds
                                                            seconds-between-samples)
                                                          true)
                                   Riemann-client display-name-of-rabbit-host)
                 (catch Exception e
                   (throw
                     (Exception.
                       (str "send-rabbitmq-stats-using-Riemann-client caught exception: " (.getMessage e)))))
                 (finally (riemann/close-client Riemann-client))
                 )]
    result))

(defn send-rabbitmq-stats-to-Riemann
  ;signature 1 passes Riemann-port
  ([rabbitmq-host-and-port
    rabbitmq-user
    rabbitmq-password
    age-of-oldest-sample-in-seconds
    seconds-between-samples
    display-name-of-rabbit-host
    Riemann-host
    Riemann-port]
   (let [r-client (make-Riemann-client Riemann-host Riemann-port)]
     (send-rabbitmq-stats-using-Riemann-client rabbitmq-host-and-port
                                               rabbitmq-user
                                               rabbitmq-password
                                               age-of-oldest-sample-in-seconds
                                               seconds-between-samples
                                               display-name-of-rabbit-host
                                               r-client)))
  ;signature 2 does not include Riemann-port. default port is used
  ([rabbitmq-host-and-port
    rabbitmq-user
    rabbitmq-password
    age-of-oldest-sample-in-seconds
    seconds-between-samples
    display-name-of-rabbit-host
    Riemann-host]
   (let [r-client (make-Riemann-client Riemann-host)]
     (send-rabbitmq-stats-using-Riemann-client rabbitmq-host-and-port
                                               rabbitmq-user
                                               rabbitmq-password
                                               age-of-oldest-sample-in-seconds
                                               seconds-between-samples
                                               display-name-of-rabbit-host
                                               r-client))))


