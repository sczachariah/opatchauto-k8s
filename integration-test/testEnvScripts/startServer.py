import sys, socket
import os

### MAIN 
argUsername = sys.argv[1]
argPassword = sys.argv[2]
serverName = sys.argv[3]
serverArgs = sys.argv[4]
nmHost = sys.argv[5]
nmPort = sys.argv[6]
domainName = sys.argv[7]
domainDir = sys.argv[8]

try:
   nmConnect(argUsername, argPassword, nmHost, nmPort, domainName, domainDir, nmType='ssl')
   prps = makePropertiesObject('')
   prps.setProperty('username', argUsername)
   prps.setProperty('password', argPassword)
   prps.setProperty('Arguments', serverArgs)
   nmStart(serverName, props=prps)
except Exception, e:
  e.printStackTrace()
  dumpStack()
  raise("Server start Failed")

exit()

