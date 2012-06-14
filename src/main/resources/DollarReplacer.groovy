import java.util.regex.*;

s = 'This is a ${type} message. \nPlease press ${button} and wait.';
subs = ["type" : "urgent", "butto" : "Help!"];
p = Pattern.compile('\\$\\{(.*?)\\}');
m = p.matcher(s);
StringBuffer b = new StringBuffer();
while(m.find()) {
    println "Key:${m.group(1)}";
    m.appendReplacement(b, subs.get(m.group(1)));
}
m.appendTail(b);
println b;