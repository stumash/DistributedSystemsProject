TCPXXXResourceManagers="$( \
    ps aux | \
    egrep 'java .*TCP.*Manager' | grep $(whoami) | grep -v 'grep' | \
    awk '{print $2}' | \
    tr '\n' ' ' \
)"

# kill all RMIXXXResourceManagers
if [ -n "$TCPXXXResourceManagers" ]
then
    echo $TCPXXXResourceManagers | xargs kill
fi
