#!/usr/bin/env bash

#!/bin/bash

echo 'Usage: ./run_rmi_servers.sh middle{hostname,port}, flight{hostname,port}, car{hostname,port}, customer{hostname,port}'
echo 'middlewareHostname="$1"'
echo 'middlewarePort="$2"'
echo 'flightHostname="$3"'
echo 'flightPort="$4"'
echo 'carHostname="$5"'
echo 'carPort="$6"'
echo 'roomHostname="$7"'
echo 'roomPort="$8"'
echo 'customerHostname="$9"'
echo 'customerPort="${10}"'

middlewareHostname="$1"
middlewarePort="$2"
flightHostname="$3"
flightPort="$4"
carHostname="$5"
carPort="$6"
roomHostname="$7"
roomPort="$8"
customerHostname="$9"
customerPort="${10}"

tmux new-session \; \
    split-window -v \; \
    split-window -h \; \
    split-window -h \; \
    select-pane -t 1 \; \
    split-window -h \; \
    select-pane -t 0 \; \
    split-window -h \; \
    select-pane -t 5 \; \
    send-keys "ssh -t ${customerHostname} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; ./run_tcp_server.sh ${customerPort} Customer\"" C-m \; \
    select-pane -t 4 \; \
    send-keys "ssh -t ${flightHostname} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; ./run_tcp_server.sh ${flightPort} Flight ${customerHostname} ${customerPort} \" " C-m \; \
    select-pane -t 3 \; \
    send-keys "ssh -t ${carHostname} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; ./run_tcp_server.sh ${carPort} Car ${customerHostname} ${customerPort} \" " C-m \; \
    select-pane -t 2 \; \
    send-keys "ssh -t ${roomHostname} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; ./run_tcp_server.sh ${roomPort} Room ${customerHostname} ${customerPort} \" " C-m \; \
    select-pane -t 1 \; \
    send-keys "ssh -t ${middlewareHostname} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; sleep .5s; ./run_tcp_middleware.sh ${middlewarePort} ${customerHostname} ${customerPort} ${flightHostname} ${flightPort} ${roomHostname} ${roomPort} ${carHostname} ${carPort} \" " C-m \;

#------------------------------------------
# Just the localhost version
#------------------------------------------

#tmux new-session \; \
#    split-window -v \; \
#    split-window -h \; \
#    split-window -h \; \
#    select-pane -t 1 \; \
#    split-window -h \; \
#    select-pane -t 0 \; \
#    split-window -h \; \
#    select-pane -t 5 \; \
#    send-keys "./run_rmi_server.sh 2003 Customer" C-m \; \
#    select-pane -t 4 \; \
#    send-keys "./run_rmi_server.sh 2000 Flight localhost 2003" C-m \; \
#    select-pane -t 3 \; \
#    send-keys "./run_rmi_server.sh 2001 Car localhost 2003" C-m \; \
#    select-pane -t 2 \; \
#    send-keys "./run_rmi_server.sh 2002 Room localhost 2003" C-m \; \
#    select-pane -t 1 \; \
#    send-keys "./run_rmi_middleware.sh 2005 localhost 2003 localhost 2000 localhost 2002 localhost 2001" C-m \;
