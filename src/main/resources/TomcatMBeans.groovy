#!/bin/bash  /usr/bin/groovy
import org.helios.vm.*;
import org.helios.gmx.*;
String match = "org.apache.catalina.startup.Bootstrap start"
def id = null;
def display = null;
VirtualMachine.list().each() {
    display = it.displayName();
    if(display.equals(match)) {
        id = it.id();
    }
}
println "Tomcat PID:${id}";
def gmx = null;
try {
    gmx = Gmx.attachInstance(id);
    println "Connected to ${gmx.getDefaultDomain()}";
    
    gmx.exec({ 
        Thread.sleep(20000);
        return Gmx.newInstance(server).mbeans("Catalina:j2eeType=WebModule,name=*,J2EEApplication=none,J2EEServer=none");
        
    });
} finally {
    try { gmx.close(); } catch (e) {}
}