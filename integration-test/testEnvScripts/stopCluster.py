import sys, socket
import os

### MAIN 
clusterName = sys.argv[1]
argUsername = sys.argv[2]
argPassword = sys.argv[3]
host = sys.argv[4]
port = sys.argv[5]

try:
  connect(argUsername, argPassword, 't3://' + host + ':' + port)
  shutdown(clusterName, 'Cluster')
  state(clusterName)
except Exception, e:
  e.printStackTrace()
  dumpStack()
  raise("stop Cluster Failed")

exit()

