from lad import client, gdsclient
import json
import sys

sid         = int(sys.argv[1])
evrTarget   = sys.argv[2]

if __name__ == '__main__': 
    print "Connecting to Session (%s) with evr target %s " % (sid, evrTarget)

    evrq = client.EvrQuery()
    evrq.addEvrName(evrTarget)
    evrq.useErt()
    evrq.addSessionNumber(sid)
    c = client.LadClient(host='localhost', port=8887)
    
    resp = c.fetchEvrs(evrq)

    print ''
    print 'params:', evrq.getParams()
    print ''

    print 'resp:', resp
    #print json.dumps(resp, indent=4)