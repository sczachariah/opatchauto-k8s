package oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests.fmwzdt;

import oracle.fmwplatform.actionframework.api.v2.Action;
import oracle.fmwplatform.actionframework.api.v2.ActionFactory;
import oracle.fmwplatform.actionframework.api.v2.ActionResult;
import oracle.fmwplatform.actionframework.api.v2.DefaultActionFactoryLocator;
import oracle.fmwplatform.credentials.credential.CredentialBuilder;
import oracle.fmwplatform.credentials.wallet.WalletStoreProvider;
import oracle.fmwplatform.envspec.model.EnvironmentModel;
import oracle.fmwplatform.envspec.model.EnvironmentModelBuilder;
import oracle.fmwplatform.envspec.model.targets.ModelTarget;
import oracle.fmwplatform.envspec.model.targets.ModelTargetFactory;
import oracle.fmwplatform.envspec.model.topology.*;
import oracle.fmwplatformqa.opatchautoqa.zdt.credential.Credential;
import oracle.fmwplatformqa.opatchautoqa.zdt.helper.CopyResource;
import oracle.fmwplatformqa.opatchautoqa.zdt.helper.OPatchAutoErrorHelper;
import oracle.fmwplatformqa.opatchautoqa.zdt.helper.RemoteExecution;
import oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests.AbstractScriptRunner;
import oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests.DomainSetup;
import oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests.StartStopOperation;
import oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests.TestEnvHelper;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.ITest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests.fmwzdt.OPatchAutoFMWZDTTest.creds;

/**
 * Created by pcbajpai on 7/17/2017.
 */
public class OPatchAutoFMWZDTSSLTest extends AbstractFMWZDTScriptRunner implements ITest {
    private static String sessionID;
    private static TestEnvHelper testEnvHelper;
    private static OPatchAutoErrorHelper.OPATCHAUTO_ERROR opatchautoError;
    boolean isSuccess;
    int exitValue;
    public static String sslDomainID = "wlsSSLDomain";
    public static String sslUser = "weblogic";
    public static String sslPassword = "welcome1";
    public static EnvironmentModel sslEnvModel = null;
    public static String sslTopologyLoc = null;
    public static String sslDomainHome = null;
    public static String sslNMHome=null;
    DomainSetup domainSetup = null;

    public OPatchAutoFMWZDTSSLTest() {
        AbstractScriptRunner.sessionIDs = new LinkedList<>();
        AbstractScriptRunner.sessionIDs.clear();
        testEnvHelper = new TestEnvHelper();
        opatchautoError = OPatchAutoErrorHelper.OPATCHAUTO_ERROR.NULL;

    }

    //create a ssl domain
    //pack the domain
    //copy template to different m/c
    // unpack to the domain
    //discover the domainsave the topology

