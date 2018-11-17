#!/usr/bin/env bash

# handle bad user input or help
#-----------------------------------

if [ -z "${1}" ] || [ "${1}" == "--help" ] || [ "${1}" == "-h" ]; then
    echo "usage: ./run.sh <configFile> [--client | --testclient multiclient mintime numclients]"
    echo ""
    echo "examples:"
    echo "    ./run.sh configs/local_rmi"
    echo "    ./run.sh configs/local_rmi --client"
    echo "    ./run.sh configs/local_rmi --testclient false"
    echo "    ./run.sh configs/local_rmi --testclient true 200 5"
    echo ""
    echo "(see configs/exampleConfigFile.txt for configFile format)"
    echo ""

    if [ -z "${1}" ]; then
        exitCode=1
    else
        exitCode=0
    fi

    exit "${exitCode}"
fi

# make sure we are in the current folder
#----------------------------------------------------

THIS_DIR="$(dirname "$(readlink -f "$0")")"
cd "${THIS_DIR}"

# set up some useful constants
#-----------------------------------

SCRIPT_DIR="${THIS_DIR}/scripts"

LIB_DIR="${THIS_DIR}/target/lib"
BUILD_DIR="${THIS_DIR}/target/classes"

BUILD_CLIENT_DIR="${BUILD_DIR}/group25/Client"

read RMI_CLIENT TCP_CLIENT < \
    <(echo "${BUILD_CLIENT_DIR}/"{RMI,TCP}"Client")

BUILD_SERVER_DIR="${BUILD_DIR}/group25/Server"
BUILD_RMI_SERVER_DIR="${SERVER_DIR}/RMI"
BUILD_TCP_SERVER_DIR="${SERVER_DIR}/TCP"

read RMI_MID_RM RMI_CAR_RM RMI_FLIGHT_RM RMI_ROOM_RM RMI_CUST_RM < \
    <(echo "${BUILD_RMI_SERVER_DIR}/RMI"{Middleware,Car,Flight,Room,Customer}"ResourceManager")

read TCP_MID_RM TCP_CAR_RM TCP_FLIGHT_RM TCP_ROOM_RM TCP_CUST_RM < \
    <(echo "${BUILD_TCP_SERVER_DIR}/TCP"{Middleware,Car,Flight,Room,Customer}"ResourceManager")

SRC_DIR="${THIS_DIR}/src/main/java"
SRC_CLIENT_DIR="${SRC_DIR}/group25/Client"
SRC_SERVER_DIR="${SRC_DIR}/group25/Server"

RES_DIR="${THIS_DIR}/src/main/resources"

# parse config file into variables
#-----------------------------------

configFile="${1}"
if [ ! -f "${configFile}" ]; then
    echo "error: configFile not found"
    exit 1
fi


while read key value1 value2; do
    case "${key}" in
        "TCP_OR_RMI")
            TCP_OR_RMI="${value1}"
            ;;
        "MID_RM")
            MID_RM_HOST="${value1}"
            MID_RM_PORT="${value2}"
            ;;
        "CAR_RM")
            CAR_RM_HOST="${value1}"
            CAR_RM_PORT="${value2}"
            ;;
        "FLIGHT_RM")
            FLIGHT_RM_HOST="${value1}"
            FLIGHT_RM_PORT="${value2}"
            ;;
        "ROOM_RM")
            ROOM_RM_HOST="${value1}"
            ROOM_RM_PORT="${value2}"
            ;;
        "CUST_RM")
            CUST_RM_HOST="${value1}"
            CUST_RM_PORT="${value2}"
            ;;
    esac
done < "${configFile}"

# validate data read from config file
#-----------------------------------

if [ -z "${TCP_OR_RMI}" ] || [ "${TCP_OR_RMI}" != "RMI" ] && [ "${TCP_OR_RMI}" != "TCP" ]; then
    echo "error in config file: key 'TCP_OR_RMI' must have value 'TCP' or 'RMI'"
    exit 1
fi

for host in "${MID_RM_HOST}" "${CAR_RM_HOST}" "${FLIGHT_RM_HOST}" "${ROOM_RM_HOST}" "${CUST_RM_HOST}"; do
    if [ -z "${host}" ]; then
        echo "error in config file: empty hostname"
        exit 1
    fi
done

