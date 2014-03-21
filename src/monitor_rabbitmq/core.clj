(ns monitor-rabbitmq.core
  (:require [monitor-rabbitmq.queues :as queues]
            [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clj-time.core :as tc]
            [clj-time.coerce :as tcoerce]
            [riemann.client :as riemann]))

(defn make-riemann-client
  ([host port] (riemann/tcp-client :host host :port port))
  ([host] (riemann/tcp-client :host host)))

(defn query-for-data [columns age-of-oldest-sample-in-seconds seconds-between-samples]
  {:query-params
    {"columns"        columns
     "msg_rates_age"  age-of-oldest-sample-in-seconds
     "msg_rates_incr" seconds-between-samples}})

(defn get-stats [url path columns age-of-oldest-sample-in-seconds seconds-between-samples]
  (:body (client/get 
           (str url path) 
           (query-for-data
             columns
             age-of-oldest-sample-in-seconds
             seconds-between-samples))))

(defn send-to-riemann [riemann-client riemann-event]
  (let [result  (riemann/send-event riemann-client riemann-event)]
    (list result riemann-event)))

(defn make-riemann-event [name-value host timestamp state]
  {:time timestamp
   :host host
   :service (first name-value)
   :metric (if (nil? (second name-value)) 0 (second name-value))
   :state state
   :tags ["rabbitmq"] } )

(defn convert-monitoring-response-to-riemann-events
  "return list of Riemann events"
  [response timestamp rabbit-host]
  (map
    (fn [name-value]
      (make-riemann-event
        name-value
        (str rabbit-host "." (first response))
        timestamp "ok"))
    (nth response 1)))

(defn send-events-to-riemann [riemann-client riemann-events]
    (map (fn [riemann-event] (send-to-riemann riemann-client riemann-event)) riemann-events))

(defn send-stats [stats stats-converter riemann-client display-name-of-rabbit-host]
  (let [timestamp (/ (tcoerce/to-long (tc/now)) 1000)]
    (doall
    (map (fn [event-set]
           (doall (send-events-to-riemann riemann-client event-set)))
         (map (fn [monitoring-values]
                (convert-monitoring-response-to-riemann-events
                  monitoring-values
                  timestamp
                  display-name-of-rabbit-host))
              (map stats-converter stats))))))

(defn rmq-url [rabbitmq-host-and-port rabbitmq-user rabbitmq-password]
  (let [query-info {:user rabbitmq-user, :password rabbitmq-password, :host-and-port rabbitmq-host-and-port}]
    (str "http://"
             (:user query-info)
             ":"
             (:password query-info)
             "@"
             (:host-and-port query-info))))

(defn send-rabbitmq-stats-using-riemann-client [rabbitmq-host-and-port
                                                rabbitmq-user
                                                rabbitmq-password
                                                age-of-oldest-sample-in-seconds
                                                seconds-between-samples
                                                display-name-of-rabbit-host
                                                riemann-client
                                                path
                                                columns
                                                stats-converter]
  (let [base-url (rmq-url rabbitmq-host-and-port rabbitmq-user rabbitmq-password)
        result (try
                 (send-stats (cheshire/parse-string (get-stats
                                                      base-url
                                                      path
                                                      columns
                                                      age-of-oldest-sample-in-seconds
                                                      seconds-between-samples)
                                                    true)
                                   stats-converter riemann-client display-name-of-rabbit-host)
                 (catch Exception e
                   (throw
                     (Exception.
                       (str "send-rabbitmq-stats-using-riemann-client caught exception: " (.getMessage e)))))
                 (finally (riemann/close-client riemann-client))
                 )]
    result))

(defn send-rabbitmq-stats-to-riemann
  ;signature 1 passes Riemann-port
  ([rabbitmq-host-and-port
    rabbitmq-user
    rabbitmq-password
    age-of-oldest-sample-in-seconds
    seconds-between-samples
    display-name-of-rabbit-host
    riemann-host
    riemann-port]
   (let [r-client (make-riemann-client riemann-host riemann-port)]
     (send-rabbitmq-stats-using-riemann-client rabbitmq-host-and-port
                                               rabbitmq-user
                                               rabbitmq-password
                                               age-of-oldest-sample-in-seconds
                                               seconds-between-samples
                                               display-name-of-rabbit-host
                                               r-client
                                               queues/path
                                               queues/query-columns-for-queue-data
                                               queues/make-queue-monitoring-values)))
  ;signature 2 does not include Riemann-port. default port is used
  ([rabbitmq-host-and-port
    rabbitmq-user
    rabbitmq-password
    age-of-oldest-sample-in-seconds
    seconds-between-samples
    display-name-of-rabbit-host
    riemann-host]
   (let [r-client (make-riemann-client riemann-host)]
     (send-rabbitmq-stats-using-riemann-client rabbitmq-host-and-port
                                               rabbitmq-user
                                               rabbitmq-password
                                               age-of-oldest-sample-in-seconds
                                               seconds-between-samples
                                               display-name-of-rabbit-host
                                               r-client
                                               queues/path
                                               queues/query-columns-for-queue-data
                                               queues/make-queue-monitoring-values))))