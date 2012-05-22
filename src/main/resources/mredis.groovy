#!/bin/bash /usr/bin/groovy

def hostName = System.getenv("HOSTNAME");
def metricPrefix = "redis.${hostName}";
def metricSubUrl = "localhost 8087";
def PID = "tcp_port";
def subscribed = new HashSet(Arrays.asList([
  "used_memory", "used_memory_lua", "connected_clients", "blocked_clients", "used_memory_rss", "used_memory_lua", "mem_fragmentation_ratio", 
  "changes_since_last_save", "bgsave_in_progress", "total_connections_received", "total_commands_processed", "instantaneous_ops_per_sec", 
  "rejected_connections", "expired_keys", "evicted_keys", "keyspace_hits", "keyspace_misses", "pubsub_channels", "pubsub_patterns", 
  "latest_fork_usec", "used_cpu_sys", "used_cpu_user", "used_cpu_sys_children", "used_cpu_user_children"
] as String[]));
def currentSection = "";
def metrics = [:];
def buffer = new StringBuilder();

"redis-cli INFO".execute().getIn().eachLine() {
  line = it.trim();
  if(!line.isEmpty()) {
    if(line.startsWith("#")) {
      currentSection = line.substring(1).trim().toLowerCase();
    } else {
      if(line.contains(":")) {
	frags = line.split(":");
	if(PID.equals(frags[0])) {
	  metricPrefix = "${metricPrefix}.${frags[1]}";
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
}
buffer.deleteCharAt(buffer.length()-1);
println buffer;
"netcat ${metricSubUrl}".toString().execute().withOutputStream() {
  it << buffer;
}