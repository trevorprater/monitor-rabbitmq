## version 0.1.5 ##
A Riemann event is created for each queue metric potentially supplied by the RabbitMQ Management API. If a metric is not supplied, the Riemann event will contain a metric with a value of 0.


### metrics that may appear in the Management API response ###
* length
* ack rate
* deliver rate
* deliver get rate
* deliver no ack rate
* get rate
* get no ack rate
* publish rate
* redeliver rate

### What's different? ###
In previous versions, one Riemann event was created for each metric supplied in the Management API response. For example, if the response message contained only _length_, _ack rate_, and _publish rate_, then only three Riemann events would have been created.


## version 0.1.4 ##
Updated README.md


## version 0.1.3 ##
Merely a signed version of 0.1.2


## version 0.1.2 ##
First release to Clojars
