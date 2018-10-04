echo 'Usage: ./run_tcp_server.sh [listening port, [RMname, [customerHostname, [customerPort]]]]'
echo '$1 RMName'
echo '$2 listening hostname'
echo '$3 listening port'
echo '$4 CustomerServer hostname'
echo '$5 CustomerServer port'

java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd)/ Server.TCP.TCP"${1}"ResourceManager "${2}" "${3}" "${4}" "${5}"
