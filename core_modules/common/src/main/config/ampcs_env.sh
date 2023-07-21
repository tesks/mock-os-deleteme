# This is a list of all environment variables used to launch AMPCS applications.
# The are set to default to AMMOS CSE locations, but may be overridden by sysadmins
# who need to override those locations, or provide alternatives in the case CSE
# is not provided with AMPCS.
# The variables here will be used if the paths they point to exist.  If not,
# AMPCS will fall back to system defaults.
export CSE_VERSION=2.7
export CSE_BASE=/opt/ammos/ampcs/cse/${CSE_VERSION}

# JRE_DIR and JDK_DIR are used to set AMPCS's JAVA_HOME.  If the JRE_DIR exists,
# it is used.  If not, the JDK_DIR is uses if it exists.  Finally, AMPCS will default
# to the JAVA_HOME already set in the system.
export JRE_DIR=/usr/lib/jvm/jre-1.8.0-openjdk
export JDK_DIR=/usr/lib/jvm/java-1.8.0-openjdk
export LD_LIBRARY_PATH=${CSE_BASE}/lib64:${LD_LIBRARY_PATH}
export PYTHON_CMD=python3

export PYTHONPATH=${CSE_BASE}/lib64/python3.9/site-packages:${CSE_BASE}/lib/python3.9/site-packages:${PYTHONPATH}
export PERL_CMD=perl
# MPCS-10794: Change ACTIVEMQ_HOME default pointer to CSE installation
export ACTIVEMQ_HOME=/opt/ammos/ampcs/cse/services/${CSE_VERSION}/activemq
export DSNE_LIB=/sfoc/classes
# AMPCS's GUIs do not currently work with GTK3 - the following makes the SWT GUI library use GTK2
export SWT_GTK3=0

# MPCS-12536: Add the MMCS CTS (Command Translation Subsystem) & CMD native libraries to the LD_LIBRARY_PATH.
export LD_LIBRARY_PATH=/ammos/tcu/lib:/ammos/cts/lib:/ammos/ttc/ro/lib:/ammos/ttc/ro/lib/tcs:/ammos/sss/lib:/usr/local/lib64:/usr/local/lib:/usr/lib64:/usr/lib:/lib64:/lib:${LD_LIBRARY_PATH}

# MPCS-10660: New environment variable used to customize mission names
export CHILL_MISSION=

# Minimum and maximum JVM heap size can be overridden with the following pattern
#CHILL_MIN_HEAP_<script_name>=<value>
#CHILL_MAX_HEAP_<script_name>=<value>
# Examples for the chill_monitor application:
#CHILL_MAX_HEAP_chill_monitor=512m
#CHILL_MAX_HEAP_chill_monitor=4096m
