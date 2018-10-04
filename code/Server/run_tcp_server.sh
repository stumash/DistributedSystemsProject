echo 'Usage: ./run_tcp_server.sh [listening port, [RMname, [customerHostname, [customerPort]]]]'
echo '$1 listening port'
echo '$2 RMName'
echo '$3 CustomerServer hostname'
echo '$4 CustomerServer port'

java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd)/ Server.TCP.TCP"${2}"ResourceManager "${1}" "${3}" "${4}"
