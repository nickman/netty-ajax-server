import redis.clients.jedis.*;
import org.helios.nativex.sigar.HeliosSigar;
import java.lang.management.*;

def jedis = null;
def r = new Random(System.nanoTime());
def sigar = HeliosSigar.getInstance();
def host = sigar.getFQDN();
def agent = ManagementFactory.getRuntimeMXBean().getName();
def script = "";

try {
    jedis = new Jedis("ub");
    if(!"PONG".equals(jedis.ping())) {
        throw new Exception("Ping failed");
    }
    println "Connecting from [${sigar.getFQDN()}]";
    //jedis.flushDB();
    long start = System.currentTimeMillis();
    for( i in 0..5000) {
        atom = trace(jedis, "${host}/${agent}/CPU/SysPercent", sigar.getCpuPerc().getSys());
        println "Atom:${atom}";
    }
    long elapsed = System.currentTimeMillis()-start;
    println "\nCompleted in ${elapsed} ms.";
} finally {
    try { jedis.quit(); } catch (e) {}   
}  


long trace(jedis, metricName, value) {
    def liveAtomSize = 40;
    long ts = System.currentTimeMillis();
    def dt = new Date(ts);
    long atom = jedis.incr("metricatoms.live.${metricName}");
   
    if(atom>liveAtomSize) {
        jedis.set("metricatoms.live.${metricName}", "0");
        atom = jedis.incr("metricatoms.live.${metricName}");
        println "Reset Atom for [metricatoms.live.${metricName}]";
    }
    Pipeline p = jedis.pipelined();
    p.zadd("metric.live.${metricName}", ts, "${atom}");
    p.hset("metricdata.live.${metricName}.${atom}", "avg", "${value}");
    p.hset("metricdata.live.${metricName}.${atom}", "date", "${dt}");
    p.expire("metricdata.live.${metricName}.${atom}", 15);
    p.sync();
    return atom;
}

return null;