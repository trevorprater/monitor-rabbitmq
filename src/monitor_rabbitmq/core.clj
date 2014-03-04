(ns monitor-rabbitmq.core
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clj-time.core :as tc]
            [clj-time.coerce :as tcoerce]
            [riemann.client :as riemann]))

(def path "/api/queues/")

(def rate-query-suffix "_details")
(def avg-rate-suffix ".avg_rate,")
(def message-stat-prefix "message_stats.")
(def rate-metric-suffix ".rate")

; These names determine the part of the query string related to message rates.
; These names also drive the names of the metrics that are sent to Riemann
(def rate-statistic-names
  (list
    "ack"
    "deliver"
    "deliver_get"
    "deliver_no_ack"
    "get"
    "get_no_ack"
    "publish"
    "redeliver"))

(defn make-message-stats-fragment[rate-statistic-names]
  (let [rate-statistic-query-parameters
        (map (fn[name](str message-stat-prefix name rate-query-suffix avg-rate-suffix))
             rate-statistic-names)]
    (apply str "name," rate-statistic-query-parameters)))

(defn make-riemann-client
  ([host port] (riemann/tcp-client :host host :port port))
  ([host] (riemann/tcp-client :host host)))

(defn query-for-queue-data [age-of-oldest-sample-in-seconds seconds-between-samples]
  {:query-params
    {"columns"
     (str
       (make-message-stats-fragment rate-statistic-names)
       "backing_queue_status.len")
     "msg_rates_age"  age-of-oldest-sample-in-seconds
     "msg_rates_incr" seconds-between-samples}})

(defn get-stats [url age-of-oldest-sample-in-seconds seconds-between-samples]
  (:body (client/get url (query-for-queue-data
                           age-of-oldest-sample-in-seconds
                           seconds-between-samples))))

(defn make-rate-pairs-fragment[rate-values]
  (let [metric-names
        (map
          (fn [rate-name] (str rate-name rate-metric-suffix))
          rate-statistic-names)]
    (map list metric-names rate-values)))

(defn get-rate-values [queue-data]
  (map (fn[rate-name]
         (let  [{{{val :avg_rate} (keyword (str rate-name "_details"))} :message_stats} queue-data]
           val))
      rate-statistic-names))

(defn make-queue-monitoring-values [queue-data]
  (let [{name :name} queue-data
        {{length :len} :backing_queue_status}  queue-data
        rate-values (get-rate-values queue-data)]
    (list name (concat (make-rate-pairs-fragment rate-values)
                       (list (list "length" length))))))

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
  [response-for-queue timestamp rabbit-host ]
  (map
    (fn [name-value]
      (make-riemann-event
        name-value
        (str rabbit-host "." (first response-for-queue))
        timestamp "ok"))
    (nth response-for-queue 1)))

(defn send-events-to-riemann [riemann-client riemann-events]
    (map (fn [riemann-event] (send-to-riemann riemann-client riemann-event)) riemann-events))

(defn send-queue-stats [queue-stats riemann-client display-name-of-rabbit-host]
  (let [timestamp (/ (tcoerce/to-long (tc/now)) 1000)]
    (doall
    (map (fn [events-for-one-queue]
           (doall (send-events-to-riemann riemann-client events-for-one-queue)))
         (map (fn [queue-monitoring-values]
                (convert-monitoring-response-to-riemann-events
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

(defn send-rabbitmq-stats-using-riemann-client [rabbitmq-host-and-port
                                                rabbitmq-user
                                                rabbitmq-password
                                                age-of-oldest-sample-in-seconds
                                                seconds-between-samples
                                                display-name-of-rabbit-host
                                                riemann-client]
  (let [url (rmq-url rabbitmq-host-and-port rabbitmq-user rabbitmq-password)
        result (try
                 (send-queue-stats (cheshire/parse-string (get-stats
                                                            url
                                                            age-of-oldest-sample-in-seconds
                                                            seconds-between-samples)
                                                          true)
                                   riemann-client display-name-of-rabbit-host)
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
                                               r-client)))
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
                                               r-client))))


