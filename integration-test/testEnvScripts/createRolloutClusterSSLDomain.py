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
    server1SSLPort = int(sys.argv[9])

    server2Host = sys.argv[10]
    server2Port = int(sys.argv[11])
    server2SSLPort = int(sys.argv[12])

    nmPort = int(sys.argv[13])

    print "domainDir: " + domain_dir
    print "domainName: " + domainName
    print "username: " + argUsername
    print "password: " + argPassword

    print "adminHost: " + adminHost
    print "adminPort: " + str(adminPort)

    print "server1Host: " + server1Host
    print "server1Port: " + str(server1Port)
    print "server1SSLPort: " + str(server1SSLPort)

    print "server2Host: " + server2Host
    print "server2Port: " + str(server2Port)
    print "server2SSLPort: " + str(server2SSLPort)

    print "nmPort: " + str(nmPort)

    selectTemplate('Basic WebLogic Server Domain')
    loadTemplates()

    cd('/')
    set('Name', domainName)
    cd('/Servers/AdminServer')
    set('Name', 'AdminServer')
    ### set('ListenPort',adminPort)
    set('ListenPortEnabled','False')
    set('ListenAddress',adminHost)
    create ('AdminServer', 'SSL');
    cd('SSL/AdminServer')
    set('ListenPort',adminPort)
    set('Enabled' , 'true')


    cd('/')

    cd('Security/' + domainName + '/User/weblogic')
    cmo.setName(argUsername)
    cmo.setPassword(argPassword)


    #
    # machines
    #
    print "create machines."
    cd ('/')
    macSSLA=create(adminHost, 'Machine')
    macSSL1=create(server1Host, 'Machine')
    macSSL2=create(server2Host, 'Machine')
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
    set('Machine', macSSLA)

    cd('/')
    cd('/Servers')
    cd('AdminServer')
    set('Machine', macSSLA)


    ###  Add Servers/Cluster
    cd('/')
    clust=create('Cluster1','Cluster')
    create('Server1','Server')
    cd('Servers/Server1')
    set('ListenPort', server1Port)
    set('ListenAddress', server1Host)
    set('ListenPortEnabled','False')
    set('Machine', macSSL1)
    set('Cluster', clust)
    create ('Server1', 'SSL');
    cd('SSL/Server1')
    set('ListenPort',server1SSLPort)
    set('Enabled' , 'true')
    cd('/')
    create('Server2','Server')
    cd('Servers/Server2')
    set('ListenPort', server2Port)
    set('ListenAddress',server2Host)
    set('ListenPortEnabled','False')
    set('Machine', macSSL2)
    set('Cluster', clust)
    create ('Server2', 'SSL');
    cd('SSL/Server2')
    set('ListenPort',server2SSLPort)
    set('Enabled' , 'true')
    cd('/')

    cd('Cluster/Cluster1')
    set('SecureReplicationEnabled', 'true')

    cd('/')

    assign('Server', 'Server1,Server2','Cluster','Cluster1')
    writeDomain(domain_dir)
    closeTemplate()
    print "Domain Created Successfully "

except Exception, e:
    e.printStackTrace()
    dumpStack()
    raise("Create Domain Failed")

