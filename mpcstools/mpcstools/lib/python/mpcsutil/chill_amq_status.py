#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
chill_check_amq.py
This utility extracts status information from the activemq utility.
"""

from __future__ import (absolute_import, division, print_function)


# To support activemq (not fuse) 5.13.0.
# activemq-admin bo longer exists. Functionality combined in activemq.
# Changes to the arguments to the commands.

import mpcsutil

import os
import subprocess
import sys
import stat

brokerStatusFile = 'AMQ_broker_status.txt'
connectionStatusFile = 'AMQ_connection_status.txt'
topicStatusFile = 'AMQ_topic_status.txt'
subscriberStatusFile = 'AMQ_subscriber_status.txt'

def _runAmqConnectionQuery(amqDir=None,statusDir='/tmp'):
    '''Run activemq connection query
    '''
    command = amqDir + '/bin/activemq query --jmxurl service:jmx:rmi:///jndi/rmi://localhost:1098/jmxrmi Type=Connect --objname BrokerName=MPCS_Message_Broker'

    connectionFile = str(statusDir) + '/' + connectionStatusFile
    amqQueryFile = open(connectionFile,'w')
    amqQuery = subprocess.Popen(command,shell=True,stdout=amqQueryFile)
    status = amqQuery.wait()
    amqQueryFile.close()
    if status != 0:
        print('ERROR: activemq connection query produced an error')
    os.chmod(connectionFile, stat.S_IWUSR | stat.S_IWGRP | stat.S_IWOTH | stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH)
    return status

def _runAmqTopicQuery(amqDir=None,statusDir='/tmp'):
    '''Run activemq topic query
    '''
    command = amqDir + '/bin/activemq query --jmxurl service:jmx:rmi:///jndi/rmi://localhost:1098/jmxrmi -QTopic=mpcs*'

    topicFile = str(statusDir) + '/' + topicStatusFile
    amqQueryFile = open(topicFile,'w')
    amqQuery = subprocess.Popen(command,shell=True,stdout=amqQueryFile)
    status = amqQuery.wait()
    amqQueryFile.close()
    if status != 0:
        print('ERROR: activemq topic query produced an error')
    os.chmod(topicFile, stat.S_IWUSR | stat.S_IWGRP | stat.S_IWOTH | stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH)
    return status

def _runAmqSubscriberQuery(amqDir=None,statusDir='/tmp'):
    '''Run activemq subscriber query
    '''
    command = amqDir + '/bin/activemq query --jmxurl service:jmx:rmi:///jndi/rmi://localhost:1098/jmxrmi Type=Subscription'

    subscriberFile = str(statusDir) + '/' + subscriberStatusFile
    amqQueryFile = open(subscriberFile,'w')
    amqQuery = subprocess.Popen(command,shell=True,stdout=amqQueryFile)
    status = amqQuery.wait()
    amqQueryFile.close()
    if status != 0:
        print('ERROR: activemq subscription query produced an error')
    os.chmod(subscriberFile, stat.S_IWUSR | stat.S_IWGRP | stat.S_IWOTH | stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH)
    return status

def _runAmqBrokerQuery(amqDir=None,statusDir='/tmp'):
    '''Run activemq broker query
    '''
    command = amqDir + '/bin/activemq query --jmxurl service:jmx:rmi:///jndi/rmi://localhost:1098/jmxrmi --objname type=Broker,brokerName=MPCS_Message_Broker'

    brokerFile = str(statusDir) + '/' + brokerStatusFile
    amqQueryFile = open(brokerFile,'w')
    amqQuery = subprocess.Popen(command,shell=True,stdout=amqQueryFile)
    status = amqQuery.wait()
    amqQueryFile.close()
    if status != 0:
        print('ERROR: activemq broker query produced an error')
    os.chmod(brokerFile, stat.S_IWUSR | stat.S_IWGRP | stat.S_IWOTH | stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH)
    return status

def _readSubscriberFile(statusDir='/tmp'):
    '''Read in the given subscriber status file.'''

    # Special check for consumer is for case when chill_down runs without chill_monitor
    # or any other subscriber.

    topics = {}
    consumer = None

    subscriberFilename = str(statusDir) + '/' + subscriberStatusFile

    #print 'Reading subscriber file %s' % (str(subscriberFilename))

    #Parse the subscriber status file
    try:
        for line in open(subscriberFilename,'r'):

            if line.startswith('destinationName'):
                topic = line.strip().split(' = ')[1]
            elif line.startswith('consumerId'):
                consumer = line.strip().split(' = ')[1]
            elif line.startswith('EnqueueCounter'):
                enqueue = line.strip().split(' = ')[1]
            elif line.startswith('DequeueCounter'):
                dequeue = line.strip().split(' = ')[1]
            elif line.startswith('DispatchedQueueSize'):
                dispatch = line.strip().split(' = ')[1]
            elif line.startswith('PendingQueueSize'):
                pending = line.strip().split(' = ')[1]
            elif line.startswith('DiscardedCount'):
                discarded = line.strip().split(' = ')[1]
            elif line.startswith('ClientId') and consumer != None:
                if str(topic).find('ActiveMQ') == -1:
                    client = {'Consumer': consumer, 'Enqueued' : enqueue, 'Dequeued' : dequeue,
                        'Pending' : pending, 'Dispatched' : dispatch, 'Discarded' : discarded }
                    topics.setdefault(topic, {}).update({consumer: client})

    except IOError:
        print('There was a problem reading the subscriber status file %s: %s' % (str(subscriberFilename),sys.exc_info()))
        raise

    print('Subscriber Information ===================================================================')
    print(' ')

    for topic in topics.keys():
        print(' ')
        print("----TOPIC " + topic + "----")
        print('    Consumer count: ' + str(len(topics[topic])))
        print(' ')
        for client in topics[topic].keys():
            clientInfo = topics[topic].get(client)
            outstanding = int(clientInfo.get('Dispatched'))
            if outstanding < 0:
                outstanding = 0
            outstanding = outstanding + int(clientInfo.get('Pending'))
            status = 'UP TO DATE'
            if outstanding > 100:
                status = 'LAGGING'
            print('    Consumer: ' + client + ' Status: ' + status)
            print('    Enqueued: ' + clientInfo.get('Enqueued') + ' Dequeued: ' + clientInfo.get('Dequeued') + ' Discarded: ' + clientInfo.get('Discarded')  + ' Pending: ' + str(outstanding))

def _readTopicFile(statusDir='/tmp'):
    '''Read in the given topic status file.'''

    topics = {}
    topic = {}
    topicFilename = str(statusDir) + '/' + topicStatusFile

    #print 'Reading topic file %s' % (str(topicFilename))

    #Parse the topic status file
    try:
        for line in open(topicFilename,'r'):

            if line.startswith('Name'):
                topic['name'] = line.strip().split(' = ')[1]
                topics[topic['name']] = topic
            elif line.startswith('ConsumerCount'):
                topic['consumers'] = line.strip().split(' = ')[1]
            elif line.startswith('ProducerCount'):
                topic['producers'] = line.strip().split(' = ')[1]
            elif line.startswith('EnqueueCount'):
                topic['enqueue'] = line.strip().split(' = ')[1]
            elif line.startswith('DequeueCount'):
                topic['dequeue'] = line.strip().split(' = ')[1]
            elif line.startswith('DispatchCount'):
                topic['dispatch'] = line.strip().split(' = ')[1]
            elif line.startswith('MemoryPercentUsage'):
                topic = {}
                topic['memory'] = line.strip().split(' = ')[1]

    except IOError:
        print('There was a problem reading the topic status file %s: %s' % (str(topicFilename),sys.exc_info()))
        raise

    print('Topic Information ========================================================================')
    print(' ')
    for topic in topics.keys():
        thisTopic = topics[topic];
        print("----TOPIC " + thisTopic.get('name') + "----")
        print('    Producer count: ' + thisTopic.get('producers') + ' Consumer count: ' + thisTopic.get('consumers') + ' Memory Used: ' + thisTopic.get('memory') + '%')
        print('    Enqueued: ' + thisTopic.get('enqueue') + ' Dequeued: ' + thisTopic.get('dequeue')  + ' Dispatched: ' + thisTopic.get('dispatch'))
        print(' ')

def _readBrokerFile(statusDir='/tmp'):
    '''Read in the given broker status file.'''

    broker = {}

    brokerFilename = str(statusDir) + '/' + brokerStatusFile

    #print 'Reading broker file %s' % (str(brokerFilename))

    #Parse the topic status file
    try:
        for line in open(brokerFilename,'r'):

            if line.startswith('brokerName'):
                broker['name'] = line.strip().split(' = ')[1]
            elif line.startswith('TotalConsumerCount'):
                broker['consumers'] = line.strip().split(' = ')[1]
            elif line.startswith('ProducerCount'):
                broker['producers'] = line.strip().split(' = ')[1]
            elif line.startswith('TotalEnqueueCount'):
                broker['enqueue'] = line.strip().split(' = ')[1]
            elif line.startswith('TotalDequeueCount'):
                broker['dequeue'] = line.strip().split(' = ')[1]
            elif line.startswith('MemoryPercentUsage'):
                broker['memory'] = line.strip().split(' = ')[1]
            elif line.startswith('TempPercentUsage'):
                broker['temp'] = line.strip().split(' = ')[1]
            elif line.startswith('StorePercentUsage'):
                broker['store'] = line.strip().split(' = ')[1]

    except IOError:
        print('There was a problem reading the broker status file %s: %s' % (str(brokerFilename),sys.exc_info()))
        raise

    if broker.get('name') == None:
        print('The MPCS Message Broker on this host is either dead or is not responding as expected')
        return False

    print('Broker Information =======================================================================')
    print(' ')
    print("----BROKER " + broker.get('name') + "----")
    print('    Consumer count: ' + broker.get('consumers') + ' Enqueued: ' + broker.get('enqueue') + ' Dequeued: ' + broker.get('dequeue'))
    print('    Memory Used: ' + broker.get('memory') + '% Store Used: ' + broker.get('store') + '% Temp Used: ' + broker.get('temp') +'%')
    print(' ')
    return True

def _readConnectionFile(statusDir='/tmp'):
    '''Read in the given connection status file.'''

    connections = {}
    connection = {}
    brokerConnectionCount = 0
    connectionFilename = str(statusDir) + '/' + connectionStatusFile

    #print 'Reading connection file %s' % (str(connectionFilename))

    #Parse the connection status file
    try:
        for line in open(connectionFilename,'r'):

            if line.startswith('connectionName') and 'ID' in line:
                brokerConnectionCount = brokerConnectionCount + 1
                connection['id'] = line.strip().split(' = ')[1]
                connections[connection['id']] = connection
            elif line.startswith('Slow'):
                connection = {}
                connection['slow'] = line.strip().split(' = ')[1]
            elif line.startswith('Blocked'):
                connection['blocked'] = line.strip().split(' = ')[1]
            elif line.startswith('Active'):
                connection['active'] = line.strip().split(' = ')[1]

    except IOError:
        print('There was a problem reading the connection status file %s: %s' % (str(connectionFilename),sys.exc_info()))
        raise

    print('Connection Information ===================================================================')
    if brokerConnectionCount != 0:
        print('    Connection count: ' + str(brokerConnectionCount))
    print(' ')
    for connection in connections.keys():
        thisConnect = connections[connection];
        #print thisConnect
        print("----CONNECTION " + thisConnect.get('id') + "----")
        print('    Slow: ' + thisConnect.get('slow') + ' Blocked: ' + thisConnect.get('blocked') + ' Active: ' + thisConnect.get('active'))
        print(' ')

def create_options():

    parser = mpcsutil.create_option_parser(usageText='chill_amq_status [--statusDir <directory> --keepFiles]')

    parser.add_option('-k','--keepFiles',action='store_true',dest='keepFiles',default=False,
                      help='Indicates that temporary status files should be kept.')
    parser.add_option('-s','--statusDir',action='store',dest='statusDir',type='string',default='/tmp',
                      help='Specifies where to write temporary status files. Defaults to /tmp.')

    return parser

def test():

    parser = create_options()
    parser.parse_args()

    #Make sure that ACTIVEMQ_HOME exists and set it if not
    try:
        amqDirectory = os.environ['ACTIVEMQ_HOME']
    except KeyError:
        print('The environment variable ACTIVEMQ_HOME is undefined.')
        print('It will be defaulted to /msop/mpcs/activemq-5.16.1')
        print()
        amqDirectory = '/msop/mpcs/activemq-5.16.1'

    #Set the defaults for no-argument operation
    outStream = 'stdout'

    status = _runAmqBrokerQuery(amqDirectory,parser.values.statusDir)

    if status == 0:
        brokerUp = _readBrokerFile(parser.values.statusDir)

    if brokerUp:
        status = _runAmqConnectionQuery(amqDirectory,parser.values.statusDir)

        if status == 0:
            _readConnectionFile(parser.values.statusDir)

        status = _runAmqTopicQuery(amqDirectory,parser.values.statusDir)

        if status == 0:
            _readTopicFile(parser.values.statusDir)

        status = _runAmqSubscriberQuery(amqDirectory,parser.values.statusDir)

        if status == 0:
            _readSubscriberFile(parser.values.statusDir)

    if brokerUp and not parser.values.keepFiles:
        os.remove(parser.values.statusDir + '/' + brokerStatusFile)
        os.remove(parser.values.statusDir + '/' + connectionStatusFile)
        os.remove(parser.values.statusDir + '/' + topicStatusFile)
        os.remove(parser.values.statusDir + '/' + subscriberStatusFile)

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