for port in "${MID_RM_PORT}" "${CAR_RM_PORT}" "${FLIGHT_RM_PORT}" "${ROOM_RM_PORT}" "${CUST_RM_PORT}"; do
    if [ -z "${port}" ]; then
        echo "error in config file: empty port"
        exit 1
    fi
    if [[ ! "${port}" =~ ^[0-9]{1,}$ ]]; then
        echo "error in config file: port must be integer"
        exit 1
    fi
done

# if ${2} == "--client", run client and exit
#----------------------------------------------

if [ "${2}" == "--client" ]; then
    cd "${BUILD_DIR}"
    java_secpol_flag="-Djava.security.policy=${RES_DIR}/java.policy"
    java "${java_secpol_flag}" -classpath "${LIB_DIR}/*:."\
            "group25.Client.${TCP_OR_RMI}Client"\
            -mwh "${MID_RM_HOST}"\
            -mwp "${MID_RM_PORT}"
    exit 0
fi

# if ${2} == "--testclient", run test client and exit
#----------------------------------------------

if [ "${2}" == "--testclient" ]; then
    if [ -z "${3}" ]; then
        echo "error: if '--testclient', then need either one more arg 'false', or 3 more args 'true' <mintime> <numclients>"
        exit 1
    fi

    if [[ "${3}" != "true" && "${3}" != "false" ]]; then
        echo "error: multiclient must be 'true' or 'false'"
        exit 1
    fi

    if [ "${3}" == "true" ]; then
        if [[ -z "${4}" && -z "${5}" ]]; then
            echo "error: if '--testclient true', then need two more args <mintime> <numclients>"
            exit 1
        fi
        if [[ ! "${4}" =~ ^[0-9]{1,}$ ]]; then
            echo "error: mintime must be non-negative integer"
            exit 1
        fi
        if [[ ! "${5}" =~ ^[0-9]{1,}$ ]]; then
            echo "error: numclients must be a positive integer"
            exit 1
        fi
    fi

    shift
    shift

    cd "${BUILD_DIR}"
    java_secpol_flag="-Djava.security.policy=${RES_DIR}/java.policy"
    java "${java_secpol_flag}" -classpath "${LIB_DIR}/*:."\
            "group25.Client.${TCP_OR_RMI}TestClient"\
            "${MID_RM_HOST}"\
            "${MID_RM_PORT}"\
            "${@}"
    exit 0
fi

# else
# generate code to run remotely via ssh
#-----------------------------------------

function run_rmi_server() {
    # ${1}: port to listen for rmiregistry
    # ${2}: type of resource manager ('Car','Flight','Room','Customer')
    # ${3}: customer resource manager hostname
    # ${4}: customer resource manager port
    echo \
        "echo -n 'Connected to '; hostname; "\
        "cd ${BUILD_DIR} > /dev/null; "\
        "(rmiregistry -J-Djava.rmi.server.useCodebaseOnly=false ${1} &); "\
        "java -Djava.security.policy=${RES_DIR}/java.policy "\
                "-classpath \"${LIB_DIR}/*:.\""\
                "group25.Server.RMI.RMI${2}ResourceManager "\
                "${1}"\
                "${3}"\
                "${4}"
}

function run_rmi_middleware() {
    # ${1}: port to listen for rmiregistry
    # ${2}: customer resource manager hostname
    # ${3}: customer resource manager port
    # ${4}: flight resource manager hostname
    # ${5}: flight resource manager port
    # ${6}: room resource manager hostname
    # ${7}: room resource manager port
    # ${8}: car resource manager hostname
    # ${9}: car resource manager port
    echo \
        "echo -n 'Connected to '; hostname; "\
        "cd ${BUILD_DIR} > /dev/null; "\
        "(rmiregistry -J-Djava.rmi.server.useCodebaseOnly=false ${1} &); "\
        "java -Djava.security.policy=${RES_DIR}/java.policy"\
                "-classpath \"${LIB_DIR}/*:.\""\
                "group25.Server.RMI.RMIMiddlewareResourceManager"\
                "${@}"
}

function run_tcp_server() {
    # ${1}: type of resource manager ('Car','Flight','Room','Customer')
    # ${2}: listening hostname
    # ${3}: listening port
    # ${4}: customer resource manager hostname
    # ${5}: customer resource manager port
    rm_type="${1}"
    shift
    echo \
        "echo -n 'Connected to '; hostname; "\
        "cd ${BUILD_DIR} > /dev/null; "\
        "java -Djava.security.policy=${RES_DIR}/java.policy"\
                "-classpath \"${LIB_DIR}/*:.\""\
                "group25.Server.TCP.TCP${rm_type}ResourceManager"\
                "${@}"
}

