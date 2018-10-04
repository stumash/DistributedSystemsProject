# kill all RMIXXXResourceManagers
ps aux | egrep 'java .*RMI.*Manager' | grep $(whoami) | grep -v 'grep' | awk '{print $2}' | xargs kill

# kill all rmiregistries
ps aux | egrep 'rmiregistry' | egrep $(whoami) | grep -v 'grep' | awk '{print $2}' | xargs kill
