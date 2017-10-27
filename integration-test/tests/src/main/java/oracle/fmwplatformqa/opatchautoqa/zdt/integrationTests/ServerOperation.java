package oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests;

import oracle.fmwplatform.actionframework.api.v2.ActionResult;
import oracle.fmwplatform.actionframework.api.v2.ActionStatusCode;
import oracle.fmwplatform.credentials.credential.CredentialBuilder;
import oracle.fmwplatform.credentials.credential.Credentials;
import oracle.fmwplatform.credentials.wallet.WalletStoreProvider;
import oracle.fmwplatform.envspec.model.EnvironmentModel;
import oracle.fmwplatform.envspec.model.domain.Server;
import oracle.fmwplatform.envspec.model.targets.ModelTarget;
import oracle.fmwplatform.envspec.model.targets.ModelTargetFactory;
import oracle.fmwplatform.envspec.model.topology.Domain;
import oracle.fmwplatform.envspec.model.topology.NodeManager;
import oracle.fmwplatform.envspec.model.topology.ServerBinding;
import oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests.fmwzdt.AbstractFMWZDTScriptRunner;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ServerOperation extends AbstractFMWZDTScriptRunner {
    protected static Credentials creds;
    protected StartStopOperation startStop;
    protected List<ModelTarget> mTarget;
    protected boolean noNM = true;

    @BeforeClass
    public void generateTopology() {
        if (envModel == null) {
            try {
                creds = new CredentialBuilder(new WalletStoreProvider(walletLocation, walletType.equals("ENCRYPTED") ? walletPassword.toCharArray() : null)).loadCredentials();
                envModel = AbstractScriptRunner.getEnvironmentModel();
                envModel.setCredentials(creds);
                for (Domain d : envModel.getDomains()) {
                    for (Server s : envModel.getServersInDomain(d)) {
                        if (s.isAdminServer()) {
                            ServerBinding sb = d.getServerBindingForServerID(s.getId());
                            d.setAdminServerUrl("t3://" + sb.getListenAddress() + ":" + sb.getListenPort());
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                topologyLoc = testOutput + File.separator + "modelOutput" + File.separator + "topologies" + File.separator + "modelOutput" + ".xml";
//                envModel = null;
            }
        }
    }

//    @BeforeClass
//    public void updateNodeMgr() throws Exception{
//        String nAddress=null;
//        String nPort=null;
//        if(envModel.getNodeManagers().size()>0) {
//        for (NodeManager n : envModel.getNodeManagers()) {
//                nAddress = n.getTuningParameters().getSettingValueByAlias("NmAddress");
//                nPort = n.getTuningParameters().getSettingValueByAlias("NmPort");
//                try {
//                    updateNMPorp(nAddress);
//                } catch (Exception e) {
//                    System.out.println("Caught Exception During NodeManager properties update : ");
//                    e.printStackTrace();
//                }
//            }
//        }else {
//            System.out.println("[INFO] : No Node Manager Configured.");
//        }
//    }

    public ServerOperation() {
        startStop = new StartStopOperation();
    }

    @Test(enabled = true, description = "Start and Stop Operation to Prepare Environmet")
    public void startStopSetup() throws Exception {
        System.out.println("\nStopping Servers : \n");
        startStopServers(StartStopOperation.SERVEROP.STOP);

        if (noNM == false) {
            System.out.println("\nStopping NodeManagers : \n");
            startStopNM(TestEnvHelper.START_STOP.STOP);
            for (int i = 0; i <= 10000; i++) {
                if (i % 1000 == 0)
                    System.out.print(".");
            }
        }
        System.out.println("");

        if (noNM == false) {
            System.out.println("\nStarting NodeManagers : \n");
            startStopNM(TestEnvHelper.START_STOP.START);
        }
        System.out.println("\nStarting Servers : \n");
        startStopServers(StartStopOperation.SERVEROP.START);
        for (int i = 0; i <= 10000; i++) {
            if (i % 1000 == 0)
                System.out.print(".");
        }
    }

    public void startStopServers(StartStopOperation.SERVEROP action) throws Exception {
        String nPort = null;
        String cName = null;
        mTarget = new ArrayList<>();
        for (Domain d : envModel.getDomains()) {
//            for (NodeManager nm : envModel.getNodeManagersForDomain(d.getId())) {
//                nPort = nm.getTuningParameters().getSettingValueByAlias("NmPort");
//            }

            if (envModel.getNodeManagersForDomain(d.getId()).size() > 0) {
                noNM = false;
            }
            try {
                System.out.println(action + " Domain " + d.getId());
                mTarget.add(ModelTargetFactory.createDomainTarget(d.getId()));
                startStopTarget(action, envModel, mTarget);
                mTarget.clear();
            } catch (Exception e) {
                System.out.println("Caught Exception During " + action + " Domain Action : ");
                e.printStackTrace();
            }
//            for (Server s : envModel.getServersInDomain(d)) {
//                if (s.isAdminServer()) {
//                    try {
//                        System.out.println(action+" Server "+s.getId());
//                        mTarget.add(ModelTargetFactory.createServerInDomainTarget(s.getId(),d.getName()));
//                        startStopTarget(action,envModel,mTarget);
//                        mTarget.clear();
//                    } catch (Exception e) {
//                        System.out.println("Caught Exception During " + action + " Server Action : ");
//                        e.printStackTrace();
//                    }
//                } else {
//                    for (ClusterBinding cb : envModel.getClusterBindingsForDomain(d)) {
//                        cName = cb.getClusterRef();
//                        try {
//                            System.out.println(action+" Cluster "+cName);
//                            mTarget.add(ModelTargetFactory.createClusterInDomainTarget(cName,d.getName()));
//                            startStopTarget(action,envModel,mTarget);
//                            mTarget.clear();
//                        } catch (Exception e) {
//                            System.out.println("Caught Exception During " + action + " Cluster Action : ");
//                            e.printStackTrace();
//                        }
//                    }

//                }
//            }
        }
    }

    public void stopManagedServerOperation() throws Exception {
        System.out.println("\nStopping Server : ");
        startStopManagedServer(StartStopOperation.SERVEROP.STOP);

    }

    public void startStopManagedServer(StartStopOperation.SERVEROP action) throws Exception {
        ArrayList<String> serverName = new ArrayList<String>();
        String sName = null;
        String server=null;
        mTarget = new ArrayList<>();

        for (Domain d : envModel.getDomains()) {
            if (envModel.getNodeManagersForDomain(d.getId()).size() > 0) {
                noNM = false;
            }
            for (Server s : envModel.getServersInDomain(d)){
                sName = s.getId();
                if (!s.isAdminServer()) {
                    serverName.add(sName);
                }
            }
            server=serverName.get(0);
            System.out.println(server);
            try {
                mTarget.add(ModelTargetFactory.createServerInDomainTarget(server,d.getId()));
                startStopTarget(action, envModel, mTarget);
                mTarget.clear();
            } catch (Exception e) {
                System.out.println("Caught Exception During Stopping Server : ");
                e.printStackTrace();
            }
        }

    }

    public void stopAdminServerOperation() throws Exception {
        System.out.println("\nStopping Server : ");
        startStopAdminServer(StartStopOperation.SERVEROP.STOP);

    }

    public void startAdminServerOperation() throws Exception {
        System.out.println("\nStarting Server : ");
        startStopAdminServer(StartStopOperation.SERVEROP.START);

    }

    public void startStopAdminServer(StartStopOperation.SERVEROP action) throws Exception {
        ArrayList<String> serverName = new ArrayList<String>();
        String sName = null;
        String server=null;
        mTarget = new ArrayList<>();

        for (Domain d : envModel.getDomains()) {
            if (envModel.getNodeManagersForDomain(d.getId()).size() > 0) {
                noNM = false;
            }
            for (Server s : envModel.getServersInDomain(d)){
                sName = s.getId();
                if (s.isAdminServer()) {
                    serverName.add(sName);
                }
            }
            server=serverName.get(0);
            System.out.println(server);
            try {
                mTarget.add(ModelTargetFactory.createServerInDomainTarget(server,d.getId()));
                startStopTarget(action, envModel, mTarget);
                mTarget.clear();
            } catch (Exception e) {
                System.out.println("Caught Exception During Stopping Server : ");
                e.printStackTrace();
            }
        }

    }

    public void startStopNM(TestEnvHelper.START_STOP action) throws Exception {
        mTarget = new ArrayList<>();
        String nAddress = null;
        String nPort = null;
        for (NodeManager n : envModel.getNodeManagers()) {
            //NM Props are no longer tuning parameters. They are attributes.
//            nAddress = n.getTuningParameters().getSettingValueByAlias("NmAddress");
//            nPort = n.getTuningParameters().getSettingValueByAlias("NmPort");
            nAddress = n.getAddress();
            if (n.getPort() == null || n.getPort().isEmpty())
                nPort = "5556";
            else
                nPort = n.getPort();
            try {
                System.out.println(action + " NodeManager " + n.getId() + " on " + nAddress + " with Port " + nPort);
                TestEnvHelper.operateNodeManager(action, nAddress);
//                mTarget.add((ModelTargetFactory.createNodeManagerOnHostTarget(n.getId(),envModel.getHostForNodeManager(n.getId()).getId())));
//                startStopTarget(action,envModel,mTarget);
//                mTarget.clear();
            } catch (Exception e) {
                System.out.println("Caught Exception During " + action + " NodeManager Action: ");
                e.printStackTrace();
            }
        }
    }

    public void startStopTarget(StartStopOperation.SERVEROP operation, EnvironmentModel eModel, List<ModelTarget> eTarget) throws Exception {
        ActionResult actionResult;
        if (noNM == false) {
            actionResult = startStop.operate(operation, eModel, eTarget, oracleHome);
        } else {
            actionResult = startStop.operateWithoutNodeManager(operation, eModel, eTarget, oracleHome);
        }
        System.out.println("Result : " + actionResult.getStatusCode());

        //Commenting due to bug 23590926 : We need to uncomment once this get fixed
        System.out.println("Result : " + actionResult.getStatusCode() + " - " + actionResult.getStatusDetail());
        Assert.assertTrue(actionResult.getStatusCode() == ActionStatusCode.SUCCESS || actionResult.getStatusCode() == ActionStatusCode.SUCCESS_NO_ACTION);
    }

//    public static void updateNMPorp(String host) throws Exception {
//        Session session;
//        ChannelExec channelExec = null;
//        ChannelSftp channelSftp = null;
//        InputStream in1 = null;
//        InputStream in2 = null;
//        boolean success = false;
//        String updateNMPropFile = "updateNMProp.sh";
//
//        try {
//            Credential sshCredential = AbstractScriptRunner.credentialManager.getCredential(host, "ssh");
//            if (sshCredential.equals(null)) {
//                throw new Exception("[ERROR] SSH Credential for host " + host + " is not available in Wallet");
//            }
//            final String hostUser = sshCredential.getUsername();
//            final String hostPassword = new String(sshCredential.getPassword());
//            System.out.println("Host        : " + host);
//            System.out.println("Username    : " + hostUser);
//            System.out.println("Password    : " + hostPassword);
//
//            //Copy NM Scripts to Host to Update NM Properties
//            JSch jsch = new JSch();
//            session = jsch.getSession(hostUser, host, 22);
//            session.setPassword(hostPassword);
//            session.setConfig("StrictHostKeyChecking", "no");
//            session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
//            session.connect(15 * 1000);
//
//            channelSftp = (ChannelSftp) session.openChannel("sftp");
//            channelSftp.connect();
//            channelSftp.cd(new File(AbstractScriptRunner.nodeManagerHome).getCanonicalPath());
//            System.out.println("[zdt-qa.info] Copying " + updateNMPropFile + " to host : " + host);
//            channelSftp.put(new FileInputStream(new File(AbstractScriptRunner.testEnvScriptsHome + File.separator +
//                    updateNMPropFile).getCanonicalPath()), updateNMPropFile);
//            Thread.sleep(5 * 1000);
//            channelSftp.chmod(Integer.parseInt("755", 8), updateNMPropFile);
//            channelSftp.disconnect();
//
//            //Update NM Properties
//            final String updateNMPropCommand = (String.format("%1$s %2$s %3$s",
//                    new File(AbstractScriptRunner.nodeManagerHome + File.separator + updateNMPropFile).getCanonicalPath(),
//                    new File(AbstractScriptRunner.domainHome).getCanonicalPath(),
//                    AbstractScriptRunner.suiteGrp));
//            System.out.println("Executing command : " + updateNMPropCommand + " on host : " + host);
//            channelExec = (ChannelExec) session.openChannel("exec");
//            channelExec.setCommand(updateNMPropCommand);
//            channelExec.connect();
//            in1 = channelExec.getInputStream();
//            BufferedReader reader = new BufferedReader(new InputStreamReader(in1));
//            String line;
//            while ((line = reader.readLine()) != null) {
//                System.out.println("[zdt-qa.info] " + line);
//                if (line.contains("nodemanager.properties Updated Successfully")) {
//                    success = true;
//                    break;
//                }
//            }
//            if (success) {
//                System.out.println("[zdt-qa.info] Node Manager Properties Updated Successfully");
//                success = false;
//            } else
//                throw new Exception("[error] Node Manager Properties Update Failed");
//            in1.close();
//            session.disconnect();
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//        }
//
//    }

    @AfterClass
    @Override
    public void cleanUpClass() {
        envModel = null;
    }
}
