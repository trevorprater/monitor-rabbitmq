(defproject monitor-rabbitmq "0.1.0-SNAPSHOT"
  :description "query RabbitMQ management API for queue statistics and send them to Riemann"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [cheshire "5.2.0"]
                 [clj-http "0.7.8"]
                 [riemann-clojure-client "0.2.6"]
                 [clj-time "0.6.0"] ]
  :repositories [  [ "snapshots" "file:///Users/jpr/repo/snapshots" ]
                   [ "releases" "file:///Users/jpr/repo/releases" ] ]
  )
