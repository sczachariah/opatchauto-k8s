#!/usr/bin/perl
##
##
## Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
##
##    NAME
##      execute.pl - <one-line expansion of the name>
##
##    DESCRIPTION
##      <short description of component this file declares/defines>
##
##    NOTES
##      <other useful comments, qualifications, etc.>
##
##    MODIFIED   (MM/DD/YY)
##    sczachar    05/19/16 - Creation

use File::Copy;
use File::Basename;
use Getopt::Long;
use File::Spec;
BEGIN
{
  use File::Basename;
  use Cwd;
  use Cwd 'abs_path';
}

$JAVA_HOME=$ENV{"JAVA_HOME"};
$HOSTNAME=$ENV{"HOSTNAME"};
$ORACLE_HOME=$ENV{"ORACLE_HOME"};
$DOMAIN_HOME=$ENV{"DOMAIN_HOME"};
$PATCH_HOME="";
$QA_PROPERTY_FILE="";
$M2_HOME=$ENV{"M2_HOME"};
$TEST_PARAMS="";
$USE_INSTALLER_JAR="";
$SKIP_JACOCO="false";
$JACOCO_INCLUDE_LIST="";
$JACOCO_EXCLUDE_LIST="*.resources.*:*.logging.*:*.exceptions.*:*.internal.*";
$JACOCO_DUMP_FILE_NAME="jacoco-it-opatchauto.exec";


$ADMIN_HOST="";
$ADMIN_PORT="";
$SERVER1_HOST="";
$SERVER1_PORT="";
$SERVER2_HOST="";
$SERVER2_PORT="";
$NM_PORT="";
$CLUSTER_NAME="";
$SERVER1_NAME="";
$SERVER2_NAME="";

GetOptions( '-ENV_PROPERTY_FILE=s' => \$ENV_PROPERTY_FILE,
 '-PATCH_HOME=s' => \$PATCH_HOME,
 '-SKIP_JACOCO=s' => \$SKIP_JACOCO,
 '-USE_INSTALLER_JAR=s' => \$USE_INSTALLER_JAR,
 '-TEST_PARAMS=s' => \$TEST_PARAMS,
 '-OPATCHAUTO_JVM_ARGS=s' => \$OPATCHAUTO_JVM_ARGS
);

############## Global Variables Initialization ##############
# set PLATFORM related info
$PLATFORM = $^O;
set_platform_info();

#Find the absolute path of Test Dir
$TEST_DIR=abs_path("..".$DIRSEP);
$OCP_HOME=$ORACLE_HOME.$DIRSEP."oracle_common".$DIRSEP."bin";
$OPATCHAUTO_HOME=$ORACLE_HOME.$DIRSEP."OPatch".$DIRSEP."auto".$DIRSEP."core".$DIRSEP."bin";
$TESTOUT_DIR=$TEST_DIR.$DIRSEP."OPatchAutoQATestOutput";
$QA_PROPERTY_FILE=$TESTOUT_DIR.$DIRSEP."qa.properties";
$EXTERNAL_PATCH="TRUE";



############### Do Operation #########
if($M2_HOME eq "" || $M2_HOME eq "%M2_HOME%"){
 findMaven_Home();
}

if($HOSTNAME eq "" || $HOSTNAME eq "%HOSTNAME%"){
 $HOSTNAME =`hostname`;
}

if($USE_INSTALLER_JAR eq "" || $USE_INSTALLER_JAR eq "%$USE_INSTALLER_JAR%"){
 $USE_INSTALLER_JAR ="TRUE";
}

if($JAVA_HOME eq "" || $JAVA_HOME eq "%JAVA_HOME%"){
 print "Please export/set JAVA_HOME environment variable and rerun\n";
 exit;
}

if($ORACLE_HOME eq ""){
 print "Please export/set ORACLE_HOME environment variable and rerun\n";
 exit;
}

