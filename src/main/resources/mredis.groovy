#!/bin/bash /usr/bin/groovy

def hostName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[1];
def metricPrefix = "redis.${hostName}";
def metricSubUrl = "localhost 8087";
def PID = "tcp_port";
def pid = null;
def submitCount = 0;
def lastMetricCount = 0;
def subscribed = new HashSet(Arrays.asList([
  "used_memory", "used_memory_lua", "connected_clients", "blocked_clients", "used_memory_rss", "used_memory_lua", "mem_fragmentation_ratio", 
  "changes_since_last_save", "bgsave_in_progress", "total_connections_received", "total_commands_processed", "instantaneous_ops_per_sec", 
  "rejected_connections", "expired_keys", "evicted_keys", "keyspace_hits", "keyspace_misses", "pubsub_channels", "pubsub_patterns", 
  "latest_fork_usec", "used_cpu_sys", "used_cpu_user", "used_cpu_sys_children", "used_cpu_user_children"
] as String[]));

def sock = new Socket("localhost", 8087);
//def gzip = new java.util.zip.GZIPOutputStream(sock.getOutputStream());
println "\n";
while(true) {
    
    def buffer = new StringBuilder();
    def currentSection = "";
    def metrics = [:];
    "redis-cli INFO".execute().getIn().eachLine() {
      
      line = it.trim();
      if(!line.isEmpty()) {
        if(line.startsWith("#")) {
          currentSection = line.substring(1).trim().toLowerCase();
        } else {
          if(line.contains(":")) {
            frags = line.split(":");
            if(PID.equals(frags[0])) {
              if(pid==null) {
                  metricPrefix = "${metricPrefix}.${frags[1]}";
                  pid = frags[1];
              }
            } else {
              if(subscribed.contains(frags[0])) {
                metrics.put("${currentSection}.${frags[0]}", frags[1]);
              }
            }
          }
        }
      }
    }    
    metrics.each() { k, v ->
        buffer.append("${metricPrefix}.${k}:${v},");
        //println "${metricPrefix}.${k}:${v}";
    }
    lastMetricCount = metrics.size();
    metrics.clear();
    buffer.deleteCharAt(buffer.length()-1);
    buffer.append(";");
    //println buffer;
    byte[] bytes = buffer.toString().getBytes();
    buffer.setLength(0);
    try {       
      //gzip = new java.util.zip.GZIPOutputStream(sock.getOutputStream(), bytes.length+1);
      //gzip << bytes;
      //gzip.finish();
      sock.getOutputStream().write(bytes); 
      sock.getOutputStream().flush();
      //"netcat localhost 8087".execute() << buffer.toString().getBytes();
      submitCount++;
      print "\rLast Metric Count: ${lastMetricCount}   Submission Count:${submitCount}\t";
    } catch (e) {}    
    Thread.sleep(5000);
    //System.exit(-1);
}


