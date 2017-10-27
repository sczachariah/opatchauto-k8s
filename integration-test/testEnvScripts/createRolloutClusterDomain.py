import sys, socket
import os

### MAIN 

try:
    domain_dir = sys.argv[1]
    domainName = sys.argv[2]
    argUsername = sys.argv[3]
    argPassword = sys.argv[4]

    adminHost = sys.argv[5]
    adminPort = int(sys.argv[6])

    server1Host = sys.argv[7]
    server1Port = int(sys.argv[8])

    server2Host = sys.argv[9]
    server2Port = int(sys.argv[10])

    nmPort = int(sys.argv[11])

    print "domainDir: " + domain_dir
    print "domainName: " + domainName
    print "username: " + argUsername
    print "password: " + argPassword

    print "adminHost: " + adminHost
    print "adminPort: " + str(adminPort)

    print "server1Host: " + server1Host
    print "server1Port: " + str(server1Port)

    print "server2Host: " + server2Host
    print "server2Port: " + str(server2Port)

    print "nmPort: " + str(nmPort)

    selectTemplate('Basic WebLogic Server Domain')
    loadTemplates()

    cd('/')
    set('Name', domainName)
    cd('/Servers/AdminServer')
    set('Name', 'AdminServer')
    set('ListenPort',adminPort)
    set('ListenAddress',adminHost)


    cd('/')

    cd('Security/' + domainName + '/User/weblogic')
    cmo.setName(argUsername)
    cmo.setPassword(argPassword)


    #
    # machines
    #
    print "create machines."
    cd ('/')
    macA=create(adminHost, 'Machine')
    mac1=create(server1Host, 'Machine')
    mac2=create(server2Host, 'Machine')
    print "created machines..."
    #
    # node managers
    #
    print "creating NodeManagerA"
    cd('/')
    cd('/Machines/'+adminHost)
    nm=create(adminHost, 'NodeManager')
    nm.setListenAddress(adminHost)
    nm.setListenPort(nmPort)
    nm.setDebugEnabled(true)
    print(nm)
    print "creating NodeManager1"
    cd('/')
    cd('/Machines/'+server1Host)
    nm=create(server1Host, 'NodeManager')
    nm.setListenAddress(server1Host)
    nm.setListenPort(nmPort)
    nm.setDebugEnabled(true)
    print(nm)
    print "creating NodeManager2"
    cd('/')
    cd('/Machines/'+server2Host)
    nm=create(server2Host, 'NodeManager')
    nm.setListenAddress(server2Host)
    nm.setListenPort(nmPort)
    nm.setDebugEnabled(true)
    print(nm)

    print "created node managers..."

    cd('/')
    cd('/Servers/AdminServer')
    set('Machine', macA)

    cd('/')
    cd('/Servers')
    cd('AdminServer')
    set('Machine', macA)


    ###  Add Servers/Cluster
    cd('/')
    clust=create('Cluster1','Cluster')
    create('Server1','Server')
    cd('Servers/Server1')
    set('ListenPort', server1Port)
    set('ListenAddress', server1Host)
    set('Machine', mac1)
    set('Cluster', clust)
    cd('/')
    create('Server2','Server')
    cd('Servers/Server2')
    set('ListenPort', server2Port)
    set('ListenAddress',server2Host)
    set('Machine', mac2)
    set('Cluster', clust)
    cd('/')


    assign('Server', 'Server1,Server2','Cluster','Cluster1')
    writeDomain(domain_dir)
    closeTemplate()
    print "Domain Created Successfully "

except Exception, e:
    e.printStackTrace()
    dumpStack()
    raise("Create Domain Failed")

