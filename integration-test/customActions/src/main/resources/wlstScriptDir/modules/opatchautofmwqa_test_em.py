import zipfile
import os
import shutil
import oracle.fmwplatform.actionframework.api.v2.ActionResult as ActionResult
import oracle.fmwplatform.actionframework.api.v2.ActionStatusCode as ActionStatusCode
#from fmwplatform_action_logger import ActionLogger
from java.util.logging import Logger


class TestEM:


    def __init__(self):
        self.__logger = None


    def updateEMApp(self, envModel, targets, extras):
        # self.__logger = ActionLogger(runtimeContext)
        # self.__logger.logStart()
        self.__logger = Logger.getLogger('SimpleProduct.sayHello')

        # self.__logger.logInfo("TESTEM.updateEMApp Execution Started")
        self.__logger.info("TESTEM.updateEMApp Execution Started")

        domains = envModel.getDomains()
        for domain in domains:
            domainPath = domain.getPath()
            # self.__logger.logInfo("My domain path is '" + domainPath + "'")
            self.__logger.info("My domain path is '" + domainPath + "'")

            domainAppPath = os.path.join(domainPath, "..", "..", "applications", os.path.basename(domainPath))
            # self.__logger.logInfo("My domain applications path is '" + domainAppPath + "'")
            self.__logger.info("My domain applications path is '" + domainAppPath + "'")

            BI_DOMAIN_APP_HOME = domainAppPath
            if not (os.path.exists(BI_DOMAIN_APP_HOME)):
                # self.__logger.logInfo(BI_DOMAIN_APP_HOME + " does not exist. Exiting.")
                self.__logger.info(BI_DOMAIN_APP_HOME + " does not exist. Exiting.")
                break

            if not (zipfile.is_zipfile("%s/em.ear" % BI_DOMAIN_APP_HOME)):
                # self.__logger.logInfo("em.ear not found or invalid archive. Exiting.")
                self.__logger.info("em.ear not found or invalid archive. Exiting.")
                break

            # Create fresh temp directory
            TEMP = "%s/temp" % BI_DOMAIN_APP_HOME
            if os.path.isdir(TEMP):
                shutil.rmtree(TEMP)
            os.mkdir(TEMP)

            try:
                JAVA_HOME = os.environ['JAVA_HOME']
                JAVA_BIN = JAVA_HOME + "/bin/jar"
                self.__logger.info("JAVA_HOME : " + JAVA_HOME)
                self.__logger.info("JAVA_BIN  : " + JAVA_BIN)
            except KeyError:
                JAVA_BIN = "jar"

            #Expand em.ear and em.ear in current directory
            os.system("unzip %s/em.ear em.war -d %s" % (BI_DOMAIN_APP_HOME, TEMP))
            os.system("unzip %s/em.war WEB-INF/web.xml -d %s" % (TEMP, TEMP))

            #Replace the context-param "org.apache.myfaces.trinidad.UPLOAD_MAX_DISK_SPACE" in web.xml
            # self.__logger.logInfo("Replacing the context-param in web.xml with new value..")
            self.__logger.info("Replacing the context-param in web.xml with new value..")
            s = open("%s/WEB-INF/web.xml" % TEMP).read()
            #s = s.replace('460800000','40960000')
            s = s.replace('40960000', '460800000')
            f = open("%s/WEB-INF/web.xml" % TEMP, 'w')
            f.write(s)
            f.close()

            #Update em.war with modified web.xml
            # self.__logger.logInfo("Updating web.xml in em.war..")
            self.__logger.info("Updating web.xml in em.war..")
            os.system("%s uvf %s/em.war -C %s WEB-INF/web.xml" % (JAVA_BIN, TEMP, TEMP))

            #Update em.ear with updated em.war
            # self.__logger.logInfo("Updating em.ear with updated em.war..")
            self.__logger.info("Updating em.ear with updated em.war..")
            os.system("%s uvf %s/em.ear -C %s em.war" % (JAVA_BIN, BI_DOMAIN_APP_HOME, TEMP))

        result = ActionResult()
        result.setStatusCode(ActionStatusCode.SUCCESS)
        result.setStatusDetail("TESTEM.updateEMApp Execution Completed")
        # self.__logger.logInfo("TESTEM.updateEMApp Execution Completed")
        self.__logger.info("TESTEM.updateEMApp Execution Completed")
        return result