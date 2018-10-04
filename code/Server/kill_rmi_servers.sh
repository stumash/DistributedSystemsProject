RMIXXXResourceManagers="$( \
    ps aux | \
    egrep 'java .*RMI.*Manager' | grep $(whoami) | grep -v 'grep' | \
    awk '{print $2}' | \
    tr '\n' ' ' \
)"

RMIRegistries="$( \
    ps aux | \
    egrep 'rmiregistry' | egrep $(whoami) | grep -v 'grep' | \
    awk '{print $2}' | \
    tr '\n' ' ' \
)"

# kill all RMIXXXResourceManagers
if [ -n "$RMIXXXResourceManagers" ]
then
    echo $RMIXXXResourceManagers | xargs kill
fi

# kill all rmiregistries
if [ -n "$RMIRegistries" ]
then
    echo $RMIRegistries | xargs kill
fi
