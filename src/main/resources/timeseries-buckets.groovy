import java.util.concurrent.*;
import java.text.*;

sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
gc = new GregorianCalendar();
gc.setTime(new Date());
gc.set(gc.SECOND, 0);
dt = gc.getTime();
period = 15;
periodUnit = TimeUnit.SECONDS;
liveWindowLength = 10;
liveWindowUnit = TimeUnit.MINUTES;
liveBuckets = TimeUnit.MILLISECONDS.convert(liveWindowLength, liveWindowUnit) / TimeUnit.MILLISECONDS.convert(period, periodUnit);


println liveBuckets;
println dt.getTime();
