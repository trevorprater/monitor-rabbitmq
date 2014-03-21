(ns monitor-rabbitmq.queues)

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

(defn make-message-stats-fragment [rate-statistic-names]
  (let [rate-statistic-query-parameters
        (map (fn[name](str message-stat-prefix name rate-query-suffix avg-rate-suffix))
             rate-statistic-names)]
    (apply str "name," rate-statistic-query-parameters)))

(defn query-columns-for-queue-data []
  (str
    (make-message-stats-fragment rate-statistic-names)
    "backing_queue_status.len"))

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