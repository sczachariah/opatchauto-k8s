package oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests.fmwzdt;

import oracle.fmwplatform.actionframework.api.v2.*;
import oracle.fmwplatform.actionframework.api.v2.standardactions.ActionConstants;
import oracle.fmwplatform.credentials.credential.Credentials;
import oracle.fmwplatform.envspec.model.EnvironmentModel;
import oracle.fmwplatform.envspec.model.EnvironmentModelBuilder;
import oracle.fmwplatform.envspec.model.targets.ModelTarget;
import oracle.fmwplatform.envspec.model.targets.ModelTargetFactory;
import oracle.fmwplatform.envspec.model.topology.Variable;
import oracle.fmwplatform.internal.tools.junit.FMWDockerOptions;
import oracle.fmwplatform.internal.tools.junit.FMWDockerRunner;
import oracle.fmwplatformqa.opatchautoqa.util.AgentHelper;
import oracle.fmwplatformqa.opatchautoqa.util.AgentInfo;
import org.apache.commons.io.FileUtils;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static oracle.fmwplatformqa.opatchautoqa.util.DockerTestUtil.*;
import static org.junit.Assert.*;

@RunWith(FMWDockerRunner.class)
public class OPatchAutoFMWZDTDockerTest {

    private String containerId = null;
    private String containerAddress = null;
    private List<AgentInfo> extraContainers = null;

    public OPatchAutoFMWZDTDockerTest() {
        try {
            System.out.println("[opatchauto.qa]Docker Test Classpath Directory : " + new File(DOCKERTEST_CLASSPATH).getCanonicalPath());
            System.out.println("[opatchauto.qa]Test Directory                  : " + new File(TEST_CLASSES_DIR).getCanonicalPath());
            System.out.println("[opatchauto.qa]Patches Directory               : " + new File(TEST_PATCHES_DIR).getCanonicalPath());
        } catch (Exception e) {
            System.out.println("[opatchauto.qa] Error setting general Properties \n");
            e.printStackTrace();
        }
    }

    @FMWDockerOptions(oracleHomesToMount = "jrf-12.2.1.3.0",
            tags = "opatchauto-fmwzdt-docker-test-singlenode",
            description = "Feature\n" + "  OPatchAuto FMWZDT\n\n"
                    + "Scenario\n" + "  Sample OPatchAuto test run on Docker \n\n",
            numberOfContainers = 1)
    public void testSample() throws Exception {
        try {
            containerId = InetAddress.getLocalHost().getHostName();
            System.out.println("[opatchauto.qa]Sample Docker run for OPatchAuto Container 1 " + containerId);

            Path dep = Paths.get(DOCKERTEST_CLASSPATH);
            displayDirectoryContents(dep.toFile());

            Path walletsDirectory = Paths.get("/test_output", containerId, "wallets", "jrf-domain-wallet");
            Files.createDirectories(walletsDirectory);
            System.out.println("Created directory - " + walletsDirectory);
            if (!Files.exists(walletsDirectory) || !Files.isWritable(walletsDirectory)) {
                fail("Could not create parent directory for wallets!");
            }

            String walletLocation = createWallet(walletsDirectory.toFile());
            EnvironmentModelBuilder builder = createModelBuilder(DEFAULT_JRF_HOME_PATH);

            System.out.println("[opatchauto.qa]Loading Model");
            EnvironmentModel model = builder.buildFromFiles("jrf-topology", "6.0.0", new File(walletLocation), "welcome1".toCharArray());
            model.getTopology().getVariables().add(new Variable("SERVER_HOST", containerId));

            System.out.println("[opatchauto.qa]Creating an Action Factory");
            ActionFactory actionFactory = DefaultActionFactoryLocator.locateActionFactory();
            Action action = actionFactory.getAction("create-domain", DEFAULT_JRF_HOME_PATH);

            System.out.println("[opatchauto.qa]Running Action");
            List<ModelTarget> targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createDomainTarget(DEFAULT_JRF_DOMAIN_NAME));
            ActionResult result = action.run(model, targets);
            System.out.println("Result = " + result);

            assertEquals("[opatchauto.qa]The ActionResult should contain a successful Status Code", ActionStatusCode.SUCCESS, result.getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
            fail("[opatchauto.qa] - " + e.getMessage() + " - see log file!");
        }
    }

