(defproject theladders/monitor-rabbitmq "2.1.1"
  :description "query RabbitMQ management API for queue statistics and send them to Riemann"
  :url "https://github.com/TheLadders/monitor-rabbitmq"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [cheshire "5.2.0"]
                 [clj-http "0.7.8"]
                 [riemann-clojure-client "0.2.6"]
                 [junit/junit "4.11"]
                 [clj-time "0.6.0"]]
  :aot :all)
