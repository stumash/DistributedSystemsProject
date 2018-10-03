all: java.policy compile-server-rmi compile-server-tcp

java.policy: FORCE
	@echo "Creating server java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

compile-server-rmi: Server/RMI/*.java Server/Interface/IResourceManager.java Server/Common/*.java
	javac Server/RMI/*.java Server/Interface/IResourceManager.java Server/Common/*.java

compile-server-tcp: Server/TCP/*.java Server/Interface/IResourceManager.java Server/Common/*.java
	javac Server/TCP/*.java Server/Interface/IResourceManager.java Server/Common/*.java

clean:
	rm -f Server/Interface/*.class Server/Common/*.class Server/RMI/*.class
	rm -f Server/TCP/*.class
	rm -f RMIInterface.jar
	rm -f java.policy


#---------------
# for Client/Makefile
#---------------

RMIInterface.jar: Server/Interface/IResourceManager.java
	@echo "Compiling RMI server interface"
	javac Server/Interface/IResourceManager.java
	jar cvf RMIInterface.jar Server/Interface/IResourceManager.class

TCP.jar: Server/TCP/*.java
	@echo Compiling TCP classes into jar
	javac Server/TCP/*.java
	jar cvf TCP.jar Server/TCP/*.class

Trace.jar: Server/Common/Trace.java
	@echo Compiling Trace.java into jar
	javac Server/Common/Trace.java
	jar cvf Trace.jar Server/Common/Trace.class

