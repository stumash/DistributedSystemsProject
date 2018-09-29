#Usage: ./run_server.sh [<rmi_name>]

# $1 registry port
# $2 resource manager name

./run_rmi.sh $1 > /dev/null 2>&1
java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd)/ Server.RMI.RMI$2ResourceManager
