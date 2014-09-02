(ns monitor-rabbitmq.queues-test
  (:require [clojure.test :refer :all]
            [monitor-rabbitmq.queues :as monitor]
            [cheshire.core :as cheshire]))

(def epsilon 0.0000000001)

(defn test-double[expect actual]
  (try (org.junit.Assert/assertEquals expect actual epsilon)
       true
       (catch AssertionError e false)))

(defn find-value[key pairs]
  (second (first (filter (fn [pair](= key (first pair))) pairs))))

(deftest test-make-queue-monitoring-values
  (let [stats (cheshire/parse-string (slurp "test/stats.json") true)
        stat (first (filter (fn [stat] (= (:name stat)  "test-queue-one"))
                            stats))
        stat-value (monitor/make-queue-monitoring-values stat "rmq")
        stat-count (count stat-value)
        name (first stat-value)
        values (second stat-value)]
    (testing "make-queue-monitoring-values"
      (is (= stat-count 2))
      (is (= name "rmq.test-queue-one"))
      (is (= (count values) 17))
      (is (= (find-value "ack" values) 12345))
      (is (= (find-value "publish" values) 111111))
      (is (test-double (find-value "ack.rate" values) 0.0))
      (is (test-double (find-value "deliver.rate" values) 23.333333333333332))
      (is (test-double (find-value "deliver_get.rate" values) 23.333333333333332))
      (is (= (find-value "deliver_no_ack.rate" values) nil))
      (is (= (find-value "get.rate" values) nil))
      (is (= (find-value "get_no_ack.rate" values) nil))
      (is (= (find-value "publish.rate" values) 0.0))
      (is (test-double (find-value "redeliver.rate" values) 23.333333333333332))
      (is (= (find-value "length" values) 1910)))))