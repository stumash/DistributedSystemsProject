#Usage: ./run_tcp_server.sh rmi_name


java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd)/ Server.TCP.TCP"${2}"ResourceManager "${1}" "${2}" "${3}"
