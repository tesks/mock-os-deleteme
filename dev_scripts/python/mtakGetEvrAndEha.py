from mtak.wrapper import *
import sys

sid = int(sys.argv[1])
evrTarget = ""
ehaTarget = ""

print "Starting MTAK on Session (%s)" % sid
mtak.wrapper.startup(key=sid)

evrTarget = "" if sys.argv[2] is None else sys.argv[2]
ehaTarget = "" if sys.argv[3] is None else sys.argv[3]
print "Started MTAK! Listening for evr %s and eha %s"% (evrTarget, ehaTarget)

evr = None
chan = None
tries = 0
while evrTarget is not "" and (evr is None or evr == []):
	tries = tries + 1
	evr = get_evr(name=evrTarget)
	if tries == 10:
		print "Unable to find EVR yet.. trying a wait"
		evr = wait_evr(name=evrTarget)
		print evr
		tries = 0

print(evr)
print ""

tries = 0
while chanTarget is not "" and (chan is None or chan == []):
	tries = tries + 1
	chan = get_eha(channelId=chanTarget)
	if tries == 10:
		print "Unable to find EHA yet.. trying a wait"
		tries = 0
		chan = wait_eha(channelId=chanTarget)
		print chan

print(chan)

print "Shutting down mtak"

success = mtak.wrapper.shutdown()