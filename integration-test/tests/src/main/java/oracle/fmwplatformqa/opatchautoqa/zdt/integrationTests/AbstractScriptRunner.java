package oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests;

import oracle.fmwplatform.envspec.environment.OfflineEnvironment;
import oracle.fmwplatform.envspec.environment.topology.DiscoveryOptions;
import oracle.fmwplatform.envspec.model.EnvironmentModel;
import oracle.fmwplatform.envspec.model.EnvironmentModelBuilder;
import oracle.fmwplatform.envspec.model.targets.ModelTarget;
import oracle.fmwplatform.envspec.model.targets.ModelTargetFactory;
import oracle.fmwplatformqa.opatchautoqa.zdt.credential.CredentialManager;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public abstract class AbstractScriptRunner {
    public static String javaHome;
    protected  static String suiteGrp = "FMWZDT";
    //OH Details
    protected static String oracleHome;
//    protected static String oracleInventory;
    protected static String domainHome;
    protected static String domainName;
    protected static String orgDomainHome;
    protected static String domainsDir;
    protected static String adminHost;
    protected static String adminPort;
    protected static String adminSSLPort;
    protected static String server1Host;
    protected static String server1Port;
    protected static String server1SSLPort;
    protected static String server2Host;
    protected static String server2Port;
    protected static String server2SSLPort;
    protected static String nmPort;
    protected static String sslDomainNMPort;
    protected static String clusterName;
    protected static String server1Name;
    protected static String server2Name;

    //Patch Details
    protected static String patchHome;
    protected static String patchExternal;
    private static LinkedHashMap<String, String> testPatches;
    protected static LinkedHashMap<String, String> allPatches;

    //Suite Details
    protected static String suiteXml;

    //Script Details
    protected static String opatchautoHome;
    protected static String ocpHome;
    protected static String scriptName;
    protected static String ocp;
    protected static String opatchAuto;
    protected static String opatch;
    protected static String opatchHome;
    protected static String wlst;
    protected static String wlstHome;
    public static String nodeManager;
    public static String nodeManagerHome;

    //Wallet Details
    protected static String walletLocation;
    protected static String walletPassword;
    protected static String walletType;
    protected static CredentialManager credentialManager;

    //Log Details
    public static String testOutput;
    protected static String logDir;
    protected static File testDir;
    protected static File patchStagingDir;
    protected static File allPatchStagingDir;
    protected static File resourcesDir;
    public static File testEnvScriptsHome;

    //Executor Details
    protected static DefaultExecutor executor;
    protected static CollectingLogOutputStream outputCollector;
    protected static CollectingLogOutputStream errorCollector;
    protected static boolean createNewExecutor = true;
    public static List<String> sessionIDs;
    public static boolean isZDTStarted = false;

    //TestCase Details
    protected static String testName = "testName";
    protected static HashMap<String, String> testNameMapping;
    protected static String mappingKey;

    //Topology location
    public static String topologyLoc;

    public AbstractScriptRunner() {
        credentialManager = new CredentialManager();

        testNameMapping = new HashMap<String, String>();

        //ZDT Tests
        testNameMapping.put("testZDTCommands", "OPATCHAUTO_WLSZDT_COMMANDS");

        testNameMapping.put("setupTestEnv", "OPATCHAUTO_WLSZDT_SETUP_TEST_ENV");

        testNameMapping.put("testCreateImageOOP", "OPATCHAUTO_WLSZDT_CREATE_IMAGE_OOP");
        testNameMapping.put("testPushImageTargetCluster", "OPATCHAUTO_WLSZDT_PUSH_IMAGE_TARGET_CLUSTER");


        testNameMapping.put("testRolloutTargetAdminServer", "OPATCHAUTO_WLSZDT_ROLLOUT_TARGET_ADMINSERVER");
        testNameMapping.put("testRolloutTargetManagedServer", "OPATCHAUTO_WLSZDT_ROLLOUT_TARGET_MANAGEDSERVER");
        testNameMapping.put("testRolloutTargetCluster", "OPATCHAUTO_WLSZDT_ROLLOUT_TARGET_CLUSTER");
        testNameMapping.put("testRollbackSuccessRollout", "OPATCHAUTO_WLSZDT_ROLLBACK_SUCCESS_ROLLOUT");


        testNameMapping.put("testRolloutTargetListOfServers", "OPATCHAUTO_WLSZDT_ROLLOUT_TARGET_LISTOFSERVERS");
        testNameMapping.put("testRolloutTargetDomain", "OPATCHAUTO_WLSZDT_ROLLOUT_TARGET_DOMAIN");

        //TODO - Kill Process has issues with NM
        testNameMapping.put("testRollbackFailedRollout", "OPATCHAUTO_WLSZDT_ROLLBACK_FAILED_ROLLOUT");

        //TODO - Kill Process has issues with NM
        testNameMapping.put("testResumeFailedRolloutTargetManagedServer", "OPATCHAUTO_WLSZDT_RESUME_FAILED_ROLLOUT_TARGET_MANAGEDSERVER");
        testNameMapping.put("testResumeFailedRolloutTargetCluster", "OPATCHAUTO_WLSZDT_RESUME_FAILED_ROLLOUT_TARGET_CLUSTER");
        testNameMapping.put("testResumeFailedRolloutTargetDomain", "OPATCHAUTO_WLSZDT_RESUME_FAILED_ROLLOUT_TARGET_DOMAIN");


        //Negative Cases
        testNameMapping.put("testRolloutInvalidTarget", "OPATCHAUTO_WLSZDT_ROLLOUT_INVALID_TARGET");
        testNameMapping.put("testRolloutDifferentTarget", "OPATCHAUTO_WLSZDT_ROLLOUT_DIFFERENT_TARGET");
        testNameMapping.put("testRolloutInvalidImageLocation", "OPATCHAUTO_WLSZDT_ROLLOUT_INVALID_IMAGELOCATION");
        testNameMapping.put("testRolloutInvalidWalletLocation", "OPATCHAUTO_WLSZDT_ROLLOUT_INVALID_WALLETLOCATION");
        testNameMapping.put("testRolloutInvalidWalletPassword", "OPATCHAUTO_WLSZDT_ROLLOUT_INVALID_WALLETPASSWORD");
        testNameMapping.put("testRolloutInvalidAdminHostname", "OPATCHAUTO_WLSZDT_ROLLOUT_INVALID_ADMIN_HOSTNAME");
        testNameMapping.put("testRolloutInvalidAdminPort", "OPATCHAUTO_WLSZDT_ROLLOUT_INVALID_ADMIN_PORT");
        testNameMapping.put("testResumeInvalidSessionID", "OPATCHAUTO_WLSZDT_RESUME_INVALID_SESSIONID");
        testNameMapping.put("testInvalidPlan", "OPATCHAUTO_WLSZDT_INVALID_PLAN");

        testNameMapping.put("testZDTOptions", "OPATCHAUTO_WLSZDT_ZDTOPTIONS");


        //OOP Tests
        testNameMapping.put("testOOPApplyBinaryPatching", "OPATCHAUTO_OOP_APPLY_BINARYPATCHING");
        testNameMapping.put("testOOPRemoteHostBinaryPatching","OPATCHAUTO_OOP_APPLY_BINARY_PATCHING_REMOTEHOST");
        testNameMapping.put("testOOPRemoteHostBinaryPatchingOnTargetOH","OPATCHAUTO_OOP_APPLY_BINARY_PATCHING_REMOTEHOST_TARGET");
        testNameMapping.put("testOOPCreateImage","OPATCHAUTO_OOP_CREATE_IMAGE");
        testNameMapping.put("testOOPCreateImageMultiplePatches","OPATCHAUTO_OOP_CREATE_IMAGE_WITH_MULTIPLE_PATCHES");
        testNameMapping.put("testOOPCreateImageOnRemoteHostTargetOH","OPATCHAUTO_OOP_CREATE_IMAGE_REMOTEHOST_TARGET");
        testNameMapping.put("testOOPCreateImageInPlace","OPATCHAUTO_OOP_CREATE_IMAGE_INPLACE");
        testNameMapping.put("testOOPApplyImageInPlace","OPATCHAUTO_OOP_APPLY_IMAGE_INPLACE");



        //FMWZDT TestCases Mapping : Begin
//        testNameMapping.put("testEnvSetup","OPATCHAUTO_FMWZDT_PRESETUP");
        testNameMapping.put("testPatchdeploy","OPATCHAUTO_FMWZDT_PATCHDEPLOY_FILE");
        testNameMapping.put("testPatchUptimeOption","OPATCHAUTO_FMWZDT_PATCH_UPTIME_OPTION");
        testNameMapping.put("testAnalyze","OPATCHAUTO_FMWZDT_ANALYZE");
        testNameMapping.put("testPatchUptimeEmpty","OPATCHAUTO_FMWZDT_PATCH_UPTIME_EMPTY");
        testNameMapping.put("testPatchUptimeNone","OPATCHAUTO_FMWZDT_PATCH_UPTIME_NONE");
        testNameMapping.put("testPatchUptimeRollingSession","OPATCHAUTO_FMWZDT_PATCH_UPTIME_ROLLINGSESSION");
        testNameMapping.put("testPatchUptimeRollingOracleHome","OPATCHAUTO_FMWZDT_PATCH_UPTIME_ROLLING_ORACLEHOME");
        testNameMapping.put("testPatchUptimeOnline","OPATCHAUTO_FMWZDT_PATCH_UPTIME_ONLINE");
        testNameMapping.put("testPatchUptimeInvalid","OPATCHAUTO_FMWZDT_PATCH_UPTIME_INVALID");
        testNameMapping.put("testPatchUptimeMultiPatch","OPATCHAUTO_FMWZDT_SINGLENODE_PATCH_UPTIME_MULITPATCH");
        testNameMapping.put("test_WNWM_UptimeAny","OPATCHAUTO_FMWZDT_SINGLENODE_WITH_NM_WITH_MAC_PATCH_UPTIME_ANY");
        testNameMapping.put("test_WNNM_UptimeAny","OPATCHAUTO_FMWZDT_SINGLENODE_WITH_NM_NO_MAC_PATCH_UPTIME_ANY");
        testNameMapping.put("test_NNWM_UptimeAny","OPATCHAUTO_FMWZDT_SINGLENODE_NO_NM_WITH_MAC_PATCH_UPTIME_ANY");
        testNameMapping.put("test_NNNM_UptimeAny","OPATCHAUTO_FMWZDT_SINGLENODE_NO_NM_NO_MAC_PATCH_UPTIME_ANY");
        testNameMapping.put("testMN_NNNM_UptimeRollingOH","OPATCHAUTO_FMWZDT_MULTINODE_NO_NM_NO_MAC_PATCH_UPTIME_ROLLING_OH");
        testNameMapping.put("testMNUptimeNoneRolling","OPATCHAUTO_FMWZDT_MULTINODE_PATCH_UPTIME_NONE_ROLLING");
        testNameMapping.put("testMNUptimeNoneParallel","OPATCHAUTO_FMWZDT_MULTINODE_PATCH_UPTIME_NONE_PARALLEL");
        testNameMapping.put("testMNTUptimeRollingParallel","OPATCHAUTO_FMWZDT_MULTINODE_WITH_TOPOLOGY_PATCH_UPTIME_NONE_PARALLEL");
        testNameMapping.put("testMNTUptimeRollingOH","OPATCHAUTO_FMWZDT_MULTINODE_WITH_TOPOLOGY_PATCH_UPTIME_ROLLING_OH");
        testNameMapping.put("testMNTUptimeRollingOHParallel","OPATCHAUTO_FMWZDT_MULTINODE_WITH_TOPOLOGY_PATCH_UPTIME_ROLLING_OH_PARALLEL");
        testNameMapping.put("testMNUptimeMultiPatchRolling","OPATCHAUTO_FMWZDT_MULTINODE_PATCH_UPTIME_MULITPATCH_ROLLING");
        testNameMapping.put("testMNUptimeMultiPatchParallel","OPATCHAUTO_FMWZDT_MULTINODE_PATCH_UPTIME_MULITPATCH_PARALLEL");
        testNameMapping.put("testMNTUptimeRollingOHOOP","OPATCHAUTO_FMWZDT_MULTINODE_WITH_TOPOLOGY_PATCH_UPTIME_ROLLING_OH_OOP");
        testNameMapping.put("testMNHostOrderChange","OPATCHAUTO_FMWZDT_MULTINODE_HOST_ORDER_CHANGE");
        testNameMapping.put("testSNDifferentServerState","OPATCHAUTO_FMWZDT_SINGLENODE_DIFFERENT_SERVER_STATE");
        testNameMapping.put("testMNDifferentServerState","OPATCHAUTO_FMWZDT_MULTINODE_DIFFERENT_SERVER_STATE");
        testNameMapping.put("testMNOnlineDiscoveryRemoteHost","OPATCHAUTO_FMWZDT_ONLINE_DISCOVERY_REMOTE_HOST");
        testNameMapping.put("testMNOnlineDiscoveryRemoteHostAdminShutdown","OPATCHAUTO_FMWZDT_ONLINE_DISCOVERY_REMOTE_HOST_ADMIN_SHUTDOWN");

        //ssl cases
        testNameMapping.put("testPatchUptimeNoneSSLDomainMultiNodeParallel","OPATCHAUTO_FMWZDT_MULTINODE_SSL_ONLY_DOMAIN_PLAN_PARALLEL");
        testNameMapping.put("testPatchUptimeSSLHybridMultiDomain","OPATCHAUTO_FMWZDT_MULTIPLE_DOMAIN_SINGLE_ORACLE_HOME");
        testNameMapping.put("testPatchUptimeNoneSSLDomainMultiNode","OPATCHAUTO_FMWZDT_MULTINODE_SSL_ONLY_DOMAIN");
        testNameMapping.put("testPatchUptimeNoneSSLDomainSingleNode","OPATCHAUTO_FMWZDT_SINGLENODE_SSL_ONLY_DOMAIN");
        //FMWZDT TestCases Mapping : End
    }

    @BeforeClass()
    public static void setupClass() throws Exception{
        //Env Variable Handling
        javaHome = System.getProperty("java.home");

        //OH Details
        oracleHome = System.getProperty("oracle.home");
//        oracleInventory = System.getProperty("oracle.inventory");
        domainHome = System.getProperty("domain.home");
        domainsDir = new File(domainHome+File.separator+"..").getCanonicalPath();
        domainName = domainHome.split(File.separator)[domainHome.split(File.separator).length - 1];
        //to use in case of QA Domain Creations.
        orgDomainHome = domainHome;
        adminHost = System.getProperty("admin.host");
        adminPort = System.getProperty("admin.port");
        server1Host = System.getProperty("server1.host");
        server1Port = System.getProperty("server1.port");
        server2Host = System.getProperty("server2.host");
        server2Port = System.getProperty("server2.port");
        nmPort = System.getProperty("nm.port");
        clusterName = System.getProperty("cluster.name");
        server1Name = System.getProperty("server1.name");
        server2Name = System.getProperty("server2.name");

        //Patch Details
        patchHome = System.getProperty("patch.home");
        patchExternal = System.getProperty("patch.external");
        allPatches = new LinkedHashMap<String, String>();

        //Suite Details
        suiteXml = System.getProperty("suite.xml");

        //Script Details
        opatchautoHome = System.getProperty("opatchauto.home");
        ocpHome = System.getProperty("ocp.home");
        opatchHome = new File(oracleHome).getCanonicalPath() + File.separator + "OPatch";
        wlstHome = System.getProperty("wlst.home");
        scriptName = "opatchauto.sh";
        opatch = "opatch";
        opatchAuto = "opatchauto.sh";
        ocp = "ocp.sh";
        wlst = "wlst.sh";
        nodeManager = "NodeManager.sh";
        if (System.getProperty("os.name").startsWith("Windows")) {
            scriptName = "opatchauto.cmd";
            opatch = "opatch.bat";
            opatchAuto = "opatchauto.cmd";
            ocp = "ocp.cmd";
            wlst = "wlst.cmd";
            nodeManager = "NodeManager.cmd";
        }
        nodeManagerHome = domainHome + File.separator + "nodemanager";

        //Wallet Details
        walletLocation = new File(System.getProperty("wallet.location")).getCanonicalPath();
        walletPassword = System.getProperty("wallet.password");
        walletType = System.getProperty("wallet.type");
        credentialManager = new CredentialManager();
        credentialManager.setWallet(new File(walletLocation), walletType.equals("ENCRYPTED") ? walletPassword.toCharArray() : null);

        //Log Details
        testOutput = System.getProperty("test.output");
        logDir = testOutput + File.separator + "test-log";
        logDir = new File(logDir).getCanonicalPath();
        resourcesDir = new File(System.getProperty("resources.dir"));
        testEnvScriptsHome = new File(System.getProperty("test.env.scripts.home"));
    }

    private static void unZip(String zipFile, String destination) {
        byte[] buffer = new byte[1024];
        try {
            //create output directory is not exists
            File folder = new File(destination);
            if (!folder.exists()) {
                folder.mkdir();
            }

            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {
                String fileName = ze.getName();
                File newFile = new File(destination + File.separator + fileName);

                //create all non exists folders else you will hit FileNotFoundException for compressed folder
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @BeforeMethod()
    public void setup(Method method) {
        //For restoring the original domain home after executing test cases that create and use a different domain home
        domainHome = orgDomainHome;

        testDir = new File(logDir + File.separator + this.getClass().getSimpleName());
        testDir.mkdirs();

        //Initialize stdOutput and stdError Collectors ang Log Locations
        setupExecutorLog();

        //Used for GTLF Test Case Name Mapping
        if (!method.equals(null))
            mappingKey = method.getName();

        System.out.println("Java Classpath (java.class.path) : ");
        System.out.println(System.getProperty("java.class.path"));
    }

    public void setupExecutorLog() {
        if(createNewExecutor) {
            //Initialize stdOutput and stdError Collectors
            executor = null;
            outputCollector = null;
            errorCollector = null;

            executor = new DefaultExecutor();
            outputCollector = new CollectingLogOutputStream();
            errorCollector = new CollectingLogOutputStream();
        }
    }

    protected void printHeader(String testName) {
        System.out.println("------------------------------------------------------------------------");
        System.out.println("Running " + this.getClass().getSimpleName() + " - " + testName);
        System.out.println("------------------------------------------------------------------------");
    }

    protected void writeToFile(String testName, String content) {
        File name = new File(testDir, testName + ".log");
        try (FileOutputStream fop = new FileOutputStream(name)) {
            if (!name.exists()) {
                name.createNewFile();
            }
            byte[] contentInBytes = content.getBytes();
            fop.write(contentInBytes);
            fop.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void processInventory(String testName, INVENTORY_OPERATION processType) {
        try {
            if (processType.equals(INVENTORY_OPERATION.COPY_RECORDEDACTIONS) || processType.equals(INVENTORY_OPERATION.MOVE_RECORDEDACTIONS)) {
                File recordedActionFile = new File(domainHome, "configpatch/inventory/recorded-actions.xml");
                File archiveRecordedActionFile = new File(testDir, testName + ".recorded-actions.xml");
                if(recordedActionFile.isFile()){
                    FileUtils.copyFile(recordedActionFile, archiveRecordedActionFile);
                    if (processType.equals(INVENTORY_OPERATION.MOVE_RECORDEDACTIONS))
                        recordedActionFile.delete();
                }
            }
            if (processType.equals(INVENTORY_OPERATION.COPY_CONFIGPATCHINV) || processType.equals(INVENTORY_OPERATION.MOVE_CONFIGPATCHINV)) {
                File configPatchInvFile = new File(domainHome, "configpatchinv.xml");
                File archiveConfigPatchInvFile = new File(testDir, testName + ".configpatchinv.xml");
                if(configPatchInvFile.isFile()) {
                    FileUtils.copyFile(configPatchInvFile, archiveConfigPatchInvFile);
                    if (processType.equals(INVENTORY_OPERATION.MOVE_CONFIGPATCHINV))
                        configPatchInvFile.delete();
                }
            }
        } catch (IOException e) {
            System.out.println("[error] Failure in " + (processType.value % 2 != 0 ? "copying " : "moving ") +
                    (processType.value <= 2 ? "recorded-actions.xml" : "configpatchinv.xml"));
        }
    }

    private void setCurrentDirectory(String dirName) throws Exception{
        File directory;       // Desired current working directory
        directory = new File(dirName).getAbsoluteFile();
        if (directory.exists() || directory.mkdirs())
            System.setProperty("user.dir", directory.getCanonicalPath());
    }

    private void setupPatchStaging(boolean execute) {
        if (execute) {
            //Create Patch Staging Dir and Initialize Test Patches List
            patchStagingDir = new File(oracleHome + File.separator + ".."+ File.separator + this.getClass().getSimpleName() + "Patches");
            patchStagingDir.mkdirs();
            testPatches = new LinkedHashMap<String, String>();

            allPatchStagingDir = new File(oracleHome + File.separator + ".."+ File.separator + this.getClass().getSimpleName() + "AllPatches");
            allPatchStagingDir.mkdirs();
        }
    }

    protected LinkedHashMap<String, String> processPatches(String testPatchHome, PATCH_OPERATION operation) throws Exception{
        setupPatchStaging((operation.getOperation().equals("none")) ? false : true);
        FileFilter patchDirFilter = new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory() && dir.getName().matches("\\d+"))
                    return true;
                else if (dir.isDirectory()) {
                    try {
                        processPatches(dir.getCanonicalPath(), PATCH_OPERATION.NONE);
                    }
                    catch (Exception ex) {
                        System.out.println("[error] Failure in determining list of patches for Automation");
                    }
                    return false;
                } else
                    return false;
            }
        };
        for (File patchDir : new File(testPatchHome).listFiles(patchDirFilter))
            testPatches.put(patchDir.getName(), patchDir.getCanonicalPath());

        if (operation.equals(PATCH_OPERATION.APPLY) || operation.equals(PATCH_OPERATION.APPLY_CUMULATIVE)) {
            for (String dir : testPatches.values()) {
                try {
                    FileUtils.copyDirectoryToDirectory(new File(dir), patchStagingDir);
                    System.out.println("Copying : " + new File(dir).getCanonicalPath() + " to Staging Directory : " + patchStagingDir );
                    FileUtils.copyDirectoryToDirectory(new File(dir), allPatchStagingDir);
                    System.out.println("Copying : " + new File(dir).getCanonicalPath() + " to Staging Directory : "+allPatchStagingDir);
                } catch (Exception e) {
                    System.out.println("[error] Failed to copy Test Patches to Staging Directory");
                }
            }
            allPatches.putAll(testPatches);
            return testPatches;
        }
        return null;
    }

    protected void createQADomain(String domainName) throws Exception {
        printHeader(testName + ".createQADomain");
        File createDomainScript = new File(resourcesDir + File.separator + "createDomain.py");

        setupExecutorLog();
        executor.setWorkingDirectory(new File(wlstHome));
        executor.setStreamHandler(new PumpStreamHandler(outputCollector, errorCollector));
        String command = (String.format("%1$s -i %2$s %3$s %4$s",
                new File(wlstHome + File.separator + wlst).getCanonicalPath(),
                createDomainScript.getCanonicalPath(),
                new File(oracleHome).getCanonicalPath(),
                domainName));
        CommandLine cmdString = CommandLine.parse(command);
        System.out.println("Executing command : " + command);

        try {
            domainHome = new File(domainHome).getCanonicalPath() + File.separator + ".." + File.separator + domainName;

            //Delete is the domain already exists to avoid failures
            if(new File(domainHome).exists()) {
                System.out.println("[info] Deleting existing domain directory " + new File(domainHome).getCanonicalPath() + "\n");
                new File(domainHome).delete();
            }
            executor.execute(cmdString);

            //Copy the configpatchinv.xml so that new domain will have same env as original domain
            if(new File(orgDomainHome + File.separator + "configpatchinv.xml").isFile())
                FileUtils.copyFileToDirectory(new File(orgDomainHome + File.separator + "configpatchinv.xml"), new File(domainHome));
        } catch (Exception e) {
            System.out.println("[error] Failure in creating domain for" + testName + "\n");
            throw e;
        } finally {
            writeToFile(testName + ".createQADomain", outputCollector.toString());
        }
    }

    @AfterClass
    public abstract void cleanUpClass();

    protected enum INVENTORY_OPERATION {
        COPY_RECORDEDACTIONS(1),
        MOVE_RECORDEDACTIONS(2),
        COPY_CONFIGPATCHINV(3),
        MOVE_CONFIGPATCHINV(4);

        private int value;

        private INVENTORY_OPERATION(int value) {
            this.value = value;
        }
    }

    protected enum PATCH_OPERATION {
        APPLY(""),
        APPLY_CUMULATIVE("-phBaseDir"),
        ROLLBACK("rollback"),
        ROLLBACK_CUMULATIVE("rollback -phBaseDir"),
        NONE("none");

        private String operation;

        PATCH_OPERATION(String op) {
            operation = op;
        }

        public String getOperation(){
            return this.operation;
        }
        public String getOperationType() {
            return this.operation.contains(" ") ? "cumulative " + operation.split(" ")[0] : operation;
        }
    }

    protected enum HELPER_COMMANDS {
        FIND_STRING("FIND_STRING");

        private String command;
        HELPER_COMMANDS(String cmd){command = cmd;}

        public String getCommand(){
            String command = "INVALID";
            switch(this.command){
                case "FIND_STRING" : command = System.getProperty("os.name").startsWith("Windows") ? "findstr /R /C:" : "grep " ;
                    break;
            }
            return command;
        }
    }

    public static EnvironmentModel getEnvironmentModel() throws Exception{
        EnvironmentModel model = getEnvironmentModel(oracleHome,domainHome, domainName,"modelOutput");
//        EnvironmentModel bootStrapModel=null;
//        try {
//            EnvironmentModelBuilder builder = new EnvironmentModelBuilder(oracleHome);
//            bootStrapModel = builder.buildFromOfflineDomainBootstrapParams(domainName, oracleHome, domainHome);
//
//
//            List<ModelTarget> targets = new ArrayList<>();
//            targets.add(ModelTargetFactory.createDomainTarget(domainName));
//
//            DiscoveryOptions options = new DiscoveryOptions(false, false, OfflineEnvironment.class);
//            model = builder.populateFromEnvironment(bootStrapModel, options, targets);
//
//            System.out.println(model.getTopology().toString());
//            model.setTopology(model.getTopology().setName("modelOutput"));
//
//            try {
//                builder.writeModelToFiles(model, new File(testOutput, "modelOutput"));
//            }
//            catch(Exception e){
//                System.out.println("Error saving EnvironmentModel. See error below :");
//                e.printStackTrace();
//            }
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//            throw e;
//        }
        return model;
    }

    public static EnvironmentModel getEnvironmentModel(String oracleHome, String domainHome, String domainID, String topologyName) throws Exception{
        EnvironmentModel model = null;
        EnvironmentModel bootStrapModel=null;
        try {
            EnvironmentModelBuilder builder = new EnvironmentModelBuilder(oracleHome);
            bootStrapModel = builder.buildFromOfflineDomainBootstrapParams(domainID, oracleHome, domainHome);

            List<ModelTarget> targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createDomainTarget(domainID));

            DiscoveryOptions options = new DiscoveryOptions(false, false, OfflineEnvironment.class);
            model = builder.populateFromEnvironment(bootStrapModel, options, targets);

            System.out.println(model.getTopology().toString());
            model.setTopology(model.getTopology().setName(topologyName));

            try {
                builder.writeModelToFiles(model, new File(testOutput, topologyName));
                String topologyLocationStr = AbstractScriptRunner.testOutput + File.separator + "modelOutput" + File.separator + "models";
                builder.writeModelToFiles(model, new File(topologyLocationStr));
            }
            catch(Exception e){
                System.out.println("Error saving EnvironmentModel. See error below :");
                e.printStackTrace();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return model;
    }
}