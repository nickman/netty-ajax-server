import redis.clients.jedis.*;
import org.helios.nativex.sigar.HeliosSigar;
import java.lang.management.*;
import java.util.concurrent.*;


def jedis = null;
def r = new Random(System.nanoTime());
def sigar = HeliosSigar.getInstance();
def host = sigar.getFQDN();
def agent = ManagementFactory.getRuntimeMXBean().getName();
def script = new File("/home/nwhitehead/hprojects/redis-ts/src/main/resources/lua/ts-add.lua").getText();
def keys = new TreeSet();
sha = null;
try {
    jedis = new Jedis("localhost");
    if(!"PONG".equals(jedis.ping())) {
        throw new Exception("Ping failed");
    }
    println "Connecting from [${sigar.getFQDN()}]";
    sha = jedis.scriptLoad(script);
    println "SHA:${sha}";
    long base = System.currentTimeMillis();
    boolean cont = true;
    int cnt = 0;
    def current = null;
    def map = new ConcurrentHashMap();
    jedis.flushDB();
    long start = System.currentTimeMillis();
    p = jedis.pipelined();
    for(i in 0..587) {
        p.evalsha(sha, 0, "${base}".toString());
        base = base+1000;
    }
    p.sync();
    long elapsed = System.currentTimeMillis()-start;
    keys = jedis.smembers("timeseries.live.members");
    lkeys =  new TreeSet();
    keys.each() { lkeys.add(Long.parseLong(it)); }
    println lkeys.size();
    println lkeys;
   
    println "Elapsed:${elapsed} ms";
    /*
    println keys;
    println "Key Count:${keys.size()}";
    println "===================================";
    perCall = elapsed/cnt;
    println "Elapsed Time:${TimeUnit.MILLISECONDS.convert(elapsed, TimeUnit.NANOSECONDS)} ms.  Total Calls:${cnt}   Nanos Per Call:${Math.floor(perCall)}";
    */
   
} finally {
    try { jedis.quit(); } catch (e) {}   
}  
return null;