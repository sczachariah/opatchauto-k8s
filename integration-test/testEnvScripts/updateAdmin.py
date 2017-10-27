import sys, socket
import os

### MAIN

try:
  readDomain('DOMAIN_HOME')
  cd('/Servers/ADMIN_SERVER_NAME')
  set('ListenAddress', 'ADMIN_SERVER_ADDRESS')
  print "Updated ADMIN_SERVER_NAME ListenAddress Successfully..."
except Exception, e:
  e.printStackTrace()
  dumpStack()
  raise("Update ADMIN_SERVER_NAME Failed..")

exit()
