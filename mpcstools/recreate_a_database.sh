#! /bin/bash

# MPCS-8612 New

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

echo -n "Enter mission name:"
read ONE_MISSION

if test -z "${ONE_MISSION}"
then
    echo "No mission entered"
    exit 1
fi

echo
echo "Mission name will be ${ONE_MISSION}"
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

echo -n "Enter the offset for this DB host (Developers can enter 0) (0):"
read OFFSET

if test -z "${OFFSET}"
then
    OFFSET=0
fi

echo

MATCH=`expr "${OFFSET}" : "[0-9][0-9]*"`

if test ${MATCH} -eq 0
then
    echo "Offset must be an integer from 0..255"
    exit 1
fi

if test ${OFFSET} -gt 255
then
    echo "Offset must be an integer from 0..255"
    exit 1
fi

echo "Offset will be ${OFFSET}"
echo

echo -n "Do you want to add the extra time indexes? (y/n):"
read EXTRA

echo

if test "X${EXTRA}" = "Xy" -o "X${EXTRA}" = "XY"
then
    EXTRA=y
    echo "Extra indexes will be added"
else
    EXTRA=n
    echo "Extra indexes will NOT be added"
fi

echo

ECDR=NONE

echo -n "Enter ECDR index type for applicable missions (ERT, SCET, NONE):"
read ECDR

ECDR=`echo $ECDR | tr "[a-z]" "[A-Z]"`

echo

if test "X$ECDR" != "XERT" -a "X$ECDR" != "XSCET" -a "X$ECDR" != "XNONE"
then
    echo "ECDR index type must be ERT or SCET or NONE" 1>&2
    exit 1
fi

if test "X$ECDR" != "XNONE"
then
    echo "ECDR index will be ${ECDR} (if required by mission)"
else
    echo "There will be no ECDR index"
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
    echo "Terminating without destroying and recreating databases"
    exit 0
fi

echo "Destroying and recreating database..."
echo

while read MISSION
do 
    if test -z "${MISSION}"
    then
        continue
    fi

    if test "X${MISSION}" != "X${ONE_MISSION}"
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
            echo "${SUFFIX} y ${LOCATION}/chill_destroy_unit_test_database"
            echo "${SUFFIX} y ${LOCATION}/chill_grant_unit_test_permissions"
            echo "${SUFFIX}   ${OFFSET} ${EXTRA} y ${ECDR} ${LOCATION}/chill_create_unit_test_database"

            echo "${SUFFIX} y ${LOCATION}/chill_destroy_mission_database"
            echo "${SUFFIX} y ${LOCATION}/chill_grant_mission_permissions"
            echo "${SUFFIX}   ${OFFSET} ${EXTRA} y ${ECDR} ${LOCATION}/chill_create_mission_database"

            continue
        fi

        echo -e "${SUFFIX}\ny\n" | ${LOCATION}/chill_destroy_unit_test_database                              >/dev/null
        echo -e "${SUFFIX}\ny\n" | ${LOCATION}/chill_grant_unit_test_permissions                             >/dev/null
        echo -e "${SUFFIX}\n${OFFSET}\n${EXTRA}\ny\n${ECDR}\n" | ${LOCATION}/chill_create_unit_test_database >/dev/null

        echo -e "${SUFFIX}\ny\n" | ${LOCATION}/chill_destroy_mission_database                                >/dev/null
        echo -e "${SUFFIX}\ny\n" | ${LOCATION}/chill_grant_mission_permissions                               >/dev/null
        echo -e "${SUFFIX}\n${OFFSET}\n${EXTRA}\ny\n${ECDR}\n" | ${LOCATION}/chill_create_mission_database   >/dev/null
    else
        echo "Skipping project ${MISSION}; no build found at ${CHILL_GDS}" 1>&2
    fi
done <${PROJECT_FILE}

echo
echo "Finished destroying and recreating all databases"
