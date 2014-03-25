(ns monitor-rabbitmq.core
  (:require [monitor-rabbitmq.queues :as queues]
            [monitor-rabbitmq.nodes :as nodes]
            [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clj-time.core :as tc]
            [clj-time.coerce :as tcoerce]
            [riemann.client :as riemann]))

(defn make-riemann-client [host port] 
  (riemann/tcp-client :host host :port port))

(defn query-for-data [columns age-of-oldest-sample-in-seconds seconds-between-samples]
  {:query-params
    {"columns"        columns
     "msg_rates_age"  age-of-oldest-sample-in-seconds
     "msg_rates_incr" seconds-between-samples}})

(defn get-stats [url path columns rmq-query-args]
  (:body (client/get 
           (str url path) 
           (query-for-data
             columns
             (:msg-rates-age rmq-query-args)
             (:msg-rates-incr rmq-query-args)))))

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
  [response timestamp]
  (map
    (fn [name-value]
      (make-riemann-event
        name-value
        (first response)
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
                  timestamp))
              (map stats-converter stats display-name-of-rabbit-host))))))

(defn rmq-url [rmq-url-args]
  (str "http://"
    (:user rmq-url-args)
    ":"
    (:password rmq-url-args)
    "@"
    (:host-and-port rmq-url-args)))

(defn send-rabbitmq-stats-using-riemann-client [rmq-url-args
                                                rmq-query-args
                                                display-name-of-rabbit-host
                                                riemann-client
                                                endpoint-args]
    (let [base-url (rmq-url rmq-url-args)
        result (try
        	(send-stats (cheshire/parse-string (get-stats
                                                      base-url
                                                      (:path endpoint-args)
                                                      ((:columns endpoint-args))
                                                      rmq-query-args)
                                                    true)
                                   (:stats-converter endpoint-args)
                                   riemann-client
                                   display-name-of-rabbit-host)
                                   (catch Exception e
                   (throw
                     (Exception.
                       (str "send-rabbitmq-stats-using-riemann-client caught exception: " (.getMessage e)))))
                 (finally (riemann/close-client riemann-client))
                 )]
    result))
    

(defn send-queues-stats-to-riemann [rabbitmq-host-and-port
                                    rabbitmq-user
                                    rabbitmq-password
                                    age-of-oldest-sample-in-seconds
                                    seconds-between-samples
                                    display-name-of-rabbit-host
                                    riemann-host
                                    &[riemann-port]]
  (let [rmq-url-args {:host-and-port rabbitmq-host-and-port :user rabbitmq-user :password rabbitmq-password}
        rmq-query-args {:msg-rates-age age-of-oldest-sample-in-seconds :msg-rates-incr seconds-between-samples}]
    (send-rabbitmq-stats-using-riemann-client rmq-url-args
                                              rmq-query-args
                                              display-name-of-rabbit-host
                                              (make-riemann-client riemann-host riemann-port)
                                              {:path queues/path
                                               :columns queues/query-columns-for-queue-data
                                               :stats-converter queues/make-queue-monitoring-values})))

(defn send-nodes-stats-to-riemann [rabbitmq-host-and-port
    rabbitmq-user
    rabbitmq-password
    age-of-oldest-sample-in-seconds
    seconds-between-samples
    display-name-of-rabbit-host
    riemann-host
    &[riemann-port]]
   (let [rmq-url-args {:host-and-port rabbitmq-host-and-port :user rabbitmq-user :password rabbitmq-password}
         rmq-query-args {:msg-rates-age age-of-oldest-sample-in-seconds :msg-rates-incr seconds-between-samples}]
     (send-rabbitmq-stats-using-riemann-client rmq-url-args
                                               rmq-query-args
                                               display-name-of-rabbit-host
                                               (make-riemann-client riemann-host riemann-port)
                                               {:path nodes/path
                                               :columns nodes/query-columns-for-node-data
                                               :stats-converter nodes/make-node-monitoring-values})))

(defn send-rabbitmq-stats-to-riemann [rabbitmq-host-and-port
                                      rabbitmq-user
                                      rabbitmq-password
                                      age-of-oldest-sample-in-seconds
                                      seconds-between-samples
                                      display-name-of-rabbit-host
                                      riemann-host
                                      &[riemann-port]]
   (let [rmq-url-args {:host-and-port rabbitmq-host-and-port :user rabbitmq-user :password rabbitmq-password}
         rmq-query-args {:msg-rates-age age-of-oldest-sample-in-seconds :msg-rates-incr seconds-between-samples}]
     
       (send-rabbitmq-stats-using-riemann-client rmq-url-args
                                               rmq-query-args
                                               display-name-of-rabbit-host
                                               (make-riemann-client riemann-host riemann-port)
                                               {:path queues/path
                                               :columns queues/query-columns-for-queue-data
                                               :stats-converter queues/make-queue-monitoring-values})
     (send-rabbitmq-stats-using-riemann-client rmq-url-args
                                               rmq-query-args
                                               display-name-of-rabbit-host
                                               (make-riemann-client riemann-host riemann-port)
                                               {:path nodes/path
                                               :columns nodes/query-columns-for-node-data
                                               :stats-converter nodes/make-node-monitoring-values})))