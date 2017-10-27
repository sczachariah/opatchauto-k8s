package oracle.fmwplatformqa.opatchautoqa.util;

import oracle.fmwplatform.actionframework.api.v2.*;
import oracle.fmwplatform.actionframework.api.v2.rest.RestActionInvoker;
import oracle.fmwplatform.credentials.credential.Credentials;
import oracle.fmwplatform.credentials.wallet.WalletStoreProvider;
import oracle.fmwplatform.envspec.model.EnvironmentModel;
import oracle.fmwplatform.envspec.model.EnvironmentModelBuilder;
import oracle.fmwplatform.envspec.model.targets.ModelTarget;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * Util class with utility methods for use in docker tests
 */
public class DockerTestUtil {

    public static final char SEP = File.separatorChar;

    public static final String DEFAULT_JAVA_PATH = "/homes/jdk/bin/java";

    public static final String DEFAULT_WLS_HOME = "wls-12.2.1.3.0";
    public static final String DEFAULT_JRF_HOME = "jrf-12.2.1.3.0";

    public static final String DEFAULT_WLS_HOME_PATH = "/homes/" + DEFAULT_WLS_HOME;
    public static final String DEFAULT_JRF_HOME_PATH = "/homes/" + DEFAULT_JRF_HOME;

    public static final String DEFAULT_WLS_DOMAIN_NAME = "wls";
    public static final String DEFAULT_WLS_DOMAIN_PATH = "/domains/" + DEFAULT_WLS_DOMAIN_NAME;
    public static final String DEFAULT_JRF_DOMAIN_NAME = "jrf";
    public static final String DEFAULT_JRF_DOMAIN_PATH = "/domains/" + DEFAULT_JRF_DOMAIN_NAME;

    public static final String ENVSPEC_MODELS_JAR_PATH = "/fmwcommon/envspec/models/target/envspec-models-2.0.0-SNAPSHOT.jar";
    public static final String STANDARDACTIONS_JAR_PATH = "/fmwcommon/standardactions/standardactions-v2/target/standardactions-v2-2.0.0-SNAPSHOT.jar";

    public static final String OC_HOME_PREFIX = "oracle_common";
    public static final String OC_MODULES_PREFIX = OC_HOME_PREFIX + SEP + "modules";
    public static final String COMMON_DIR = OC_MODULES_PREFIX + SEP + "fmwplatform" + SEP + "common";
    public static final String OC_PLUGINS_PREFIX = OC_HOME_PREFIX + SEP + "plugins";
    public static final String PLUGINS_DIR = OC_PLUGINS_PREFIX + SEP + "fmwplatform";

    public static final String DOCKERTEST_CLASSPATH = "/home/fmwpltfm/acceptance.dockertest.classpath/";
    public static final String WORKSPACE = "/scratch/fmwpltfm/jenkins/workspace/";
    public static final String JOB_NAME = "opatchauto-docker-tests-12.2.1.3.0";
    public static final String TEST_CLASSES_DIR = DOCKERTEST_CLASSPATH + WORKSPACE + JOB_NAME + "/target/test-classes";
    public static final String TEST_PATCHES_DIR = DOCKERTEST_CLASSPATH + WORKSPACE + JOB_NAME + "/target/test-patches";

    public static final String DEFAULT_MODEL_PATH = TEST_CLASSES_DIR + "/models";

    public static final String DEFAULT_SSH_KEY_LOCATION = "/home/fmwpltfm/.ssh/id_rsa";

    public static ActionFactory actionFactory = null;

    public static String createWallet(File walletLocation) throws Exception {
        try {
            System.out.println("[opatchauto.qa]Creating Credentials");
            Credentials credentials = new Credentials();
            credentials.setCredential("WLS/ADMIN", "weblogic", "welcome1".toCharArray());
            credentials.setCredential("WLS/NODE", "weblogic", "welcome1".toCharArray());

            System.out.println("[opatchauto.qa]Creating Wallet " + walletLocation.getCanonicalFile());
            walletLocation.mkdirs();
            WalletStoreProvider walletStoreProvider = new WalletStoreProvider(walletLocation, "welcome1".toCharArray());
            walletStoreProvider.createWallet();
            walletStoreProvider.storeCredentials(credentials);
            walletStoreProvider.closeWallet(false);
        } catch (Exception e) {
            System.out.println("[opatchauto.qa]Error creating Wallet");
            e.printStackTrace();
            throw e;
        } finally {
            return walletLocation.getCanonicalPath();
        }
    }

