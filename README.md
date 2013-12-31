monitor-rabbitmq
================

Query the RabbitMQ management API for queue statistics and send to Riemann


usage
=====

(ns monitor-rabbitmq.example
  (:require [monitor-rabbitmq.core :as monitor]))

(monitor/send-rabbitmq-stats-to-Riemann rmq
                                          r-user
                                          r-pass
                                          age-of-oldest-sample-in-seconds
                                          seconds-between-samples
                                          rmq-display-name
                                          Riemann-host)

See src/test/monitor_rabbitmq/example.clj

