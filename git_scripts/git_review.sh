#!/bin/sh

# Script used to force a Collaborator review when one is not created automaticallly
#
# Usage
#
function usage() {
	echo ""
	echo "git_review.sh Usage:"
	echo ""
	echo "Usage: git_review <pull-request-target> <pull-request-source> [<review-number>]"
	echo ""
	echo "  pull-request-target: The branch into which the Pull Request will be merged"
	echo "  pull-request-source: The branch that will be merged into the pull-request-target"
	echo "  review-number      : The review number to add the files to (defaults to \"new\")"
	echo ""
	echo "This script generates a Collaborator review for changes made on a feature branch (pull-request-source)"
	echo "since the branch was created or last merged with the basis branch (pull-request-target)"
	echo ""
	exit
}

#
# Locate JAVA
#
if [ "X${JAVA_HOME}" == "X" ]; then
	echo "ERROR: JAVA_HOME is not set. Please set it to point to the installation directory of your Java VMs."
	exit -1
fi 
JAVA_GLOBAL_OPTIONS=""
JAVA="${JAVA_HOME}/bin/java"
if [[ ! -x ${JAVA} ]]; then
	echo "ERROR: The file \"${JAVA}\" is not executable. Check that JAVA_HOME is set correctly. It is currently set to \"${JAVA_HOME}\""
	exit -1
fi

#
# Locate ccollab
#
if [ "X${CCOLLAB_INSTALLATION}" == "X" ]; then
	echo "CCOLLAB_INSTALLATION is not set. Please set it to point to the installation directory of your Collaborator command line client application."
	exit -1
fi
CCOLLAB_GLOBAL_OPTIONS="--no-browser --scm accurev --debug"
CCOLLAB="${CCOLLAB_INSTALLATION}/ccollab"
if [[ ! -x ${CCOLLAB} ]]; then
	echo "ERROR: The file \"${CCOLLAB}\" is not executable. Check that CCOLLAB_INSTALLATION is set correctly. It is currently set to \"${CCOLLAB_INSTALLATION}\""
	exit -1
fi

if [ "X${1}" == "X" ]; then
	echo "ERROR: The pull-request-target must be specified as the first argument on the command line"
	usage
fi

if [ "X${2}" == "X" ]; then
	echo "ERROR: The pull-request-source must be specified as the second argument on the command line"
	usage
fi

REVIEW_NUMBER="new"
if [ "X${3}" != "X" ]; then
	REVIEW_NUMBER=${3}
fi

#
# Get command line arguments
#
MERGE_BASIS="origin/${1}"
MERGE_BRANCH="origin/${2}"
BASIS_BRANCH=`git merge-base --all ${MERGE_BASIS} ${MERGE_BRANCH}`
GIT_CMD="git diff --name-only ${BASIS_BRANCH} ${MERGE_BRANCH}"
CCOLLAB_CMD="${CCOLLAB} addgitdiffs ${REVIEW_NUMBER} ${BASIS_BRANCH} ${MERGE_BRANCH}"

echo "REVIEW_NUMBER=${REVIEW_NUMBER}"
echo "MERGE_BASIS=$MERGE_BASIS"
echo "MERGE_BRANCH=$MERGE_BRANCH"
echo "BASIS_BRANCH=$BASIS_BRANCH"
echo "${GIT_CMD}"
echo "${CCOLLAB_CMD}"
${GIT_CMD}
${CCOLLAB_CMD}
