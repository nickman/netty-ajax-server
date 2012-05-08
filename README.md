netty-ajax-server
=================

A netty server that demonstrates various ways of using netty to implement Ajax push to the browser.

The following push protocols are implemented:

 * Long Polling
 * HTTP Streaming
 * WebSockets

Screenshot
==========

![Netty Ajax Push Server][1]

To run the server, clone this project locally and then run:

 1. mvn clean install
 2. mvn exec:java
 3. Point your browser to http://localhost:8087


[1]: https://github.com/nickman/netty-ajax-server/raw/master/images/netty-ajax.png?raw=true "Helios"
