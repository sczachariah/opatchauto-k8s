import sys, socket
import os

### MAIN
domainHome = sys.argv[1]
nmHome = sys.argv[2]
nmHost = sys.argv[3]
nmPort = sys.argv[4]


try:
    startNodeManager(verbose='true', NodeManagerHome=nmHome, ListenAddress=nmHost, ListenPort=nmPort)
except Exception, e:
    e.printStackTrace()
    dumpStack()
    raise("Node Manager start Failed")

exit()