if($DOMAIN_HOME eq ""){
 print "Please export/set DOMAIN_HOME environment variable and rerun\n";
 exit;
}

if($ENV_PROPERTY_FILE eq ""){
 print "Please pass the ENV_PROPERTY_FILE with test environment details\n";
 exit;
}

if($PATCH_HOME eq ""){
 $PATCH_HOME=$TEST_DIR.$DIRSEP."target".$DIRSEP."test-patches";
 $EXTERNAL_PATCH="FALSE";
}

if($SKIP_JACOCO eq "" || $SKIP_JACOCO eq "FALSE"){
 $SKIP_JACOCO="false";
 print "Running Tests with JaCoCo Enabled.\n";
}

if($SKIP_JACOCO eq "true" || $SKIP_JACOCO eq "TRUE"){
 $SKIP_JACOCO="true";
 print "Running Tests with JaCoCo Disabled.\n";
}

if($TEST_PARAMS eq ""){
 $TEST_PARAMS="-DsuiteXmlFile=mats.xml";
}

if($TEST_PARAMS!~/mats/ && $EXTERNAL_PATCH eq "TRUE"){
 print "[error] Only MATS suite can be invoked using External Patches. Please rerun using correct suite";
}
else{
 operation();
}

#Sub Module Definitions Starts Here...

