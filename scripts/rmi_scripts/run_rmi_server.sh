#!/usr/bin/env bash
echo 'Usage: ./run_rmi_server.sh [rmiport, [RMname, [customerHostname, [customerPort]]]]'
echo '$1 registry port'
echo '$2 RMname'
echo '$3 CustomerServer hostname'
echo '$4 CustomerServer port'

run_rmi.sh $1 > /dev/null 2>&1
java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd)/ Server.RMI.RMI$2ResourceManager $1 $3 $4
