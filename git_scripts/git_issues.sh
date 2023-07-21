#!/bin/bash

#
# Usage
#
function usage() {
	echo ""
	echo "git_issues.sh Usage:"
	echo ""
	echo "Usage: git_issues [closest_tag] [current_branch]"
	echo ""
	echo "  closest_tag:    Optionally specify the tag from which to display added commits."
	echo "                  If not specified, defaults to the closest (most recent) tag in the DAG."
	echo "  current_branch: Optionally specify the branch to which to compute the added commits."
	echo "                  If not specified, defaults to the current branch."
	echo ""
	echo "This script will compute the commits between the closest (most recent) tag in the DAG."
	echo "and the current branch. It is most useful in validating commits against build manifests."
	echo ""
	exit -1
}

if [ "$1X" == "-hX" ] || [ "$2X" == "-hX" ]; then
	usage
fi

if [ "$1X" != "X" ]; then
	closest_tag=$1
else
	closest_tag=$(git describe --abbrev=0 --tags)
#	closest_tag=${closest_tag%%Dev}Int
fi

if [ "$2X" != "X" ]; then
	current_branch=$2
else
	current_branch=$(git symbolic-ref -q HEAD)
	current_branch=${current_branch##refs/heads/}
	current_branch=${current_branch:-HEAD}
fi

echo
commitCmd="git log --oneline --no-merges refs/tags/${closest_tag}..origin/${current_branch}"
mergeCmd="git log --oneline --merges refs/tags/${closest_tag}..origin/${current_branch}"

echo "The JIRA's committed between ${closest_tag} and ${current_branch} are:"
echo
commits=`${commitCmd} | cut -d' ' -f2 | cut -d: -f1 | grep MPCS | sort | uniq`
if [ $? -ne 0 ]; then
	usage
fi

merges=`${mergeCmd} | grep from | cut -d' ' -f 7 | cut -d '/' -f 2 | sort | uniq`
if [ $? -ne 0 ]; then
	usage
fi

function printAndCount() {
	echo "$1"
	((uniqueJIRAs+=1))
}

uniqueJIRAs=0
for i in ${commits}; do
	found=0
	msg=""
	for j in ${merges}; do
		if [ "$i" == "$j" ]; then
			found=1
			printAndCount "${i}"
			break
		fi
	done
	if [ "${found}" == 0 ]; then
		msg="${i}: WARNING: $i does not have an associated MERGE"
		for j in ${merges}; do
			if [ "${i:0:5}" != "MPCS-" ] || [ "${j:0:5}" != "MPCS-" ]; then
				msg="${msg}: Malformed JIRA Key: COMMIT: ${i}/MERGE: ${j}"
			fi
			
			if [ "$i" == "${j:0:9}" ] || \
			   [ "$i" == "${j:0:10}" ]; then
				msg="${msg} -- Partial JIRA Keys Mismatch ($i != $j)"	
				break
			fi
		done
	fi
	if [ "X${msg}" != "X" ]; then
		printAndCount "${msg}"
	fi
done
echo "=========="
echo "${uniqueJIRAs} Total JIRA Count"
echo

