monitor-rabbitmq provides utility functions that request statistics from the RabbitMQ Management API and convert them to Riemann events

### What does the functions do? ###

The functions makes requests to the RabbitMQ Management API for the following information about:

Endpoint | Stats | Endpoint | Stats 
-------- | ----- | -------- | -----
queues | length | nodes | fd_used
 | ack rate | | fd_total
 | deliver rate | | sockets_used
 | deliver get rate | | sockets_total
 | deliver no ack rate | | mem_used
 | get rate | | mem_limit
 | get no ack rate | | mem_alarm
 | publish rate | | disk_free_limit
 | redeliver rate | | disk_free_alarm
 | | | proc_used
 | | | proc_total

Each statistic is converted to a Riemann event and sent to a Riemann server.

### What does the Riemann event look like? ###
```clj
{:time 1390593087006,
    :host "rabbitmq.super.awesome.queue", ; this is the rmq-display-name composed with the queue or node name
    :service "publish.rate",
    :metric 0.0,
    :state "ok",
    :tags ["rabbitmq"]}
```

### Using monitor-rabbitmq ###

In the project.clj file at the top level of your project, add monitor-rabbitmq as a dependency:

```clj
(defproject my-rabbitmq-app "0.1.3"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [theladders/monitor-rabbitmq "0.2.0" ]])
```

## Code examples ##


### using the function ###

Calling the send-rabbitmq-stats-to-riemann function will always send stats from all endpoints.

There are also send-**[endpoint]**-rabbitmq-stats-to-riemann functions to only send stats from a specific endpoint, if so desired.

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

; Note: port is optional below
(defn do-it-with-default-Riemann-port []
  (monitor/send-rabbitmq-stats-to-riemann rmq
                                          r-user
                                          r-pass
                                          age-of-oldest-sample-in-seconds
                                          seconds-between-samples
                                          rmq-display-name
                                          Riemann-host
                                          Riemann-port))
```

### upcoming changes ###

* Function and variable names will change (no upper case) to better match Clojure conventions.
* A new version will change how parameters are passed to send-rabbitmq-stats-to-Riemann.
* More documentation about the meaning of the RabbitMQ statistics and how RabbitMQ gathers them.


