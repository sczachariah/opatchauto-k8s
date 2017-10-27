import oracle.fmwplatform.actionframework.api.v2.ActionResult as ActionResult
import oracle.fmwplatform.actionframework.api.v2.ActionStatusCode as ActionStatusCode
#from fmwplatform_action_logger import ActionLogger
from java.util.logging import Logger


class SimpleProduct:


    def __init__(self):
        self.__logger = None

    # Re-enable Action Logger once bug 21294454 is fixed
    def sayHello(self, envModel, targets, extras):
        # self.__logger = ActionLogger(runtimeContext)
        # self.__logger.logStart()
        self.__logger = Logger.getLogger('SimpleProduct.sayHello')

        # self.__logger.logInfo("Hello")
        self.__logger.info("Hello")
        topology = envModel.getTopology()
        # self.__logger.logInfo("My topology version is '" + str(topology.getVersion()) + "'")
        self.__logger.info("My topology version is '" + str(topology.getVersion()) + "'")

        # Host Handling
        hosts = envModel.getHosts()
        for host in hosts:
            # self.__logger.logInfo("My host id is             '" + str(host.getId()) + "'")
            # self.__logger.logInfo("My host address is        '" + str(host.getAddress()) + "'")
            self.__logger.info("My host id is             '" + str(host.getId()) + "'")
            self.__logger.info("My host address is        '" + str(host.getAddress()) + "'")

        #Oracle Home Handling
        oracleHomes = envModel.getOracleHomes()
        for oracleHome in oracleHomes:
            # self.__logger.logInfo("My oracleHome id is       '" + str(oracleHome.getId()) + "'")
            # self.__logger.logInfo("My oracleHome path is     '" + str(oracleHome.getPath()) + "'")
            self.__logger.info("My oracleHome id is       '" + str(oracleHome.getId()) + "'")
            self.__logger.info("My oracleHome path is     '" + str(oracleHome.getPath()) + "'")

        #Domain Handling
        domains = envModel.getDomains()
        for domain in domains:
            # self.__logger.logInfo("My domain id is           '" + str(domain.getId()) + "'")
            # self.__logger.logInfo("My domain path is         '" + str(domain.getPath()) + "'")
            self.__logger.info("My domain id is           '" + str(domain.getId()) + "'")
            self.__logger.info("My domain path is         '" + str(domain.getPath()) + "'")

            #Credential Handling
            #   log.info("Cred : " + str(envModel.getCredentials()))
            #   log.info("Cred 1 :" + str(envModel.getCredentials().getCredentials()))
            #   log.info("Cred 1 :" + str(envModel.getCredentials().getCredentials().toString()))
            #   credentials = envModel.getCredentials().getCredentials()
            #   for credential in credentials:
            #       log.info("My credential name is     '" + str(credential.getName()) + "'")
            #       log.info("My credential username is '" + str(credential.getUsername()) + "'")
            #       log.info("My credential password is '" + "".join(credential.getPassword()) + "'")

        result = ActionResult()
        result.setStatusCode(ActionStatusCode.SUCCESS)
        result.setStatusDetail("Hello There")
        return result


    def sayHowAreYou(self, envModel, targets, extras):
        # self.__logger = ActionLogger(runtimeContext)
        # self.__logger.logStart()
        self.__logger = Logger.getLogger('SimpleProduct.sayHowAreYou')

        # self.__logger.logInfo("How Are You ?")
        self.__logger.info("How Are You ?")

        result = ActionResult()
        result.setStatusCode(ActionStatusCode.SUCCESS)
        result.setStatusDetail("I'm Fine! Thankyou")
        return result