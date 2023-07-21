#!/bin/sh
#
#+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
#
#   Script Name:    chill_ping_jms.sh (Test for an active Message Bus)
#
#   Description:    Test for an active message service on a specific port
#
#   Note: 			Fuse and Active are [basically] synonymous
#					also - we are not currently supporting Fiorano.  
#					Fiorano hasn't been tested with JMS since 1.0
#
#   Usage: 			chill_ping_jms.sh [--jmsHost hostID --jmsPort portID -q]
#					Default Host is local-host, default Port is 61614
#					-q or --quiet => no Errors or Warnings will be displayed 
#					to stdout.
#
#                   --validatePort is for use by chill startup scripts.
#                   --noJMS is now obeyed.
#
#   Output:     	Exit value {$?}:
#					0 => the service is running; 
#					2 => the service is Not running
#					1 => bad parameters [usage]
#
#  =============================================================================
#  History
#  Modification
#  Rm references to Fiorano: not supported Rm echo msg w/ --quiet
#  Change default port from 1098 to 61614
#  Change to use localhost rather than `hostname` as the host default
#  Add checks for bad port (--validatePort)
#  Corrected command line options for 'date' to be compatible with Mac OSX
#+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
#+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
msg( )
{
	if $quiet; then
		return
	fi
	echo "\n"
	echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! $1 !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
	echo "chill_ping_jms.sh: TEST-FOR-ACTIVE-MESSAGE-SERVICE-\n$1: $2"
	echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! $1 !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
	echo "\n"
}

#+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
#+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++


hostId=localhost
portNumber=61614
paramList=("$@")
quiet=false
validatePort=false

skipJms=0
lvar=0

# Fixed parameters to take either single or double dash,
# since CLI for Java doesn't care.

for param in "${paramList[@]}"
do
	lvar=$[$lvar+1]

	case "$param" in
	  --jmsHost | -jmsHost)
			# echo "Override JMS Host with: ${paramList[lvar]}"
			hostId="${paramList[lvar]}"
			;;
	  --jmsPort | -jmsPort)
			# echo "Override JMS Port with: ${paramList[lvar]}"
			portNumber="${paramList[lvar]}"
			;;
	  --quiet | -quiet | --q | -q)
			# echo "Running in Quiet mode: no printed output"
			quiet=true
			;;
	  --validatePort | -validatePort)
			# echo "Validate JMS port"
			validatePort=true
			;;
	  --noJMS | -noJMS | --J | -J)
			# echo "Skip JMS check"
			skipJms=1
			;;
	  --help | -help | --h | -h)
			skipJms=1
			;;
	  --version | -version | --v | -v)
			skipJms=1
			;;
	esac

done

if [ $skipJms == 1 ]; then
   exit 0
fi

if test "X$validatePort" = "Xtrue"
then
    DATE=`date -u +"%Y-%m-%dT%T.000 GMT"`

    if test $portNumber -eq $portNumber 2>/dev/null
    then
        # It's an integer

        if test $portNumber -lt 0 -o $portNumber -gt 65535
        then
            echo "FATAL [$DATE]: JMS port of $portNumber is outside of permissible range [0,65535]"

            exit 1
        fi

    else
        echo "FATAL [$DATE]: JMS port must be an integer, but '$portNumber' is invalid"

        exit 1
    fi
fi

tempfoo=`basename $0.XXXXXXXXXX`
TEMP=`mktemp -t ${tempfoo}` || exit 1

chmod 777 $TEMP

# not supported on atb system: stty flusho
# Use openssl's s_client to ping jms instead of telnet
# TODO: Adjust s_client args for client authentication
# No ssl client args causes stack trace in activemq.log but returns similar to telnet
#   -CAfile cacert.pem -cert clientcert.pem -key clientkey.pem -state -tls1_2
`(sleep 1; echo "quit") | openssl s_client -connect ${hostId}:${portNumber} > $TEMP 2>&1`
#not supported on atb system: stty -flusho

status=0

if cat "${TEMP}" | grep -i "CONNECTED" > /dev/null; then
	# echo "-------->Yup - service is up 'n running"
	status=0
else
	msg "ERROR" "ActiveMQ is NOT running on ${hostId} at port ${portNumber}"
	status=2
fi

rm -f $TEMP

exit ${status}		# check value via #?
