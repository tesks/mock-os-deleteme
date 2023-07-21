#!/usr/bin/python
#
# Creates an AMPCS R7.x compatible-dictionary set from an existing
# dictionary set, by copying all the dictionary XML files, unpacking the 
# mission-supplied jar files found in the dictionary library directory, 
# recompiling any java files therein with R7 libraries, and re-packing 
# them into jar files.
#
#
# Version Note: Script added in support of Jira MPCS-7031 on 2/2/15.

import subprocess
import os
import argparse
import sys
import datetime
import tempfile
import shutil
import errno
import string

def checkChillGds():
    '''Utility function to check that CHILL_GDS is defined. Does not
    currently verify that it points to an R7 AMPCS version specifically.
      
      Arguments
      ----------
      None
      
      Returns
      --------
      None. Exits the interpreter if CHILL_GDS not defined.
    '''
    
    global CHILL_GDS
    CHILL_GDS = os.environ.get('CHILL_GDS')
    if CHILL_GDS is None:
       print 'The environment variable "CHILL_GDS" is not set. It must point to a valid AMPCS R7.X installation.'
       sys.exit(-1)   
  
def cleanupAndAbort(dirToClean):
    
    '''Utility function to cleanup and abort the application with -1 status.
      
      Arguments
      ----------
      dirToClean - Output directory to remove (string)
      
      Returns
      --------
      NEVER. Exits the interpreter with -1 status.
    '''
    
    shutil.rmtree(dirToClean, True)
    sys.exit(-1)

                
class DictionaryUpdateUtility:
    '''Class to perform a number of tasks related to dictionary update, including 
    copying the dictionary, and unpacking, recompiling, and re-packing the dictionary jars.
       
     Attributes
     -----------
     inputDir - Path to the input dictionary directory
     outputDir- Path to the output dictionary directory
     inputLibDir - Path to the library sub-directory for the input dictionary
     outputLibDir - - Path to the library sub-directory for the output dictionary
    '''
    
    def __init__(self, inputDir, outputDir, libDir):
        '''Class initializer function.
        
        Arguments
        ---------
        inputDir - Path to the input dictionary directory
        outputDir- Path to the output dictionary directory
        libDir - Name of the library subdirectory for the input dictionary
     
        Returns:
        -------
        New object instance.
        '''
        self.inputDir = inputDir
        self.outputDir = outputDir
        self.inputLibDir = os.path.join(inputDir, libDir)
        self.outputLibDir = os.path.join(outputDir, libDir)
        
    def copyToOutput(self):
        '''Utility function to copy the input dictionary tree to the output dictionary
        location. Destination directory must NOT already exist.
      
          Arguments
          ----------
          NONE
          
          Returns
          --------
          0 if successful
          -1 if error
        '''
        
        try:
            print "Copying tree from " + self.inputDir + " to " + self.outputDir
            shutil.copytree(self.inputDir, self.outputDir)
            return 0
        except shutil.Error as e:
            print('Dictionary directory not copied. Error: %s' % e)
            return 1
        # Any error saying that the directory doesn't exist
        except OSError as e:
            print('Dictionary directory not copied. Error: %s' % e)
            return -1
                    
    def listJars(self):  
        '''Function to get a list of all the jar files in the library directory
        of the input dictionary. Does not recurse through the library tree.
        
        Returns
        --------
        List of jar file paths
        '''
        
        files = []
        for filename in os.listdir(self.inputLibDir):
             fPath = os.path.join(self.inputLibDir, filename)
             if filename.endswith(".jar"):
                 files.append(fPath)
        return files
    
    def unjar(self, jarFile):
         '''Function to unpack a specific jar file to a temporary directory.
        
         Arguments
         ---------
         jarFile - the path to the jar file to unpack
        
         Returns
         --------
         The path to the temporary directory where the jar was unpacked, or None if the unpack fails
         '''
        
         jarDir = tempfile.gettempdir() + "/" + os.path.basename(jarFile) + "_unjar"
         try:
             os.makedirs(jarDir)
         except OSError as exc:
             if exc.errno == errno.EEXIST and os.path.isdir(jarDir):
                pass
             else: 
                print "The un-jar of file " + jarFile + " failed because the temporary directory could not be created"
                return None
                 
         dirOp = Chdir(jarDir)
         jarArgs = ["jar", "xvf", jarFile]
         retCode = subprocess.call(jarArgs)
         if retCode != 0:
            print "The un-jar of file " + jarFile + " failed"
            return None
        
         return jarDir
     
    def listJavas(self, javaDir):
         '''Function to list the java files under a specific directory (recurses the whole tree).
        
         Arguments
         ---------
         javaDir - the path to the directory containing java files
        
         Returns
         --------
         List of java file paths for all java files under javaDir
         '''
        
         files = []
         for name in os.listdir(javaDir):
               filename = os.path.join(javaDir, name)
               if os.path.isfile(filename):
                   if filename.endswith(".java"):
                       files.append(filename)
               else:        
                   files.extend(self.listJavas(filename))
         return files
    
    def compileJavas(self, javaDir):
         '''Function to compile the java files under a specific directory (recurses the whole tree).
         If the compile fails, the user is given the option to correct them in place and try again.
        
         Arguments
         ---------
         javaDir - the path to the directory containing java files
        
         Returns
         --------
         0 if java files found and compile successful
         1 if no java files found (no compile needed)
         -1 if error
         '''
        
         javaFiles = self.listJavas(javaDir)
         if javaFiles == []:
            return 1
        
         compiler = os.path.join(CHILL_GDS, "bin/tools/chill_java_compile")
         compilerArgs = [compiler] + [javaDir]
         compilerArgs.extend(javaFiles)  
        
         while True:
            retCode = subprocess.call(compilerArgs)
            
            if retCode != 0:
                print "The compile of files in temporary directory " + javaDir + " failed"
                inputYorN = raw_input("Do you want to attempt to fix the code in place (Y/N)? ")
                if inputYorN.startswith("Y") or inputYorN.startswith("y"):
                    inputDone = raw_input("Press return when ready to re-attempt compilation...")
                else:
                    return -1 
            else:    
                return 0
          
    def recreateJar(self, javaDir, jarFile):
         '''Function to build a directory of files into a jar file.
        
         Arguments
         ---------
         javaDir - the path to the directory containing files to be packed
         jarFile - the desired location of the new jar file
        
         Returns
         --------
         0 if jar was successful
         -1 if error
         '''
        
         jarArgs = ["jar", "cvf", jarFile, "-C", javaDir, "."]  
         retCode = subprocess.call(jarArgs)
         if retCode != 0:     
             print "The re-jar of files in temporary directory " + os.path.basename(javaDir) + " failed"
             return -1
         return 0 
     
    def rebuildAllJars(self):
         '''Function to rebuild all jars under the input dictionary and place them under
         the output dictionary.
        
         Arguments
         ---------
         NONE
        
         Returns
         --------
         0 if successful
         -1 if error
         '''
         
         # Loop through all the jar files in the input dictionary
         for jarFile in self.listJars():
            
            # Unjar the file to a temp directory
            jarDir = self.unjar(jarFile)
            if jarDir == None:
                print "Unable to unpack jar file " + jarFile
                return -1;
            else:
                # Recompile the java files in the temp directory
                retCode = self.compileJavas(jarDir)
                if retCode != 0: 
                    return -1       
                else:    
                    # Create a new jar from the temp directory contents, under the lib directory for the otuput
                    # dictionary
                    retCode = self.recreateJar(jarDir, os.path.join(self.outputLibDir, os.path.basename(jarFile)))  
                    if retCode != 0:
                        return -1   
         return 0               
      
