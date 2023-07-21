#!/tps/bin/python

'''********************************************************************************************
Description:  Retrieves the version information in the pom.xml and release.properties file
              from the last build.  Prints this info to the console, and saves it to a file.
Date:         Initial revision:  11/08/2014

*********************************************************************************************'''
# get_pom_versions.py

import sys
import re
import os
import fnmatch
import subprocess

# Set up environment

buildRoot = os.environ['AMPCS_WORKSPACE_ROOT']
outFile = os.environ['HOME'] + '/get_pom_versions.out'
projects = ['ampcs-super-pom', 'generic', 'datagen']

# Find the version in the "pom identification" block in each file.
# Remove the '-SNAPSHOT' extension.

blockPat = r'.*POM Identification.*'
blockC = re.compile(blockPat,re.I)
corePat = r'\s*<version>(\d+\.\d+\.\d+B\d+)-SNAPSHOT</version>\s*'
coreC = re.compile(corePat,re.I)
msnPat  = r'\s*<version>(\d+\.\d+\.\d\_\d\.\d\.\dB\d+)-SNAPSHOT</version>\s*'
msnC = re.compile(msnPat,re.I)

projDict = {}
cont = 0
for msn in projects:
    if 'super' in msn:
        continue
    if 'core' in msn:
        file = buildRoot + '/core' + '/ampcs-super-pom.xml'
    else:
        file = buildRoot + '/' + msn + '/pom.xml'
    print 'Fetching pom identification number in ' + file

    foundID = 0
    foundBlock = 0
    superLines = open(file, 'r')
    for i in superLines:
        m = blockC.match(i)
        if m:
            foundBlock = 1
            i = superLines.next()
            while not ('<properties>' in i):
                i = superLines.next()
                if 'core' in msn:
                    m = coreC.match(i)
                else:
                    m = msnC.match(i)
                if m:
                    foundID = 1
                    ver = m.group(1)
                    projDict[msn] = ver
                else:
                    if msn == 'datagen':
                        m = coreC.match(i)
                        if m:
                            foundID = 1
                            ver = m.group(1)
                            projDict[msn] = ver
        
        if foundID == 1:
            break
    superLines.close()

    if foundID == 0:
        print '   Warning:  could not find AMPCS version in POM Identification block'
        cont += 1

if cont > 0:
    print
    print ' Please check pom files before continuing with the build ...'
    print
    sys.exit(1)

print "\nVersion values from the pom.xml files, minus the '-SNAPSHOT' extension"
print "(note that they're also stored in file ~/get_pom_versions.out)"
print "Enter these numbers in response to 'mvn release:prepare...' command:\n"
f = open(outFile, 'w')
for i in projects:
    if 'super' in i:
        line = i + ' = ' + projDict['core_dictionary']
    else:
        line = i + ' = ' + projDict[i]
    print line
    f.write(line + '\n')
f.close()

print
