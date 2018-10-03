all: java.policy compile-client compile-client-tcp

java.policy: FORCE
	@echo "Creating client java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

compile-client: ../Server/RMIInterface.jar ../Server/Trace.jar ../Server/TCP.jar
	javac -cp ../Server/RMIInterface.jar:../Server/Trace.jar:../Server/TCP.jar Client/*.java

../Server/RMIInterface.jar: ../Server/Server/Interface/IResourceManager.java
	make -C ../Server/ RMIInterface.jar

compile-client-tcp: ../Server/TCP.jar ../Server/Trace.jar
	javac -cp ../Server/TCP.jar:../Server/Trace.jar:../Server/RMIInterface.jar:. Client/TCPClient.java

../Server/TCP.jar: ../Server/Server/TCP/*.java
	make -C ../Server/ TCP.jar

../Server/Trace.jar: ../Server/Server/Common/Trace.java
	make -C ../Server/ Trace.jar

clean:
	rm -f Client/*.class
	rm -f ../Server/RMIInterface.jar
	rm -f ../Server
	rm -f java.policy

FORCE: ;