    @BeforeClass
    public void setupEnvironment() {
        try {
            domainSetup = new DomainSetup();
            credentialManager.addCredential(sslDomainID + "/ADMIN", sslUser, sslPassword.toCharArray());
            credentialManager.addCredential(sslDomainID + "/NM", sslUser, sslPassword.toCharArray());
            domainSetup.createDomain(AbstractScriptRunner.oracleHome, sslDomainID, AbstractScriptRunner.testEnvScriptsHome + File.separator + "createRolloutClusterSSLDomain.py", sslUser, sslPassword, true);
            sslDomainHome = AbstractScriptRunner.domainsDir + File.separator + sslDomainID;
            sslNMHome= sslDomainHome+File.separator+"nodeManager";
            domainSetup.packDomain(AbstractScriptRunner.oracleHome, sslDomainID, AbstractScriptRunner.testOutput, sslDomainID, true);
            if (sslEnvModel == null) {
                try {
                    creds = new CredentialBuilder(new WalletStoreProvider(walletLocation, walletType.equals("ENCRYPTED") ? walletPassword.toCharArray() : null)).loadCredentials();
                    sslEnvModel = AbstractScriptRunner.getEnvironmentModel(AbstractScriptRunner.oracleHome, sslDomainHome, sslDomainID, "sslTopology");
                    sslEnvModel.setCredentials(creds);
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    sslTopologyLoc = AbstractScriptRunner.testOutput + File.separator + "sslTopology" + File.separator + "topologies" + File.separator + "sslTopology" + ".xml";
//                envModel = null;
                }
            }
//            System.out.println(sslEnvModel.getTopology().toString());
            setupRemote();
            startStopDomain(StartStopOperation.SERVEROP.START, sslEnvModel, sslDomainID);
//            start the domain then stop the domain

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @AfterClass
    @Override
    public void cleanUpClass() {
        AbstractScriptRunner.mappingKey = "";
        AbstractScriptRunner.testName = "";
        patchOperation = AbstractScriptRunner.PATCH_OPERATION.NONE;
        testPatchLocation = "";
        AbstractScriptRunner.topologyLoc = "";
//        try {
//            FileUtils.deleteDirectory(AbstractScriptRunner.patchStagingDir);
//            FileUtils.deleteDirectory(AbstractScriptRunner.allPatchStagingDir);
//        } catch (IOException ioe) {
//            ioe.printStackTrace();
//            System.out.println("[error] Failed to delete Patch Staging Directory.");
//        }
        cleanTestPatches();
        AbstractScriptRunner.sessionIDs.clear();
    }

    @Override
    public String getTestName() {
        AbstractScriptRunner.testName = AbstractScriptRunner.testNameMapping.get(AbstractScriptRunner.mappingKey);
        return AbstractScriptRunner.testName;
    }

    @AfterTest
    public void cleanTestPatches() {
        try {
//            System.out.println("Cleaning Test Patch Directory : ");
            FileUtils.cleanDirectory(AbstractScriptRunner.patchStagingDir);
            FileUtils.cleanDirectory(AbstractScriptRunner.allPatchStagingDir);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.out.println("[error] Failed to delete Patch Staging Directory.");
        }
    }

    public void setupRemote() throws Exception {
        CopyResource copyResource = new CopyResource();
        for (Host h : sslEnvModel.getHosts()) {
            if (!adminHost.equals(h.getAddress())) {
                Credential sshCredential = AbstractScriptRunner.credentialManager.getCredential(h.getAddress(), "ssh");
                copyResource.copysrcFileToRemote(AbstractScriptRunner.testOutput + File.separator + sslDomainID + ".jar", sshCredential, h.getAddress(), AbstractScriptRunner.oracleHome);
                String cmd = domainSetup.unpackDomainComand(AbstractScriptRunner.oracleHome, sslDomainID, AbstractScriptRunner.oracleHome + File.separator + sslDomainID + ".jar");
                RemoteExecution.executeCommand(sshCredential, h.getAddress(), cmd);
                String updateNM =(String.format("%1$s %2$s %3$s",
                                new File(AbstractScriptRunner.nodeManagerHome + File.separator + "updateNMProp.sh").getCanonicalPath(),
                                new File(sslDomainHome).getCanonicalPath(),
                                AbstractScriptRunner.suiteGrp));
                RemoteExecution.executeCommand(sshCredential, h.getAddress(), updateNM);
            }
        }

    }

    public void rollbackPatch() {
        try {
            CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.ROLLBACK, FMWZDT_PLAN.ROLLING_PLAN,
                    testPatchLocation, sessionID, AbstractScriptRunner.domainHome, AbstractScriptRunner.adminHost, AbstractScriptRunner.adminPort, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");

            int exitValue = 1;
            try {
                System.out.println("Removing Patch Config Inventory");
                processInventory(AbstractScriptRunner.testName, AbstractScriptRunner.INVENTORY_OPERATION.MOVE_CONFIGPATCHINV);
                System.out.println("Removing Patch Recorded Actions");
                processInventory(AbstractScriptRunner.testName, AbstractScriptRunner.INVENTORY_OPERATION.MOVE_RECORDEDACTIONS);

                exitValue = AbstractScriptRunner.executor.execute(cmdString);
                Assert.assertEquals(0, exitValue);
            } catch (Exception ex) {
                throw ex;
            }
            setupExecutorLog();
            AbstractScriptRunner.executor.setWorkingDirectory(new File(AbstractScriptRunner.opatchautoHome));
            AbstractScriptRunner.executor.setStreamHandler(new PumpStreamHandler(AbstractScriptRunner.outputCollector, AbstractScriptRunner.errorCollector));
            cleanTestPatches();
        } catch (Exception e) {
            System.out.println("[error]: Exception Occured While Patch RollBack." + e);
//            System.out.println("Restoring ORACLE_HOME: " +oracleHome);
//            System.out.println("Restoring DOMAIN_HOME: " +domainHome);
        }
    }


    @Test(priority = 1, enabled = true, description = "OPATCHAUTO_FMWZDT_MULTINODE_SSL_ONLY_DOMAIN_PLAN_PARALLEL")
    public void testPatchUptimeNoneSSLDomainMultiNodeParallel() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("NONE");
        System.out.println("Test Patch Location : " + testPatchLocation);
        CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLY, FMWZDT_PLAN.PARALLEL_PLAN,
                sessionID, testPatchLocation, AbstractScriptRunner.oracleHome, sslDomainHome, server1Host, server2Host, AbstractScriptRunner.adminHost, AbstractScriptRunner.adminSSLPort, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");

        exitValue = 1;
        try {
            try {
                exitValue = AbstractScriptRunner.executor.execute(cmdString);
                Assert.assertEquals(0, exitValue);
            } catch (Exception ex) {
                System.out.println("[Error] : Command Execution Failed!!");
            } finally {
                writeToFile(AbstractScriptRunner.testName + ".console", AbstractScriptRunner.outputCollector.toString());
            }
            setupExecutorLog();
            AbstractScriptRunner.executor.setWorkingDirectory(new File(AbstractScriptRunner.opatchautoHome));
            AbstractScriptRunner.executor.setStreamHandler(new PumpStreamHandler(AbstractScriptRunner.outputCollector, AbstractScriptRunner.errorCollector));

            isSuccess = false;
            if (!AbstractScriptRunner.outputCollector.toString().contains("failure") || !AbstractScriptRunner.outputCollector.toString().contains("Failed") ||
                    !AbstractScriptRunner.outputCollector.toString().contains("OPatchAuto failed"))
                isSuccess = true;

            boolean diffFoundInLists;
            boolean isTopologyFilePassedFlg = false;
            diffFoundInLists = startStopExecutionOrderProcessing.readSessionLogToCheckStartStopSequence(oracleHome, domainHome, Collections.<String>emptyList(), topologyLoc, sessionLog, String.valueOf(FMWZDT_PLAN.PARALLEL_PLAN), testName);

            if (!isSuccess) {
                System.out.println("Success Status = " + isSuccess);
                throw new Exception("[error] Exception Occured During execution ");
            } else {
                if (!diffFoundInLists) {
                    System.out.println("Success Status = " + diffFoundInLists);
                    System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
                } else {
                    System.out.println("Success Status = " + diffFoundInLists);
                    throw new Exception("[error] Diff Found in Lists . Exception Occured During execution ");
                }
            }
        } catch (Exception e) {
            rollbackPatch();
            e.printStackTrace();
            System.out.println("[Failure]: " + AbstractScriptRunner.testName + "\n");
            throw e;
        }
    }

    @Test(priority = 2, enabled = true, description = "OPATCHAUTO_FMWZDT_MULTINODE_SSL_ONLY_DOMAIN")
    public void testPatchUptimeNoneSSLDomainMultiNode() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("ROLLING");
        System.out.println("Test Patch Location : " + testPatchLocation);
        CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLY, FMWZDT_PLAN.PARALLEL_PLAN,
                sessionID, testPatchLocation, AbstractScriptRunner.oracleHome, sslDomainHome, server1Host, server2Host, AbstractScriptRunner.adminHost, AbstractScriptRunner.adminSSLPort, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");

        exitValue = 1;
        try {
            try {
                exitValue = AbstractScriptRunner.executor.execute(cmdString);
                Assert.assertEquals(0, exitValue);
            } catch (Exception ex) {
                System.out.println("[Error] : Command Execution Failed!!");
            } finally {
                writeToFile(AbstractScriptRunner.testName + ".console", AbstractScriptRunner.outputCollector.toString());
            }
            setupExecutorLog();
            AbstractScriptRunner.executor.setWorkingDirectory(new File(AbstractScriptRunner.opatchautoHome));
            AbstractScriptRunner.executor.setStreamHandler(new PumpStreamHandler(AbstractScriptRunner.outputCollector, AbstractScriptRunner.errorCollector));

            isSuccess = false;
            if (!AbstractScriptRunner.outputCollector.toString().contains("failure") || !AbstractScriptRunner.outputCollector.toString().contains("Failed") ||
                    !AbstractScriptRunner.outputCollector.toString().contains("OPatchAuto failed"))
                isSuccess = true;

            boolean diffFoundInLists;
            boolean isTopologyFilePassedFlg = false;
            diffFoundInLists = startStopExecutionOrderProcessing.readSessionLogToCheckStartStopSequence(oracleHome, domainHome, Collections.<String>emptyList(), topologyLoc, sessionLog, String.valueOf(FMWZDT_PLAN.PARALLEL_PLAN), testName);

            if (!isSuccess) {
                System.out.println("Success Status = " + isSuccess);
                throw new Exception("[error] Exception Occured During execution ");
            } else {
                if (!diffFoundInLists) {
                    System.out.println("Success Status = " + diffFoundInLists);
                    System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
                } else {
                    System.out.println("Success Status = " + diffFoundInLists);
                    throw new Exception("[error] Diff Found in Lists . Exception Occured During execution ");
                }
            }
        } catch (Exception e) {
            rollbackPatch();
            e.printStackTrace();
            System.out.println("[Failure]: " + AbstractScriptRunner.testName + "\n");
            throw e;
        }
    }

