#!/tps/bin/perl
##############################################################
#
# Filename:  chill_tarcm.p
#
# Purpose: Build a tarball release of MPCS 
#
#
##############################################################

$| = 1;  #set auto flush 

use Getopt::Std;

%options=();
getopts("d:v:i:k:oh",\%options);

if (defined $options{h})
{
   print("\nchill_tarcm.p <options>\n\n");
   print("Command Line Arguments: \n\n");
   print("-v <mpcsVersion>    The MPCS version in the form X.Y.ZBNN (e.g., 5.0.0B15)\n");
   print("-k <mpcstoolsVersion>    The MPCSTools version in the form X.Y.Z.A (X.Y.Z should match MPCS version).\n");
   print("-i <cmId>           The CM ID in the form MGSS-AMMOS-XX.YY.ZZZ-<etc>\n");
   print("-o                  Only build the MPCSTools CM tar. Do not tar up the MPCS mission code.\n");
   print("-h                  Display this help text.\n\n");
#   print("Environment Variables: \n\n");
#   print("CM_DIR - Points to the current CM directory. Defaults to /home/mpcsbuild/cm/builds/Int_test.\n\n");
   exit(0);
}

@projects = ('generic');

#########################################
# Parse Command Line Options
########################################

#
#The -o argument on the command line means only do MPCSTools
#
if (!defined $options{o})
{
	#Read the MPCS version
	if (!defined $options{v})
	{
	   REDO_VERSION: print 'Enter MPCS version (e.g., 5.0.0B15): ';
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
}

#Read the MPCSTools version
if (!defined $options{k})
{
   REDO_VERSION: print 'Enter MPCSTools version X.Y.Z.A: ';
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

#Read the CM ID
if (!defined $options{i})
{
   	REDO_CMID: print 'Enter complete CM ID MGSS-AMMOS-XX.YY.ZZZ-<etc>: ';
   	$cmId = <STDIN>;
   	chomp $cmId;
   	if ($cmId eq "")
   	{
   		goto REDO_CMID;
   	}
}
else
{
	$cmId = $options{i};
}

$cmId =~ s/^\s+//;
$cmId =~ s/\s+$//;

########################################
#Create the MPCSTools tar file
########################################
$mpcs = "mpcs-$mpcsVersion";
$mpcstools = "mpcstools-" . $mpcstoolsVersion;
$mpcstoolsTarFile = $mpcstools . ".tar";

print "CM ID = $cmId\n";
print "MPCSTools Version = $mpcstools\n";

#Build tar file names
foreach $proj (@projects) {
   $toolTar = $mpcstools . '_' . $proj . '_rw.tar';
   $allTar  = $proj . '-' . $mpcs . '-all.tar';
   $roTar   = $mpcs . '_' . $proj . '.tar';
   $rwTar   = $mpcs . '_' . $proj . '_rw.tar';

   push(@tools,$toolTar);
   push(@all,$allTar);
   $projList = join(' ',$allTar,$roTar,$rwTar,$toolTar);
   push(@projTar,$projList)
}

#MPCSTools tar files

if (defined $options{o})
{	
   	$cm_tar = $cmId . "-" . $mpcstools . ".tar";
  
	$list = join(' ',@tools);
	$cmd = 'tar cf ' . $cm_tar . ' ' . $list;

	print "$cmd\n";
	system ("$cmd");
}
else
{	
	print "MPCS Version = $mpcsVersion\n";
        print "Create overall mission tar files\n";

	foreach $proj (@projTar) {
	   $cmd = 'tar cf ' . $proj;   
	   print "$cmd\n";
	   system("$cmd");

	}
	
	$cm_tar = $cmId . "-" . $mpcs . ".tar";

	$list = join(' ',@all);
        $cmd = 'tar cf ' . $cm_tar . ' ' . $list;
	print "$cmd\n";
	system("$cmd");

}

print "Done.\n";
