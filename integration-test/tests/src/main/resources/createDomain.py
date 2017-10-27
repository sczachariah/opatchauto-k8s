# Run wlst to create a compact domain

import sys, socket
import os

### MAIN
argOracleHome = sys.argv[1]
argDomainName = sys.argv[2]

try:
    print("[info] reading wls template.")
    readTemplate(argOracleHome +"/wlserver/common/templates/wls/wls.jar", "Compact")
    cd('/')
    cd('Security/base_domain/User/weblogic')
    cmo.setPassword("welcome1")
    setOption('OverwriteDomain', 'true')
    print("[info] writing wls template.")
    writeDomain(argOracleHome + "/../" + argDomainName)
    closeTemplate()
    print("[info] domain "+ argDomainName +" created successfully.")
except Exception, e:
    e.printStackTrace()
    dumpStack()
    raise("Create Domain Failed")

exit()