sub operation(){
 $FMWPLT_MODULES_HOME=$ORACLE_HOME.$DIRSEP."oracle_common".$DIRSEP."modules".$DIRSEP."fmwplatform";
 $FMWPLT_PLUGINS_HOME=$ORACLE_HOME.$DIRSEP."oracle_common".$DIRSEP."plugins".$DIRSEP."fmwplatform";

 $TESTOUT_DIR=$TEST_DIR.$DIRSEP."OPatchAutoQATestOutput";
 mkdir($TESTOUT_DIR);

 #Generating qa.properties file if not provided by the user
# if($QA_PROPERTY_FILE  eq "" || $QA_PROPERTY_FILE eq "%QA_PROPERTY_FILE%"){
  generatePropFile();
 
 #Setting Required Env Variables
 $ENV{"OPATCHAUTO_JVM_ARGS"}=$OPATCHAUTO_JVM_ARGS;
 $ENV{"M2_HOME"}=$M2_HOME;
 $ENV{"M2"}=$M2_HOME.$DIRSEP."bin";
 $ENV{"JAVA_HOME"}=$JAVA_HOME;
 $ENV{PATH}="$ENV{M2}:$ENV{PATH}";
 $ENV{"SKIP_JACOCO"}=$SKIP_JACOCO;
 #$ENV{"ORACLE_HOME"}=$ORACLE_HOME;
 #$ENV{"MW_HOME"}=$ORACLE_HOME;
 print "Tests are running using below Environment\n";
 print "JAVA_HOME           : $JAVA_HOME\n";
 print "OPATCHAUTO_JVM_ARGS : $OPATCHAUTO_JVM_ARGS\n";
 print "HOSTNAME            : $HOSTNAME\n";
 print "M2_HOME             : $M2_HOME \n";
 print "\n";
 print "MW_HOME\/ORACLE_HOME: $ORACLE_HOME \n";
 print "OCP_HOME            : $OCP_HOME \n";
 print "OPATCHAUTO_HOME     : $OPATCHAUTO_HOME\n";
 print "DOMAIN_HOME         : $DOMAIN_HOME\n";
 print "\n";
 print "TEST_DIR            : $TEST_DIR\n";
 print "PATCH_HOME          : $PATCH_HOME \n";
 print "TEST_PARAMS         : $TEST_PARAMS\n";
 print "USE_INSTALLER_JAR : $USE_INSTALLER_JAR\n";
 chdir($TEST_DIR);

 #Get groupID and artifcat ID from pom
 $groupId="";
 $artifactId="";
 readPom();
 $localRepo_groupId=$groupId.".".$artifactId;
 print "GAV                 : $localRepo_groupId\n";

  if($USE_INSTALLER_JAR eq "TRUE"){
   $APIInstallLog= $WORKDIR.$DIRSEP."api.install.log";

   #Get fmwplatformVersion from ORACLE_HOME
   $ENVSPEC_VERSION=getFMWPLTVersion($FMWPLT_MODULES_HOME,"common","envspec");
   $ACTFMWK_V2_VERSION=getFMWPLTVersion($FMWPLT_MODULES_HOME,"common","actionframework-v2");
   $ACTFMWK_API_V2_VERSION=getFMWPLTVersion($FMWPLT_MODULES_HOME,"common","actionframework-api-v2");
   $STDACTION_V2_VERSION=getFMWPLTVersion($FMWPLT_PLUGINS_HOME,"actions","standardactions-v2");

   print "Tests will be using the ORACLE_HOME version of fmwplatform jars.\n";
   print "envspec.version=$ENVSPEC_VERSION\n";
   print "actfmwk.v2.version=$ACTFMWK_V2_VERSION\n";
   print "actfmwk.api.v2.version=$ACTFMWK_API_V2_VERSION\n";
   print "stdaction.v2.version=$STDACTION_V2_VERSION\n";

   writeQAVersion();
  }

  if($USE_INSTALLER_JAR ne "TRUE"){
   print "Tests will be run using the QA version of fmwplatform common jars.\n";
   readQAVersion();
   $catQAVersionCmd="cat qa.versions";
   if(system($catQAVersionCmd)==0)
   {
    print "\n";
   }
   $USE_INSTALLER_JAR="FALSE";
  }


 processJACOCOProps();

 $buildlog= $TESTOUT_DIR.$DIRSEP."build.log";
 $customActionCmd=$ENV{"M2"}.$DIRSEP."mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file  -Dfile=customActions.jar -DgroupId=$localRepo_groupId -DartifactId=customActions > $buildlog 2>&1";
 $patchesCmd=$ENV{"M2"}.$DIRSEP."mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file  -Dfile=patches.jar -DgroupId=$localRepo_groupId -DartifactId=patches >> $buildlog 2>&1";
 $testsCmd=$ENV{"M2"}.$DIRSEP."mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file  -Dfile=tests.jar -DgroupId=$localRepo_groupId -DartifactId=tests >> $buildlog 2>&1";

 $jacocoCmd="";
   if($SKIP_JACOCO eq "false"){
    $jacocoCmd="-Djacoco.skip=$SKIP_JACOCO -Djacoco.dump.file.name=$JACOCO_DUMP_FILE_NAME -Djacoco.include.list=$JACOCO_INCLUDE_LIST -Djacoco.exclude.list=$JACOCO_EXCLUDE_LIST";
   }

 if(system("$customActionCmd") == 0 && system("$patchesCmd") == 0 && system("$testsCmd") == 0){
 $versionCmd="-Denvspec.version=$ENVSPEC_VERSION -Dactfmwk.v2.version=$ACTFMWK_V2_VERSION -Dactfmwk.api.v2.version=$ACTFMWK_API_V2_VERSION -Dstdaction.v2.version=$STDACTION_V2_VERSION";
 $cmd="$ENV{M2}$DIRSEP"."mvn -P run-tests install $versionCmd -Dqa.properties=$QA_PROPERTY_FILE -Dtest.output=$TESTOUT_DIR $TEST_PARAMS >> $buildlog 2>&1";
 print("Executing .. $cmd \n");
 if(system("$cmd") == 0){
 print "$cmd Executed Successfully. Please Check ".$TESTOUT_DIR." for logs\n";
 }
 }
 $JunitOUT_DIR=$TESTOUT_DIR.$DIRSEP."report-files";
}