    @Test(priority = 3, enabled = true, description = "OPATCHAUTO_FMWZDT_SINGLENODE_SSL_ONLY_DOMAIN")
    public void testPatchUptimeNoneSSLDomainSingleNode() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("NONE");
        System.out.println("Test Patch Location : " + testPatchLocation);
        CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLY, FMWZDT_PLAN.ROLLING_PLAN,
                testPatchLocation, sessionID, sslDomainHome, AbstractScriptRunner.adminHost, AbstractScriptRunner.adminSSLPort, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");


        setupExecutorLog();
        AbstractScriptRunner.executor.setWorkingDirectory(new File(AbstractScriptRunner.opatchautoHome));
        AbstractScriptRunner.executor.setStreamHandler(new PumpStreamHandler(AbstractScriptRunner.outputCollector, AbstractScriptRunner.errorCollector));
        exitValue = 1;
        try {
            delayExecution(10);
            try {
                exitValue = AbstractScriptRunner.executor.execute(cmdString);
                Assert.assertEquals(0, exitValue);
            } catch (Exception ex) {
                System.out.println("[Error] : Command Execution Failed!!");
            } finally {
                writeToFile(AbstractScriptRunner.testName + ".console", AbstractScriptRunner.outputCollector.toString());
            }

            isSuccess = false;
            if (!AbstractScriptRunner.outputCollector.toString().contains("failure") || !AbstractScriptRunner.outputCollector.toString().contains("Failed") ||
                    !AbstractScriptRunner.outputCollector.toString().contains("OPatchAuto failed")) {
                isSuccess = true;
            } else {
                isSuccess = false;
            }

            if (!isSuccess) {
                System.out.println("Success Status = " + isSuccess);
                throw new Exception("[error] Exception Occured During execution ");
            } else {
                System.out.println("Success Status = " + isSuccess);
                System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
            }

        } catch (Exception e) {
            rollbackPatch();
            e.printStackTrace();
            System.out.println("[Failure]: " + AbstractScriptRunner.testName + "\n");
            throw e;
        }
    }

    @Test(priority = 4, enabled = true, description = "OPATCHAUTO_FMWZDT_MULTIPLE_DOMAIN_SINGLE_ORACLE_HOME")
    public void testPatchUptimeSSLHybridMultiDomain() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("ROLLING");
        System.out.println("Test Patch Location : " + testPatchLocation);
        String topoLoc= getHybridTopology();
        System.out.println("Topology File Location : " + topoLoc);
        CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLYOOP, FMWZDT_PLAN.ROLLING_PLAN,
                testPatchLocation, sessionID, topoLoc, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");
        exitValue = 1;

        setupExecutorLog();
        AbstractScriptRunner.executor.setWorkingDirectory(new File(AbstractScriptRunner.opatchautoHome));
        AbstractScriptRunner.executor.setStreamHandler(new PumpStreamHandler(AbstractScriptRunner.outputCollector, AbstractScriptRunner.errorCollector));
        try {
            try {
                exitValue = AbstractScriptRunner.executor.execute(cmdString);
                Assert.assertEquals(0, exitValue);
            } catch (Exception ex) {
                System.out.println("[Error] : Command Execution Failed!!");
                throw new Exception(ex);
            } finally {
                writeToFile(AbstractScriptRunner.testName + ".console", AbstractScriptRunner.outputCollector.toString());
            }


            boolean hasMessage = false;
            if (AbstractScriptRunner.outputCollector.toString().contains("OPatchAuto failed")) {
                hasMessage = false;
            } else {
                hasMessage = true;
            }

            isSuccess = true;
            List<String> results = new ArrayList<String>();
            File[] files = AbstractScriptRunner.testDir.listFiles();
            for (File file : files) {
                if (file.isFile() && file.getName().toString().contains("OOP")) {
                    results.add(file.getName());
                }
            }

            for (String f : results) {
                Scanner scanner = null;
                scanner = new Scanner(new FileInputStream(new File(AbstractScriptRunner.testDir, f)));
                while (scanner.hasNext()) {
                    if (scanner.nextLine().contains("OPatchAuto failed")) {
                        isSuccess = false;
                    }
                }
                scanner.close();
            }

            if (isSuccess && hasMessage) {
                System.out.println("Session Log output Status = " + isSuccess);
                System.out.println("Console output Status = " + hasMessage);
                System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
            } else {
                System.out.println("Session Log output Status = " + isSuccess);
                System.out.println("Console output Status = " + hasMessage);
                System.out.println("[Error] OPatchauto failed. ");
                throw new Exception("[Error] OPatchauto failed. ");
            }

        } catch (Exception e) {
            rollbackPatch();
//            e.printStackTrace();
            System.out.println("[Failure]: " + AbstractScriptRunner.testName + "\n");
            throw e;
        }
    }

    public void startStopDomain(StartStopOperation.SERVEROP serverop, EnvironmentModel model, String domainID) throws Exception {
        Domain d = model.getDomainById(domainID);
        List<ModelTarget> domainTargetList = new ArrayList<>();
        List<ModelTarget> nmTargetList = new ArrayList<>();
        List<NodeManager> nmList = model.getNodeManagersForDomain(d.getId());
        Properties extraParameters = new Properties();
        if (nmList.size() > 0) {
            for (NodeManager nm : nmList) {
                nmTargetList.add(ModelTargetFactory.createNodeManagerOnHostTarget(nm.getName(), model.getHostForNodeManager(nm.getId()).getAddress()));
            }
        } else {
            extraParameters.setProperty("NoNodeManager", "True");
        }

        domainTargetList.add(ModelTargetFactory.createDomainTarget(d.getId()));

        if (serverop.equals(StartStopOperation.SERVEROP.START)) {
            runAction(serverop, nmTargetList, extraParameters);
        }
        runAction(serverop, domainTargetList, extraParameters);
        if (serverop.equals(StartStopOperation.SERVEROP.STOP)) {
            runAction(serverop, nmTargetList, extraParameters);
        }


    }

    public ActionResult runAction(StartStopOperation.SERVEROP serverop, List<ModelTarget> mTargets, Properties extraParam) throws Exception {

        ActionFactory actionFactory = DefaultActionFactoryLocator.locateActionFactory();
        Action action = actionFactory.getAction(serverop.getOperation(), oracleHome);
        ActionResult actionResult = action.run(envModel, mTargets, extraParam);
        return actionResult;
    }

    public String getHybridTopology() throws Exception {
        String topoLoc = null;
        EnvironmentModel hybridModel = sslEnvModel;
        List<NodeManager> nmList = new ArrayList<>();
        List<Domain> domainList = null;
        List<MappedDomain> mappedDomainList = new ArrayList<>();
        for (OracleHome oh : envModel.getOracleHomes()) {
            if (oh.getPath().equals(oracleHome)) {
                domainList = envModel.getDomainsUsingOracleHome(oh.getId());
            }
            for(NodeManager nm : envModel.getNodeManagers()){
                if(oh.getPath().equals(oracleHome) && envModel.getOracleHomeForNodeManager(nm.getId()).equals(oh.getId())){
                    nmList.add(nm);
                }
            }
        }

        for(Host h: envModel.getHosts()){
            for(MappedOracleHome mapOH: h.getMappedOracleHomes()){
                for(Domain d : domainList){
                    mappedDomainList.add(mapOH.getMappedDomain(d));
                }
            }
        }

        hybridModel.getTopology().setDomains(domainList);
        hybridModel.getTopology().setNodeManagers(nmList);
        for (Host h : hybridModel.getHosts()) {
            for (MappedOracleHome mappedOracleHome : h.getMappedOracleHomes()) {
                if (mappedOracleHome.getOracleHome().getPath().equals(oracleHome)) {
                        mappedOracleHome.addAllMappedDomains(mappedDomainList);
                }
            }

        }

        hybridModel.getTopology().setName("HybridTopology");
        EnvironmentModelBuilder builder = new EnvironmentModelBuilder();
        builder.writeModelToFiles(hybridModel, new File(testOutput));
        topoLoc = new File(testOutput + File.separator + "models" + File.separator + "topologies" + File.separator + "HybridTopology" + ".xml").getCanonicalPath();

        return topoLoc;
    }

}

