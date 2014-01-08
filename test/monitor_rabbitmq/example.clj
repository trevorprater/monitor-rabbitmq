(ns monitor-rabbitmq.example
  (:require [monitor-rabbitmq.core :as monitor]))

(def rmq "my-rabbitmq.net:4321")
(def r-user "monitoring")
(def r-pass  "easy-pass")
(def rmq-display-name  "rabbit-mq-qa")
(def Riemann-host "my-riemann.net")
(def Riemann-port 9000)
(def age-of-oldest-sample-in-seconds 300)
(def seconds-between-samples 15)

(defn do-it-with-default-Riemann-port []
  (monitor/send-rabbitmq-stats-to-Riemann rmq
                                          r-user
                                          r-pass
                                          age-of-oldest-sample-in-seconds
                                          seconds-between-samples
                                          rmq-display-name
                                          Riemann-host))
(defn do-it-with-Riemann-port []
  (monitor/send-rabbitmq-stats-to-Riemann rmq
                                          r-user
                                          r-pass
                                          age-of-oldest-sample-in-seconds
                                          seconds-between-samples
                                          rmq-display-name
                                          Riemann-host
                                          Riemann-port))