sub getFMWPLTVersion($$$){
 my($jarHome,$group,$artifactID) = @_;
 my $groupID = "oracle.fmwplatform.$group";
 my $version="";
 my $JAR=$jarHome.$DIRSEP.$group.$DIRSEP.$artifactID.".jar";
 $getManifestCmd="jar xf $JAR META-INF/MANIFEST.MF";
 if(system("$getManifestCmd") == 0)
 {
  $manifest="META-INF".$DIRSEP."MANIFEST.MF";
  open(FILE,"<$manifest") or die ("not able to open $manifest file");
  while(my $my_line = <FILE>)
  {
   last if($version ne "");
   $my_line=~s/^\s+|\s+$//g;
   if($my_line=~/Implementation-Version/)
   {
    $my_line=~s/Implementation-Version: //g;
    $version=$my_line;
   }
  }
  $removeMetaCmd="rm -rf META-INF";
  if(system("$removeMetaCmd") == 0)
  {
   print ""
  }

  #Commented as maven install file will not pull all dependencies.
#$installArtifactCmd=$ENV{"M2"}.$DIRSEP."mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=$JAR >> $APIInstallLog 2>&1";
#  #print "Executing $installArtifactCmd\n";
#        #if(system("$installArtifactCmd") == 0){
#                 #print "Installed $groupID:$artifactID:$version to Local Repo\n"
#                           #}
   return $version;
      }
 else
 {
    print "Unable to determine ORACLE_HOME version of fmwplatform common jar";
    $USE_INSTALLER_JAR="FALSE";
    return "";
 }
}


sub readPom(){
$pom=$TEST_DIR.$DIRSEP."pom.xml";
open(FILE,"<$pom") or die ("not able to open $pom file");
while(my $my_line = <FILE>)
{
last if($groupId ne "" && $artifactId ne "");
$my_line=~s/^\s+|\s+$//g;
if($my_line=~/groupId/){
$my_line=~s/<\/?groupId>//g;
$groupId=$my_line;
}
if($my_line=~/artifactId/){
$my_line=~s/<\/?artifactId>//g;
$artifactId=$my_line;
}
}
}

sub readQAVersion(){
 #Read from qa.versions
 $qaVersions=$TEST_DIR.$DIRSEP."qa.versions";
 open(FILE,"<$qaVersions") or die ("Not able to open $qaVersions File");
 while(my $my_line = <FILE>)
 {
  #last if($ENVSPEC_VERSION ne "");
  last if($ENVSPEC_VERSION ne "" && $ACTFMWK_V2_VERSION ne "" && $ACTFMWK_API_V2_VERSION ne "" && $STDACTION_V2_VERSION ne "");
  $my_line=~s/^\s+|\s+$//g;
  if($my_line=~/envspec.version/){
    $my_line=~s/envspec.version\=//g;
    $ENVSPEC_VERSION=$my_line;
   }
  if($my_line=~/actfmwk.v2.version/){
    $my_line=~s/actfmwk.v2.version\=//g;
    $ACTFMWK_V2_VERSION=$my_line;
   }
  if($my_line=~/actfmwk.api.v2.version/){
    $my_line=~s/actfmwk.api.v2.version\=//g;
    $ACTFMWK_API_V2_VERSION=$my_line;
   }
  if($my_line=~/stdaction.v2.version/){
    $my_line=~s/stdaction.v2.version\=//g;
    $STDACTION_V2_VERSION=$my_line;
   }
 }
}

sub writeQAVersion() {
  $QA_VERSION_FILE=$TEST_DIR.$DIRSEP."oraclehomeqa.versions";
  print $QA_VERSION_FILE."\n";
  open(QAVERFILE,">$QA_VERSION_FILE") or die("Unable to open $QA_VERSION_FILE file");
  print QAVERFILE "envspec.version=".$ENVSPEC_VERSION."\n";
  print QAVERFILE "actfmwk.v2.version=".$ACTFMWK_V2_VERSION."\n";
  print QAVERFILE "actfmwk.api.v2.version=".$ACTFMWK_API_V2_VERSION."\n";
  print QAVERFILE "stdaction.v2.version=".$STDACTION_V2_VERSION."\n";
  close(QAVERFILE);
  }


