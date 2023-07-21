#! /bin/bash
#
# Script used by the Jenkins Baseline / Adaptation Builds

# 05/16/2016  Disabled the 'force' option. All it did was prevent reversion after
# an error was detected. This can still be accomplished manually by issuing a '-X' option
# immediately after a failed run.
#
# TODO: Fix so that false failure is not reported in the first place.
#
dateSuffix=`date "+%Y%m%d_%H%M%S"`

function usage() {
	echo "set_pom_version Usage:"
	echo "Usage: create_micro_adaptation -s | -r"
	echo "  -s<version>: Set SNAPSHOT VERSION"
	echo "  -r<version>: Set Release Version"
	echo "  -X         : Revert last operation"
#	echo "  -F         : Force -- override auto-revert on error"
	echo "  -n         : Dry-Run -- check for consistent numbering, but change nothing"
	echo "  -h         : Show this help message"
	echo ""
	echo "NOTE: "
	echo "  set_pom_versios.sh requires two invocations to change a version number from"
	echo "  a SNAPSHOT version to a different SNAPSHOT version, or from a  RELEASE"
	echo "  versionto a different RELEASE version."
	echo ""
	echo "  EXAMPLES:"
	echo "     For RELEASE:"
	echo "       set_pom_versons.sh -r 8.0.0B1-SNAPSHOT"
	echo "       (Searches for all 8.0.0B1-SNAPSHOT versions and converts them to"
	echo "        8.0.0B1"
	echo "     For SNAPSHOT (incremental):"
	echo "       set_pom_version.sh -s 8.0.0B1 8.0.0B2"
	echo "       (Searches for all 8.0.0B1 versions and converts them to"
	echo "        8.0.0B2-SNAPSHOT"
	echo ""
 	exit
}

function getVersionRoot() {
	x=$(echo "${1}" | cut -d'B' -f 1)
	echo "${x}B"
}
function getBuildNumber() {
	x=$(echo "${1}" | cut -d'B' -f 2)
	echo "B${x}"
}

#
# Parse command linee
#
# TODO: Fix so that false failure is not reported in the first place.
force="TRUE" # Temporarily set force to "TRUE" as default to prevent reversion if errors detected.
dryrun="FALSE"
echo "test6"
while getopts "s:r:XhFn:" opt; do
	echo $opt
    case "$opt" in
		r)  operation="release"
			version=${OPTARG%%-SNAPSHOT}
			echo "REL"
			;;
		s)  operation="snapshot"
			version=${OPTARG%%-SNAPSHOT}
			echo "oooooo"
			;;
		X)  operation="revert"
			echo "eyyyy"
			;;
# 		F)	force="TRUE"
# 			echo "*** FORCING ***: Will promote version even if there are errors..."
# 			;;
		n)	dryrun="TRUE"
			echo "*** DRY RUN ***: checking for errors without making changes..."
			;;
		h)	echo "usagesdsds " 
			usage
			;;
		*)  echo "lastssdsds " 
			usage
			;;
    esac
done
shift $((OPTIND-1))
target=$1
buildRoot="$(getVersionRoot ${version})"
buildNumber="$(getBuildNumber ${version})"
targetBuildNumber="$(getBuildNumber ${target})"
finished="FALSE"
ok="FALSE"


