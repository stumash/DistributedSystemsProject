#!/usr/bin/env bash

echo 'Usage: run_rmi_middleware.sh [middleport, [customerHostname, [customerPort, [flightHostname, [flightPort,'
echo ' [roomHostname, [roomPort, [carHostname, [carPort]]]]]]]]]'
echo '$1 middleware port'
echo '$2 customer hostname'
echo '$3 customer port'
echo '$4 flight hostname'
echo '$5 flight port'
echo '$6 room hostname'
echo '$7 room port'
echo '$8 car hostname'
echo '$9 car port'

java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd)/ Server.RMI.RMIMiddlewareResourceManager "${@}"
