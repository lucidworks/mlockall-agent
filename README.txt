
ABOUT MLOCKALL AGENT
--------------------

mlockall-agent.jar is a Java Agent that can be used in conjunction with any 
java application to cause the virtual memory used by the application to be 
"locked" in memory -- preventing it from being swapped to disk.

In an ideal world, any performance critical java application would be run
on dedicated hardware with swap disabled -- but people don't always get
to run their software in an ideal world.  So this agent fills in the gaps.

REQUIREMENTS
------------

Building the code requires Java 5, Apache Ant, and a jna.jar 3.2.7 or newer

  https://ant.apache.org/
  https://github.com/twall/jna

Running the agent requires jna.jar 3.2.7 or newer


USING MLOCKALL AGENT
--------------------

To run a java application with this agent enabled:

 0) ensure that your system supports mlock and that the effective user of 
    your java process has a sufficient ulimit for "max locked memory" 
    (ie: "ulimit -l")
 1) ensure that you configure the min heap size = the max heap size
 3) ensure that the "jna.jar" is either in the same directory as the 
    mlockall-agent.jar, or is at the same path used when mlockall-agent.jar, 
    was assembled. (see below)
 2) specify the path to the mlockall-agent.jar using the "-javaagent" 
    command line switch.

Example...

  $ ls -1 lib
  mlockall-agent.jar
  jna.jar
  $ java -Xms1024m -Xmx1024m -javaagent:lib/mlockall-agent.jar -jar yourapp.jar 

By default, the agent will cause the process to fail immediately if mlockall 
can not be used for any reason.  An "optional" argument can be added to the 
agent to specify that you would like it to try to run, but not fail if there 
is any problem.  This may be useful for developing cross platform start 
scripts where you would like a "best attempt" at locking the memory, but can 
live w/o it if there is a problem...

  $ java -Xms1024m -Xmx1024m -javaagent:lib/mlockall-agent.jar=optional -jar yourapp.jar 

The mlockall-agent.jar can also be run explicitly to provide a simple 
demo/test mode of whether mlockall() works on your system...

  $ less src/MLockAgent.java 
  $ java -jar build/jar/mlockall-agent.jar 
  Demo mode of mlockall...
  Demo mode of mlockall failed
  Exception in thread "main" java.lang.IllegalStateException: Unable to lock JVM memory, error num: 12
  	at MLockAgent.agent(MLockAgent.java:134)
  	at MLockAgent.main(MLockAgent.java:70)
  $ ulimit -l unlimited
  $ java -jar build/jar/mlockall-agent.jar 
  Demo mode of mlockall...
  mlockall finished, sleeping so you can observe process info
  ^C


BUILDING AND ASSEMBLING
-----------------------

Apache Ant is used to compile the code and assemble the agent jar file.  

Use "ant -p" to see the list of build options.

At the present time it is necessary for you to explicitly point the ant build 
system to a copy of the JNA jar (using an ant build property) when compiling 
the code, and when assembling the jar file.  Examples...

  # specify the path on the command line
  $ ant -Djna.jar.path=/usr/share/java/jna-3.2.7.jar compile jar

  # specify the path in a file that can be reused
  $ echo "jna.jar.path=/usr/share/java/jna-3.2.7.jar" > build.properties
  $ ant clean compile
  $ ant jar

Because of how Java Agents are loaded by the JVM, the value of the 
"jna.jar.path" used when running "ant jar" will be included in the jar 
manifest information as part of the "Boot-Class-Path" for the agent.  

At run time, the JVM will look for the JNA jar using:
  * "jna.jar" in the same directory as mlockall-agent.jar
  * the name of the jar file specified using jna.jar.path in the same 
    directory as mlockall-agent.jar
  * the full path of the jar file specif-ed using jna.jar.path 



FAQ
---

### Is this agent a good substitute for running with swap disabled?

No, mlockall-agent.jar is not a good substitute for running with swap disabled.
If you have the resources to run your important java processes on dedicated 
hardware with swap disabled that is recommended instead of using this agent -- 
but not everyone has that luxury, and this agent is for you.

### /proc/<pid>/status suggests that not all my heap is locked in memory?

For this agent to run effectively, you must configure your JVM with an initial 
min heap size equivalent to your max heap size, otherwise it will allocate 
additional memory (to grow the heap) after the agent has run mlockall() and 
that new portion of the heap will not be locked into RAM.

### Why does the agent only lock memory at the start of the process?

See the comments in the code related to the MCL_CURRENT constant

### What about mmap'ed data?

See the comments in the code related to the MCL_CURRENT constant

### I'm getting an error even though I set "ulimit -l" bigger then the heap?

The heap is not the only memory in use by your java process, you need to 
ensure the ulimit is big enough for all of the memory used by your java process.
