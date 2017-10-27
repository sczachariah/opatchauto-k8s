import oracle.fmwplatform.actionframework.ActionResult as ActionResult
import oracle.fmwplatform.actionframework.ActionStatusCode as ActionStatusCode
#from fmwplatform_action_logger import ActionLogger
from java.util.logging import Logger
# import wlstModule as wlst


class SetDSTimeout:


    def __init__(self):
        self.__logger = None

    # Re-enable Action Logger once bug 21294454 is fixed
    def updateDS(self, envModel, targets, extras):
        #self.__logger = ActionLogger(runtimeContext)
        self.__logger = Logger.getLogger('set_ds_connect_timeout')
        #self.__logger.logStart()

        #self.__logger.logInfo("set_ds_connect_timeout Execution Started")
        self.__logger.info("set_ds_connect_timeout Execution Started")
        WLS.setShowLSResult(0)

        adminUser = 'weblogic'
        adminPassword = 'welcome1'
        adminServerUrl = 't3://slc00slc.us.oracle.com:7001'

        wlst.connect(adminUser, adminPassword, adminServerUrl)
        updateTimeout()
        wlst.disconnect()

        result = ActionResult()
        result.setStatusCode(ActionStatusCode.SUCCESS)
        result.setStatusDetail("Test Case 106 : set_ds_connect_timeout Execution Completed")
        #self.__logger.logInfo("set_ds_connect_timeout Execution Completed")
        self.__logger.info("set_ds_connect_timeout Execution Completed")
        return result


    #call the actual worker method to change the value in these domains
    def updateTimeout(self):
        wlst.edit()
        wlst.startEdit()
        timeout = 120000
        wlst.cd('/JDBCSystemResources')
        datasourceList = wlst.ls('c', returnMap='true', returnType='c')
        for jdbc in datasourceList:
            com = wlst.getMBean(jdbc + '/JDBCResource/' + jdbc + '/JDBCDataSourceParams/' + jdbc)
            DS = com.getDataSourceList()
            if DS is not None and len(DS) > 0:
                #this is a multidatasouce, skip it.
                continue
            #now navigate and update the property
            path = jdbc + '/JDBCResource/' + jdbc + '/JDBCDriverParams/' + jdbc + '/Properties/' + jdbc + '/Properties'
            com = wlst.getMBean(path)
            props = wlst.ls(path, returnMap='true', returnType='c')
            if 'oracle.net.CONNECT_TIMEOUT' not in props:
                #self.__logger.logInfo("Creating property oracle.net.CONNECT_TIMEOUT")
                self.__logger.info("Creating property oracle.net.CONNECT_TIMEOUT")
                wlst.create('oracle.net.CONNECT_TIMEOUT', 'Property')
            wlst.cd('/JDBCSystemResources/' + path + '/oracle.net.CONNECT_TIMEOUT')
            #self.__logger.logInfo("Setting property oracle.net.CONNECT_TIMEOUT: " + str(timeout) + " in " + jdbc)
            self.__logger.info("Setting property oracle.net.CONNECT_TIMEOUT: " + str(timeout) + " in " + jdbc)
            wlst.cmo.setValue(str(timeout))
            wlst.cd('/JDBCSystemResources')
        wlst.activate(block='true')


