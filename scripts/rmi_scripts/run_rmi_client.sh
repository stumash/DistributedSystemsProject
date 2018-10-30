#!/usr/bin/env bash
echo 'Usage: ./run_client.sh [<server_hostname> [<server_port>]]'
echo '$1 hostname of middleware (default localhost)'
echo '$2 port number of middleware (default 2005)'

java -Djava.security.policy=java.policy -cp ../Server/RMIInterface.jar:../Server/TCP.jar:../Server.Trace.jar:. Client.RMIClient $1 $2
