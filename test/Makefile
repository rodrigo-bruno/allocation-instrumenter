jdk = /home/rbruno/mercurial/jdk8/build/linux-x86_64-normal-server-release/jdk

ap = /home/rbruno/git/olr-ap/target/java-allocation-instrumenter-3.0-SNAPSHOT.jar

# JIT force!
opts = -XX:+UseG1GC -Xloggc:jvm.log -XX:+PrintGCDetails -XX:G1HeapRegionSize=32m -Xms4g -Xmx4g -XX:NewSize=256m -XX:MaxNewSize=256m -XX:CompileThreshold=10 -XX:MaxInlineSize=0

# Disable JIT
#opts = -XX:+UseG1GC -Xloggc:jvm.log -XX:+PrintGCDetails -XX:G1HeapRegionSize=32m -Xms4g -Xmx4g -XX:NewSize=256m -XX:MaxNewSize=256m -Djava.compiler=NONE 

profiler = -javaagent:$(ap)=/tmp/oga.output

all:
	$(jdk)/bin/javac *.java

run:
	$(jdk)/bin/java $(opts) -cp .  Test2

run-agent:
	$(jdk)/bin/java $(opts) $(profiler) -cp . Test2

clean:
	rm *.class *.log
