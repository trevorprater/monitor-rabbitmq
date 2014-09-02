(ns monitor-rabbitmq.queues)

(def path "/api/queues/")

(def rate-query-suffix "_details")
(def avg-rate-suffix ".avg_rate,")
(def message-stat-prefix "message_stats.")
(def rate-metric-suffix ".rate")

(def statistic-names
  (list
    "ack"
    "deliver"
    "deliver_get"
    "deliver_no_ack"
    "get"
    "get_no_ack"
    "publish"
    "redeliver"))

(defn make-message-stats-fragment [statistic-names]
  (let [query-parameters
        (map (fn[name](str message-stat-prefix name ","))
             statistic-names)]
    (apply str query-parameters)))

(defn make-message-stats-rate-fragment [statistic-names]
  (let [query-parameters
        (map (fn[name](str message-stat-prefix name rate-query-suffix avg-rate-suffix))
             statistic-names)]
    (apply str "name," query-parameters)))

(defn query-columns-for-queue-data []
  (str
    (make-message-stats-fragment statistic-names)
    (make-message-stats-rate-fragment statistic-names)
    "backing_queue_status.len"))

(defn make-rate-pairs-fragment[rate-values]
  (let [metric-names
        (map
          (fn [rate-name] (str rate-name rate-metric-suffix))
          statistic-names)]
    (map list metric-names rate-values)))

(defn get-count-values [queue-data]
  (map (fn[rate-name]
         (let [{{val (keyword rate-name)}:message_stats} queue-data]
           val))
    statistic-names))

(defn get-rate-values [queue-data]
  (map (fn[rate-name]
         (let  [{{{val :avg_rate} (keyword (str rate-name "_details"))} :message_stats} queue-data]
           val))
      statistic-names))

(defn make-queue-monitoring-values [queue-data display-name-of-rabbit-host]
  (let [{name :name} queue-data
        {{length :len} :backing_queue_status}  queue-data
        count-values (get-count-values queue-data)
        rate-values (get-rate-values queue-data)]
    (list
      (clojure.string/join "." [display-name-of-rabbit-host name])
      (concat (make-rate-pairs-fragment rate-values)
        (map list statistic-names count-values)
        (list (list "length" length))))))