#!/bin/sh

export JAVA_HOME=$1

echo 'Creating SSL Certificate...'

$JAVA_HOME/bin/keytool -genkeypair -alias SSLTest -keyalg RSA -keysize 1024 -validity 365 -keypass welcome1 -keystore identity.jks -storepass welcome1 << EOF
`hostname -i`
FMWPlatform QA
Oracle Corporation
Salt Lake City
Utah
US
yes
EOF

$JAVA_HOME/bin/keytool -selfcert -keyalg RSA -alias SSLTest -keystore identity.jks << EOF
welcome1
EOF

$JAVA_HOME/bin/keytool -exportcert -file selfsign.cer -alias SSLTest -keystore identity.jks -storepass welcome1
$JAVA_HOME/bin/keytool -printcert -file selfsign.cer

#$JAVA_HOME/bin/keytool -export -alias SSLTest -file selfsign.cer -keystore identity.jks -storepass welcome1
$JAVA_HOME/bin/keytool -import -alias SSLTest -file selfsign.cer -keystore trust.jks -storepass welcome1 << EOF
yes
EOF