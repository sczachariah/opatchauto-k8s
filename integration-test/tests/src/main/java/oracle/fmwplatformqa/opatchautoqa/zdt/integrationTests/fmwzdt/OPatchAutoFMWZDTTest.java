package oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests.fmwzdt;

import com.jcraft.jsch.*;
import oracle.fmwplatform.credentials.credential.CredentialBuilder;
import oracle.fmwplatform.credentials.credential.Credentials;
import oracle.fmwplatform.credentials.wallet.WalletStoreProvider;
import oracle.fmwplatform.envspec.model.topology.Domain;
import oracle.fmwplatform.envspec.model.topology.Host;
import oracle.fmwplatformqa.opatchautoqa.zdt.credential.Credential;
import oracle.fmwplatformqa.opatchautoqa.zdt.helper.CopyResource;
import oracle.fmwplatformqa.opatchautoqa.zdt.helper.OPatchAutoErrorHelper;
import oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests.AbstractScriptRunner;
import oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests.TestEnvHelper;
import oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests.ServerOperation;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.ITest;
import org.testng.annotations.*;

import java.io.*;
import java.util.*;


public class OPatchAutoFMWZDTTest extends AbstractFMWZDTScriptRunner implements ITest {
    private static String sessionID;
    private static TestEnvHelper testEnvHelper;
    private static OPatchAutoErrorHelper.OPATCHAUTO_ERROR opatchautoError;
    protected static Credentials creds;
    boolean isSuccess;
    boolean isSingleHost=false;
    int exitValue;

    public OPatchAutoFMWZDTTest() {
        AbstractScriptRunner.sessionIDs = new LinkedList<>();
        AbstractScriptRunner.sessionIDs.clear();
        testEnvHelper = new TestEnvHelper();
        opatchautoError = OPatchAutoErrorHelper.OPATCHAUTO_ERROR.NULL;

    }

    @BeforeMethod
    public void getLastSessionID() {
        if (AbstractScriptRunner.sessionIDs.size() > 0)
            sessionID = AbstractScriptRunner.sessionIDs.get(AbstractScriptRunner.sessionIDs.size() - 1);
        else
            sessionID = "";
        System.out.println("Last Session ID : " + sessionID);
    }


    public void copyResourceRemote() throws Exception {
        CopyResource copyResource = new CopyResource();
        for (Host h : envModel.getHosts()) {
            Credential sshCredential = AbstractScriptRunner.credentialManager.getCredential(h.getAddress(), "ssh");
            copyResource.copysrcFileToRemote(AbstractScriptRunner.testOutput + File.separator + "OPatchAutoIntegrationTestsCustomActions.jar", sshCredential, h.getAddress(), AbstractScriptRunner.oracleHome);
        }
    }

    public void stopManaged() throws Exception{
        ServerOperation serverOperation=new ServerOperation();
        serverOperation.stopManagedServerOperation();
    }

    public void stopAdmin() throws Exception{
        ServerOperation serverOperation=new ServerOperation();
        serverOperation.stopAdminServerOperation();
    }

    public void startAdmin() throws Exception{
        ServerOperation serverOperation=new ServerOperation();
        serverOperation.startAdminServerOperation();
    }

    public boolean singleHost() throws Exception{
//        ArrayList<String> nHost = new ArrayList<String>();
//        for (Domain d : envModel.getDomains()){
//            for (Host h : envModel.getHosts()){
//                nHost.add(h.getAddress());
//            }
//        }
        if (envModel.getHosts().size()>1){
            isSingleHost=false;
        }else {
            isSingleHost=true;
        }
        return isSingleHost;
    }

    //    @AfterMethod (alwaysRun = false)
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

    @Override
    @AfterClass
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

    //TODO Method to ping servers and check if it is up or not with or without NodeManager.

