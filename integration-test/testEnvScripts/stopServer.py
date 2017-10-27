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
   nmKill(serverName)
except Exception, e:
  e.printStackTrace()
  dumpStack()
  raise("Server stop Failed")

exit()

