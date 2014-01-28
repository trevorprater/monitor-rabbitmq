monitor-rabbitmq provides a utility function that requests queue statistics from the RabbitMQ Management API and converts them to Riemann events

### What does the function do? ###

The function makes a request to the RabbitMQ Management API for the following information about all queues:
* length
* ack rate
* deliver rate
* deliver get rate
* deliver no ack rate
* get rate
* get no ack rate
* publish rate
* redeliver rate

Each statistic is converted to a Riemann event and sent to a Riemann server.

### What does the Riemann event look like? ###
```clj
{:time 1390593087006,
    :host "rabbitmq.push.notification.sender", ; this is the rmq-display-name composed with the queue name
    :service "publish.rate",
    :metric 0.0,
    :state "ok",
    :tags ["rabbitmq"]}
```

### Using monitor-rabbitmq ###

In the project.clj file at the top level of your project, add monitor-rabbitmq as a dependency:

```clj
(defproject app-monitor-rabbitmq "0.1.3"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [theladders/monitor-rabbitmq "0.1.4" ]])
```

## Code examples ##


### using the function ###


```clj
(ns monitor-rabbitmq.example
  (:require [monitor-rabbitmq.core :as monitor]))

(def rmq "my-rabbitmq.net:4321")  ; host and port of RabbitMQ Management API
(def r-user "monitoring")  ; user name for Management API
(def r-pass  "easy-pass")  ; password for above user name
(def rmq-display-name  "rabbitmq")  ; composed with the queue name to make the host value of the Riemann event
(def Riemann-host "my-riemann.net")  ; host name of Riemann server
(def Riemann-port 9000)  ; port used by Riemann server
(def age-of-oldest-sample-in-seconds 300)  ; the first data point used to calculate average rate
(def seconds-between-samples 15)  ; the sampling rate

; this function uses the version of send-rabbitmq-stats-to-Riemann that does not require a Riemann port number
(defn do-it-with-default-Riemann-port []
  (monitor/send-rabbitmq-stats-to-Riemann rmq
                                          r-user
                                          r-pass
                                          age-of-oldest-sample-in-seconds
                                          seconds-between-samples
                                          rmq-display-name
                                          Riemann-host))

; here, we use the version of the function which allows us to specify the Riemann port                                         
(defn do-it-with-Riemann-port []
  (monitor/send-rabbitmq-stats-to-Riemann rmq
                                          r-user
                                          r-pass
                                          age-of-oldest-sample-in-seconds
                                          seconds-between-samples
                                          rmq-display-name
                                          Riemann-host
                                          Riemann-port))
```

### upcoming changes ###

* A new version will change how parameters are passed to send-rabbitmq-stats-to-Riemann.
* More documentation about the meaning of the RabbitMQ statistics and how RabbitMQ gathers them.


