#!/tps/bin/perl
##############################################################
#
# Filename:  chill_maketar.p
#
# Purpose: Create a tarball of chill files per mission
# Script Used by the Jenkins deploy build helpers
#
##############################################################

$| = 1;  #set auto flush 

use Getopt::Std;
use File::Find;
use Cwd;

%options=();
getopts("m:v:k:h",\%options);

if (defined $options{h})
{
   print("\nchill_maketar.p <options>\n\n");
   print("Command Line Arguments: \n\n");
   print("-m <mission>        The name of the mission to tar up (e.g \"msl\"). Can also be \"mpcstools\".\n");
   print("-v <mpcsVersion>    The MPCS version in the form X.Y.Z\n");
   print("-k <mpcstoolsVersion>    The MPCSTools version in the form X.Y.Z.A (X.Y.Z should match MPCS version).\n");
   print("-h                  Display this help text.\n\n");
   print("Environment Variables: \n\n");
   print("CM_DIR - Points to the current CM directory. Defaults to /home/mpcsbuild/cm/git_builds/AMPCS_dev\n\n");
   exit(0);
}

#######################################
# Parse command line options
#######################################

#Parse mission
if (!defined $options{m})
{
   REDO_MISSION: print 'Enter mission: ';
   $mission = <STDIN>;
   chomp $mission;
   if ($mission eq "")
   {
      goto REDO_MISSION;
   }
}
else
{
   $mission = $options{m};
}

#Parse MPCS version
if (!defined $options{v})
{
	REDO_VERSION: print 'Enter MPCS version X.Y.Z: ';
    $mpcsVersion = <STDIN>;
	chomp $mpcsVersion;
	if ($mpcsVersion eq "")
	{
		goto REDO_VERSION;
	}
}
else
{	
	$mpcsVersion = $options{v};
}

#Parse MPCSTools version
if (!defined $options{k})
{
	REDO_VERSION: print 'Enter MPCS Tools version X.Y.Z.W: ';
    $mpcstoolsVersion = <STDIN>;
	chomp $mpcstoolsVersion;
	if ($mpcstoolsVersion eq "")
	{
		goto REDO_VERSION;
	}
}
else
{	
	$mpcstoolsVersion = $options{k};
}

#Parse CM Dir
$cmDir = $ENV{CM_DIR};
if ($cmDir eq "")
{
	$cmDir = "/home/mpcsbuild/cm/git_builds/AMPCS_dev";
}

$distbase = "$cmDir/$mission/dist";
print "$distbase\n";
$mpcsDist = "$distbase/$mission";
$mpcstoolsDist = "$distbase/mpcstools/$mission/mpcstools";
$pwd = $ENV{PWD};

#Check if dist exists
if (!-e $mpcsDist)
{
   print "Directory does not exist: $mpcsDist\n";
   exit;
}   

print "CM Dir = $cmDir\n"; 
print "Mission = $mission\n";
print "MPCS Version = $mpcsVersion\n";
print "MPCSTools Version = $mpcstoolsVersion\n";

#######################################
# Create output filenames
#######################################

$mpcsReleaseName = "mpcs-". "$mpcsVersion";
$mpcsRoReleaseName = $mpcsReleaseName . "_".$mission . "_ro";
$mpcsRwReleaseName = $mpcsReleaseName . "_".$mission . "_rw";
$mpcsTarFileName = $mpcsReleaseName . "_" . $mission. ".tar";
$mpcsRwTarFileName = $mpcsRwReleaseName . ".tar";
	
$mpcstoolsReleaseName = "mpcstools-". "$mpcstoolsVersion";
$mpcstoolsRwReleaseName = $mpcstoolsReleaseName . "_".$mission . "_rw";
$mpcstoolsRwTarFileName = $mpcstoolsRwReleaseName . ".tar";

#######################################
# Do the r/o portion for MPCS
#######################################

mkdir $mpcsReleaseName;
chdir $mpcsReleaseName;

print ("cp -r $mpcsDist/bin .\n");
system ("cp -r $mpcsDist/bin .");
print ("cp -r $mpcsDist/lib .\n");
system ("cp -r $mpcsDist/lib .");
print ("cp -r $mpcsDist/schema .\n");
system ("cp -r $mpcsDist/schema .");
print ("cp -r $mpcsDist/tps .\n");
system ("cp -r $mpcsDist/tps .");

# Added by Ashley Shamilian for MPCS-7676 JIRA
if($mission=="generic"){
print ("cp -r $mpcsDist/examples .\n");
system ("cp -r $mpcsDist/examples .");
}


