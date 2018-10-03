#Usage: ./run_tcp_server.sh rmi_name

# $1 rmi_name: resource manager name

java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd)/ Server.TCP.TCP"${1}"ResourceManager