class Chdir:
    '''Simple class to change directory upon instance creation and restore to original 
    directory upon instance deletion.
    
    Attributes
    ---------
    savedPath     - the path to the original directory
    '''
           
    def __init__(self, newPath):  
        self.savedPath = os.getcwd()
        os.chdir(newPath)

    def __del__(self):
        os.chdir(self.savedPath)

#                                                                                                                                                
# Main Entry Point                                                                                                                               
#                                                                                                                                                
if __name__ == "__main__":
    
    # Make sure environment is defined
    checkChillGds()
    
    argParser = argparse.ArgumentParser(description=
        'Create an AMPCS R7.X compatible dictionary directory from an existing dictionary directory')

    argParser.add_argument('-i', '--inputDictionary', help='Path to input dictionary version directory (required)',
                           default=None, required=True)
    argParser.add_argument('-o', '--outputDictionary',
                           help='Output dictionary directory to be written (defaults to input dictionary directory + _R7)',
                           default=None)
    argParser.add_argument('-l', '--alternateLibDir',
                           help='Sub-directory under the version directory where jars to be recompiled are found (defaults to lib)',
                           default='lib')

    # Parse arguments and print help if something is obviously missing
    args = argParser.parse_args()
    if len(sys.argv) < 1:
        argParser.print_help()
        sys.exit(-1)

    # Construct output dictionary directory name
    if args.outputDictionary == None:
        outputDir = string.rstrip(args.inputDictionary, "/") + "_R7"
    else:
        outputDir = args.outputDirectory   
    
    # Create dictionary utility instance
    dictUtil = DictionaryUpdateUtility(args.inputDictionary, outputDir, args.alternateLibDir)
        
    # Check the list of the jar files in the dictionary lib directory
    # If there are none, exit because this is a pointless exercise       
    jarsToCompile = dictUtil.listJars()   
    if jarsToCompile == []:
        print "There are no jar files in " + inputLibDir + " to re-build"
        sys.exit(1)
        
    # Copy the entire dictionary tree from the old version directory to the new one    
    retCode = dictUtil.copyToOutput()
    if retCode != 0:
        print "Unable to copy dictionary directory to " + outputDir
        sys.exit(-1)
    
    # Now rebuild all the jar files in the input dictionary and place them into the
    # output dictionary
    retCode = dictUtil.rebuildAllJars()
    if retCode != 0: 
        print "Dictionary update failed"
        cleanupAndAbort(outputDir)          
                   
    print "Dictionary files copied from " + args.inputDictionary + " to " + outputDir               
    print "All Java libraries in successfully updated"        
    
    sys.exit(0)