    public static void addCredentialsToWallet(File walletLocation, Credentials credentials) throws Exception {
        try {
            System.out.println("[opatchauto.qa]Adding Credentials to wallet");
            WalletStoreProvider walletStoreProvider = new WalletStoreProvider(walletLocation, "welcome1".toCharArray());
            walletStoreProvider.storeCredentials(credentials);
            walletStoreProvider.closeWallet(false);
        } catch (Exception e) {
            System.out.println("[opatchauto.qa]Error adding credentials to Wallet");
            e.printStackTrace();
            throw e;
        }
    }

    public static EnvironmentModelBuilder createModelBuilder(String oracleHome) {
        System.out.println("[opatchauto.qa]Creating Builder");
        EnvironmentModelBuilder builder = new EnvironmentModelBuilder(oracleHome);
        builder.prependModelSearchLocation(DEFAULT_MODEL_PATH);
        builder.prependModelSearchLocation(ENVSPEC_MODELS_JAR_PATH);
        return builder;
    }

    public static ActionFactory createActionFactory() throws Throwable {
        System.out.println("[opatchauto.qa]Creating an Action Factory");
        actionFactory = DefaultActionFactoryLocator.locateActionFactory();
        return actionFactory;
    }

    public static void runAction(String actionName, EnvironmentModel environmentModel, List<ModelTarget> targets, Properties extra) throws Throwable {
        if (actionFactory == null)
            actionFactory = DefaultActionFactoryLocator.locateActionFactory();

        Action action = actionFactory.getAction(actionName, DockerTestUtil.DEFAULT_JRF_HOME_PATH);
        ActionResult result = action.run(environmentModel, targets, extra);
        System.out.println("[opatchauto.qa]Result = " + result);
        assertEquals("[opatchauto.qa]The ActionResult should contain a successful Status Code", ActionStatusCode.SUCCESS, result.getStatusCode());
    }

    public static void runRestAction(String url, String actionName, EnvironmentModel environmentModel, List<ModelTarget> targets, Properties extra) throws Throwable {
        RestActionInvoker restActionInvoker = new RestActionInvoker(url);
        ActionResult result = restActionInvoker.runAction(actionName, environmentModel, targets, extra);
        System.out.println("[opatchauto.qa]Result = " + result);
        assertEquals("[opatchauto.qa]The ActionResult should contain a successful Status Code", ActionStatusCode.SUCCESS, result.getStatusCode());
    }

    public static void copyFMWCommonJars(String sourceOracleHome, String destinationOracleHome, String destinationHostAddress) throws Throwable {
        ProcessBuilder pb;
        Process process;

        System.out.println("[opatchauto.qa]Copying latest fmwcommon artifacts to oracleHome in " + destinationHostAddress);
        String[] copyJarsCommand = {"scp", "-pr", "-o", "StrictHostKeyChecking=no",
                new File(sourceOracleHome + SEP + COMMON_DIR).getCanonicalPath() + SEP + ".",
                destinationHostAddress + ":" + new File(destinationOracleHome + SEP + COMMON_DIR).getCanonicalPath()};
        System.out.println("[opatchauto.qa]Executing command " + Arrays.toString(copyJarsCommand));
        pb = new ProcessBuilder(copyJarsCommand);
        pb.inheritIO();
        process = pb.start();
        process.waitFor();

        System.out.println("[opatchauto.qa]Copying latest fmwcommon plugins to oracleHome in " + destinationHostAddress);
        String[] copyPluginsCommand = {"scp", "-pr", "-o", "StrictHostKeyChecking=no",
                new File(sourceOracleHome + SEP + PLUGINS_DIR).getCanonicalPath() + SEP + ".",
                destinationHostAddress + ":" + new File(destinationOracleHome + SEP + PLUGINS_DIR).getCanonicalPath()};
        System.out.println("[opatchauto.qa]Executing command " + Arrays.toString(copyPluginsCommand));
        pb = new ProcessBuilder(copyPluginsCommand);
        pb.inheritIO();
        process = pb.start();
        process.waitFor();
    }
}

