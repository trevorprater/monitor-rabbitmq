(ns monitor-rabbitmq.nodes)

(def path "/api/nodes/")

; These names determine the part of the query string related to message rates.
; These names also drive the names of the metrics that are sent to Riemann
(def node-statistic-names
  (list
    "fd_used"
    "fd_total"
    "sockets_used"
    "sockets_total"
    "mem_used"
    "mem_limit"
    "mem_alarm"
    "disk_free_limit"
    "disk_free"
    "disk_free_alarm"
    "proc_used"
    "proc_total"))

(defn query-columns-for-node-data []
  (clojure.string/join "," (cons "name" node-statistic-names)))

(defn make-node-monitoring-values [node-data & _]
  (list 
    (last (clojure.string/split (:name node-data) #"@"))
    (map (fn [stat-name]
           (list stat-name ((keyword stat-name) node-data)))
      node-statistic-names)))