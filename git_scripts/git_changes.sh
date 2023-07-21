#!/bin/sh

#
# Usage
#
function usage() {
	echo "Usage: "
}

first_revision=$1
second_revision=$2
jira_prefix="MPCS"

echo
echo "JIRAs"
echo "====="
git log -m --oneline --no-merges origin/${first_revision}..origin/${second_revision}  | cut -d" " -f2- | awk '{$1=$1};1' | grep "^[ \t]*${jira_prefix}-*[0-9]\+" | sed "s/\(${jira_prefix}\)-*\([0-9]*\)[ -:]*/\1-\2: /g" | sort

echo
echo "NON-JIRAs"
echo "========="
git log -m --oneline --no-merges origin/${first_revision}..origin/${second_revision} | awk '{$1=$1};1' | grep -v "^.* ${jira_prefix}-*[0-9]\+"
