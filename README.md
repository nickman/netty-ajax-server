netty-ajax-server
=================

A netty server that demonstrates various ways of using netty to implement Ajax push to the browser.

Related articles

 * [Netty Tutorial Part 1: Introduction to Netty][1]
 * [Netty Tutorial Part 1.5: On Channel Handlers and Channel Options][2]

The following push protocols are implemented:

 * Long Polling
 * HTTP Streaming
 * WebSockets

Screenshot
==========

![Netty Ajax Push Server][3]

To run the server, clone this project locally and then run:

 1. mvn clean install
 2. mvn exec:java
 3. Point your browser to http://localhost:8087

[1]: http://seeallhearall.blogspot.com/2012/05/netty-tutorial-part-1-introduction-to.html
[2]: http://seeallhearall.blogspot.com/2012/06/netty-tutorial-part-15-on-channel.html
[3]: https://github.com/nickman/netty-ajax-server/raw/master/images/netty-ajax.png?raw=true "Helios"