    @BeforeClass
    public void generateTopology() {
        if (envModel == null) {
            try {
                creds = new CredentialBuilder(new WalletStoreProvider(walletLocation, walletType.equals("ENCRYPTED") ? walletPassword.toCharArray() : null)).loadCredentials();
                envModel = AbstractScriptRunner.getEnvironmentModel();
                envModel.setCredentials(creds);
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                AbstractScriptRunner.topologyLoc = AbstractScriptRunner.testOutput + File.separator + "modelOutput" + File.separator + "models" + File.separator + "topologies" + File.separator + "modelOutput" + ".xml";
//                envModel = null;
            }
        }
        try {
            copyResourceRemote();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @BeforeClass
//    public void preSetup() throws Exception {
//        try {
//            System.out.println("Stopping Servers : ");
//            startStopServers(TestEnvHelper.START_STOP.STOP);
//        } catch (Exception e) {
//            System.out.println("Caught Exception During SERVER STOP Action: ");
//            e.printStackTrace();
//        }
//
//        try {
//            System.out.println("Stopping NodeManagers : ");
//            startStopNM(TestEnvHelper.START_STOP.STOP);
//            for (int i = 0; i <= 10000; i++) {
//                if (i % 1000 == 0)
//                    System.out.print(".");
//            }
//        } catch (Exception e) {
//            System.out.println("Caught Exception During NODEMANAGER STOP Action: ");
//            e.printStackTrace();
//        }
//
//        System.out.println("");
//
//        try {
//            System.out.println("Starting NodeManagers : ");
//            startStopNM(TestEnvHelper.START_STOP.START);
//        } catch (Exception e) {
//            System.out.println("Caught Exception During NODEMANAGER START Action: ");
//            e.printStackTrace();
//        }
//        try {
//            System.out.println("Starting Servers : ");
//            startStopServers(TestEnvHelper.START_STOP.START);
//            for (int i = 0; i <= 10000; i++) {
//                if (i % 1000 == 0)
//                    System.out.print(".");
//            }
//        } catch (Exception e) {
//            System.out.println("Caught Exception During SERVER START Action: ");
//            e.printStackTrace();
//        }
//    }

//    public void startStopServers(TestEnvHelper.START_STOP action) throws Exception {
//        String aHost = null;
//        String aPort = null;
//        String aName = null;
//        String nPort = null;
//        String cName = null;
//        String srvrHost = null;
//        String srvrName = null;
//        List<String> operCluster = new ArrayList<>();
//        for (Domain d : envModel.getDomains()) {
//            for (NodeManager nm : envModel.getNodeManagersForDomain(d.getId())) {
//                nPort = nm.getTuningParameters().getSettingValueByAlias("NmPort");
//            }
//            for (Server s : envModel.getServersInDomain(d))
//                if (s.isAdminServer()) {
//                    aName = d.getServerBindingForServerID(s.getId()).getName();
//                    aHost = d.getServerBindingForServerID(s.getId()).getListenAddress();
//                    aPort = d.getServerBindingForServerID(s.getId()).getListenPort();
//                    TestEnvHelper.operateServer(action, TestEnvHelper.SERVER_TYPE.ADMIN, aName, aHost, aHost, nPort, new File(domainHome).getCanonicalPath());
//                } else {
//                    for (ClusterBinding cb : envModel.getClusterBindingsForDomain(d)) {
//                        cName = cb.getClusterRef();
//                        if (!operCluster.contains(cName)) {
//                            TestEnvHelper.operateCluster(action, cName, aHost, aPort);
//                            operCluster.add(cName);
//                        }
//                    }
//                }
//            for(Server s : envModel.getServersInDomain(d))
//                if(!s.isAdminServer()){
//                    srvrName=d.getServerBindingForServerID(s.getId()).getName();
//                    srvrHost=d.getServerBindingForServerID(s.getId()).getListenAddress();
//                    aPort=d.getServerBindingForServerID(s.getId()).getListenPort();
//                    TestEnvHelper.operateServer(action, TestEnvHelper.SERVER_TYPE.MANAGED_SERVER, srvrName, srvrHost, aHost, nPort, new File(domainHome).getCanonicalPath());
//                }
//        }
//    }
//
//    public void startStopNM(TestEnvHelper.START_STOP action) throws Exception {
//        String nAddress = null;
//        for (NodeManager n : envModel.getNodeManagers()) {
//            nAddress = n.getTuningParameters().getSettingValueByAlias("NmAddress");
//            TestEnvHelper.operateNodeManager(action, nAddress);
//        }
//    }

//    public void startServers() throws Exception {
//        TestEnvHelper.operateNodeManager(TestEnvHelper.START_STOP.START, adminHost);
//        TestEnvHelper.operateNodeManager(TestEnvHelper.START_STOP.START, server1Host);
//        TestEnvHelper.operateNodeManager(TestEnvHelper.START_STOP.START, server2Host);
//        TestEnvHelper.operateServer(TestEnvHelper.START_STOP.START, TestEnvHelper.SERVER_TYPE.ADMIN, "AdminServer", adminHost, adminHost, nmPort, new File(domainHome).getCanonicalPath());
//        TestEnvHelper.operateCluster(TestEnvHelper.START_STOP.START, clusterName, adminHost, adminPort);
//    }
//
//    public void stopServers() throws Exception {
//        TestEnvHelper.operateCluster(TestEnvHelper.START_STOP.STOP, clusterName, adminHost, adminPort);
//        TestEnvHelper.operateServer(TestEnvHelper.START_STOP.STOP, TestEnvHelper.SERVER_TYPE.ADMIN, "AdminServer", adminHost, adminHost, nmPort, new File(domainHome).getCanonicalPath());
//        TestEnvHelper.operateNodeManager(TestEnvHelper.START_STOP.STOP, server1Host);
//        TestEnvHelper.operateNodeManager(TestEnvHelper.START_STOP.STOP, server2Host);
//        TestEnvHelper.operateNodeManager(TestEnvHelper.START_STOP.STOP, adminHost);
//    }


    @Test(priority = 1, enabled = true, description = "OPATCHAUTO_FMWZDT_PATCHDEPLOY_FILE")
    public void testPatchdeploy() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("ANALYZE");
        if (AbstractScriptRunner.patchExternal.equalsIgnoreCase("true")) {
            testPatchLocation = AbstractScriptRunner.patchHome;
        }
        System.out.println("Test Patch Location : " + testPatchLocation);

        try {
            List<String> results = new ArrayList<String>();
            File[] files = new File(testPatchLocation).listFiles();
            for (File file : files) {
                if (file.isDirectory() && new File(file, "/etc/config/patchdeploy.xml").exists()) {
                    System.out.println("Patchdeploy.xml exist in :" + file.getCanonicalPath());
                } else {
                    throw new Exception("[error] Patchdeploy does not exist in : " + file.getAbsolutePath());
                }
            }
            System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
        } catch (Exception e) {
//            e.printStackTrace();
            System.out.println("[Failure]: " + AbstractScriptRunner.testName + "\n");
            throw e;
        }


    }

    @Test(priority = 2, enabled = true, description = "OPATCHAUTO_FMWZDT_PATCH_UPTIME_OPTION")
    public void testPatchUptimeOption() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("ANALYZE");
        if (AbstractScriptRunner.patchExternal.equalsIgnoreCase("true")) {
            testPatchLocation = AbstractScriptRunner.patchHome;
        }
        System.out.println("Test Patch Location : " + testPatchLocation);

        try {

            List<String> results = new ArrayList<String>();
            List<String> resultScan = new ArrayList<String>();
            File[] files = new File(testPatchLocation).listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    File f1 = new File(file, "/etc/config/patchdeploy.xml");
                    if (f1.exists()) {
                        results.add(f1.getCanonicalPath());
                    }
                } else {
                    throw new Exception("[error] Patchdeploy does not exist in : " + file.getAbsolutePath());
                }
            }

//            System.out.println("Results output : " + results);
            System.out.println("Below patchdeploy files must contain patch-uptime-option tag :");
            for (String f : results) {
                Scanner scanner = null;
                System.out.println("File Name : " + f);
                scanner = new Scanner(new FileInputStream(new File(f)));
                while (scanner.hasNext()) {
                    if (scanner.nextLine().contains("patch-uptime-option")) {
                        resultScan.add(f);
                        System.out.println(" File " + f + " contains patch-uptime-option tag.");
                    }
                }
                scanner.close();
            }
//            System.out.println("Results scan output :" + resultScan);
            if (results.equals(resultScan) && !results.equals(null)) {
                isSuccess = true;
            } else {
                isSuccess = false;
            }