function run_tcp_middleware() {
    # ${1}:  middleware hostname
    # ${2}:  middleware port
    # ${3}:  customer hostname
    # ${4}:  customer port
    # ${5}:  flight hostname
    # ${6}:  flight port
    # ${7}:  room hostname
    # ${8}:  room port
    # ${9}:  car hostname
    # ${10}: car port
    echo \
        "echo -n 'Connected to '; hostname; "\
        "cd ${BUILD_DIR} > /dev/null; "\
        "java -Djava.security.policy=${RES_DIR}/java.policy"\
                "-classpath \"${LIB_DIR}/*:.\""\
                "Server.TCP.TCPMiddlewareResourceManager"\
                "${@}"
}

# run code in tmux splits over ssh
#-----------------------------------

if [ "${TCP_OR_RMI}" == "RMI" ]; then
    tmux new-session \; \
        split-window -v \; \
        split-window -h \; \
        split-window -h \; \
        select-pane -t 1 \; \
        split-window -h \; \
        select-pane -t 0 \; \
        split-window -h \; \
        select-pane -t 5 \; \
        send-keys "ssh -t ${CUST_RM_HOST} \"$(run_rmi_server ${CUST_RM_PORT} Customer)\"" C-m \; \
        select-pane -t 4 \; \
        send-keys "ssh -t ${FLIGHT_RM_HOST} \"$(run_rmi_server ${FLIGHT_RM_PORT} Flight ${CUST_RM_HOST} ${CUST_RM_PORT})\"" C-m \; \
        select-pane -t 3 \; \
        send-keys "ssh -t ${CAR_RM_HOST} \"$(run_rmi_server ${CAR_RM_PORT} Car ${CUST_RM_HOST} ${CUST_RM_PORT})\"" C-m \; \
        select-pane -t 2 \; \
        send-keys "ssh -t ${ROOM_RM_HOST} \"$(run_rmi_server ${ROOM_RM_PORT} Room ${CUST_RM_HOST} ${CUST_RM_PORT})\"" C-m \; \
        select-pane -t 1 \; \
        send-keys "ssh -t ${MID_RM_HOST} \"$(run_rmi_middleware ${MID_RM_PORT} ${CUST_RM_HOST} ${CUST_RM_PORT}"\
                  "${FLIGHT_RM_HOST} ${FLIGHT_RM_PORT} ${ROOM_RM_HOST} ${ROOM_RM_PORT} ${CAR_RM_HOST}) ${CAR_RM_PORT}\"" C-m \;
elif [ "${TCP_OR_RMI}" == "TCP" ]; then
    tmux new-session \; \
        split-window -v \; \
        split-window -h \; \
        split-window -h \; \
        select-pane -t 1 \; \
        split-window -h \; \
        select-pane -t 0 \; \
        split-window -h \; \
        select-pane -t 5 \; \
        send-keys "ssh -t ${CUST_RM_HOST} \"$(run_tcp_server Customer ${CUST_RM_HOST} ${CUST_RM_PORT})\"" C-m \; \
        select-pane -t 4 \; \
        send-keys "ssh -t ${FLIGHT_RM_HOST} \"$(run_tcp_server Flight ${FLIGHT_RM_HOST} ${FLIGHT_RM_PORT} ${CUST_RM_HOST} ${CUST_RM_PORT})\"" C-m \; \
        select-pane -t 3 \; \
        send-keys "ssh -t ${CAR_RM_HOST} \"$(run_tcp_server Car ${CAR_RM_HOST} ${CAR_RM_PORT} ${CUST_RM_HOST} ${CUST_RM_PORT})\"" C-m \; \
        select-pane -t 2 \; \
        send-keys "ssh -t ${ROOM_RM_HOST} \"$(run_tcp_server Room ${ROOM_RM_HOST} ${ROOM_RM_PORT} ${CUST_RM_HOST} ${CUST_RM_PORT})\"" C-m \; \
        select-pane -t 1 \; \
        send-keys "ssh -t ${MID_RM_HOST} \"$(run_tcp_middleware ${MID_RM_HOST} ${MID_RM_PORT} ${CUST_RM_HOST} ${CUST_RM_PORT}"\
                  "${FLIGHT_RM_HOST} ${FLIGHT_RM_PORT} ${ROOM_RM_HOST} ${ROOM_RM_PORT} ${CAR_RM_HOST}) ${CAR_RM_PORT}\"" C-m \;
fi
