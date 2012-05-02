	var ws = null; 
	var webSocketCapable=(WebSocket!=null);
	var is_chrome = /chrome/.test( navigator.userAgent.toLowerCase() );
	var running = false;
	var timeoutHandle = -1;
	var xhr = null;
	var pushtype = "";
	var outputChart = true; 
	var chartData = {};
	var chartPlots = {};
	var lastData = null;
	var dataListeners = {};
	/**
	 * Initializes the client
	 */
	$(function(){
		$(".err-msg").hide();
		$(".err-msg").css('width', '40%');	
		$(".err-msg").bind('click', function() {
			$(".err-msg").hide();
		});
		
		$('div.busyindicator').css({'display':'none'});
		$('#display').resizable().draggable();
		$( "#accordion" ).accordion({collapsible: true, active:false}).css('width', '40%');
		$( "#accordion" ).accordion({
			   change: function(event, ui) {
				   $('input[type="radio"][name="pushtype"]').attr('checked', null);
				   ui.newHeader.next('div').children('input[type="radio"]').attr('checked', 'checked');
				   var newPs = $('input[type="radio"][name="pushtype"][checked="checked"]')[0].id;
				   $.cookie('ajax.push.pushtype', newPs, { expires: 365 });
				   $('h3.pushtypeh').removeClass('pushtypesel')
				   $('#' + newPs).parent('div').last().prev().addClass('pushtypesel')
			   }
		});
		$('button').button();
		if(!webSocketCapable) {
			$('#ws').remove();	
			$('.websock').remove();
		}
		if(is_chrome) {
			$('#streamer').remove();
			$('#streamer_label').remove();
			$('.stream').remove();
		}
		$('input[type="radio"][name="pushtype"]').bind('select', function(event) {
			console.info("Radio Selected");
			console.dir(event);
		});
		$('#controlButton').bind('click', function() {
			if(running) {
				stop();
				$('#controlButton').button({label: "Start"});
				running = false;
			}  else  {
				if(start()) {						
					$('#controlButton').button({label: "Stop"});
					running = true;
				}
			}
		});								
		$('#clearButton').bind('click', function() {
			$('#display').children().remove();
			$('.counter').attr('value', '0');
		});
		$("#outputFormat").bind('click', function() {
			if(outputChart) {
				outputChart = false;				
				$("#outputFormat").button({ label: "Output:Raw" })
				$("#displayChart").hide();
				$("#displayRaw").show();
			} else {
				outputChart = true;
				$("#outputFormat").button({ label: "Output:Charts" })
				$("#displayChart").show();
				$("#displayRaw").hide();				
			}
			$.cookie('ajax.push.format', outputChart, { expires: 365 });
		});
		$("#displayChart").show();
		
		var savedPushType = $.cookie('ajax.push.pushtype');
		if(savedPushType!=null) {
			console.info("Restored Last Push Type:" + savedPushType);
			$('#' + savedPushType).attr("checked", "checked");
			var ps = $('input[type="radio"][name="pushtype"][checked="checked"]');
			if(ps.size()==1) {
				$('h3.pushtypeh').removeClass('pushtypesel')
				$('input[type="radio"][name="pushtype"][checked="checked"]').parent('div').last().prev().addClass('pushtypesel')
			}
		}
		var savedFormat = $.cookie('ajax.push.format');
		if(savedFormat!=null) {
			if(savedFormat) {
				console.info("Restored Format. Chart:" + savedFormat);
				outputChart = savedFormat;
				if(!outputChart) {								
					$("#outputFormat").button({ label: "Output:Raw" })
					$("#displayChart").hide();
					$("#displayRaw").show();
				} else {					
					$("#outputFormat").button({ label: "Output:Charts" })
					$("#displayChart").show();
					$("#displayRaw").hide();				
				}				
			}
		}
//		$("#gridMaskInput").val($.cookie('metric_browser.gridMaskInput') || "");	
//		$.cookie('metric_browser.gridMaskInput', expr, { expires: 365 });
		addCharts();
	});
	/**
	 * Turns the busy indicator on
	 */
	function busyOn() {
		$('div.busyindicator').css({'display':'block'});
	}
	/**
	 * Turns the busy indicator off
	 */
	function busyOff() {
		$('div.busyindicator').css({'display':'none'});
	}
	/**
	 * Displays an error message in an error dialog
	 * @param message The error message
	 */
	function errorMessage(message) {
		$('#err-text').text(message);		
		$(".err-msg").css('position', 'relative').css('zIndex', 9999);
		$(".err-msg").show();
		//$(".err-msg").dialog("option", "width", 500));
	}
	/**
	 * Starts the push
	 */
	function start() {
		chartData = [{label: "Boss Active Threads", data: []}, {label: "Worker Active Threads", data: []}];
		var pType = $("input:radio[name='pushtype'][checked='checked']");
		if(pType==null || pType.size()<1) {			
			errorMessage("No push type was selected. Pick a push type.")
			return false;
		} 
		pushtype = pType[0].id;
		if(pushtype==null) {
			console.error("No push type");
			return false;
		}
		var name = null;
		if(pushtype=="lpoll") {
			startLongPoll();
			name = "Long Polling";
		} else if(pushtype=="streamer") {
			startStream();
			name = "Http Streaming";
		} else if(pushtype=="ws") {
			startWebSocket();
			name = "WebSockets";
		}
		$('#statemsg').html("Started Push Using " + name);
		return true;
	}
	/**
	 * Starts the streaming push
	 */
	function startStream() {
		busyOn();
		xhr = $.ajaxSettings.xhr(); 
		xhr.multipart = true;
		xhr.open('GET', '/streamer', true);
		var on = onEvent;			
		xhr.onreadystatechange = function() {
			if (xhr.readyState == 1) {
				busyOn();
			}
			if (xhr.readyState == 4) {         
		    	try {
		    		busyOff();
		        	var json = $.parseJSON(xhr.responseText);
		        	on(json);		        	
		    	} catch (e) {
		    		on({'error':e});	
		    	}					    	
		    } 
		}; 
		xhr.send(null);									
	}
	/**
	 * Starts the long poll push
	 */
	function startLongPoll() {
		var on = onEvent;
		var timeout = null;
		timeout = $('#lpolltimeout').attr('value');
		if(isNumber(timeout)) {
			timeout = '/?timeout=' + timeout;
		} else {
			timeout = '';
		}
		busyOn();
		xhr = $.getJSON("/lpoll" + timeout, function(events) {
			  on(events);
			})
			.error(function(req,msg) {
				if(msg!='abort') {
					console.error('Error on longpoll:' + msg);
				}
			})
			.complete(function() {
				busyOff();
				if(!running) return; 
				timeoutHandle = setTimeout(function() { 
					if(running) startLongPoll(); 
				}, 500); 
			});
	}
	/**
	 * Starts the web socket push
	 */
	function startWebSocket() {
		var wsUrl = 'ws://' + document.location.host + '/ws';
		console.info('WebSocket URL:[%s]', wsUrl);
		ws = new WebSocket(wsUrl); 
		var on = onEvent;		
		ws.onopen = function() {
			busyOn();
		    console.info("WebSocket Opened");
		}; 
		ws.onerror = function(e) {
			busyOff();
			console.info("WebSocket Error");
			console.dir(e);
		}; 
		ws.onclose = function() { 
			busyOff();
			console.info("WebSocket Closed"); 
		}; 
		ws.onmessage = function(msg) {			
			var json = $.parseJSON(msg.data);
			on(json);
		}; 
	}
	/**
	 * Stops the push
	 */
	function stop() {
		if(xhr!=null) {
			try { xhr.abort(); } catch (e) {}
			xhr = null;
		} else if(ws!=null) {
			try { ws.close(); } catch (e) {}
			ws = null;					
		}
		if(timeoutHandle!=null) {
			clearTimeout(timeoutHandle);
		}
		$('#statemsg').html("");
		pushtype = "";
	}
	/**
	 * Called when data is delivered through push
	 * @param data A JSON object to be rendered
	 */
	function onEvent(data) {
		increment('#' + pushtype + 'count', 'value');
		if(data!=null) {
			lastData = data;
			$('#displayRaw').append(formatJson(data));
			if($('#displayRaw').children().size()>20) {
				$('#displayRaw').children().first().remove();
			}			
			notifyListeners(data);
		}
	}
	/**
	 * Formats the data to be displayed
	 * @param json The json object to render
	 * @returns {String} The rendered string
	 */
	function formatJson(json) {
		var row = '<table border="1" class="rawdata"><tr><td>'  + $.format.date(new Date(), "MM/dd/yy hh:mm:ss") + '</td>';
		$.each(json, function(k,v){
			row += '<td><b>' + k + '</b>:&nbsp;' + addCommas(v) + '</td>';
		});
		row += '</tr></table>';
		return $(row).css('margin-bottom', 0).css('margin-top', 0);
		//return row;
	}
	/**
	 * Formats json fields that are numbers
	 * @param nStr The string to format as a number
	 * @returns A formated number, or the same string passed in if not a number
	 */
	function addCommas(nStr) {
		if(!isNumber(nStr)) return nStr;
		nStr += '';
		x = nStr.split('.');
		x1 = x[0];
		x2 = x.length > 1 ? '.' + x[1] : '';
		var rgx = /(\d+)(\d{3})/;
		while (rgx.test(x1)) {
			x1 = x1.replace(rgx, '$1' + ',' + '$2');
		}
		return x1 + x2;
	}
	/**
	 * Tests the passed value to see if it is a number
	 * @param n The string to test
	 * @returns {Boolean} true if it is a number
	 */
	function isNumber(n) {
		if(n==null) return false;
		return !isNaN(parseFloat(n)) && isFinite(n);
	}
	
	/**
	 * 
	 * @param expr
	 */
	function increment(expr, at) {
		var value = $(expr).attr(at);
		if(isNumber(value)) {
			value = parseInt(value)+1;
		}
		$(expr).attr(at, value);
	}
	
	/**
	 * Adds a new data listener to be notified when the key specified json data is available
	 * @param listener The listener to notify
	 */
	function addDataListener(listener) {
		$.each(listener.dataKeys, function(index, key) {
			var arr = dataListeners[key];
			if(arr==null) {
				arr = [];
				dataListeners[key] = arr;
			}
			arr.push(listener);		
		});		
	}
	
	/**
	 * Notifies registered listeners of incoming data matching the listener's data key
	 * @param json The json data
	 */
	function notifyListeners(json) {
		if(Object.keys(dataListeners).length<1) return;
		var ts = json.ts;
		if(ts==null) return;
		decompose(json, ts);	
		$.each(dataListeners, function(index, arrOfListeners){
			$.each(arrOfListeners, function(i, listener){
				listener.onComplete();
			});
		});
	}
	
	/**
	 * Recurses through the json data and calls listeners when a matching key with registered listeners is found
	 * @param data The json data
	 * @param ts The timestamp
	 * @param context The current data key (null on first call)
	 */
	function decompose(data, ts, context) {
		if(context==null) context = [];
	    $.each(data, function(k, v) {	        
	    	context.push(k);
	    	if(!$.isPlainObject(v) || k.charAt(k.length-1)=='*') {
	    		var dataKey = context.join('.');
	    		var listeners = dataListeners[dataKey];
	    		if(listeners!=null && listeners.length>0) {	    			
	    			$.each(listeners, function(index, listener){
	    				listener.onData(v, ts, dataKey);
	    			});
	    		}	    		
	    	}
	        decompose(v, ts, context);       
	        context.pop();
	    });
	}

	function addCharts() {
		var activeThreadsChart = new LineChart({		
			dataKeys: ['threadPools.worker.activeThreads', 'threadPools.boss.activeThreads'],
			labels: ["Worker", "Boss"],
			title: "Thread Pool Active Threads"
		});		
		addDataListener(activeThreadsChart);
		var completedTasksChart = new LineChart({		
			dataKeys: ['threadPools.worker.completedTasks', 'threadPools.boss.completedTasks'],
			labels: ["Worker", "Boss"],
			title: "Thread Pool Completed Tasks"
		});
		addDataListener(completedTasksChart);
		
		var threadStatesChart = new PieChart({		
			dataKeys: ['thread-states*'],
			title: "Thread States"
		});
		addDataListener(threadStatesChart);
		
	}
	


	var LineChart = Class.create({
		init: function(props){
			var cm = this;
			var display = $('#displayChart');
			$.each(props, function(key, value) {
				if($.isPlainObject(value)) {
					$.extend(cm[key], value);
				} else {
					cm[key] = value;
				}
			});
			cm.jkey = cm.title.replace(/ /g, '');
			cm.dataSpec = [];
			$.each(cm.labels, function(index, value) {
				var dataArr = [];
				cm.dataSpec.push({label: value, data: dataArr});
				cm.dataArrays[cm.dataKeys[index]] = dataArr;
			});
			cm.placeHolder = $('<div id="' + cm.jkey + '" class="chartplaceholder"></div>');
			
			display.append(cm.placeHolder);
			var style = $.cookie(cm.jkey);
			if(style!=null) {
				$('#' + cm.jkey).attr('style', style);
			} else {
				$.each(cm.divCss, function(key, value) {
					cm.placeHolder.css(key, value);				
				});				
			}
			cm.plot = $.plot($('#' + cm.jkey), cm.dataSpec, cm.options);
			cm.decoratePlaceHolder();
        	if(cm.seriesSize==null) {
        		cm.seriesSize=20;
        	}
		},
		dataKeys: [],
		labels:[],
		dataArrays: {},
		title: '',
		jkey: '',
		seriesSize: null,
		dataSpec: [],
		divCss:  {width:250, height:150}, 
		options: {
			legend: { show: true, noColumns: 1, labelFormatter: this.labelFormatter, backgroundOpacity: 0.4 },
			xaxis: {mode: "time", timeformat: "%M:%S"}, 
			series: { 
				lines: { show: true }, 
				points: { show: true }
			},
			grid: {
				borderColor: null,
				borderWidth: 0
			}
		},
		placeHolder: null,
		plot: null,	
		decoratePlaceHolder: function() {
			var cm = this;
			this.placeHolder
    		.draggable({
    			stop: function(event, ui){
    				$.cookie(cm.jkey, cm.placeHolder.attr('style'), { expires: 365 });
    			}
    		})
    		.resizable({ 
    			grid: [50, 50],
    			helper: "ui-resizable-helper",
    			resize: function(event, ui){
	        		cm.plot.resize();
	        		cm.plot.setupGrid();
	        		cm.plot.draw();
	        		$.cookie(cm.jkey, cm.placeHolder.attr('style'), { expires: 365 });	        		
    			}
    		});
			$('#' + cm.jkey).prepend($('<div align="middle" class="chartTitle">' + cm.title + '</div>'))			
		},
	    onData: function(v, ts, dataKey) {
	        this.dataArrays[dataKey].push([ts, v]);
	    },
	    onComplete: function() {
	    	var maxSize = this.seriesSize;
        	$.each(this.dataArrays, function(key, array){
        		if(array.length>maxSize) {
        			array.shift();
        		}
        	});
        	this.plot.setData(this.dataSpec);
        	this.plot.setupGrid();        	        
        	this.plot.getAxes().yaxis.max = Math.round(this.plot.getAxes().yaxis.max * 1.1);
        	this.plot.draw();
	            	
	    },
	    labelFormatter: function (label, series) {
	    	return '<a href="#' + label + '">' + label + '</a>';
	    }
	}); 
	
	var PieChart = Class.create(LineChart.prototype, {
		options: {
			series: {
	            pie: {
	                show: true,
	                radius: 'auto'
	            }
			}
		},
		onData: function(v, ts, dataKey) {
			this.dataSpec = [];
			var d = this.dataSpec;
			$.each(v, function(key, value){
				d.push({ label: key,  data: value});
			});
		},
		onComplete: function() {
			this.plot = $.plot($('#' + this.jkey), this.dataSpec, this.options);	
			this.decoratePlaceHolder();
		}
	}); 
	