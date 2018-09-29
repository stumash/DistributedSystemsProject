#!/bin/bash

#TODO: SPECIFY THE HOSTNAMES OF 4 CS MACHINES (lab1-1, cs-2, etc...)
MACHINES=()

tmux new-session \; \
	split-window -h \; \
	split-window -v \; \
	split-window -v \; \
	select-layout main-vertical \; \
	select-pane -t 2 \; \
	send-keys "ssh -t ${MACHINES[0]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; ./run_server.sh 2000 Flight\"" C-m \; \
	select-pane -t 3 \; \
	send-keys "ssh -t ${MACHINES[1]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; ./run_server.sh 2001 Car\"" C-m \; \
	select-pane -t 4 \; \
	send-keys "ssh -t ${MACHINES[2]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; ./run_server.sh 2002 Room\"" C-m \; \
	select-pane -t 1 \; \
	send-keys "ssh -t ${MACHINES[3]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; sleep .5s; ./run_middleware.sh ${MACHINES[0]} ${MACHINES[1]} ${MACHINES[2]}\"" C-m \;
