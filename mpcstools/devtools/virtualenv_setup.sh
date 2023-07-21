#!/bin/sh

# MPCS-6301 - 6/19/2014: Create this script to
# set up a standard python development environment for AMPCS
# python script development.
# The script will check to make sure three programs are installed:
# python, virtualenv, pip, and virtualenvwrapper.  It will
# also set up the .bash_profile for the virtualenvwrapper aliases
# and WORKON_HOME directory if allowed. 
# It will then create the virtualenv chill_env and install
# mpcstools dependencies within.


# 
# @arg $1 -> executable to check for
# @return zero if the executable can be found 
function executable_exists() {
	local executable_name=$1
	which "$executable_name" > /dev/null
    if ! which "$executable_name" > /dev/null ; then
        echo "Could not find $executable_name; Please install it."
        return 1
    fi
    return 0
}

# Check if a directory exists.  
# @param $1 - the directory to check
# return 0 if it does, non-zero if it doesn't
function dir_exists() {
	local dir_name=$1
	if [ -d "$dir_name" ]; then
		return 0
    fi
    return 1
}
 
 # Check if an env already exists, as indicated by the presences of bin/activate.
 # @param $1 - the directory the potential env lives in
 # @param $2 - the env name (i.e. the directory name that should not already be used)
 # Return 0 if the env DOES NOT exist.
 # Return non-zero if the env DOES exist
function env_does_not_exist {

	local env_parent_dir="$1"
	local env_name="$2"

	if ! dir_exists $env_parent_dir ; then
		echo "AMPCS_WORKSPACE_ROOT is not a directory.  It is set to $AMPCS_WORKSPACE_ROOT"
		return 1
	fi


	if [ -f "$env_name/bin/activate" ] ; then
		echo "Env already exists in location: $env_name"
		return 1
	fi
	return 0

}

# Assume the env is created, and install python packages within.
# @param $1- directory name of the directory the env lives inu
# @param $2 - the name of the env
# @return - whatever the pip execution returns
function install_dependencies() {
	local dir_name=$1
	local env_name=$2

	source "$dir_name/$env_name/bin/activate"

	local pip_cmd="pip install -r "
	local requirements_dir="$AMPCS_WORKSPACE_ROOT/core_$AMPCS_WORKSPACE_SUFFIX"
	requirements_dir="$requirements_dir/mpcstools/lib/python"
	local reqirements_file="ampcs_requirements.txt"
	
    # Adding to fix python building link error
    export ARCHFLAGS=-Wno-error=unused-command-line-argument-hard-error-in-future
	pip_cmd="$pip_cmd$requirements_dir/$reqirements_file"
	$pip_cmd

	return $?
}

# Do the actual env creation
# @param $1 - the directory to create the env win
# @return: whether installing dependencies in the env succeded
function create_env() {
	dir_name="$1"
	env_name="chill_env"
	if env_does_not_exist "$dir_name"  "$env_name" ; then
		virtualenv "$dir_name/$env_name"
		install_dependencies $dir_name $env_name
	fi
	return $?
}

# Configure virtualenvwrapper settings.  Check if WORKON_HOME is set in the .bash_profile.  
# Put standard location for it in if not. Also source the virtualenvwrapper.sh that defines
# commands like "workon".
function setup_virtualenvwrapper() {

	if grep "WORKON_HOME" "$HOME/.bash_profile" ; then
		echo "WORKON_HOME is already set in your bash_profile, but was not set in the shell. Please resolve this and retry this script."
		return 1
	fi

	echo "export WORKON_HOME=\$HOME/.virtualenvs" >> "$HOME/.bash_profile"
	export WORKON_HOME=\$HOME/.virtualenvs
	if ! grep "source \/usr\/local\/bin\/virtualenvwrapper.sh" "$HOME/.bash_profile" ; then
		echo "source /usr/local/bin/virtualenvwrapper.sh" >> "$HOME/.bash_profile"
		source /usr/local/bin/virtualenvwrapper.sh
	fi
	export WORKON_HOME="$HOME/.virtualenvs"
	source /usr/local/bin/virtualenvwrapper.sh
	return 0
}


# virtualenvwrapper is useful for developers and it'd make sense to have all developers use it to avoid awkward
# souce $env/bin/activate commands.  
# @param $1 - the value of WORKON_HOME to use with virtualenv wrapper.  If this is unset, then the function prompts
# 			the user to allow it to modify .bash_profile. If yes, virtualenvwrapper is setup.  If not, use AMPCS_WORKSPACE_ROOT
# 
function create_env_with_wrapper() {
	if [[ -z "$1" ]] ; then
		read -p "Ok to modify your ~/.bash_profile for virtualenvwrapper? (y/n)" response
		if [[ "X$response" = "Xy" ]] ; then
			if ! setup_virtualenvwrapper ; then
				return $?
			fi
		else 
			export WORKON_HOME="$AMPCS_WORKSPACE_ROOT"
		fi
	fi
	echo "WORKON_HOME is $WORKON_HOME"
	create_env "$WORKON_HOME"
	return $?
}

function main() {

	local ret_val="0"
	# Check if the required executables are installed
	for executable in python virtualenv pip
	do
		executable_exists "$executable"
		ret_val=`expr $ret_val + $?`
	done
	if [[ $ret_val -ne 0 ]] ; then
		echo "Exiting script; retry after installing the above program".
		exit 1
	fi

	# Check for virtualenvwrapper, which is not an executable
	ret_val=$?
	if [ -f /usr/local/bin/virtualenvwrapper.sh ] ; then
		create_env_with_wrapper "$WORKON_HOME"
	else
		echo "Please install virtualenvwrapper"
		exit 1
	fi
	exit $?
}

main
