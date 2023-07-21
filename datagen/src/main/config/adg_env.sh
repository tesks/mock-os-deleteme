# This is a list of all environment variables used to launch ADG applications.
# The are set to default to AMMOS CSE locations, but may be overridden by sysadmins
# who need to override those locations, or provide alternatives in the case CSE
# is not provided with ADG.
# The variables here will be used if the paths they point to exist.  If not,
# ADG will fall back to system defaults.
export CSE_VERSION=1.6
export CSE_BASE=/opt/ammos/ampcs/cse/${CSE_VERSION}

# JRE_DIR and JDK_DIR are used to set AMPCS's JAVA_HOME.  If the JRE_DIR exists,
# it is used.  If not, the JDK_DIR is uses if it exists.  Finally, AMPCS will default
# to the JAVA_HOME already set in the system.
export JRE_DIR=/usr/lib/jvm/jre-1.8.0-openjdk
export JDK_DIR=/usr/lib/jvm/java-1.8.0-openjdk
export LD_LIBRARY_PATH=${CSE_BASE}/lib64:${LD_LIBRARY_PATH}
