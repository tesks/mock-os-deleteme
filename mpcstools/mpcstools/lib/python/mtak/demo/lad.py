import mpcsutil
import StringIO
import time
import urllib
import xml.parsers.expat

url = 'http://localhost:8182/lad/80'
columns = []
lad = {}
data = ''
receiveTime = 0
eventTime = ''

def makeChannelFromCsv(csv):

    global eventTime, receiveTime

    fields = csv.split(',')
    chan = mpcsutil.channel.ChanVal()

    for i in range(0,len(columns)):

        column = columns[i].lower()
        field = fields[i][1:-1]

        if column == 'channel id' or column == 'id':

            chan.channelId = field

        elif column == 'type':

            chan.type = field

        elif column == 'sclk':

            chan.sclk = field

        elif column == 'scet':

            chan.scet = field

        elif column == 'ert':

            chan.ert = field

        elif column == 'raw':

            chan.dn = field

        elif column == 'value':

            chan.eu = field

    chan.dn = mpcsutil.channel.formatDn(chan.type,chan.dn)
    if chan.type.lower() == "status":
        chan.status = chan.eu
        chan.eu = None
    if chan.eu:
        chan.eu = float(chan.eu)
    chan.receiveTime = receiveTime
    chan.eventTime = eventTime
    chan.alarms = []
    chan.dnUnits = ''
    chan.euUnits = ''

    return chan

def startElement(name,attrs):

    global eventTime, data

    data = ''
    if name == 'ChannelLadMessage':

        eventTime = attrs['eventTime']

def endElement(name):

    global columns, data, lad, makeChannelFromCsv

    if name == 'Columns':

        columns = data.split(',')

    elif name == 'Data':

      lines = data.split('\n')

      for line in lines:

          x = line.strip()
          if x:
              chan = makeChannelFromCsv(x)
              lad[chan.channelId] = chan

def characters(chars):

    global data

    data += chars

def test():

    ladStream = None
    try:

        parser = xml.parsers.expat.ParserCreate()

        parser.StartElementHandler = startElement
        parser.EndElementHandler = endElement
        parser.CharacterDataHandler = characters

        print('URL = {}'.format(url))
        ladStream = urllib.urlopen(url)
        
        receiveTime = time.time()
        print('Receive Time = {}'.format(receiveTime))

        parser.ParseFile(ladStream)

    except xml.parsers.expat.ExpatError as e:
        raise

    finally:
        if ladStream:
            ladStream.close()

    print('Total Entries = {}'.format(len(lad)))
    [print('{}'.format(_value)) for _value in lad.values()]

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
