# kill all RMIXXXResourceManagers
ps aux | egrep 'java .*RMI.*Manager' | grep -v 'grep' | awk '{print $2}' | xargs kill

# kill all rmiregistries
ps aux | egrep 'rmiregistry' | egrep 'smasha2|ndevas' | grep -v 'grep' | awk '{print $2}' | xargs kill
