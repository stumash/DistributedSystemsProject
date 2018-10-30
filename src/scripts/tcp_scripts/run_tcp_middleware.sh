#!/usr/bin/env bash

echo 'Usage: run_tcp_middleware.sh [middleport, [customerHostname, [customerPort, [flightHostname, [flightPort,'
echo ' [roomHostname, [roomPort, [carHostname, [carPort]]]]]]]]]'
echo '$1 middleware hostname'
echo '$2 middleware port'
echo '$3 customer hostname'
echo '$4 customer port'
echo '$5 flight hostname'
echo '$6 flight port'
echo '$7 room hostname'
echo '$8 room port'
echo '$9 car hostname'
echo '$10 car port'

java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd)/ Server.TCP.TCPMiddlewareResourceManager "${@}"