while [ "X${finished}" == "XFALSE" ]
do
	case "$operation" in
		release)	echo "RELEASING: ${version}"
					stragglers=$(for i in `find $AMPCS_WORKSPACE_ROOT -name "*pom.xml" | grep -v versionBackup`; do
						result=$(grep "${buildRoot}" $i | grep -v "${buildNumber}-SNAPSHOT")
						if [ "X${result}" != "X" ]; then
							echo "$(echo ${i}): $(echo ${result}),"
						fi
					done)
					if [ "X" != "X${stragglers}" ]; then
						echo "The following files are inconsistently versioned to be released from '${version}-SNAPSHOT' to '${version}':"
						echo "${stragglers}" | tr , ' ' | sed 's/^/  /g'
						echo "Check the above files and manually edit to match '${version}-SNAPSHOT' before proceeding."
						echo "*** ERRORS ENCOUNTERED ***"
					else
						ok="TRUE"					
						echo "RELEASE is good to go!"
					fi
					
					if [ "X${dryrun}" == "XTRUE" ] || ([ "X${force}" == "XFALSE" ] && [ "X${ok}" == "XFALSE" ]); then
						echo "*** NO ACTION TAKEN ***"
						finished="TRUE"
						exit -1
					else
						#
						# Delete previous backups
						#
						for i in `find $AMPCS_WORKSPACE_ROOT -name "*pom.xml.versionBackup_*"`; do
							rm $i
						done

						#
						# Save latest backup
						#
						before=$(for i in `find $AMPCS_WORKSPACE_ROOT -name "*pom.xml" | grep -v versionBackup`; do
							cp $i $i.versionBackup_$dateSuffix
							cmd="<version>.*${version}-SNAPSHOT</version>" 
							egrep "$cmd" $i
						done | wc -l)
				
						if [ $before -eq 0 ]; then
							echo "Cannot promote Version '${version}' to release. Could not find '${version}' in any pom.xml file"
							finished="TRUE"
							exit -1
						fi
				
						for i in `find $AMPCS_WORKSPACE_ROOT -name "*pom.xml" | grep -v versionBackup`; do
							cmd="s/\(<version>.*${version}\)-SNAPSHOT\(<\/version>\)/\1\2/g"
							sed -e $cmd $i > $i.sed
						done
				
						after=$(for i in `find $AMPCS_WORKSPACE_ROOT -name "*pom.xml" | grep -v versionBackup`; do
							cmd="<version>.*${version}</version>"
							egrep $cmd $i.sed
						done | wc -l)
				
						if [[ "X$before" != "X$after" ]]; then
							echo "Inconsistent version transition encountered. There were $before instances found, but only $after instances modified."
							echo "Rolling back changes."
							for i in `find $AMPCS_WORKSPACE_ROOT -name "*pom.xml" | grep -v versionBackup`
								do rm $i.sed $i.versionBackup_${dateSuffix}
							done
							finished="TRUE"
							exit -1
						fi
				
						for i in `find $AMPCS_WORKSPACE_ROOT -name "*pom.xml" | grep -v versionBackup`
							do mv $i.sed $i
						done
						echo $dateSuffix > $AMPCS_WORKSPACE_ROOT/pom_version_suffix
					
						stragglers=$(for i in `find $AMPCS_WORKSPACE_ROOT -name "*pom.xml" | grep -v versionBackup`; do
							result=$(grep "${buildRoot}" $i | grep -v "${buildNumber}")
							if [ "X${result}" != "X" ]; then
								echo "$(echo ${i}): $(echo ${result}),"
							fi
						done)
						if [ "X" != "X${stragglers}" ]; then
							echo "The following files are inconsistently versioned to be released from '${version}-SNAPSHOT' to '${version}':"
							echo "${stragglers}" | tr , ' ' | sed 's/^/  /g'
							echo "Error RELEASING version '${version}'."	
							operation=revert
							finished=${force}
							if [ "X${finished}" == "XTRUE" ]; then
								echo "'${version}' RELEASED with ERRORS!!! Check the above files and manually edit to match '${version}' before committing."
							else
								echo "RELEASE to '${version}' FAILED!!!"
							fi
							continue
						else
							echo "'${version}' RELEASED."
							finished="TRUE"
						fi
					fi
					;;
		snapshot)	echo "SNAPSHOTTING: '${version}' to '${target}-SNAPSHOT'"
					stragglers=$(for i in `find $AMPCS_WORKSPACE_ROOT -name "*pom.xml" | grep -v versionBackup`; do
						result=$(grep "${buildRoot}" $i | grep -v "${buildNumber}")
						if [ "X${result}" != "X" ]; then
							echo "$(echo ${i}): $(echo ${result}),"
						fi
						result=$(grep "${buildRoot}" $i | grep '\-SNAPSHOT')
						if [ "X${result}" != "X" ]; then
							echo "$(echo ${i}): $(echo ${result}),"
						fi
					done)
					if [ "X" != "X${stragglers}" ]; then
						echo "The following files are inconsistently versioned to snapshot from '${version}' to '${target}-SNAPSHOT':"
						echo "${stragglers}" | tr , ' ' | sed 's/^/  /g'
						echo "Check the above files and manually edit to match '${version}' before proceeding."
						echo "*** ERRORS ENCOUNTERED ***"
					else
						ok="TRUE"					
						echo "SNAPSHOT is good to go!"
					fi
					if [ "X${dryrun}" == "XTRUE" ] || ([ "X${force}" == "XFALSE" ] && [ "X${ok}" == "XFALSE" ]); then
						echo "*** NO ACTION TAKEN ***"
						finished="TRUE"
						exit -1
					else
						#
						# Delete previous backups
						#
						for i in `find $AMPCS_WORKSPACE_ROOT -name "*pom.xml.versionBackup_*"`; do
							rm $i
						done

						#
						# Save latest backup
						#
						before=$(for i in `find $AMPCS_WORKSPACE_ROOT -name "*pom.xml" | grep -v versionBackup`; do
							cp $i $i.versionBackup_$dateSuffix
							cmd="<version>.*${version}</version>" 
							egrep "$cmd" $i
						done | wc -l)
						
						if [ $before -eq 0 ]; then
							echo "Cannot promote Version '${version}' to '${target}-SNAPSHOT'. Could not find '${version}' in any pom.xml file"
							finished="TRUE"
							exit -1
						fi
				
						for i in `find $AMPCS_WORKSPACE_ROOT -name "*pom.xml" | grep -v versionBackup`; do
							cmd="s/\(<version>.*\)${version}\(<\/version>\)/\1${target}-SNAPSHOT\2/g"
							sed -e $cmd $i > $i.sed
						done
				
						after=$(for i in `find $AMPCS_WORKSPACE_ROOT -name "*pom.xml" | grep -v versionBackup`; do
							cmd="<version>.*${target}-SNAPSHOT</version>"
							egrep $cmd $i.sed
						done | wc -l)
				
						if [[ "X$before" != "X$after" ]]; then
							echo "Inconsistent version transition encountered. There were $before instances found, but only $after instances modified."
							echo "Rolling back changes."
							for i in `find $AMPCS_WORKSPACE_ROOT -name "*pom.xml" | grep -v versionBackup`
								do rm $i.sed
							done
							finished="TRUE"
							exit -1
						fi
				
						for i in `find $AMPCS_WORKSPACE_ROOT -name "*pom.xml" | grep -v versionBackup`
							do mv $i.sed $i
						done
						echo $dateSuffix > $AMPCS_WORKSPACE_ROOT/pom_version_suffix
						
						stragglers=$(for i in `find $AMPCS_WORKSPACE_ROOT -name "*pom.xml" | grep -v versionBackup`; do
							result=$(grep "${buildRoot}" $i | grep -v "${targetBuildNumber}")
							if [ "X${result}" != "X" ]; then
								echo "$(echo ${i}): $(echo ${result}),"
							fi
							result=$(grep "${buildRoot}" $i | grep -v '\-SNAPSHOT')
							if [ "X${result}" != "X" ]; then
								echo "$(echo ${i}): $(echo ${result}),"
							fi
						done)
						
						if [ "X" != "X${stragglers}" ]; then
							echo "The following files are inconsistently versioned to snapshot from '${version}' to '${target}-SNAPSHOT':"
							echo "${stragglers}" | tr , ' ' | sed 's/^/  /g'
							echo "Failed to SNAPSHOT '${version}' to '${target}-SNAPSHOT'."	
							operation=revert
							finished=${force}
							if [ "X${finished}" == "XTRUE" ]; then
								echo "'${version}' SNAPSHOTTED with ERRORS!!! Check the above files and manually edit to match '${target}-SNAPSHOT' before committing."
							else
								echo "SNAPSHOTING to '${version}' FAILED!!!"
							fi
							continue
						else
							echo "'${version}' SNAPSHOTTED to '${target}-SNAPSHOT'."
							finished="TRUE"
						fi
					fi
					;;
		revert)		echo "REVERTING..."
					if [ "X${dryrun}" != "XTRUE" ]; then
						if [ ! -e $AMPCS_WORKSPACE_ROOT/pom_version_suffix ]; then
							echo "Cannot revert version number promotion. Previous file data does not exist."
							finished="TRUE"
							exit -1
						fi
				
						lastDateSuffix=`cat $AMPCS_WORKSPACE_ROOT/pom_version_suffix`
						for i in `find $AMPCS_WORKSPACE_ROOT -name "*pom.xml" | grep -v versionBackup`; do
							mv $i.versionBackup_$lastDateSuffix $i
						done
						
						rm $AMPCS_WORKSPACE_ROOT/pom_version_suffix
						echo "Last promotion REVERTED."
						finished="TRUE"
					else
						echo "*** NO ACTION TAKEN ***"
						finished="TRUE"
						exit -1
					fi
					;;
		*)			usage
					;;
	esac
done
