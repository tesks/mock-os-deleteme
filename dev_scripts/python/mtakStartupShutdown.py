from mtak.wrapper import *
import sys

sid = int(sys.argv[1])
print "Starting MTAK (%s)" % sid
mtak.wrapper.startup(key=sid)

print "Started MTAK "

print "Shutting down mtak"

success = mtak.wrapper.shutdown()
print "Status=%s" % success