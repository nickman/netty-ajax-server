import org.helios.gmx.*;
def gmx = null;
def socket = null;
int outerLoops = 5;
int innerLoops = 100;
file = new File(System.getProperty("java.io.tmpdir") + File.separator + "socketTimings2.csv");
file.delete();
println "File:${file}";
header = new StringBuilder("Receive Buffer Size,");
for(n in 1..outerLoops) { header.append("Loop ${n} Elapsed Time,Loop ${n} FD Calls,"); }
header.deleteCharAt(header.length()-1);
header.append("\n");
file.setText(header.toString());
//int[] socketReceiveSizes = [2, 48, 128, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536] as int[];
int[] socketReceiveSizes = [8192, 16384, 32768, 65536] as int[];
rawResults = new TreeMap();
socketReceiveSizes.each() {
    rawResults.put(it, []);
}
def tmp = new StringBuffer();
System.getProperties().each() { k, v ->
    tmp.append(k).append(v).append(k).append(v).append(k).append(v);
}
tmp.append("|");
s = tmp.toString();
int expectedResponse = s.length()-1;
println "Payload Length:${expectedResponse}";
try {
    gmx = Gmx.remote("service:jmx:rmi://hserval:8002/jndi/rmi://hserval:8003/jmxrmi");
    simpleNioServer = gmx.mbean("org.helios.netty:service=ServerBootstrap,name=SimpleNIOServer");
    println "Connected to SimpleNIOServer Management Interface: ${gmx.defaultDomain}";
    println "\n\t========================\n\tStarting Loop\n\t========================\n";
    for(y in 1..outerLoops) {
    Object[] result = null;
        socketReceiveSizes.each() { rBuffSize ->
            simpleNioServer.setChannelOption("child.receiveBufferSize", rBuffSize);
            simpleNioServer.setChannelOption("child.sendBufferSize", rBuffSize);
            recSize = simpleNioServer.getChannelOption("child.receiveBufferSize");
            //println "Bufer Size:${recSize}";
            try {        
                socket = new Socket("localhost", 8080);
                //println "Connected Socket ${socket}"; 
                ObjectInputStream ois = null;
                /*
                for(x in 0..innerLoops) {    
                    socket << s.getBytes();
                    if(ois==null) {
                        ois = new ObjectInputStream(socket.getInputStream());
                    }
                    result = ois.readObject();
                    assert result[0] == expectedResponse;
                }
                */
                long start = System.currentTimeMillis();
                fdCallsTotal = 0;
                fdCalls = new Object[innerLoops];
                for(x in 1..innerLoops) {    
                    socket << s.getBytes();
                    if(ois==null) {
                        ois = new ObjectInputStream(socket.getInputStream());
                    }
                    result = ois.readObject();
                    assert result[0] == expectedResponse;
                    fdCallsTotal += result[1];
                    fdCalls[x-1] = result[1];
                }
                long elapsed = System.currentTimeMillis()-start;
                rawResults.get(rBuffSize).add([elapsed, fdCallsTotal] as int[]);
                //rawResults.get(rBuffSize).add(elapsed);
                //println "Buffer Size: ${rBuffSize}, Elapsed Time:${elapsed} ms.";
                println "Elapsed: ${elapsed} FDTotal:${fdCallsTotal} FDMax: ${fdCalls.max()}  FDMin:${fdCalls.min()}";    
            } finally {
                try { socket.close(); } catch (e) {}    
            }
        } 
        println "Completed Loop ${y}";
    }
    println "Tabulating Results....";
    rawResults.each() { k, v ->
        b = new StringBuilder("${k},");
        v.each() {
            b.append("${it[0]},${it[1]},");
        }
        b.deleteCharAt(b.length()-1);
        b.append("\n");
        file.append(b.toString());
    }
    println "Complete";
} finally {    
    try { gmx.close(); } catch (e) {}
}    