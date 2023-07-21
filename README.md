# AMPCS installation Instructions

## Overview
The AMMOS Mission Data Processing and Control System (AMPCS) is a Java and Python based software product set that can perform spacecraft commanding and telemetry processing in a variety of environments. It includes capabilities for the following:

- Reading project command and telemetry dictionaries
- Processing downlink telemetry in a variety of formats
- Archiving both raw and processed telemetry
- Querying and plotting of archived data
- Viewing telemetry and telemetry status in real-time
- Reconstructing engineering and science data product files
- Building commands
- Controlling the uplink of commands and files
- Archiving command logs
- Responding to telemetry and command events in real-time

## Requirements
Supported OS: 
- Red Hat Enterprise Linux 8.5 
- Red Hat Enterprise Linux 8.7
- Red Hat Enterprise Linux 9

Required Software:
- Java 1.8 only (future versions not supported yet)
- Python 3.9+
- ActiveMQ 5.16.1 (recommended since it works with java 1.8)
- MariaDb 10.3+
- Maven 3.6+

AMPCS Dependencies:

In order for the uplink functionality to run, we need the following dependencies to be installed, that will be provided separately:
These will be installed in the /ammos directory by default.
- CTS
- SLINC II
- TCU
- SSS
- GDSUSER

## Install Java 8
https://access.redhat.com/documentation/en-us/openjdk/8/html-single/installing_and_using_openjdk_8_for_rhel/index
### Install wget
```
sudo dnf search wget
sudo dnf install wget
```
### Download Java 8 tar.gz file
```
cd /opt
sudo mkdir java
cd java
sudo wget https://builds.openlogic.com/downloadJDK/openlogic-openjdk/8u362-b09/openlogic-openjdk-8u362-b09-linux-x64.tar.gz
```
### Extract Java
```
sudo tar zxvf openlogic-openjdk-8u362-b09-linux-x64.tar.gz
sudo mv openlogic-openjdk-8u362-b09-linux-x64 openjdk-1.8
```
### Setup environment variables in the ~/.bash_profile file
Add the following variables to the `~/.bash_profile` file
```
export JAVA_HOME=/opt/java/openjdk-1.8
export PATH=$PATH:$HOME/bin:$JAVA_HOME/bin
```
Once you added these variables to the ~/.bash_profile file, reload the environment
```
source ~/.bash_profile
```
### Let the operating system know where Java is and which version to use
```
sudo update-alternatives --install "/usr/bin/java" "java" "/opt/java/openjdk-1.8/bin/java" 1
sudo update-alternatives --install "/usr/bin/javac" "javac" "/opt/java/openjdk-1.8/bin/javac" 1
sudo update-alternatives --install "/usr/bin/java" "javaws" "/opt/java/openjdk-1.8/bin/javaws" 1

sudo update-alternatives --set java /opt/java/openjdk-1.8/bin/java
sudo update-alternatives --set javac /opt/java/openjdk-1.8/bin/javac
sudo update-alternatives --set javaws /opt/java/openjdk-1.8/bin/javaws
```
Running `java -version` should now work
```
java -version
openjdk version "1.8.0_362-362"
OpenJDK Runtime Environment (build 1.8.0_362-362-b09)
OpenJDK 64-Bit Server VM (build 25.362-b09, mixed mode)
```

## Install ActiveMQ
See https://www.tecmint.com/install-apache-activemq-on-centos-rhel/

Replace activemq.xml under <activemq>/conf/ with the one provided with AMPCS 

### is ActiveMQ running?
```
netstat -na|grep 61614
```

### Download ActiveMQ tar.gz file
```
cd /opt
sudo mkdir activemq
cd activemq
sudo wget https://archive.apache.org/dist/activemq/5.16.1/apache-activemq-5.16.1-bin.tar.gz
```
### Extract ActiveMQ 
```
sudo tar zxvf apache-activemq-5.16.1-bin.tar.gz
```

### Start ActiveMQ
```
cd /opt/activemq/apache-activemq-5.16.1/bin
./activemq start
```

## Python 3.9+
- Python 3.9 should be already installed in RHEL 9
- For RHEL 8+, upgrade Python to 3.9
```
sudo dnf install -y python39
sudo update-alternatives --config python3
# Select Python 3.9
python3 --version
```

