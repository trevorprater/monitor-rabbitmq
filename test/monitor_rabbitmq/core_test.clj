(ns monitor-rabbitmq.core-test
  (:require [clojure.test :refer :all]
            [monitor-rabbitmq.core :as monitor]
            [cheshire.core :as cheshire]))


;(deftest a-test
;  (testing "FIXME, I fail."
;    (is (= 0 1))))


;(deftest good-test
;  (testing "2=2"
;    (is (= 2 2))))

(deftest test-make-queue-monitoring-values
  (let [stats (cheshire/parse-string (slurp "test/stats.json") true)
        stat (first (filter (fn [stat] (= (:name stat)  "match-jobseekers-with-jobs"))
                            stats))
        stat-value (monitor/make-queue-monitoring-values stat)
        stat-count (count stat-value)
        name (first stat-value)
        values (second stat-value)]
    (testing "make-queue-monitoring-values"
      (is (= stat-count 2))
      (is (= name "match-jobseekers-with-jobs"))
      (is (= (count values) 9))
      (is (= (find-value "ack.rate" values) 0.0))
      (is (= (find-value "deliver.rate" values) 23.333333333333332))
      (is (= (find-value "deliver_get.rate" values) 23.333333333333332))
      (is (= (find-value "deliver_no_ack.rate" values) nil))
      (is (= (find-value "get.rate" values) nil))
      (is (= (find-value "get_no_ack.rate" values) nil))
      (is (= (find-value "publish.rate" values) 0.0))
      (is (= (find-value "redeliver.rate" values) 23.333333333333332))
      (is (= (find-value "length" values) 1910)))))

(defn find-value[key pairs]
  (second (first (filter (fn [pair](= key (first pair))) pairs))))

(def args-map
  {:rmq "rabbitmq-qa-2.laddersoffice.net:15672"
   :r-user "monitoring"
   :r-pass  "ladders"
   :rmq-display-name  "rabbitmq"
   :Riemann-host "riemann-qa-1.laddersoffice.net"}
  )

(defn get-stats-from-qa[]
  (monitor/get-stats (monitor/rmq-url
                       (:rmq args-map) (:r-user args-map) (:r-pass args-map))
                     300
                     15))