# Added by Lori Nakamura, to remove the bin/test subdirectory and lib/*-tests.jar files
# for Maven "release builds"

print "remove bin test subdirectory\n";
$test_dir  = $pwd ."/". $mpcsReleaseName . "/bin/test";
$cmd = "rm -r $test_dir";
print "$cmd\n";
system ("$cmd");

print "remove lib test jars\n";
$test_dir  = $pwd ."/". $mpcsReleaseName . "/lib";
$cmd = "rm $test_dir/*-tests.jar";
print "$cmd\n";
system ("$cmd");

# Added by Ashley Shamilian, to remove lib/mission/tests.jar files
print "remove lib/mission test jars\n";
$test_dir  = $pwd ."/". $mpcsReleaseName . "/lib";
$cmd = "rm $test_dir/$mission/*-tests.jar";
print "$cmd\n";
system ("$cmd");

# Added by Ashley Shamilian to include the REAMDE.TXT file in the tar for Export Requirement (MPCS-2627)
print ("cp -r $mpcsDist/../../../core/README.TXT .\n");
system ("cp -r $mpcsDist/../../../core/README.TXT .");

# Added by Lori Nakamura, to include the dist.properties file needed for adg_convert_evr_xml (MPCS-5575)
	
$cmd = "cp -r $mpcsDist/dist.properties .";
print "$cmd\n";
system ("$cmd");

#create links for the directories in rw
system ("ln -s ../../../mpcs_rw/$mission/$mpcsReleaseName/config config");
system ("ln -s ../../../mpcs_rw/$mission/$mpcsReleaseName/templates templates");
system ("ln -s ../../../mpcs_rw/$mission/$mpcsReleaseName/test test");

chdir "./bin";
 
system ("rm -rf mpcstools");
# system ("ln -s ../../../../mpcs_rw/$mission/mpcstools/$mpcsReleaseName/current/bin mpcstools");

chdir "../lib";
 
system ("rm -rf python");
system ("ln -s ../../../../mpcs_rw/$mission/mpcstools/$mpcsReleaseName/current/lib/python python");

system ("rm -rf perl");
system ("ln -s ../../../../mpcs_rw/$mission/mpcstools/$mpcsReleaseName/current/lib/perl perl");

chdir $pwd;
print "Create ro tarfile: tar cf $mpcsTarFileName $mpcsReleaseName\n";
system ("tar cf $mpcsTarFileName $mpcsReleaseName");

#system ("mv $mpcsReleaseName $mpcsRoReleaseName");
system ("rm -rf $mpcsReleaseName");

#######################################
# Do the r/w portion for MPCS
#######################################
mkdir ($mpcsReleaseName);
chdir ($mpcsReleaseName);
print "remove ro bin and lib directories\n";
$bin_dir  = $pwd ."/". $mpcsReleaseName . "/bin";
$lib_dir  = $pwd ."/". $mpcsReleaseName . "/lib";
rmdir $bin_dir;
rmdir $lib_dir;
print ("cp -r $mpcsDist/config .\n");
system ("cp -r $mpcsDist/config .");
print ("cp -r $mpcsDist/templates .\n");
system ("cp -r $mpcsDist/templates .");
print "Create test link\n";

#Create test link
system ("ln -s /data/msop/mpcs_rw/$mission/test test");

chdir ($pwd);

print "Create rw tarfile: tar cf $mpcsRwTarFileName $mpcsReleaseName\n";
system ("tar cf $mpcsRwTarFileName $mpcsReleaseName");
system ("rm -rf $mpcsReleaseName");

#######################################
# Do the rw portion for MPCSTools
#######################################
mkdir ($mpcsReleaseName);
chdir ($mpcsReleaseName);
mkdir ($mpcstoolsReleaseName);
chdir ($mpcstoolsReleaseName);

print("cp -r $mpcstoolsDist/* .\n");
system("cp -r $mpcstoolsDist/* .");

print "remove test subdirectories\n";
find(\&wanted, '.');

chdir ($pwd);

print "Create rw tarfile: tar cf $mpcstoolsRwTarFileName $mpcsReleaseName\n";
system ("tar cf $mpcstoolsRwTarFileName $mpcsReleaseName");
system ("rm -rf $mpcsReleaseName");

print "Done.\n";

sub wanted {

   if ( (-d $_) && ($_ eq 'test') ) {
      print "Removing $File::Find::name\n";
      $cmd = "rm -r $_";
      system ("$cmd");
   }
}