sub generatePropFile(){
 print $QA_PROPERTY_FILE."\n";
 open(ENVFILE,"$ENV_PROPERTY_FILE") or die("Unable to open $ENV_PROPERTY_FILE file");
 open(FILE,">$QA_PROPERTY_FILE") or die("Unable to create $QA_PROPERTY_FILE file");
  print FILE "java.home=".$JAVA_HOME."\n";
  print FILE "oracle.home=".$ORACLE_HOME."\n";
  print FILE "domain.home=".$DOMAIN_HOME."\n";
  print FILE "admin.host=".$ADMIN_HOST."\n";
  print FILE "admin.port=".$ADMIN_PORT."\n";
  print FILE "server1.host=".$SERVER1_HOST."\n";
  print FILE "server1.port=".$SERVER1_PORT."\n";
  print FILE "server2.host=".$SERVER2_HOST."\n";
  print FILE "server2.port=".$SERVER2_PORT."\n";
  print FILE "nm.port=".$NM_PORT."\n";
  print FILE "cluster.name=".$CLUSTER_NAME."\n";
  print FILE "server1.name=".$SERVER1_NAME."\n";
  print FILE "server2.name=".$SERVER2_NAME."\n";
  print FILE "wallet.location=".$WALLET_LOCATION."\n";
  print FILE "wallet.password=".$WALLET_PASSWORD."\n";
  print FILE "wallet.type=".$WALLET_TYPE."\n";
  print FILE "patch.home=".$PATCH_HOME."\n";
  print FILE "ocp.home=".$OCP_HOME."\n";
  print FILE "opatchauto.home=".$OPATCHAUTO_HOME."\n";
  print FILE "patch.external=".$EXTERNAL_PATCH."\n";

 foreach(<ENVFILE>){
  print FILE  $_;
  }

 close(FILE);
 close(ENVFILE);

 if ( $PLATFORM =~ 'win' || $PLATFORM =~ 'Win' ) {
  $fileModificationCmd="perl -p -i.bkp -e \"s|\\\\|//|g\" $QA_PROPERTY_FILE";
  if(system("$fileModificationCmd") == 0)
  {
   print "Executing $fileModificationCmd for updating $QA_PROPERTY_FILE for $PLATFORM\n";
  }
 }
}

sub processJACOCOProps(){
 $JACOCO_DUMP_FILE_NAME="jacoco-it-opatchauto.exec";
 $JACOCO_INCLUDE_LIST="com.oracle.glcm.patch.auto.*";
 $JACOCO_EXCLUDE_LIST=$JACOCO_EXCLUDE_LIST."";

 if (index($TEST_PARAMS, "opatchautoqa") != -1) {
     $JACOCO_DUMP_FILE_NAME="jacoco-it-opatchauto.exec";
     $JACOCO_INCLUDE_LIST="*";
     $JACOCO_EXCLUDE_LIST=$JACOCO_EXCLUDE_LIST."";
     print "Configured OCP JACOCO props\n";
 }
 if (index($TEST_PARAMS, "mats") != -1) {
     $SKIP_JACOCO="true";
     $JACOCO_DUMP_FILE_NAME="";
     $JACOCO_INCLUDE_LIST="";
     $JACOCO_EXCLUDE_LIST="";
     print "Disabled JACOCO for mats suite\n";
 }
}

sub findMaven_Home(){
 @files=glob($ORACLE_HOME.$DIRSEP."oracle_common".$DIRSEP."modules".$DIRSEP."org.apache.maven*");
 $M2_HOME=$files[$#files];
}

sub set_platform_info(){
 $PLATFORM = $^O;
 if ( $PLATFORM =~ 'win' || $PLATFORM =~ 'Win' ) {
   $DIRSEP = '\\';
   $PATHSEP =';';
 }
 else {
  $DIRSEP = '/' ;
  $PATHSEP = ':';
 }
}