            if (isSuccess) {
                System.out.println("Success Status = " + isSuccess);
                System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
            } else {
                System.out.println("Success Status = " + isSuccess);
                throw new Exception("[error] patch-uptime-option tag missing from patchdeploy.xml file. ");
            }

        } catch (Exception e) {
//            e.printStackTrace();
            System.out.println("[Failure]: " + AbstractScriptRunner.testName + "\n");
            throw e;
        }
    }

    @Test(priority = 3, enabled = true, description = "OPATCHAUTO_FMWZDT_ANALYZE")
    public void testAnalyze() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("ANALYZE");
        if (AbstractScriptRunner.patchExternal.equalsIgnoreCase("true")) {
            testPatchLocation = AbstractScriptRunner.patchHome;
        }
        System.out.println("Test Patch Location : " + testPatchLocation);
        String command = (String.format("%1$s apply -analyze" +
                        " -phBaseDir %2$s" +
                        " -topology %3$s" +
                        " -wallet %4$s" +
                        (AbstractScriptRunner.walletType.equals("ENCRYPTED") ? " -walletPassword %5$s" : "%5$s") +
                        " -wls-admin-host %6$s" +
                        " -log %7$s",
                new File(AbstractScriptRunner.opatchautoHome + File.separator + AbstractScriptRunner.scriptName).getCanonicalPath(),
                testPatchLocation,
                AbstractScriptRunner.topologyLoc,
                AbstractScriptRunner.walletLocation,
                AbstractScriptRunner.walletPassword,
                AbstractScriptRunner.adminHost,
                new File(AbstractScriptRunner.testDir, AbstractScriptRunner.testName + ".session.log").getCanonicalPath()));
        CommandLine cmdString = CommandLine.parse(command);
        System.out.println("Executing command : " + command);
        int exitValue = 1;
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

            setupExecutorLog();
            AbstractScriptRunner.executor.setWorkingDirectory(new File(AbstractScriptRunner.opatchautoHome));
            AbstractScriptRunner.executor.setStreamHandler(new PumpStreamHandler(AbstractScriptRunner.outputCollector, AbstractScriptRunner.errorCollector));
            isSuccess = false;
            if (AbstractScriptRunner.outputCollector.toString().contains("OPatchAuto failed")) {
                isSuccess = false;
            } else {
                isSuccess = true;
            }
            if (isSuccess) {
                System.out.println("Success Status = " + isSuccess);
                System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
            } else {
                System.out.println("Success Status = " + isSuccess);
                throw new Exception("[error] Incompatible patch. ");
            }

        } catch (Exception e) {
//            e.printStackTrace();
            System.out.println("[Failure]: " + AbstractScriptRunner.testName + "\n");
            throw e;
        }

    }

    @Test(priority = 4, enabled = true, description = "OPATCHAUTO_FMWZDT_MULTINODE_PATCH_UPTIME_NONE_ROLLING")
    public void testMNUptimeNoneRolling() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("NONE");
        System.out.println("Test Patch Location : " + testPatchLocation);
        CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLY, FMWZDT_PLAN.ROLLING_PLAN, sessionID, testPatchLocation,
                AbstractScriptRunner.oracleHome, AbstractScriptRunner.domainHome, AbstractScriptRunner.server1Host, AbstractScriptRunner.server2Host, AbstractScriptRunner.adminHost, AbstractScriptRunner.adminPort, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");
        setupExecutorLog();
        AbstractScriptRunner.executor.setWorkingDirectory(new File(AbstractScriptRunner.opatchautoHome));
        AbstractScriptRunner.executor.setStreamHandler(new PumpStreamHandler(AbstractScriptRunner.outputCollector, AbstractScriptRunner.errorCollector));
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


            isSuccess = false;
            if (AbstractScriptRunner.outputCollector.toString().contains("OPATCHAUTO-71067") || AbstractScriptRunner.outputCollector.toString().contains("Unsupported patch plan")) {
                isSuccess = true;
            } else {
                isSuccess = false;
            }

            if (!isSuccess) {
                System.out.println("Success Status = " + isSuccess);
                throw new Exception("[error] Exception Occured During execution ");
            } else {
                boolean diffFoundInLists;
                List<String> hostAddresses = new ArrayList<>();
                hostAddresses.add(AbstractScriptRunner.adminHost);
                hostAddresses.add(AbstractScriptRunner.server1Host);
                hostAddresses.add(AbstractScriptRunner.server2Host);
                diffFoundInLists = startStopExecutionOrderProcessing.readSessionLogToCheckStartStopSequence(oracleHome, domainHome, hostAddresses, null, sessionLog, String.valueOf(FMWZDT_PLAN.ROLLING_PLAN), testName);

                if(!diffFoundInLists) {
                    System.out.println("Success Status = " + diffFoundInLists);
                    System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
                }else{
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

    @Test(priority = 5, enabled = true, description = "OPATCHAUTO_FMWZDT_MULTINODE_PATCH_UPTIME_NONE_PARALLEL")
    public void testMNUptimeNoneParallel() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("NONE");
        System.out.println("Test Patch Location : " + testPatchLocation);
        CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLY, FMWZDT_PLAN.PARALLEL_PLAN, sessionID, testPatchLocation,
                AbstractScriptRunner.oracleHome, AbstractScriptRunner.domainHome, AbstractScriptRunner.server1Host, AbstractScriptRunner.server2Host, AbstractScriptRunner.adminHost, AbstractScriptRunner.adminPort, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");

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
                    !AbstractScriptRunner.outputCollector.toString().contains("OPatchAuto failed")) {
                isSuccess = true;
            } else {
                isSuccess = false;
            }

            if (!isSuccess) {
                System.out.println("Success Status = " + isSuccess);
                throw new Exception("[error] Exception Occured During execution ");
            } else {
                boolean diffFoundInLists;
                List<String> hostAddresses = new ArrayList<>();
                hostAddresses.add(AbstractScriptRunner.adminHost);
                hostAddresses.add(AbstractScriptRunner.server1Host);
                hostAddresses.add(AbstractScriptRunner.server2Host);
                diffFoundInLists = startStopExecutionOrderProcessing.readSessionLogToCheckStartStopSequence(oracleHome, domainHome, hostAddresses, null, sessionLog, String.valueOf(FMWZDT_PLAN.PARALLEL_PLAN), testName);

                if(!diffFoundInLists) {
                    System.out.println("Success Status = " + diffFoundInLists);
                    System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
                }else{
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

    @Test(priority = 6, enabled = true, description = "OPATCHAUTO_FMWZDT_MULTINODE_WITH_TOPOLOGY_PATCH_UPTIME_NONE_PARALLEL")
    public void testMNTUptimeRollingParallel() throws Exception {

        printHeader(AbstractScriptRunner.testName);
        getTestPatches("NONE");
        System.out.println("Test Patch Location : " + testPatchLocation);
        System.out.println("Topology File Location : " + AbstractScriptRunner.topologyLoc);
        CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLY, FMWZDT_PLAN.PARALLEL_PLAN,
                testPatchLocation, sessionID, AbstractScriptRunner.topologyLoc, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");

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
                    !AbstractScriptRunner.outputCollector.toString().contains("OPatchAuto failed")) {
                isSuccess = true;
            } else {
                isSuccess = false;
            }

            if (!isSuccess) {
                System.out.println("Success Status = " + isSuccess);
                throw new Exception("[error] Exception Occured During execution ");
            } else {
                boolean diffFoundInLists;
                diffFoundInLists = startStopExecutionOrderProcessing.readSessionLogToCheckStartStopSequence(oracleHome, domainHome, Collections.<String>emptyList(), topologyLoc, sessionLog, String.valueOf(FMWZDT_PLAN.PARALLEL_PLAN), testName);
                if(!diffFoundInLists) {
                    System.out.println("Success Status = " + diffFoundInLists);
                    System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
                }else{
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

    @Test(priority = 7, enabled = true, description = "OPATCHAUTO_FMWZDT_MULTINODE_WITH_TOPOLOGY_PATCH_UPTIME_ROLLING_OH")
    public void testMNTUptimeRollingOH() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("ROLLING");
        System.out.println("Test Patch Location : " + testPatchLocation);
        System.out.println("Topology File Location : " + AbstractScriptRunner.topologyLoc);
        CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLY, FMWZDT_PLAN.ROLLING_PLAN,
                testPatchLocation, sessionID, AbstractScriptRunner.topologyLoc, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");
        exitValue = 1;
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
            setupExecutorLog();
            AbstractScriptRunner.executor.setWorkingDirectory(new File(AbstractScriptRunner.opatchautoHome));
            AbstractScriptRunner.executor.setStreamHandler(new PumpStreamHandler(AbstractScriptRunner.outputCollector, AbstractScriptRunner.errorCollector));

            isSuccess = false;
            if (AbstractScriptRunner.outputCollector.toString().contains("failure") || AbstractScriptRunner.outputCollector.toString().contains("Failed") ||
                    AbstractScriptRunner.outputCollector.toString().contains("OPatchAuto failed")) {
                isSuccess = false;
            } else {
                isSuccess = true;
            }

            if (!isSuccess) {
                System.out.println("Success Status = " + isSuccess);
                throw new Exception("[error] Exception Occured During execution ");
            } else {
                boolean diffFoundInLists;
                diffFoundInLists = startStopExecutionOrderProcessing.readSessionLogToCheckStartStopSequence(oracleHome, domainHome, Collections.<String>emptyList(), topologyLoc, sessionLog, String.valueOf(FMWZDT_PLAN.ROLLING_PLAN), testName);
                if(!diffFoundInLists) {
                    System.out.println("Success Status = " + diffFoundInLists);
                    System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
                }else{
                    System.out.println("Success Status = " + diffFoundInLists);
                    throw new Exception("[error] Diff Found in Lists . Exception Occured During execution ");
                }
            }

        } catch (Exception e) {
//            rollbackPatch();
//            e.printStackTrace();
            System.out.println("[Failure]: " + AbstractScriptRunner.testName + "\n");
            throw e;
        } finally {
            rollbackPatch();
        }
    }

    @Test(priority = 8, enabled = true, description = "OPATCHAUTO_FMWZDT_MULTINODE_WITH_TOPOLOGY_PATCH_UPTIME_ROLLING_OH_PARALLEL")
    public void testMNTUptimeRollingOHParallel() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("ROLLING");
        System.out.println("Test Patch Location : " + testPatchLocation);
        System.out.println("Topology File Location : " + AbstractScriptRunner.topologyLoc);
        CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLY, FMWZDT_PLAN.PARALLEL_PLAN,
                testPatchLocation, sessionID, AbstractScriptRunner.topologyLoc, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");
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
                    !AbstractScriptRunner.outputCollector.toString().contains("OPatchAuto failed")) {
                isSuccess = true;
            } else {
                isSuccess = false;
            }

            if (!isSuccess) {
                System.out.println("Success Status = " + isSuccess);
                throw new Exception("[error] Exception Occured During execution ");
            } else {
                boolean diffFoundInLists;
                diffFoundInLists = startStopExecutionOrderProcessing.readSessionLogToCheckStartStopSequence(oracleHome, domainHome, Collections.<String>emptyList(), topologyLoc, sessionLog, String.valueOf(FMWZDT_PLAN.PARALLEL_PLAN), testName);
                if(!diffFoundInLists) {
                    System.out.println("Success Status = " + diffFoundInLists);
                    System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
                }else{
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

    @Test(priority = 9, enabled = true, description = "OPATCHAUTO_FMWZDT_MULTINODE_PATCH_UPTIME_MULITPATCH_ROLLING")
    public void testMNUptimeMultiPatchRolling() throws Exception {
        /* we are actually covering OPATCHAUTO_FMWZDT_MULTINODE_WITH_TOPOLOGY_PATCH_UPTIME_MULITPATCH_ROLLING
         * in thi case so it covers multinode with multipatch case as well
         */
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("MULTIPATCH");
        System.out.println("Test Patch Location : " + testPatchLocation);
        System.out.println("Topology File Location : " + AbstractScriptRunner.topologyLoc);
        CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLY, FMWZDT_PLAN.ROLLING_PLAN,
                testPatchLocation, sessionID, AbstractScriptRunner.topologyLoc, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");
        setupExecutorLog();
        AbstractScriptRunner.executor.setWorkingDirectory(new File(AbstractScriptRunner.opatchautoHome));
        AbstractScriptRunner.executor.setStreamHandler(new PumpStreamHandler(AbstractScriptRunner.outputCollector, AbstractScriptRunner.errorCollector));
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


            isSuccess = false;
            if (AbstractScriptRunner.outputCollector.toString().contains("OPATCHAUTO-71067") || AbstractScriptRunner.outputCollector.toString().contains("Unsupported patch plan")) {
                isSuccess = true;
            } else {
                isSuccess = false;
            }

            if (!isSuccess) {
                System.out.println("Success Status = " + isSuccess);
                throw new Exception("[error] Excepecting Exception. ");
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

    @Test(priority = 10, enabled = true, description = "OPATCHAUTO_FMWZDT_MULTINODE_PATCH_UPTIME_MULITPATCH_PARALLEL")
    public void testMNUptimeMultiPatchParallel() throws Exception {

        /* we are actually covering OPATCHAUTO_FMWZDT_MULTINODE_WITH_TOPOLOGY_PATCH_UPTIME_MULITPATCH_PARALLEL
         * in thi case so it covers multinode with multipatch case as well
         */
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("MULTIPATCH");
        System.out.println("Test Patch Location : " + testPatchLocation);
        System.out.println("Topology File Location : " + AbstractScriptRunner.topologyLoc);
        CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLY, FMWZDT_PLAN.PARALLEL_PLAN,
                testPatchLocation, sessionID, AbstractScriptRunner.topologyLoc, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");

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

    @Test(priority = 11, enabled = false, description = "OPATCHAUTO_FMWZDT_SINGLENODE_WITH_NM_WITH_MAC_PATCH_UPTIME_ANY")
    public void test_WNWM_UptimeAny() throws Exception {
    }

    @Test(priority = 12, enabled = false, description = "OPATCHAUTO_FMWZDT_SINGLENODE_WITH_NM_NO_MAC_PATCH_UPTIME_ANY")
    public void test_WNNM_UptimeAny() throws Exception {
    }

    @Test(priority = 13, enabled = false, description = "OPATCHAUTO_FMWZDT_SINGLENODE_NO_NM_WITH_MAC_PATCH_UPTIME_ANY")
    public void test_NNWM_UptimeAny() throws Exception {
    }

    @Test(priority = 14, enabled = false, description = "OPATCHAUTO_FMWZDT_SINGLENODE_NO_NM_NO_MAC_PATCH_UPTIME_ANY")
    public void test_NNNM_UptimeAny() throws Exception {
    }

    @Test(priority = 15, enabled = false, description = "OPATCHAUTO_FMWZDT_MULTINODE_NO_NM_NO_MAC_PATCH_UPTIME_ROLLING_OH")
    public void testMN_NNNM_UptimeRollingOH() throws Exception {
    }

    @Test(priority = 16, enabled = true, description = "OPATCHAUTO_FMWZDT_PATCH_UPTIME_EMPTY")
    public void testPatchUptimeEmpty() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("EMPTY");
        System.out.println("Test Patch Location : " + testPatchLocation);
        CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLY, FMWZDT_PLAN.ROLLING_PLAN,
                testPatchLocation, sessionID, AbstractScriptRunner.domainHome, AbstractScriptRunner.adminHost, AbstractScriptRunner.adminPort, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");

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

            if (!isSuccess) {
                System.out.println("Success Status = " + isSuccess);
                throw new Exception("[error] Exception Occured During execution ");
            } else {
                boolean diffFoundInLists;
                diffFoundInLists = startStopExecutionOrderProcessing.readSessionLogToCheckStartStopSequence(oracleHome, domainHome, Collections.singletonList(AbstractScriptRunner.adminHost), null, sessionLog, String.valueOf(FMWZDT_PLAN.ROLLING_PLAN), testName);
                if(!diffFoundInLists) {
                    System.out.println("Success Status = " + diffFoundInLists);
                    System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
                }else{
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
        System.out.println("[success] Completed " + AbstractScriptRunner.testName);
    }

    @Test(priority = 17, enabled = true, description = "OPATCHAUTO_FMWZDT_PATCH_UPTIME_NONE")
    public void testPatchUptimeNone() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("NONE");
        System.out.println("Test Patch Location : " + testPatchLocation);
        CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLY, FMWZDT_PLAN.ROLLING_PLAN,
                testPatchLocation, sessionID, AbstractScriptRunner.domainHome, AbstractScriptRunner.adminHost, AbstractScriptRunner.adminPort, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");

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

            if (!isSuccess) {
                System.out.println("Success Status = " + isSuccess);
                throw new Exception("[error] Exception Occured During execution ");
            } else {
                boolean diffFoundInLists;
                diffFoundInLists = startStopExecutionOrderProcessing.readSessionLogToCheckStartStopSequence(oracleHome, domainHome, Collections.singletonList(AbstractScriptRunner.adminHost), null, sessionLog, String.valueOf(FMWZDT_PLAN.ROLLING_PLAN), testName);
                if(!diffFoundInLists) {
                    System.out.println("Success Status = " + diffFoundInLists);
                    System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
                }else{
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

    @Test(priority = 18, enabled = true, description = "OPATCHAUTO_FMWZDT_PATCH_UPTIME_ROLLINGSESSION")
    public void testPatchUptimeRollingSession() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("SESSION");
        System.out.println("Test Patch Location : " + testPatchLocation);
        CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLY, FMWZDT_PLAN.ROLLING_PLAN,
                testPatchLocation, sessionID, AbstractScriptRunner.domainHome, AbstractScriptRunner.adminHost, AbstractScriptRunner.adminPort, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");

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

            if (!isSuccess) {
                System.out.println("Success Status = " + isSuccess);
                throw new Exception("[error] Exception Occured During execution ");
            } else {
                boolean diffFoundInLists;
                diffFoundInLists = startStopExecutionOrderProcessing.readSessionLogToCheckStartStopSequence(oracleHome, domainHome, Collections.singletonList(AbstractScriptRunner.adminHost), null, sessionLog, String.valueOf(FMWZDT_PLAN.ROLLING_PLAN), testName);
                if(!diffFoundInLists) {
                    System.out.println("Success Status = " + diffFoundInLists);
                    System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
                }else{
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

    @Test(priority = 19, enabled = true, description = "OPATCHAUTO_FMWZDT_PATCH_UPTIME_ROLLING_ORACLEHOME")
    public void testPatchUptimeRollingOracleHome() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("ROLLING");
        System.out.println("Test Patch Location : " + testPatchLocation);
        CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLY, FMWZDT_PLAN.ROLLING_PLAN,
                testPatchLocation, sessionID, AbstractScriptRunner.domainHome, AbstractScriptRunner.adminHost, AbstractScriptRunner.adminPort, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");

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

            if (!isSuccess) {
                System.out.println("Success Status = " + isSuccess);
                throw new Exception("[error] Exception Occured During execution ");
            } else {
                boolean diffFoundInLists;
                diffFoundInLists = startStopExecutionOrderProcessing.readSessionLogToCheckStartStopSequence(oracleHome, domainHome, Collections.singletonList(AbstractScriptRunner.adminHost), null, sessionLog, String.valueOf(FMWZDT_PLAN.ROLLING_PLAN), testName);
                if(!diffFoundInLists) {
                    System.out.println("Success Status = " + diffFoundInLists);
                    System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
                }else{
                    System.out.println("Success Status = " + diffFoundInLists);
                    throw new Exception("[error] Diff Found in Lists . Exception Occured During execution ");
                }
            }
        } catch (Exception e) {
            rollbackPatch();
            e.printStackTrace();
            System.out.println("[Failure}: " + AbstractScriptRunner.testName + "\n");
            throw e;
        }
    }

    @Test(priority = 20, enabled = true, description = "OPATCHAUTO_FMWZDT_PATCH_UPTIME_ONLINE")
    public void testPatchUptimeOnline() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("ONLINE");
        System.out.println("Test Patch Location : " + testPatchLocation);
        CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLY, FMWZDT_PLAN.ROLLING_PLAN,
                testPatchLocation, sessionID, AbstractScriptRunner.domainHome, AbstractScriptRunner.adminHost, AbstractScriptRunner.adminPort, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");

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
                    !AbstractScriptRunner.outputCollector.toString().contains("OPatchAuto failed")) {
                isSuccess = true;
            } else {
                isSuccess = false;
            }

            if (!isSuccess) {
                System.out.println("Success Status = " + isSuccess);
                throw new Exception("[error] Exception Occured During execution ");
            } else {
                boolean diffFoundInLists;
                diffFoundInLists = startStopExecutionOrderProcessing.readSessionLogToCheckStartStopSequence(oracleHome, domainHome, Collections.singletonList(AbstractScriptRunner.adminHost), null, sessionLog, String.valueOf(FMWZDT_PLAN.ROLLING_PLAN), testName);
                if(!diffFoundInLists) {
                    System.out.println("Success Status = " + diffFoundInLists);
                    System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
                }else{
                    System.out.println("Success Status = " + diffFoundInLists);
                    throw new Exception("[error] Diff Found in Lists . Exception Occured During execution ");
                }
            }
        } catch (Exception e) {
            rollbackPatch();
            e.printStackTrace();
            System.out.println("[Failure}: " + AbstractScriptRunner.testName + "\n");
            throw e;
        }
    }

    @Test(priority = 21, enabled = true, description = "OPATCHAUTO_FMWZDT_SINGLENODE_PATCH_UPTIME_MULITPATCH")
    public void testPatchUptimeMultiPatch() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("MULTIPATCH");
        System.out.println("Test Patch Location : " + testPatchLocation);
        CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLY, FMWZDT_PLAN.ROLLING_PLAN,
                testPatchLocation, sessionID, AbstractScriptRunner.domainHome, AbstractScriptRunner.adminHost, AbstractScriptRunner.adminPort, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");


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

    @Test(priority = 22, enabled = true, description = "OPATCHAUTO_FMWZDT_PATCH_UPTIME_INVALID")
    public void testPatchUptimeInvalid() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("INVALID");
        System.out.println("Test Patch Location : " + testPatchLocation);
        CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLY, FMWZDT_PLAN.ROLLING_PLAN,
                testPatchLocation, sessionID, AbstractScriptRunner.domainHome, AbstractScriptRunner.adminHost, AbstractScriptRunner.adminPort, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");

        setupExecutorLog();
        AbstractScriptRunner.executor.setWorkingDirectory(new File(AbstractScriptRunner.opatchautoHome));
        AbstractScriptRunner.executor.setStreamHandler(new PumpStreamHandler(AbstractScriptRunner.outputCollector, AbstractScriptRunner.errorCollector));
        exitValue = 1;
        try {
            try {
                exitValue = AbstractScriptRunner.executor.execute(cmdString);
                Assert.assertEquals(0, exitValue);
            } catch (ExecuteException ex) {
                System.out.println("[Error] : Command Execution Failed!!");
            } finally {
                writeToFile(AbstractScriptRunner.testName + ".console", AbstractScriptRunner.outputCollector.toString());
            }

            isSuccess = false;
            if (AbstractScriptRunner.outputCollector.toString().contains("Failed to load patch") || AbstractScriptRunner.outputCollector.toString().contains("Failed") ||
                    AbstractScriptRunner.outputCollector.toString().contains("OPatchAuto failed")) {
                isSuccess = true;
            } else {
                isSuccess = false;
            }

            if (!isSuccess) {
                System.out.println("Success Status = " + isSuccess);
                rollbackPatch();
                throw new Exception("[error] Exception Occured During execution ");
            } else {
                System.out.println("Success Status = " + isSuccess);
                System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[Failure]: " + AbstractScriptRunner.testName + "\n");
            throw e;
        }
    }

    @Test(priority = 23, enabled = true, description = "OPATCHAUTO_FMWZDT_MULTINODE_WITH_TOPOLOGY_PATCH_UPTIME_ROLLING_OH_OOP")
    public void testMNTUptimeRollingOHOOP() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("ROLLING");
        System.out.println("Test Patch Location : " + testPatchLocation);
        System.out.println("Topology File Location : " + AbstractScriptRunner.topologyLoc);
        CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLYOOP, FMWZDT_PLAN.ROLLING_PLAN,
                testPatchLocation, sessionID, AbstractScriptRunner.topologyLoc, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");
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

    @Test(priority = 24, enabled = true, description = "OPATCHAUTO_FMWZDT_MULTINODE_HOST_ORDER_CHANGE")
    public void testMNHostOrderChange() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("ROLLING");
        System.out.println("Test Patch Location : " + testPatchLocation);
        CommandLine cmdString = fmwZDTHostOrder(FMWZDT_OPERATION.APPLY, FMWZDT_PLAN.ROLLING_PLAN, sessionID, testPatchLocation,
                AbstractScriptRunner.oracleHome, AbstractScriptRunner.domainHome, AbstractScriptRunner.server1Host, AbstractScriptRunner.server2Host, AbstractScriptRunner.adminHost, AbstractScriptRunner.adminPort, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");

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

            if (hasMessage) {
                System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
            } else {
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

    @Test(priority = 25, enabled = true, description = "OPATCHAUTO_FMWZDT_SINGLENODE_DIFFERENT_SERVER_STATE")
    public void testSNDifferentServerState() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        if (singleHost()) {
            getTestPatches("ROLLING");
            System.out.println("Test Patch Location : " + testPatchLocation);
            System.out.println("Topology File Location : " + AbstractScriptRunner.topologyLoc);
            stopManaged();
            CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLY, FMWZDT_PLAN.ROLLING_PLAN,
                    testPatchLocation, sessionID, AbstractScriptRunner.topologyLoc, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");
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

                isSuccess = false;
                if (AbstractScriptRunner.outputCollector.toString().contains("failure") || AbstractScriptRunner.outputCollector.toString().contains("Failed") ||
                        AbstractScriptRunner.outputCollector.toString().contains("OPatchAuto failed")) {
                    isSuccess = false;
                } else {
                    isSuccess = true;
                }

                if (isSuccess == false) {
                    System.out.println("Success Status = " + isSuccess);
                    throw new Exception("[Error] Patching Operation Failed. ");
                } else {
                    System.out.println("Success Status = " + isSuccess);
                    System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
                }

            } catch (Exception e) {
//            rollbackPatch();
//            e.printStackTrace();
                System.out.println("[Failure]: " + AbstractScriptRunner.testName + "\n");
                throw e;
            } finally {
                rollbackPatch();
            }
        }else{
            System.out.println("This test case is for Single Node environment, Hence skipping this test case.");
            System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
        }
    }

    @Test(priority = 26, enabled = true, description = "OPATCHAUTO_FMWZDT_MULTINODE_DIFFERENT_SERVER_STATE")
    public void testMNDifferentServerState() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("ROLLING");
        System.out.println("Test Patch Location : " + testPatchLocation);
        System.out.println("Topology File Location : " + AbstractScriptRunner.topologyLoc);
        stopManaged();
        CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLY, FMWZDT_PLAN.ROLLING_PLAN,
                testPatchLocation, sessionID, AbstractScriptRunner.topologyLoc, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");
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

            isSuccess = false;
            if (AbstractScriptRunner.outputCollector.toString().contains("failure") || AbstractScriptRunner.outputCollector.toString().contains("Failed") ||
                    AbstractScriptRunner.outputCollector.toString().contains("OPatchAuto failed")) {
                isSuccess = false;
            } else {
                isSuccess = true;
            }

            if (isSuccess == false) {
                System.out.println("Success Status = " + isSuccess);
                throw new Exception("[Error] Patching Operation Failed. ");
            } else {
                System.out.println("Success Status = " + isSuccess);
                System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
            }

        } catch (Exception e) {
//            rollbackPatch();
//            e.printStackTrace();
            System.out.println("[Failure]: " + AbstractScriptRunner.testName + "\n");
            throw e;
        } finally {
            rollbackPatch();
        }
    }

    @Test(priority = 27, enabled = true, description = "OPATCHAUTO_FMWZDT_ONLINE_DISCOVERY_REMOTE_HOST")
    public void testMNOnlineDiscoveryRemoteHost() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("ROLLING");
        System.out.println("Test Patch Location : " + testPatchLocation);
        try {
            ArrayList<String> mServer = new ArrayList<String>();
            String managedServer1=null;
            String managedServer2=null;
            String username=null;
            String password=null;

            for (Host h : envModel.getHosts()) {
                mServer.add(h.getAddress());
            }
            managedServer1=mServer.get(1);
            managedServer2=mServer.get(2);

            Session session;
            ChannelExec channelExec = null;
            ChannelSftp channelSftp = null;
            InputStream in1 = null;
            System.out.println("Managed server 1 : "+managedServer1);
            System.out.println("Managed server 2 : "+managedServer2);

            boolean success = false;
            for (oracle.fmwplatform.credentials.credential.Credential c : creds){
//                System.out.println("Alias : " +c.getAlias());
                if(c.getAlias().equals(managedServer1+":ssh")){
                    username=c.getUsername();
                    password=new String(c.getPassword());

                }
            }
            final String hostUser = username;
            final String hostPassword = password;
            System.out.println("Host        : " + managedServer1);

            //Copy NM Scripts to Host to Update NM Properties
            JSch jsch = new JSch();
            session = jsch.getSession(hostUser, managedServer1, 22);
            session.setPassword(hostPassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            session.connect(15 * 1000);

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            channelSftp.cd(oracleHome + File.separator + ".." + File.separator);
            System.out.println("[zdt-qa.info] Copying wallet to host : " + managedServer1);
            File sourceFile = new File(walletLocation);
            File[] files = sourceFile.listFiles();

            if (files != null && !sourceFile.getName().startsWith(".")) {
                channelSftp.cd(oracleHome + File.separator + ".." + File.separator);
                SftpATTRS attrs = null;

                // check if the directory is already existing
                try {
                    attrs = channelSftp.stat(oracleHome + File.separator + ".." + File.separator + "/" + sourceFile.getName());
                } catch (Exception e) {
                    System.out.println(oracleHome + File.separator + ".." + File.separator + "/" + sourceFile.getName() + " not found");
                }

                // else create a directory
                if (attrs != null) {
                    System.out.println("Directory exists IsDir=" + attrs.isDir());
                } else {
                    System.out.println("Creating dir " + sourceFile.getName());
                    channelSftp.mkdir(sourceFile.getName());
                }

                for (File f : files) {
                    channelSftp.put(f.getAbsolutePath(), oracleHome + File.separator + ".." + File.separator + "/" + sourceFile.getName());
                }
            }

            Thread.sleep(5 * 1000);
            channelSftp.disconnect();
            sessionLog = new File(File.separator + "net" + File.separator + adminHost + File.separator + testDir, testName + "_APPLY" + ".session.log").getCanonicalPath();
            final String updateCommand = (String.format("%1$s apply %2$s" +
                            " %3$s" +
                            " -oh %4$s" +
                            " -instance %5$s" +
                            " -host %6$s" +
                            " -wallet %7$s" +
                            (walletType.equals("ENCRYPTED") ? " -walletPassword %8$s" : "%8$s") +
                            " -wls-admin-host %9$s" +
                            " -log %10$s",
                    new File(opatchautoHome + File.separator + scriptName).getCanonicalPath(),
                    patchOperation.getOperation(),
                    File.separator + "net" + File.separator + adminHost + File.separator + testPatchLocation,
                    oracleHome,
                    domainHome,
                    adminHost.trim() + "," + managedServer1.trim() + "," + managedServer2.trim(),
                    walletLocation,
                    walletPassword,
                    adminHost.trim() + ":" + adminPort.trim(),
                    new File(sessionLog).getCanonicalPath()));
            channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setCommand(updateCommand);
            channelExec.connect();
            in1 = channelExec.getInputStream();
            System.out.println("Executing Command : " + updateCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in1));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[zdt-qa.info] " + line);
                if (line.contains("OPatchAuto successful.")) {
                    success = true;
                    break;
                }
            }
            if (success) {
                System.out.println("[zdt-qa.info] OPatchAuto successful.");
                System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
                success = false;
            } else
                throw new Exception("[error] OPatchAuto Failed");
            in1.close();
            session.disconnect();
        }catch (Exception e) {
////            rollbackPatch();
            e.printStackTrace();
            System.out.println("[Failure]: " + AbstractScriptRunner.testName + "\n");
            throw e;
        } finally {
//            rollbackPatch();
        }
//        exitValue = 1;
//        setupExecutorLog();
//        AbstractScriptRunner.executor.setWorkingDirectory(new File(AbstractScriptRunner.opatchautoHome));
//        AbstractScriptRunner.executor.setStreamHandler(new PumpStreamHandler(AbstractScriptRunner.outputCollector, AbstractScriptRunner.errorCollector));
//        try {
//            try {
//                exitValue = AbstractScriptRunner.executor.execute(cmdString);
//                Assert.assertEquals(0, exitValue);
//            } catch (Exception ex) {
//                System.out.println("[Error] : Command Execution Failed!!");
//                throw new Exception(ex);
//            } finally {
//                writeToFile(AbstractScriptRunner.testName + ".console", AbstractScriptRunner.outputCollector.toString());
//            }
//
//            isSuccess = false;
//            if (AbstractScriptRunner.outputCollector.toString().contains("failure") || AbstractScriptRunner.outputCollector.toString().contains("Failed") ||
//                    AbstractScriptRunner.outputCollector.toString().contains("OPatchAuto failed")) {
//                isSuccess = false;
//            } else {
//                isSuccess = true;
//            }
//
//            if (isSuccess == false) {
//                System.out.println("Success Status = " + isSuccess);
//                throw new Exception("[Error] Patching Operation Failed. ");
//            } else {
//                System.out.println("Success Status = " + isSuccess);
//                System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
//            }
//
//        } catch (Exception e) {
////            rollbackPatch();
////            e.printStackTrace();
//            System.out.println("[Failure]: " + AbstractScriptRunner.testName + "\n");
//            throw e;
//        } finally {
//            rollbackPatch();
//        }
    }

    @Test(priority = 28, enabled = true, description = "OPATCHAUTO_FMWZDT_ONLINE_DISCOVERY_REMOTE_HOST_ADMIN_SHUTDOWN")
    public void testMNOnlineDiscoveryRemoteHostAdminShutdown() throws Exception {
        printHeader(AbstractScriptRunner.testName);
        getTestPatches("ROLLING");
        System.out.println("Test Patch Location : " + testPatchLocation);

        stopAdmin();
        try {
            ArrayList<String> mServer = new ArrayList<String>();
            String managedServer1=null;
            String managedServer2=null;
            String username=null;
            String password=null;

            for (Host h : envModel.getHosts()) {
                mServer.add(h.getAddress());
            }
            managedServer1=mServer.get(1);
            managedServer2=mServer.get(2);

            Session session;
            ChannelExec channelExec = null;
            ChannelSftp channelSftp = null;
            InputStream in1 = null;

            boolean success = false;
            for (oracle.fmwplatform.credentials.credential.Credential c : creds){
//                System.out.println("Alias : " +c.getAlias());
                if(c.getAlias().equals(managedServer1+":ssh")){
                    username=c.getUsername();
                    password=new String(c.getPassword());

                }
            }
            final String hostUser = username;
            final String hostPassword = password;
            System.out.println("Host        : " + managedServer1);

            //Copy NM Scripts to Host to Update NM Properties
            JSch jsch = new JSch();
            session = jsch.getSession(hostUser, managedServer1, 22);
            session.setPassword(hostPassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            session.connect(15 * 1000);

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            channelSftp.cd(oracleHome + File.separator + ".." + File.separator);
            System.out.println("[zdt-qa.info] Copying wallet to host : " + managedServer1);
            File sourceFile = new File(walletLocation);
            File[] files = sourceFile.listFiles();

            if (files != null && !sourceFile.getName().startsWith(".")) {
                channelSftp.cd(oracleHome + File.separator + ".." + File.separator);
                SftpATTRS attrs = null;

                // check if the directory is already existing
                try {
                    attrs = channelSftp.stat(oracleHome + File.separator + ".." + File.separator + "/" + sourceFile.getName());
                } catch (Exception e) {
                    System.out.println(oracleHome + File.separator + ".." + File.separator + "/" + sourceFile.getName() + " not found");
                }

                // else create a directory
                if (attrs != null) {
                    System.out.println("Directory exists IsDir=" + attrs.isDir());
                } else {
                    System.out.println("Creating dir " + sourceFile.getName());
                    channelSftp.mkdir(sourceFile.getName());
                }

                for (File f : files) {
                    channelSftp.put(f.getAbsolutePath(), oracleHome + File.separator + ".." + File.separator + "/" + sourceFile.getName());
                }
            }

            Thread.sleep(5 * 1000);
            channelSftp.disconnect();
            sessionLog = new File(File.separator + "net" + File.separator + adminHost + File.separator + testDir, testName + "_APPLY" + ".session.log").getCanonicalPath();
            final String updateCommand = (String.format("%1$s apply %2$s" +
                            " %3$s" +
                            " -oh %4$s" +
                            " -instance %5$s" +
                            " -host %6$s" +
                            " -wallet %7$s" +
                            (walletType.equals("ENCRYPTED") ? " -walletPassword %8$s" : "%8$s") +
                            " -wls-admin-host %9$s" +
                            " -log %10$s",
                    new File(opatchautoHome + File.separator + scriptName).getCanonicalPath(),
                    patchOperation.getOperation(),
                    File.separator + "net" + File.separator + adminHost + File.separator + testPatchLocation,
                    oracleHome,
                    domainHome,
                    adminHost.trim() + "," + managedServer1.trim() + "," + managedServer2.trim(),
                    walletLocation,
                    walletPassword,
                    adminHost.trim() + ":" + adminPort.trim(),
                    new File(sessionLog).getCanonicalPath()));
            channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setCommand(updateCommand);
            channelExec.connect();
            in1 = channelExec.getInputStream();
            System.out.println("Executing Command : " + updateCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in1));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[zdt-qa.info] " + line);
                if (line.contains("There is no server running at t3://"+adminHost)) {
                    success = true;
                }
            }
            if (success) {
                System.out.println("[zdt-qa.info] Failing during Online Discovery as Admin Server is not running.");
                System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
                success = false;
            } else
                throw new Exception("[error] OPatchAuto Failed");
            in1.close();
            session.disconnect();
        }catch (Exception e) {
////            rollbackPatch();
            e.printStackTrace();
            System.out.println("[Failure]: " + AbstractScriptRunner.testName + "\n");
            throw e;
        } finally {
            startAdmin();
            rollbackPatch();
        }
//        CommandLine cmdString = fmwZDT(FMWZDT_OPERATION.APPLY, FMWZDT_PLAN.PARALLEL_PLAN, sessionID, testPatchLocation,
//                AbstractScriptRunner.oracleHome, AbstractScriptRunner.domainHome, AbstractScriptRunner.server1Host, AbstractScriptRunner.server2Host, AbstractScriptRunner.adminHost, AbstractScriptRunner.adminPort, AbstractScriptRunner.walletLocation, AbstractScriptRunner.walletPassword, "");
//
//        exitValue = 1;
//        setupExecutorLog();
//        AbstractScriptRunner.executor.setWorkingDirectory(new File(AbstractScriptRunner.opatchautoHome));
//        AbstractScriptRunner.executor.setStreamHandler(new PumpStreamHandler(AbstractScriptRunner.outputCollector, AbstractScriptRunner.errorCollector));
//        try {
//            try {
//                exitValue = AbstractScriptRunner.executor.execute(cmdString);
//                Assert.assertEquals(0, exitValue);
//            } catch (Exception ex) {
//                System.out.println("[Error] : Command Execution Failed!!");
//                throw new Exception(ex);
//            } finally {
//                writeToFile(AbstractScriptRunner.testName + ".console", AbstractScriptRunner.outputCollector.toString());
//            }
//
//            isSuccess = false;
//            if (AbstractScriptRunner.outputCollector.toString().contains("failure") || AbstractScriptRunner.outputCollector.toString().contains("Failed") ||
//                    AbstractScriptRunner.outputCollector.toString().contains("OPatchAuto failed")) {
//                isSuccess = false;
//            } else {
//                isSuccess = true;
//            }
//
//            if (isSuccess == false) {
//                System.out.println("Success Status = " + isSuccess);
//                throw new Exception("[Error] Patching Operation Failed. ");
//            } else {
//                System.out.println("Success Status = " + isSuccess);
//                System.out.println("[Success]: " + AbstractScriptRunner.testName + "\n");
//            }
//
//        } catch (Exception e) {
////            rollbackPatch();
////            e.printStackTrace();
//            System.out.println("[Failure]: " + AbstractScriptRunner.testName + "\n");
//            throw e;
//        } finally {
//            rollbackPatch();
//        }
    }

}