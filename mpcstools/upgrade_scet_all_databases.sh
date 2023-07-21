#! /bin/bash

# MPCS-8612
# NB: You cannot use this script to populate the extended tables.

# Set to something other than "no" and nothing will actually be done
DEBUG=no

DQ='"'

PROJECT_FILE="mpcstools/devtools/database_support/MissionBuildSpec.txt"

if test ! -r "${PROJECT_FILE}"
then
    echo "Project file ${PROJECT_FILE} does not exist" 1>&2
    exit 1
fi

echo "Using project file ${PROJECT_FILE}"
echo

THIS_HOST=`uname -n`

echo -n "Enter host name (${THIS_HOST}):"
read HOST

if test -z "${HOST}"
then
    HOST="${THIS_HOST}"
fi

echo
echo "Host name will be ${HOST}"
echo

echo -n "Enter user name with permissions to perform database actions (root):"
read USER

if test  -z "${USER}"
then
    USER="root"
fi

echo
echo "User name will be ${USER}"
echo

echo -n "Enter password for that user, if any:"
read -s PASSWORD

echo

if test -n "${PASSWORD}"
then
    echo "There is a password"
else
    echo "There will be no password"
fi

echo

echo -n "Enter database name suffix, if any:"
read SUFFIX

echo

if test -n "${SUFFIX}"
then
    echo "Database suffix will be ${DQ}${SUFFIX}${DQ}"
else
    echo "There will be no database suffix"
fi

echo

echo -n "Enter additional MySQL options, if any:"
read OPTIONS

echo

if test -n "${OPTIONS}"
then
    echo "Additional options are ${DQ}${OPTIONS}${DQ}"
else
    echo "There will be no additional options"
fi

TEMP="--host=${HOST} --user=${USER}"
DTEMP="${TEMP}"

if test -n "${PASSWORD}"
then
    TEMP="${TEMP} --password=${PASSWORD}"
    DTEMP="${DTEMP} --password=<suppressed>"
fi

if test -n "${OPTIONS}"
then
    TEMP="${TEMP} ${OPTIONS}"
    DTEMP="${DTEMP} ${OPTIONS}"
fi

export CHILL_MYSQL_OPTIONS="${TEMP}"

echo
echo "MySQL options to be used: ${DQ}${DTEMP}${DQ}"
echo

echo -n "Do you want to proceed? (y/n):"
read ANSWER

echo

if test "X${ANSWER}" != "Xy" -a "X${ANSWER}" != "XY"
then
    echo "Terminating without upgrading databases"
    exit 0
fi

echo "Upgrading all databases..."
echo

while read MISSION
do 
    if test -z "${MISSION}"
    then
        continue
    fi

    export CHILL_GDS=adaptations/${MISSION}/dist/${MISSION}

    if test -d ${CHILL_GDS}
    then
        echo $MISSION

        LOCATION=${CHILL_GDS}/bin/admin

        if test "X${DEBUG}" != "Xno"
        then
            echo "${SUFFIX} n y ${LOCATION}/chill_upgrade_extended_scet_mission_database"
            echo "${SUFFIX} n y ${LOCATION}/chill_upgrade_extended_scet_unit_test_database"

            continue
        fi

        echo -e "${SUFFIX}\nn\ny\n" | ${LOCATION}/chill_upgrade_extended_scet_mission_database   >/dev/null
        echo -e "${SUFFIX}\nn\ny\n" | ${LOCATION}/chill_upgrade_extended_scet_unit_test_database >/dev/null
    else
        echo "Skipping project ${MISSION}; no build found at ${CHILL_GDS}" 1>&2
    fi
done <${PROJECT_FILE}

echo
echo "Finished upgrading all databases"
