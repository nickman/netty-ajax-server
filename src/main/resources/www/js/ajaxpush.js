	var ws = null; 
	var webSocketCapable=(WebSocket!=null);
	var is_chrome = /chrome/.test( navigator.userAgent.toLowerCase() );
	var running = false;
	var xhr = null;
	$(function(){
		if(!webSocketCapable) {
			$('#ws').remove();					
		}
		if(is_chrome) {
			$('#streamer').remove();
			$('#streamer_label').remove();
		}
		$('#controlButton').bind('click', function() {
			if(running) {
				stop();
				$('#controlButton').text("Start");
				running = false;
			}  else  {
				start();						
				$('#controlButton').text("Stop");
				running = true;						
			}
		});								
		// jQuery Init Stuff Here.
	});			
	function start() {
		var pushtype = $("input:radio[name='pushtype'][checked='checked']")[0].id;
		if(pushtype=="lpoll") {
			startLongPoll();
		} else if(pushtype=="streamer") {
			startStream();
		} else if(pushtype=="ws") {
			startWebSocket();
		}
	}
	function startStream() {
		xhr = $.ajaxSettings.xhr(); 
		xhr.multipart = true;
		xhr.open('GET', '/streamer', true);
		var on = onEvent;				
		xhr.beforeSend = function(r) {						
			r.setRequestHeader("Keep-Alive", "true")	
			r.setRequestHeader("Timeout", "3000")
		};
		xhr.onreadystatechange = function() {
		    if (xhr.readyState == 4) {         
		    	try {
		        	var json = $.parseJSON(xhr.responseText);
		        	on(json);
		    	} catch (e) {
		    		on({'error':e});	
		    	}					    	
		    } 
		}; 
		xhr.send(null);									
	}
	function startLongPoll() {
		xhr = $.ajaxSettings.xhr(); 
		xhr.open('GET', '/lpoll', true);
		var on = onEvent;		
		xhr.setRequestHeader("Connection", "close");
		xhr.setRequestHeader("Timeout", "4000");
		//xhr.beforeSend = function(r) {						
		xhr.onreadystatechange = function() {
			console.info("Ready State:%s", xhr.readyState);
			if (xhr.readyState == 1) {
				xhr.setRequestHeader("Connection", "close");
			}
		    if (xhr.readyState == 3) {         
		    	try {
		        	var json = $.parseJSON(xhr.responseText);
		        	on(json);
		    	} catch (e) {
		    		on({'error':e});	
		    	} finally {
		    		startLongPoll();
		    	}					    	
		    } 
		};
		xhr.send(null);				
	}
	function startWebSocket() {
		var wsUrl = document.documentURI.replace('http', 'ws') + 'ws';
		console.info('WebSocket URL:[%s]', wsUrl);
		ws = new WebSocket(wsUrl); 
		var on = onEvent;		
		ws.onopen = function() { 
		    console.info("WebSocket Opened");
		}; 
		ws.onerror = function(e) { 
			console.info("WebSocket Error");
			console.dir(e);
		}; 
		ws.onclose = function() { 
			console.info("WebSocket Closed"); 
		}; 
		ws.onmessage = function(msg) {
        	var json = $.parseJSON(msg.data);
        	on(json);									
		}; 
		// Here is how to send some data to the server 
		//ws.send('some data'); 				
		
	}
	function stop() {
		if(xhr!=null) {
			try { xhr.abort(); } catch (e) {}
			xhr = null;
		} else if(ws!=null) {
			try { ws.close(); } catch (e) {}
			ws = null;					
		}
	}
	function onEvent(data) {
		if(data!=null) {
			var value = data.timeout==null ? data.used : "timeout";
			//var row = '<table border="1"><tr><td>Time:'  + new Date() + '</td>'
			var row = '<li>' + value + "</li>";
			$('#memdisplay').append(row);
			
			//committed,	init , max , used
			if($('#memdisplay').children().size()>20) {
				$('#memdisplay').children().first().remove();
			}
		}
	}
	function formatJson(json) {
		var row = '<table border="1"><tr><td>Time:'  + new Date() + '</td>';
		
		row += '</tr></table>';
		return row;
	}