## Install git and clone AMPCS
```
sudo dnf install -y git
# clone AMPCS repository
cd ~
git clone https://github.com/NASA-AMMOS/AMPCS.git
```

## Install other auxiliary tools
```
sudo dnf install -y nc
# telnet is needed to check if ActiveMQ is alive when running chill_down
sudo dnf install -y telnet 
sudo dnf whatprovides netstat
sudo dnf install -y net-tools
```

## Install Maven

### Download Maven tar.gz file
```
cd /opt
sudo mkdir maven
cd maven
sudo wget https://archive.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
```
### Extract Maven
```
sudo tar zxvf apache-maven-3.6.3-bin.tar.gz
sudo ln -s apache-maven-3.6.3 maven
```
### Setup environment variables in the ~/.bash_profile file
Add the following variables to the `~/.bash_profile` file
```
export M2_HOME=/opt/maven/maven
export PATH=${M2_HOME}/bin:${PATH}
```

Once you added these variables to the `~/.bash_profile` file, read and execute the file
```
source ~/.bash_profile
```

Running `mvn -version` should now work
```
mvn -version
Maven home: /opt/maven/maven
Java version: 1.8.0_362-362, vendor: OpenLogic-OpenJDK, runtime: /opt/java/openjdk-1.8
```

## Install MariaDB

---
https://www.cyberciti.biz/faq/install-mariadb-on-rhel-8-redhat-enterprise-linux/
```
sudo dnf install -y mariadb-server
sudo systemctl start mariadb.service
sudo systemctl enable mariadb.service  # can stop, start, restart, or status
sudo mysql_secure_installation  # secure mariaDB. this is mandatory
```

Follow this to wipe out /var/lib/mysql content, then start mariadb:
https://support.plesk.com/hc/en-us/articles/360026031354-Cannot-start-MariaDB-on-Plesk-server-the-directory-var-lib-mysql-is-not-empty-so-initialization-cannot-be-done

```
systemctl stop mariadb
mkdir /var/lib/mysql_bk && mv /var/lib/mysql/* /var/lib/mysql_bk
systemctl start mariadb
systemctl stop mariadb
```

## Build AMPCS
```
cd ~/AMPCS
mvn -DskipTests -DskipPythonTests clean install 
```
At this point, ~/AMPCS/adaptations/generic/dist/generic should exist.

### Clean test JARs
```
dev_scripts/bash/clean_test_jars.sh ~/AMPCS
```

## Settings to put in the `~/.bash_profile` file
```
export ACTIVEMQ_HOME=/opt/activemq/apache-activemq-5.16.1
export PATH=$ACTIVEMQ_HOME/bin:$PATH
export AMPCS_WORKSPACE_ROOT=~/AMPCS
export CHILL_GDS=${AMPCS_WORKSPACE_ROOT}/adaptations/generic/dist/generic
```
Once you added these variables to the ~/.bash_profile file, read and execute the file
```
source ~/.bash_profile
```

## Install Python modules
```
sudo dnf install python3-pip -y
cd $CHILL_GDS/lib/python/
pip3 install -r ampcs_requirements.txt
```

## Create databases

### Create mission database
```
$CHILL_GDS/bin/admin/chill_grant_mission_permissions -u root -p

# y
# y
# ERT

$CHILL_GDS/bin/admin/chill_create_mission_database -u root -p
# blank
# 0
# y
# y
# no password
```

### Create unit test database
```
$CHILL_GDS/bin/admin/chill_grant_unit_test_permissions -u root -p
$CHILL_GDS/bin/admin/chill_create_unit_test_database -u root -p
```

### Create PDPP database - if needed
```
$CHILL_GDS/bin/admin/chill_grant_pdpp_permissions -u root -p
$CHILL_GDS/bin/admin/chill_create_pdpp_database -u root -p
```

## Configuring AMPCS
AMPCS configuration can be changed in ~/CHILL/ampcs.properties.

These properties will ovveride the defaults that are set in .properties files across the code.

As an example, setting the location of the CXML compiler, based on the default mentioned above in AMPCS Dependencies
```
command.dictionary.cts.compiler.path=/ammos/cts/bin/cxml
```