    @FMWDockerOptions(oracleHomesToMount = "jrf-12.2.1.3.0",
            tags = "opatchauto-fmwzdt-docker-test-multinode",
            description = "Feature\n" + "  OPatchAuto FMWZDT\n\n"
                    + "Scenario\n" + "  OPatchAuto ZDT in Rolling plan \n\n",
            numberOfContainers = 3)
    public void testZDTRollingRemoteHost() throws Throwable {
        String testName = "testZDTRollingRemoteHost";
        List<ModelTarget> targets;
        ProcessBuilder pb;
        Process process;
        try {
            /**Get main and extra container addresses*/
            containerId = InetAddress.getLocalHost().getHostName();
            containerAddress = InetAddress.getLocalHost().getHostAddress();
            extraContainers = AgentHelper.getAgentInfos();

            assertNotNull("[opatchauto.qa]Additional containers should not be null", extraContainers);
            assertEquals("[opatchauto.qa]There should be two additional containers created", 2, extraContainers.size());
            AgentInfo hostInfo1 = extraContainers.get(0);
            AgentInfo hostInfo2 = extraContainers.get(1);

            System.out.println("[opatchauto.qa]Sample Docker run for OPatchAuto Container 1 " + containerId + " : " + containerAddress);
            System.out.println("[opatchauto.qa]Sample Docker run for OPatchAuto Container 2 " + hostInfo1.getHostName() + " - " + hostInfo1.getAddress());
            System.out.println("[opatchauto.qa]Sample Docker run for OPatchAuto Container 3 " + hostInfo2.getHostName() + " - " + hostInfo2.getAddress());

            /**Create oracle home directory*/
            Path homesDirectory = Paths.get("/scratch", "/homes", containerId);

            /**Create oraInventory directory*/
            Path oraInventoryDirectory = Paths.get("/scratch", "/homes", containerId, "oraInventory");

            /**Create domain directory*/
            Path domainsDirectory = Paths.get("/domains", containerId);
            Files.createDirectories(domainsDirectory);
            System.out.println("Created directory - " + domainsDirectory);
            if (!Files.exists(domainsDirectory) || !Files.isWritable(domainsDirectory)) {
                fail("Could not create parent directory for domain!");
            }

            /**Create wallet directory*/
            Path walletsDirectory = Paths.get("/test_output", containerId, "wallets", "jrf-multinode-domain-wallet");
            Files.createDirectories(walletsDirectory);
            System.out.println("Created directory - " + walletsDirectory);
            if (!Files.exists(walletsDirectory) || !Files.isWritable(walletsDirectory)) {
                fail("Could not create parent directory for wallets!");
            }

            /**Create wallet and add credentials*/
            createWallet(walletsDirectory.toFile());
            Credentials credentials = new Credentials();
            credentials.setCredential(containerId + ":wls", "weblogic", "welcome1".toCharArray());
            credentials.setCredential(DEFAULT_JRF_DOMAIN_NAME + "/ADMIN", "weblogic", "welcome1".toCharArray());
            credentials.setCredential(DEFAULT_JRF_DOMAIN_NAME + "/NM", "weblogic", "welcome1".toCharArray());
            addCredentialsToWallet(walletsDirectory.toFile(), credentials);

            /**Create builder, load model and add model variables*/
            EnvironmentModelBuilder builder = createModelBuilder(DEFAULT_JRF_HOME_PATH);
            System.out.println("[opatchauto.qa]Loading Model");
            EnvironmentModel model = builder.buildFromFiles("jrf-multinode-topology", "6.0.0", walletsDirectory.toFile(), "welcome1".toCharArray());
            model.getTopology().getVariables().add(new Variable("SERVER_HOST", containerAddress));
            model.getTopology().getVariables().add(new Variable("SERVER1_HOST", hostInfo1.getAddress()));
            model.getTopology().getVariables().add(new Variable("SERVER2_HOST", hostInfo2.getAddress()));
            model.getOracleHomeById(DEFAULT_JRF_HOME).setPath
                    (new File(homesDirectory + File.separator + DEFAULT_JRF_HOME).getCanonicalPath());
            model.getOracleHomeById(DEFAULT_JRF_HOME).setInventoryLocation
                    (new File(oraInventoryDirectory + "").getCanonicalPath());
            model.getDomainById(DEFAULT_JRF_DOMAIN_NAME).setPath
                    (new File(domainsDirectory + File.separator + DEFAULT_JRF_DOMAIN_NAME).getCanonicalPath());
            System.out.println("[opatchauto.qa]Loaded Model - \n" + model.getTopology().toString());

            /**Create action factory to get an action*/
            createActionFactory();

            /**Execute install-oraclehome action in all containers*/
            System.out.println("[opatchauto.qa]Running install-oraclehome Action in container " + containerId);
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createOracleHomeTarget(DEFAULT_JRF_HOME));
            runAction("install-oraclehome", model, targets, new Properties());

            System.out.println("[opatchauto.qa]Running install-oraclehome Action in container " + hostInfo1.getAddress());
            runRestAction(hostInfo1.getUrl(), "install-oraclehome", model, targets, new Properties());
            System.out.println("[opatchauto.qa]Running install-oraclehome Action in container " + hostInfo2.getAddress());
            runRestAction(hostInfo2.getUrl(), "install-oraclehome", model, targets, new Properties());

            /**Copy latest fmwcommon artifacts and plugins to new oraclehome. The source oh will be the one
             * requested to be mounted in docker options*/
//            copyFMWCommonJars(DEFAULT_JRF_HOME_PATH, model.getOracleHomeById(DEFAULT_JRF_HOME).getPath(), containerAddress);
//            copyFMWCommonJars(DEFAULT_JRF_HOME_PATH, model.getOracleHomeById(DEFAULT_JRF_HOME).getPath(), hostInfo1.getAddress());
//            copyFMWCommonJars(DEFAULT_JRF_HOME_PATH, model.getOracleHomeById(DEFAULT_JRF_HOME).getPath(), hostInfo2.getAddress());

            /**Execute create-domain action*/
            System.out.println("[opatchauto.qa]Running create-domain Action");
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createDomainTarget(DEFAULT_JRF_DOMAIN_NAME));
            runAction("create-domain", model, targets, new Properties());

