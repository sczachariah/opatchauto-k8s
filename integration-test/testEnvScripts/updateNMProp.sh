#!/bin/sh

export CURRENT_DIR=`dirname $0`
export CURRENT_DIR=`cd "$CURRENT_DIR" 2>/dev/null && pwd`

export DOMAIN_HOME=$1
export NM_HOME=$DOMAIN_HOME/nodemanager

export SUITE_GRP=$2
echo DOMAIN_HOME    : $DOMAIN_HOME
echo NM_HOME        : $NM_HOME


if [ -e "$DOMAIN_HOME/nodemanager.properties" ]
then
    echo Copying default nodemanager.properties to $NM_HOME
    cp -r $DOMAIN_HOME/nodemanager.properties $NM_HOME/nodemanager.properties
fi


echo Updating nodemanager.properties in $NM_HOME
if [ $SUITE_GRP eq "WLSZDT" ]
then
    sed -i.orig 's/CrashRecoveryEnabled=false/CrashRecoveryEnabled=true/1g' $NM_HOME/nodemanager.properties
    echo CrashRecoveryEnabled has been set to true.
else
    sed -i.orig 's/CrashRecoveryEnabled=true/CrashRecoveryEnabled=false/1g' $NM_HOME/nodemanager.properties
    echo CrashRecoveryEnabled has been set to false.
fi
sed -i.orig 's/StopScriptEnabled=false/StopScriptEnabled=true/1g' $NM_HOME/nodemanager.properties
sed -i.orig 's/QuitEnabled=false/QuitEnabled=true/1g' $NM_HOME/nodemanager.properties


#Only to be used when running startNodeManager() using WLST
#echo "KeyStores=CustomIdentityAndCustomTrust" >> $NM_HOME/nodemanager.properties
#echo "CustomIdentityKeyStoreFileName=$DOMAIN_HOME/security/DemoIdentity.jks" >> $NM_HOME/nodemanager.properties
#echo "CustomIdentityKeyStorePassPhrase=DemoIdentityKeyStorePassPhrase" >> $NM_HOME/nodemanager.properties
#echo "CustomIdentityAlias=demoidentity" >> $NM_HOME/nodemanager.properties
#echo "CustomIdentityPrivateKeyPassPhrase=DemoIdentityPassPhrase" >> $NM_HOME/nodemanager.properties

echo nodemanager.properties Updated Successfully
