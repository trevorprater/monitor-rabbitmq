(ns monitor-rabbitmq.nodes-test
  (:require [clojure.test :refer :all]
            [monitor-rabbitmq.nodes :as monitor]
            [cheshire.core :as cheshire]))

(defn find-value[key pairs]
  (second (first (filter (fn [pair](= key (first pair))) pairs))))

(deftest test-make-nodes-monitoring-values
  (let [stats (cheshire/parse-string (slurp "test/nodes-stats.json") true)
        stat (first (filter (fn [stat] (= (:name stat)  "rabbit@rabbitmq-1"))
                            stats))
        stat-value (monitor/make-node-monitoring-values stat)
        stat-count (count stat-value)
        name (first stat-value)
        values (second stat-value)]
    (testing "make-node-monitoring-values"
      (is (= stat-count 2))
      (is (= name "rabbitmq-1"))
      (is (= (count values) 12))
      (is (= (find-value "fd_used" values) 230))
      (is (= (find-value "fd_total" values) 64000))
      (is (= (find-value "sockets_used" values) 10))
      (is (= (find-value "sockets_total" values) 12))
      (is (= (find-value "mem_used" values) 12345))
      (is (= (find-value "mem_limit" values) 67890))
      (is (= (find-value "mem_alarm" values) false))
      (is (= (find-value "disk_free_limit" values) 12345))
      (is (= (find-value "disk_free" values) 12345))
      (is (= (find-value "disk_free_alarm" values) true))
      (is (= (find-value "proc_used" values) 1))
      (is (= (find-value "proc_total" values) 2)))))