            /**Run pack-domain action to pack the domain*/
            Path packedDomainTemplate = Paths.get("/test_output", containerId, model.getDomainById(DEFAULT_JRF_DOMAIN_NAME).getName() + ".jar");
            Properties packExtra = new Properties();
            packExtra.setProperty(ActionConstants.PACK_DOMAIN_template, packedDomainTemplate.toString());
            packExtra.setProperty(ActionConstants.PACK_DOMAIN_templateName, model.getDomainById(DEFAULT_JRF_DOMAIN_NAME).getName());
            packExtra.setProperty(ActionConstants.PACK_DOMAIN_managed, "true");
            System.out.println("[opatchauto.qa]Running pack-domain Action");
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createDomainTarget(DEFAULT_JRF_DOMAIN_NAME));
            runAction("pack-domain", model, targets, packExtra);
            assertTrue("[opatchauto.qa]The packed domain file should exist", packedDomainTemplate.toFile().exists());

            /**Unpack the domain into extra containers*/
            Properties unpackExtra = new Properties();
            unpackExtra.setProperty(ActionConstants.UNPACK_DOMAIN_template, packedDomainTemplate.toString());
            System.out.println("[opatchauto.qa]Unpacking domain onto container " + hostInfo1.getAddress());
            runRestAction(hostInfo1.getUrl(), "unpack-domain", model, targets, unpackExtra);
            System.out.println("[opatchauto.qa]Unpacking domain onto container " + hostInfo2.getAddress());
            runRestAction(hostInfo2.getUrl(), "unpack-domain", model, targets, unpackExtra);

            /**Start node managers of all hosts*/
            System.out.println("[opatchauto.qa]Starting Node Manager of container " + containerId);
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createNodeManagerOnHostTarget("machine1-nm", "machine1"));
            runAction("start", model, targets, new Properties());

            System.out.println("[opatchauto.qa]Starting Node Manager of container " + hostInfo1.getAddress());
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createNodeManagerOnHostTarget("machine2-nm", "machine2"));
            runRestAction(hostInfo1.getUrl(), "start", model, targets, new Properties());

            System.out.println("[opatchauto.qa]Starting Node Manager of container " + hostInfo2.getAddress());
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createNodeManagerOnHostTarget("machine3-nm", "machine3"));
            runRestAction(hostInfo2.getUrl(), "start", model, targets, new Properties());

            /**Start and Stop AdminServer for the first time*/
            Properties startStopExtra = new Properties();
            startStopExtra.setProperty(ActionConstants.START_STOP_noNodeManager, "true");
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createServerInDomainTarget("AdminServer", DEFAULT_JRF_DOMAIN_NAME));
            System.out.println("[opatchauto.qa]Starting Admin Server");
            runAction("start", model, targets, startStopExtra);
            System.out.println("[opatchauto.qa]Stopping Admin Server");
            runAction("stop", model, targets, startStopExtra);

            /**Start domain using node managers*/
            System.out.println("[opatchauto.qa]Starting Domain using node manager");
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createDomainTarget(DEFAULT_JRF_DOMAIN_NAME));
            runAction("start", model, targets, new Properties());

            /**START OPATCHAUTO OPERATIONS*/

            /**Copy test-patches to common folder inside /test_output*/
            Path allPatchesDirectory = Paths.get("/test_output", containerId, "test-patches");
            FileUtils.copyDirectory(new File(TEST_PATCHES_DIR), allPatchesDirectory.toFile());

            /**Run OPatchAuto command from remote host using ssh*/
            System.out.println("[opatchauto.qa]Executing opatchauto from container " + hostInfo1.getAddress());
            String sessionLog = new File("/test_output/" + containerId, testName + "_APPLY" + ".session.log").getCanonicalPath();

            String[] opatchautoCommand = {"/bin/bash", "-c", "ssh " + containerAddress +
                    " \"export JAVA_HOME=/homes/jdk && " +
                    new File(model.getOracleHomeById(DEFAULT_JRF_HOME).getPath() + "/OPatch/auto/core/bin/opatchauto.sh").getCanonicalPath() +
                    " apply " + new File(allPatchesDirectory + "/fmwzdtPatches/ROLLING/99999705/").getCanonicalPath() +
                    " -instance " + model.getDomainById(DEFAULT_JRF_DOMAIN_NAME).getPath() +
                    " -host " + containerId.trim() + "," + hostInfo1.getAddress().trim() + "," + hostInfo2.getAddress().trim() +
                    " -wallet " + walletsDirectory.toFile().getCanonicalPath() +
                    " -walletPassword welcome1" +
                    " -sshuserequivalence " +
                    " -sshaccesskey " + DEFAULT_SSH_KEY_LOCATION +
                    " -wls-admin-host " + containerId.trim() + ":" + "7001" +
                    " -log " + sessionLog +
                    "\""};
            System.out.println("[opatchauto.qa]Executing command " + Arrays.toString(opatchautoCommand));

            pb = new ProcessBuilder(opatchautoCommand);
            pb.inheritIO();
            process = pb.start();
            process.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage() + " - see log file!");
        }
    }

    @FMWDockerOptions(oracleHomesToMount = "jrf-12.2.1.3.0",
            tags = "mats",
            description = "Feature\n" + "  OPatchAuto FMWZDT\n\n"
                    + "Scenario\n" + "  OPatchAuto ZDT Binary patching \n\n",
            numberOfContainers = 3)
    public void testZDTBinaryPatching() throws Throwable {
        String testName = "testZDTBinaryPatching";
        List<ModelTarget> targets;
        ProcessBuilder pb;
        Process process;
        try {
            /**Get main and extra container addresses*/
            containerId = InetAddress.getLocalHost().getHostName();
            containerAddress = InetAddress.getLocalHost().getHostAddress();
            extraContainers = AgentHelper.getAgentInfos();

            assertNotNull("[opatchauto.qa]Additional containers should not be null", extraContainers);
            assertEquals("[opatchauto.qa]There should be two additional containers created", 2, extraContainers.size());
            AgentInfo hostInfo1 = extraContainers.get(0);
            AgentInfo hostInfo2 = extraContainers.get(1);

            System.out.println("[opatchauto.qa]Sample Docker run for OPatchAuto Container 1 " + containerId + " : " + containerAddress);
            System.out.println("[opatchauto.qa]Sample Docker run for OPatchAuto Container 2 " + hostInfo1.getHostName() + " - " + hostInfo1.getAddress());
            System.out.println("[opatchauto.qa]Sample Docker run for OPatchAuto Container 3 " + hostInfo2.getHostName() + " - " + hostInfo2.getAddress());

            /**Create oracle home directory*/
            Path homesDirectory = Paths.get("/scratch", "/homes", containerId);

            /**Create oraInventory directory*/
            Path oraInventoryDirectory = Paths.get("/scratch", "/homes", containerId, "oraInventory");

            /**Create domain directory*/
            Path domainsDirectory = Paths.get("/domains", containerId);
            Files.createDirectories(domainsDirectory);
            System.out.println("Created directory - " + domainsDirectory);
            if (!Files.exists(domainsDirectory) || !Files.isWritable(domainsDirectory)) {
                fail("Could not create parent directory for domain!");
            }

            /**Create wallet directory*/
            Path walletsDirectory = Paths.get("/test_output", containerId, "wallets", "jrf-multinode-domain-wallet");
            Files.createDirectories(walletsDirectory);
            System.out.println("Created directory - " + walletsDirectory);
            if (!Files.exists(walletsDirectory) || !Files.isWritable(walletsDirectory)) {
                fail("Could not create parent directory for wallets!");
            }

            /**Create wallet and add credentials*/
            createWallet(walletsDirectory.toFile());
            Credentials credentials = new Credentials();
            credentials.setCredential(containerId + ":wls", "weblogic", "welcome1".toCharArray());
            credentials.setCredential(DEFAULT_JRF_DOMAIN_NAME + "/ADMIN", "weblogic", "welcome1".toCharArray());
            credentials.setCredential(DEFAULT_JRF_DOMAIN_NAME + "/NM", "weblogic", "welcome1".toCharArray());
            addCredentialsToWallet(walletsDirectory.toFile(), credentials);

            /**Create builder, load model and add model variables*/
            EnvironmentModelBuilder builder = createModelBuilder(DEFAULT_JRF_HOME_PATH);
            System.out.println("[opatchauto.qa]Loading Model");
            EnvironmentModel model = builder.buildFromFiles("jrf-multinode-topology", "6.0.0", walletsDirectory.toFile(), "welcome1".toCharArray());
            model.getTopology().getVariables().add(new Variable("SERVER_HOST", containerAddress));
            model.getTopology().getVariables().add(new Variable("SERVER1_HOST", hostInfo1.getAddress()));
            model.getTopology().getVariables().add(new Variable("SERVER2_HOST", hostInfo2.getAddress()));
            model.getOracleHomeById(DEFAULT_JRF_HOME).setPath
                    (new File(homesDirectory + File.separator + DEFAULT_JRF_HOME).getCanonicalPath());
            model.getOracleHomeById(DEFAULT_JRF_HOME).setInventoryLocation
                    (new File(oraInventoryDirectory + "").getCanonicalPath());
            model.getDomainById(DEFAULT_JRF_DOMAIN_NAME).setPath
                    (new File(domainsDirectory + File.separator + DEFAULT_JRF_DOMAIN_NAME).getCanonicalPath());
            System.out.println("[opatchauto.qa]Loaded Model - \n" + model.getTopology().toString());

            /**Create action factory to get an action*/
            createActionFactory();

            /**Execute install-oraclehome action in all containers*/
            System.out.println("[opatchauto.qa]Running install-oraclehome Action in container " + containerId);
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createOracleHomeTarget(DEFAULT_JRF_HOME));
            runAction("install-oraclehome", model, targets, new Properties());

            System.out.println("[opatchauto.qa]Running install-oraclehome Action in container " + hostInfo1.getAddress());
            runRestAction(hostInfo1.getUrl(), "install-oraclehome", model, targets, new Properties());
            System.out.println("[opatchauto.qa]Running install-oraclehome Action in container " + hostInfo2.getAddress());
            runRestAction(hostInfo2.getUrl(), "install-oraclehome", model, targets, new Properties());

            /**Copy latest fmwcommon artifacts and plugins to new oraclehome. The source oh will be the one
             * requested to be mounted in docker options*/
//            copyFMWCommonJars(DEFAULT_JRF_HOME_PATH, model.getOracleHomeById(DEFAULT_JRF_HOME).getPath(), containerAddress);
//            copyFMWCommonJars(DEFAULT_JRF_HOME_PATH, model.getOracleHomeById(DEFAULT_JRF_HOME).getPath(), hostInfo1.getAddress());
//            copyFMWCommonJars(DEFAULT_JRF_HOME_PATH, model.getOracleHomeById(DEFAULT_JRF_HOME).getPath(), hostInfo2.getAddress());


            /**START OPATCHAUTO OPERATIONS*/

            /**Copy test-patches to common folder inside /test_output*/
            Path allPatchesDirectory = Paths.get("/test_output", containerId, "test-patches");
            FileUtils.copyDirectory(new File(TEST_PATCHES_DIR), allPatchesDirectory.toFile());

            /**Run OPatchAuto command from remote host using ssh*/
            System.out.println("[opatchauto.qa]Executing opatchauto from container " + hostInfo1.getAddress());
            String sessionLog = new File("/test_output/" + containerId, testName + "_APPLY" + ".session.log").getCanonicalPath();

            String[] opatchautoCommand = {"/bin/bash", "-c", "ssh " + containerId +
                    " \"export JAVA_HOME=/homes/jdk && " +
                    new File(model.getOracleHomeById(DEFAULT_JRF_HOME).getPath() + "/OPatch/auto/core/bin/opatchauto.sh").getCanonicalPath() +
                    " apply " + new File(allPatchesDirectory + "/fmwzdtPatches/ROLLING/99999705/").getCanonicalPath() +
//                    " -instance " + model.getDomainById(DEFAULT_JRF_DOMAIN_NAME).getPath() +
                    " -host " + containerId.trim() + "," + hostInfo1.getAddress().trim() + "," + hostInfo2.getAddress().trim() +
                    " -wallet " + walletsDirectory.toFile().getCanonicalPath() +
                    " -walletPassword welcome1" +
//                    " -plan parallel" +
                    " -sshuserequivalence " +
                    " -sshaccesskey " + DEFAULT_SSH_KEY_LOCATION +
                    " -wls-admin-host " + containerId.trim() + ":" + "7001" +
                    " -logLevel FINEST " +
                    " -log " + sessionLog +
                    "\""};
            System.out.println("[opatchauto.qa]Executing command " + Arrays.toString(opatchautoCommand));

            pb = new ProcessBuilder(opatchautoCommand);
            pb.inheritIO();
            process = pb.start();
            process.waitFor();

            FileUtils.copyDirectory(new File(model.getOracleHomeById(DEFAULT_JRF_HOME).getPath() + "/cfgtoollogs/opatchauto/").getCanonicalFile(), allPatchesDirectory.toFile());

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage() + " - see log file!");
        }
    }

    @FMWDockerOptions(oracleHomesToMount = "jrf-12.2.1.3.0",
            tags = "mats,opatchauto-fmwzdt-docker-test-multinode",
            description = "Feature\n" + "  OPatchAuto FMWZDT\n\n"
                    + "Scenario\n" + "  OPatchAuto ZDT with Patch Uptime as Online \n\n",
            numberOfContainers = 3)
    public void testZDTPatchUptimeOnline() throws Throwable {
        String testName = "testZDTPatchUptimeOnline";
        List<ModelTarget> targets;
        ProcessBuilder pb;
        Process process;
        try {
            /**Get main and extra container addresses*/
            containerId = InetAddress.getLocalHost().getHostName();
            containerAddress = InetAddress.getLocalHost().getHostAddress();
            extraContainers = AgentHelper.getAgentInfos();

            assertNotNull("[opatchauto.qa]Additional containers should not be null", extraContainers);
            assertEquals("[opatchauto.qa]There should be two additional containers created", 2, extraContainers.size());
            AgentInfo hostInfo1 = extraContainers.get(0);
            AgentInfo hostInfo2 = extraContainers.get(1);

            System.out.println("[opatchauto.qa]Sample Docker run for OPatchAuto Container 1 " + containerId + " : " + containerAddress);
            System.out.println("[opatchauto.qa]Sample Docker run for OPatchAuto Container 2 " + hostInfo1.getHostName() + " - " + hostInfo1.getAddress());
            System.out.println("[opatchauto.qa]Sample Docker run for OPatchAuto Container 3 " + hostInfo2.getHostName() + " - " + hostInfo2.getAddress());

            /**Create oracle home directory*/
            Path homesDirectory = Paths.get("/scratch", "/homes", containerId);

            /**Create oraInventory directory*/
            Path oraInventoryDirectory = Paths.get("/scratch", "/homes", containerId, "oraInventory");

            /**Create domain directory*/
            Path domainsDirectory = Paths.get("/domains", containerId);
            Files.createDirectories(domainsDirectory);
            System.out.println("Created directory - " + domainsDirectory);
            if (!Files.exists(domainsDirectory) || !Files.isWritable(domainsDirectory)) {
                fail("Could not create parent directory for domain!");
            }

            /**Create wallet directory*/
            Path walletsDirectory = Paths.get("/test_output", containerId, "wallets", "jrf-multinode-domain-wallet");
            Files.createDirectories(walletsDirectory);
            System.out.println("Created directory - " + walletsDirectory);
            if (!Files.exists(walletsDirectory) || !Files.isWritable(walletsDirectory)) {
                fail("Could not create parent directory for wallets!");
            }

            /**Create wallet and add credentials*/
            createWallet(walletsDirectory.toFile());
            Credentials credentials = new Credentials();
            credentials.setCredential(containerId + ":wls", "weblogic", "welcome1".toCharArray());
            credentials.setCredential(DEFAULT_JRF_DOMAIN_NAME + "/ADMIN", "weblogic", "welcome1".toCharArray());
            credentials.setCredential(DEFAULT_JRF_DOMAIN_NAME + "/NM", "weblogic", "welcome1".toCharArray());
            addCredentialsToWallet(walletsDirectory.toFile(), credentials);

            /**Create builder, load model and add model variables*/
            EnvironmentModelBuilder builder = createModelBuilder(DEFAULT_JRF_HOME_PATH);
            System.out.println("[opatchauto.qa]Loading Model");
            EnvironmentModel model = builder.buildFromFiles("jrf-multinode-topology", "6.0.0", walletsDirectory.toFile(), "welcome1".toCharArray());
            model.getTopology().getVariables().add(new Variable("SERVER_HOST", containerAddress));
            model.getTopology().getVariables().add(new Variable("SERVER1_HOST", hostInfo1.getAddress()));
            model.getTopology().getVariables().add(new Variable("SERVER2_HOST", hostInfo2.getAddress()));
            model.getOracleHomeById(DEFAULT_JRF_HOME).setPath
                    (new File(homesDirectory + File.separator + DEFAULT_JRF_HOME).getCanonicalPath());
            model.getOracleHomeById(DEFAULT_JRF_HOME).setInventoryLocation
                    (new File(oraInventoryDirectory + "").getCanonicalPath());
            model.getDomainById(DEFAULT_JRF_DOMAIN_NAME).setPath
                    (new File(domainsDirectory + File.separator + DEFAULT_JRF_DOMAIN_NAME).getCanonicalPath());
            System.out.println("[opatchauto.qa]Loaded Model - \n" + model.getTopology().toString());

            /**Create action factory to get an action*/
            createActionFactory();

            /**Execute install-oraclehome action in all containers*/
            System.out.println("[opatchauto.qa]Running install-oraclehome Action in container " + containerId);
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createOracleHomeTarget(DEFAULT_JRF_HOME));
            runAction("install-oraclehome", model, targets, new Properties());

            System.out.println("[opatchauto.qa]Running install-oraclehome Action in container " + hostInfo1.getAddress());
            runRestAction(hostInfo1.getUrl(), "install-oraclehome", model, targets, new Properties());
            System.out.println("[opatchauto.qa]Running install-oraclehome Action in container " + hostInfo2.getAddress());
            runRestAction(hostInfo2.getUrl(), "install-oraclehome", model, targets, new Properties());

            /**Copy latest fmwcommon artifacts and plugins to new oraclehome. The source oh will be the one
             * requested to be mounted in docker options*/
            copyFMWCommonJars(DEFAULT_JRF_HOME_PATH, model.getOracleHomeById(DEFAULT_JRF_HOME).getPath(), containerAddress);
            copyFMWCommonJars(DEFAULT_JRF_HOME_PATH, model.getOracleHomeById(DEFAULT_JRF_HOME).getPath(), hostInfo1.getAddress());
            copyFMWCommonJars(DEFAULT_JRF_HOME_PATH, model.getOracleHomeById(DEFAULT_JRF_HOME).getPath(), hostInfo2.getAddress());

            /**Execute create-domain action*/
            System.out.println("[opatchauto.qa]Running create-domain Action");
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createDomainTarget(DEFAULT_JRF_DOMAIN_NAME));
            runAction("create-domain", model, targets, new Properties());

            /**Run pack-domain action to pack the domain*/
            Path packedDomainTemplate = Paths.get("/test_output", containerId, model.getDomainById(DEFAULT_JRF_DOMAIN_NAME).getName() + ".jar");
            Properties packExtra = new Properties();
            packExtra.setProperty(ActionConstants.PACK_DOMAIN_template, packedDomainTemplate.toString());
            packExtra.setProperty(ActionConstants.PACK_DOMAIN_templateName, model.getDomainById(DEFAULT_JRF_DOMAIN_NAME).getName());
            packExtra.setProperty(ActionConstants.PACK_DOMAIN_managed, "true");
            System.out.println("[opatchauto.qa]Running pack-domain Action");
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createDomainTarget(DEFAULT_JRF_DOMAIN_NAME));
            runAction("pack-domain", model, targets, packExtra);
            assertTrue("[opatchauto.qa]The packed domain file should exist", packedDomainTemplate.toFile().exists());

            /**Unpack the domain into extra containers*/
            Properties unpackExtra = new Properties();
            unpackExtra.setProperty(ActionConstants.UNPACK_DOMAIN_template, packedDomainTemplate.toString());
            System.out.println("[opatchauto.qa]Unpacking domain onto container " + hostInfo1.getAddress());
            runRestAction(hostInfo1.getUrl(), "unpack-domain", model, targets, unpackExtra);
            System.out.println("[opatchauto.qa]Unpacking domain onto container " + hostInfo2.getAddress());
            runRestAction(hostInfo2.getUrl(), "unpack-domain", model, targets, unpackExtra);

            /**Start node managers of all hosts*/
            System.out.println("[opatchauto.qa]Starting Node Manager of container " + containerId);
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createNodeManagerOnHostTarget("machine1-nm", "machine1"));
            runAction("start", model, targets, new Properties());

            System.out.println("[opatchauto.qa]Starting Node Manager of container " + hostInfo1.getAddress());
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createNodeManagerOnHostTarget("machine2-nm", "machine2"));
            runRestAction(hostInfo1.getUrl(), "start", model, targets, new Properties());

            System.out.println("[opatchauto.qa]Starting Node Manager of container " + hostInfo2.getAddress());
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createNodeManagerOnHostTarget("machine3-nm", "machine3"));
            runRestAction(hostInfo2.getUrl(), "start", model, targets, new Properties());

            /**Start and Stop AdminServer for the first time*/
            Properties startStopExtra = new Properties();
            startStopExtra.setProperty(ActionConstants.START_STOP_noNodeManager, "true");
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createServerInDomainTarget("AdminServer", DEFAULT_JRF_DOMAIN_NAME));
            System.out.println("[opatchauto.qa]Starting Admin Server");
            runAction("start", model, targets, startStopExtra);
            System.out.println("[opatchauto.qa]Stopping Admin Server");
            runAction("stop", model, targets, startStopExtra);

            /**Start domain using node managers*/
            System.out.println("[opatchauto.qa]Starting Domain using node manager");
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createDomainTarget(DEFAULT_JRF_DOMAIN_NAME));
            runAction("start", model, targets, new Properties());

            /**START OPATCHAUTO OPERATIONS*/

            /**Copy test-patches to common folder inside /test_output*/
            Path allPatchesDirectory = Paths.get("/test_output", containerId, "test-patches");
            FileUtils.copyDirectory(new File(TEST_PATCHES_DIR), allPatchesDirectory.toFile());

            /**Run OPatchAuto command from remote host using ssh*/
            System.out.println("[opatchauto.qa]Executing opatchauto from container " + hostInfo1.getAddress());
            String sessionLog = new File("/test_output/" + containerId, testName + "_APPLY" + ".session.log").getCanonicalPath();

            String[] opatchautoCommand = {"/bin/bash", "-c", "ssh " + containerAddress +
                    " \"export JAVA_HOME=/homes/jdk && " +
                    new File(model.getOracleHomeById(DEFAULT_JRF_HOME).getPath() + "/OPatch/auto/core/bin/opatchauto.sh").getCanonicalPath() +
                    " apply " + new File(allPatchesDirectory + "/fmwzdtPatches/ONLINE/99999704/").getCanonicalPath() +
                    " -instance " + model.getDomainById(DEFAULT_JRF_DOMAIN_NAME).getPath() +
                    " -host " + containerId.trim() + "," + hostInfo1.getAddress().trim() + "," + hostInfo2.getAddress().trim() +
                    " -wallet " + walletsDirectory.toFile().getCanonicalPath() +
                    " -walletPassword welcome1" +
                    " -plan rolling" +
                    " -sshuserequivalence " +
                    " -sshaccesskey " + DEFAULT_SSH_KEY_LOCATION +
                    " -wls-admin-host " + containerId.trim() + ":" + "7001" +
                    " -log " + sessionLog +
                    "\""};
            System.out.println("[opatchauto.qa]Executing command " + Arrays.toString(opatchautoCommand));

            pb = new ProcessBuilder(opatchautoCommand);
            pb.inheritIO();
            process = pb.start();
            process.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage() + " - see log file!");
        }
    }

    @FMWDockerOptions(oracleHomesToMount = "jrf-12.2.1.3.0",
            tags = "opatchauto-fmwzdt-docker-test-multinode",
            description = "Feature\n" + "  OPatchAuto FMWZDT\n\n"
                    + "Scenario\n" + "  OPatchAuto ZDT with Patch Uptime as NONE in Paralle plan \n\n",
            numberOfContainers = 3)
    public void testZDTNoneParallel() throws Throwable {
        String testName = "testZDTNoneParallel";
        List<ModelTarget> targets;
        ProcessBuilder pb;
        Process process;
        try {
            /**Get main and extra container addresses*/
            containerId = InetAddress.getLocalHost().getHostName();
            containerAddress = InetAddress.getLocalHost().getHostAddress();
            extraContainers = AgentHelper.getAgentInfos();

            assertNotNull("[opatchauto.qa]Additional containers should not be null", extraContainers);
            assertEquals("[opatchauto.qa]There should be two additional containers created", 2, extraContainers.size());
            AgentInfo hostInfo1 = extraContainers.get(0);
            AgentInfo hostInfo2 = extraContainers.get(1);

            System.out.println("[opatchauto.qa]Sample Docker run for OPatchAuto Container 1 " + containerId + " : " + containerAddress);
            System.out.println("[opatchauto.qa]Sample Docker run for OPatchAuto Container 2 " + hostInfo1.getHostName() + " - " + hostInfo1.getAddress());
            System.out.println("[opatchauto.qa]Sample Docker run for OPatchAuto Container 3 " + hostInfo2.getHostName() + " - " + hostInfo2.getAddress());

            /**Create oracle home directory*/
            Path homesDirectory = Paths.get("/scratch", "/homes", containerId);

            /**Create oraInventory directory*/
            Path oraInventoryDirectory = Paths.get("/scratch", "/homes", containerId, "oraInventory");

            /**Create domain directory*/
            Path domainsDirectory = Paths.get("/domains", containerId);
            Files.createDirectories(domainsDirectory);
            System.out.println("Created directory - " + domainsDirectory);
            if (!Files.exists(domainsDirectory) || !Files.isWritable(domainsDirectory)) {
                fail("Could not create parent directory for domain!");
            }

            /**Create wallet directory*/
            Path walletsDirectory = Paths.get("/test_output", containerId, "wallets", "jrf-multinode-domain-wallet");
            Files.createDirectories(walletsDirectory);
            System.out.println("Created directory - " + walletsDirectory);
            if (!Files.exists(walletsDirectory) || !Files.isWritable(walletsDirectory)) {
                fail("Could not create parent directory for wallets!");
            }

            /**Create wallet and add credentials*/
            createWallet(walletsDirectory.toFile());
            Credentials credentials = new Credentials();
            credentials.setCredential(containerId + ":wls", "weblogic", "welcome1".toCharArray());
            credentials.setCredential(DEFAULT_JRF_DOMAIN_NAME + "/ADMIN", "weblogic", "welcome1".toCharArray());
            credentials.setCredential(DEFAULT_JRF_DOMAIN_NAME + "/NM", "weblogic", "welcome1".toCharArray());
            addCredentialsToWallet(walletsDirectory.toFile(), credentials);

            /**Create builder, load model and add model variables*/
            EnvironmentModelBuilder builder = createModelBuilder(DEFAULT_JRF_HOME_PATH);
            System.out.println("[opatchauto.qa]Loading Model");
            EnvironmentModel model = builder.buildFromFiles("jrf-multinode-topology", "6.0.0", walletsDirectory.toFile(), "welcome1".toCharArray());
            model.getTopology().getVariables().add(new Variable("SERVER_HOST", containerAddress));
            model.getTopology().getVariables().add(new Variable("SERVER1_HOST", hostInfo1.getAddress()));
            model.getTopology().getVariables().add(new Variable("SERVER2_HOST", hostInfo2.getAddress()));
            model.getOracleHomeById(DEFAULT_JRF_HOME).setPath
                    (new File(homesDirectory + File.separator + DEFAULT_JRF_HOME).getCanonicalPath());
            model.getOracleHomeById(DEFAULT_JRF_HOME).setInventoryLocation
                    (new File(oraInventoryDirectory + "").getCanonicalPath());
            model.getDomainById(DEFAULT_JRF_DOMAIN_NAME).setPath
                    (new File(domainsDirectory + File.separator + DEFAULT_JRF_DOMAIN_NAME).getCanonicalPath());
            System.out.println("[opatchauto.qa]Loaded Model - \n" + model.getTopology().toString());

            /**Create action factory to get an action*/
            createActionFactory();

            /**Execute install-oraclehome action in all containers*/
            System.out.println("[opatchauto.qa]Running install-oraclehome Action in container " + containerId);
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createOracleHomeTarget(DEFAULT_JRF_HOME));
            runAction("install-oraclehome", model, targets, new Properties());

            System.out.println("[opatchauto.qa]Running install-oraclehome Action in container " + hostInfo1.getAddress());
            runRestAction(hostInfo1.getUrl(), "install-oraclehome", model, targets, new Properties());
            System.out.println("[opatchauto.qa]Running install-oraclehome Action in container " + hostInfo2.getAddress());
            runRestAction(hostInfo2.getUrl(), "install-oraclehome", model, targets, new Properties());

            /**Copy latest fmwcommon artifacts and plugins to new oraclehome. The source oh will be the one
             * requested to be mounted in docker options*/
            copyFMWCommonJars(DEFAULT_JRF_HOME_PATH, model.getOracleHomeById(DEFAULT_JRF_HOME).getPath(), containerAddress);
            copyFMWCommonJars(DEFAULT_JRF_HOME_PATH, model.getOracleHomeById(DEFAULT_JRF_HOME).getPath(), hostInfo1.getAddress());
            copyFMWCommonJars(DEFAULT_JRF_HOME_PATH, model.getOracleHomeById(DEFAULT_JRF_HOME).getPath(), hostInfo2.getAddress());

            /**Execute create-domain action*/
            System.out.println("[opatchauto.qa]Running create-domain Action");
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createDomainTarget(DEFAULT_JRF_DOMAIN_NAME));
            runAction("create-domain", model, targets, new Properties());

            /**Run pack-domain action to pack the domain*/
            Path packedDomainTemplate = Paths.get("/test_output", containerId, model.getDomainById(DEFAULT_JRF_DOMAIN_NAME).getName() + ".jar");
            Properties packExtra = new Properties();
            packExtra.setProperty(ActionConstants.PACK_DOMAIN_template, packedDomainTemplate.toString());
            packExtra.setProperty(ActionConstants.PACK_DOMAIN_templateName, model.getDomainById(DEFAULT_JRF_DOMAIN_NAME).getName());
            packExtra.setProperty(ActionConstants.PACK_DOMAIN_managed, "true");
            System.out.println("[opatchauto.qa]Running pack-domain Action");
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createDomainTarget(DEFAULT_JRF_DOMAIN_NAME));
            runAction("pack-domain", model, targets, packExtra);
            assertTrue("[opatchauto.qa]The packed domain file should exist", packedDomainTemplate.toFile().exists());

            /**Unpack the domain into extra containers*/
            Properties unpackExtra = new Properties();
            unpackExtra.setProperty(ActionConstants.UNPACK_DOMAIN_template, packedDomainTemplate.toString());
            System.out.println("[opatchauto.qa]Unpacking domain onto container " + hostInfo1.getAddress());
            runRestAction(hostInfo1.getUrl(), "unpack-domain", model, targets, unpackExtra);
            System.out.println("[opatchauto.qa]Unpacking domain onto container " + hostInfo2.getAddress());
            runRestAction(hostInfo2.getUrl(), "unpack-domain", model, targets, unpackExtra);

            /**Start node managers of all hosts*/
            System.out.println("[opatchauto.qa]Starting Node Manager of container " + containerId);
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createNodeManagerOnHostTarget("machine1-nm", "machine1"));
            runAction("start", model, targets, new Properties());

            System.out.println("[opatchauto.qa]Starting Node Manager of container " + hostInfo1.getAddress());
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createNodeManagerOnHostTarget("machine2-nm", "machine2"));
            runRestAction(hostInfo1.getUrl(), "start", model, targets, new Properties());

            System.out.println("[opatchauto.qa]Starting Node Manager of container " + hostInfo2.getAddress());
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createNodeManagerOnHostTarget("machine3-nm", "machine3"));
            runRestAction(hostInfo2.getUrl(), "start", model, targets, new Properties());

            /**Start and Stop AdminServer for the first time*/
            Properties startStopExtra = new Properties();
            startStopExtra.setProperty(ActionConstants.START_STOP_noNodeManager, "true");
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createServerInDomainTarget("AdminServer", DEFAULT_JRF_DOMAIN_NAME));
            System.out.println("[opatchauto.qa]Starting Admin Server");
            runAction("start", model, targets, startStopExtra);
            System.out.println("[opatchauto.qa]Stopping Admin Server");
            runAction("stop", model, targets, startStopExtra);

            /**Start domain using node managers*/
            System.out.println("[opatchauto.qa]Starting Domain using node manager");
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createDomainTarget(DEFAULT_JRF_DOMAIN_NAME));
            runAction("start", model, targets, new Properties());

            /**START OPATCHAUTO OPERATIONS*/

            /**Copy test-patches to common folder inside /test_output*/
            Path allPatchesDirectory = Paths.get("/test_output", containerId, "test-patches");
            FileUtils.copyDirectory(new File(TEST_PATCHES_DIR), allPatchesDirectory.toFile());

            /**Run OPatchAuto command from remote host using ssh*/
            System.out.println("[opatchauto.qa]Executing opatchauto from container " + hostInfo1.getAddress());
            String sessionLog = new File("/test_output/" + containerId, testName + "_APPLY" + ".session.log").getCanonicalPath();

            String[] opatchautoCommand = {"/bin/bash", "-c", "ssh " + containerAddress +
                    " \"export JAVA_HOME=/homes/jdk && " +
                    new File(model.getOracleHomeById(DEFAULT_JRF_HOME).getPath() + "/OPatch/auto/core/bin/opatchauto.sh").getCanonicalPath() +
                    " apply " + new File(allPatchesDirectory + "/fmwzdtPatches/NONE/99999701/").getCanonicalPath() +
                    " -instance " + model.getDomainById(DEFAULT_JRF_DOMAIN_NAME).getPath() +
                    " -host " + containerId.trim() + "," + hostInfo1.getAddress().trim() + "," + hostInfo2.getAddress().trim() +
                    " -wallet " + walletsDirectory.toFile().getCanonicalPath() +
                    " -walletPassword welcome1" +
                    " -plan parallel" +
                    " -sshuserequivalence " +
                    " -sshaccesskey " + DEFAULT_SSH_KEY_LOCATION +
                    " -wls-admin-host " + containerId.trim() + ":" + "7001" +
                    " -log " + sessionLog +
                    "\""};
            System.out.println("[opatchauto.qa]Executing command " + Arrays.toString(opatchautoCommand));

            pb = new ProcessBuilder(opatchautoCommand);
            pb.inheritIO();
            process = pb.start();
            process.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage() + " - see log file!");
        }
    }

    @FMWDockerOptions(oracleHomesToMount = "jrf-12.2.1.3.0",
            tags = "opatchauto-fmwzdt-docker-test-multinode",
            description = "Feature\n" + "  OPatchAuto FMWZDT\n\n"
                    + "Scenario\n" + "  OPatchAuto ZDT with Patch Uptime as NONE in Paralle plan sample test\n\n",
            numberOfContainers = 3)
    public void testZDTNoneParallelSample() throws Throwable {
        String testName = "testZDTNoneParallelSample";
        List<ModelTarget> targets;
        ProcessBuilder pb;
        Process process;
        try {
            /**Get main and extra container addresses*/
            containerId = InetAddress.getLocalHost().getHostName();
            containerAddress = InetAddress.getLocalHost().getHostAddress();
            extraContainers = AgentHelper.getAgentInfos();

            assertNotNull("[opatchauto.qa]Additional containers should not be null", extraContainers);
            assertEquals("[opatchauto.qa]There should be two additional containers created", 2, extraContainers.size());
            AgentInfo hostInfo1 = extraContainers.get(0);
            AgentInfo hostInfo2 = extraContainers.get(1);

            System.out.println("[opatchauto.qa]Sample Docker run for OPatchAuto Container 1 " + containerId + " : " + containerAddress);
            System.out.println("[opatchauto.qa]Sample Docker run for OPatchAuto Container 2 " + hostInfo1.getHostName() + " - " + hostInfo1.getAddress());
            System.out.println("[opatchauto.qa]Sample Docker run for OPatchAuto Container 3 " + hostInfo2.getHostName() + " - " + hostInfo2.getAddress());

            /**Create oracle home directory*/
            Path homesDirectory = Paths.get("/scratch", "/homes", containerId);

            /**Create oraInventory directory*/
            Path oraInventoryDirectory = Paths.get("/scratch", "/homes", containerId, "oraInventory");

            /**Create domain directory*/
            Path domainsDirectory = Paths.get("/domains", containerId);
            Files.createDirectories(domainsDirectory);
            System.out.println("Created directory - " + domainsDirectory);
            if (!Files.exists(domainsDirectory) || !Files.isWritable(domainsDirectory)) {
                fail("Could not create parent directory for domain!");
            }

            /**Create wallet directory*/
            Path walletsDirectory = Paths.get("/test_output", containerId, "wallets", "jrf-multinode-domain-wallet");
            Files.createDirectories(walletsDirectory);
            System.out.println("Created directory - " + walletsDirectory);
            if (!Files.exists(walletsDirectory) || !Files.isWritable(walletsDirectory)) {
                fail("Could not create parent directory for wallets!");
            }

            /**Create wallet and add credentials*/
            createWallet(walletsDirectory.toFile());
            Credentials credentials = new Credentials();
            credentials.setCredential(containerId + ":wls", "weblogic", "welcome1".toCharArray());
            credentials.setCredential(DEFAULT_JRF_DOMAIN_NAME + "/ADMIN", "weblogic", "welcome1".toCharArray());
            credentials.setCredential(DEFAULT_JRF_DOMAIN_NAME + "/NM", "weblogic", "welcome1".toCharArray());
            addCredentialsToWallet(walletsDirectory.toFile(), credentials);

            /**Create builder, load model and add model variables*/
            EnvironmentModelBuilder builder = createModelBuilder(DEFAULT_JRF_HOME_PATH);
            System.out.println("[opatchauto.qa]Loading Model");
            EnvironmentModel model = builder.buildFromFiles("jrf-multinode-topology", "6.0.0", walletsDirectory.toFile(), "welcome1".toCharArray());
            model.getTopology().getVariables().add(new Variable("SERVER_HOST", containerAddress));
            model.getTopology().getVariables().add(new Variable("SERVER1_HOST", hostInfo1.getAddress()));
            model.getTopology().getVariables().add(new Variable("SERVER2_HOST", hostInfo2.getAddress()));
            model.getOracleHomeById(DEFAULT_JRF_HOME).setPath
                    (new File(homesDirectory + File.separator + DEFAULT_JRF_HOME).getCanonicalPath());
            model.getOracleHomeById(DEFAULT_JRF_HOME).setInventoryLocation
                    (new File(oraInventoryDirectory + "").getCanonicalPath());
            model.getDomainById(DEFAULT_JRF_DOMAIN_NAME).setPath
                    (new File(domainsDirectory + File.separator + DEFAULT_JRF_DOMAIN_NAME).getCanonicalPath());
            System.out.println("[opatchauto.qa]Loaded Model - \n" + model.getTopology().toString());

            /**Create action factory to get an action*/
            createActionFactory();

            /**Execute install-oraclehome action in all containers*/
            System.out.println("[opatchauto.qa]Running install-oraclehome Action in container " + containerId);
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createOracleHomeTarget(DEFAULT_JRF_HOME));
            runAction("install-oraclehome", model, targets, new Properties());

            System.out.println("[opatchauto.qa]Running install-oraclehome Action in container " + hostInfo1.getAddress());
            runRestAction(hostInfo1.getUrl(), "install-oraclehome", model, targets, new Properties());
            System.out.println("[opatchauto.qa]Running install-oraclehome Action in container " + hostInfo2.getAddress());
            runRestAction(hostInfo2.getUrl(), "install-oraclehome", model, targets, new Properties());

            /**Copy latest fmwcommon artifacts and plugins to new oraclehome. The source oh will be the one
             * requested to be mounted in docker options*/
//            copyFMWCommonJars(DEFAULT_JRF_HOME_PATH, model.getOracleHomeById(DEFAULT_JRF_HOME).getPath(), containerAddress);
//            copyFMWCommonJars(DEFAULT_JRF_HOME_PATH, model.getOracleHomeById(DEFAULT_JRF_HOME).getPath(), hostInfo1.getAddress());
//            copyFMWCommonJars(DEFAULT_JRF_HOME_PATH, model.getOracleHomeById(DEFAULT_JRF_HOME).getPath(), hostInfo2.getAddress());

            /**Execute create-domain action*/
            System.out.println("[opatchauto.qa]Running create-domain Action");
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createDomainTarget(DEFAULT_JRF_DOMAIN_NAME));
            runAction("create-domain", model, targets, new Properties());

            /**Run pack-domain action to pack the domain*/
            Path packedDomainTemplate = Paths.get("/test_output", containerId, model.getDomainById(DEFAULT_JRF_DOMAIN_NAME).getName() + ".jar");
            Properties packExtra = new Properties();
            packExtra.setProperty(ActionConstants.PACK_DOMAIN_template, packedDomainTemplate.toString());
            packExtra.setProperty(ActionConstants.PACK_DOMAIN_templateName, model.getDomainById(DEFAULT_JRF_DOMAIN_NAME).getName());
            packExtra.setProperty(ActionConstants.PACK_DOMAIN_managed, "true");
            System.out.println("[opatchauto.qa]Running pack-domain Action");
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createDomainTarget(DEFAULT_JRF_DOMAIN_NAME));
            runAction("pack-domain", model, targets, packExtra);
            assertTrue("[opatchauto.qa]The packed domain file should exist", packedDomainTemplate.toFile().exists());

            /**Unpack the domain into extra containers*/
            Properties unpackExtra = new Properties();
            unpackExtra.setProperty(ActionConstants.UNPACK_DOMAIN_template, packedDomainTemplate.toString());
            System.out.println("[opatchauto.qa]Unpacking domain onto container " + hostInfo1.getAddress());
            runRestAction(hostInfo1.getUrl(), "unpack-domain", model, targets, unpackExtra);
            System.out.println("[opatchauto.qa]Unpacking domain onto container " + hostInfo2.getAddress());
            runRestAction(hostInfo2.getUrl(), "unpack-domain", model, targets, unpackExtra);

            /**Start node managers of all hosts*/
            System.out.println("[opatchauto.qa]Starting Node Manager of container " + containerId);
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createNodeManagerOnHostTarget("machine1-nm", "machine1"));
            runAction("start", model, targets, new Properties());

            System.out.println("[opatchauto.qa]Starting Node Manager of container " + hostInfo1.getAddress());
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createNodeManagerOnHostTarget("machine2-nm", "machine2"));
            runRestAction(hostInfo1.getUrl(), "start", model, targets, new Properties());

            System.out.println("[opatchauto.qa]Starting Node Manager of container " + hostInfo2.getAddress());
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createNodeManagerOnHostTarget("machine3-nm", "machine3"));
            runRestAction(hostInfo2.getUrl(), "start", model, targets, new Properties());

            /**Start and Stop AdminServer for the first time*/
            Properties startStopExtra = new Properties();
            startStopExtra.setProperty(ActionConstants.START_STOP_noNodeManager, "true");
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createServerInDomainTarget("AdminServer", DEFAULT_JRF_DOMAIN_NAME));
            System.out.println("[opatchauto.qa]Starting Admin Server");
            runAction("start", model, targets, startStopExtra);
            System.out.println("[opatchauto.qa]Stopping Admin Server");
            runAction("stop", model, targets, startStopExtra);

            /**Start domain using node managers*/
            System.out.println("[opatchauto.qa]Starting Domain using node manager");
            targets = new ArrayList<>();
            targets.add(ModelTargetFactory.createDomainTarget(DEFAULT_JRF_DOMAIN_NAME));
            runAction("start", model, targets, new Properties());

            /**START OPATCHAUTO OPERATIONS*/

            /**Copy test-patches to common folder inside /test_output*/
            Path allPatchesDirectory = Paths.get("/test_output", containerId, "test-patches");
            FileUtils.copyDirectory(new File(TEST_PATCHES_DIR), allPatchesDirectory.toFile());

            /**Run OPatchAuto command from remote host using ssh*/
            System.out.println("[opatchauto.qa]Executing opatchauto from container " + hostInfo1.getAddress());
            String sessionLog = new File("/test_output/" + containerId, testName + "_APPLY" + ".session.log").getCanonicalPath();

            String[] opatchautoCommand = {"/bin/bash", "-c", "ssh " + containerAddress +
                    " \"export JAVA_HOME=/homes/jdk && " +
                    new File(model.getOracleHomeById(DEFAULT_JRF_HOME).getPath() + "/OPatch/auto/core/bin/opatchauto.sh").getCanonicalPath() +
                    " apply " + new File(allPatchesDirectory + "/fmwzdtPatches/NONE/99999701/").getCanonicalPath() +
                    " -instance " + model.getDomainById(DEFAULT_JRF_DOMAIN_NAME).getPath() +
                    " -host " + containerId.trim() + "," + hostInfo1.getAddress().trim() + "," + hostInfo2.getAddress().trim() +
                    " -wallet " + walletsDirectory.toFile().getCanonicalPath() +
                    " -walletPassword welcome1" +
                    " -plan parallel" +
                    " -sshuserequivalence " +
                    " -sshaccesskey " + DEFAULT_SSH_KEY_LOCATION +
                    " -wls-admin-host " + containerId.trim() + ":" + "7001" +
                    " -log " + sessionLog +
                    "\""};
            System.out.println("[opatchauto.qa]Executing command " + Arrays.toString(opatchautoCommand));

            pb = new ProcessBuilder(opatchautoCommand);
            pb.inheritIO();
            process = pb.start();
            process.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage() + " - see log file!");
        }
    }



    public static void displayDirectoryContents(File dir) {
        try {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    System.out.println("directory:" + file.getCanonicalPath());
                    displayDirectoryContents(file);
                } else {
                    System.out.println("     file:" + file.getCanonicalPath());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
