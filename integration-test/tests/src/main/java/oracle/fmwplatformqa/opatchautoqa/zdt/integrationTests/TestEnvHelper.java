package oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import oracle.fmwplatformqa.opatchautoqa.zdt.credential.Credential;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.Scanner;

public class TestEnvHelper {

    public enum SERVER_TYPE {
        ADMIN("Server"),
        MANAGED_SERVER("Server"),
        NM("NodeManager");

        private String server;

        SERVER_TYPE(String srvr) {
            server = srvr;
        }

        public String getServerType() {
            return this.server;
        }
    }

    public enum START_STOP {
        START("start"),
        STOP("stop"),
        KILL_NM("KILL_NM"),
        KILL_SERVER("KILL_SERVER");

        private String operation;

        START_STOP(String op) {
            operation = op;
        }

        public String getOperation() {
            return this.operation;
        }

        public String getCommand() {
            String command = "INVALID";
            switch (this.operation) {
                case "KILL_NM":
                    //2 is SIGINT 9 is SIGKILL
                    command = "ps -ef | grep weblogic.NodeManager | cut -c 10-15 | sed ':a;N;$!ba;s/\\n/ /g' | xargs kill -2";
                    break;
            }
            return command;
        }
    }

    public TestEnvHelper() {
    }

    private static void printHeader(String operation) {
        System.out.println("------------------------------------------------------------------------");
        System.out.println("Running " + operation);
        System.out.println("------------------------------------------------------------------------");
    }

    public static void operateNodeManager(START_STOP operation, String host) throws Exception {
        printHeader((operation.getOperation() + " Node Manager").toUpperCase() + " on host " + host);
        Session session;
        ChannelExec channelExec = null;
        ChannelSftp channelSftp = null;
        InputStream in1 = null;
        InputStream in2 = null;
        boolean success = false;
        String updateNMProp = "updateNMProp.sh";

        try {
            Credential sshCredential = AbstractScriptRunner.credentialManager.getCredential(host, "ssh");
            if (sshCredential.equals(null)) {
                throw new Exception("[error] SSH Credential for host " + host + " is not available in Wallet");
            }
            final String hostUser = sshCredential.getUsername();
            final String hostPassword = new String(sshCredential.getPassword());
            System.out.println("Host        : " + host);
            System.out.println("Username    : " + hostUser);
            System.out.println("Password    : " + hostPassword);

            //Copy NM Scripts to Host to Update NM Properties
            JSch jsch = new JSch();
            session = jsch.getSession(hostUser, host, 22);
            session.setPassword(hostPassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            session.connect(15 * 1000);

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            channelSftp.cd(new File(AbstractScriptRunner.nodeManagerHome).getCanonicalPath());
            System.out.println("[zdt-qa.info] Copying " + updateNMProp + " to host : " + host);
            channelSftp.put(new FileInputStream(new File(AbstractScriptRunner.testEnvScriptsHome + File.separator +
                    updateNMProp).getCanonicalPath()), updateNMProp);
            Thread.sleep(5 * 1000);
            channelSftp.chmod(Integer.parseInt("755", 8), updateNMProp);
            channelSftp.disconnect();

            //Update NM Properties
            final String updateNMPropCommand = (String.format("%1$s %2$s %3$s",
                    new File(AbstractScriptRunner.nodeManagerHome + File.separator + updateNMProp).getCanonicalPath(),
                    new File(AbstractScriptRunner.domainHome).getCanonicalPath(),
                    AbstractScriptRunner.suiteGrp));
            System.out.println("Executing command : " + updateNMPropCommand + " on host : " + host);
            channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setCommand(updateNMPropCommand);
            channelExec.connect();
            in1 = channelExec.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in1));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[zdt-qa.info] " + line);
                if (line.contains("nodemanager.properties Updated Successfully")) {
                    success = true;
                    break;
                }
            }
            if (success) {
                System.out.println("[zdt-qa.info] Node Manager Properties Updated Successfully");
                success = false;
            } else
                throw new Exception("[error] Node Manager Properties Update Failed");
            in1.close();
            session.disconnect();

            //Execute NM Start/Stop
            if (operation.equals(START_STOP.START) || operation.equals(START_STOP.STOP)) {
                final String operateNodeManager = "nohup " + new File(AbstractScriptRunner.domainHome + File.separator + "bin" +
                        File.separator + operation.getOperation() + AbstractScriptRunner.nodeManager).getCanonicalPath();
                System.out.println("Executing command : " + operateNodeManager + " on host : " + host);
                jsch = new JSch();
                session = jsch.getSession(hostUser, host, 22);
                session.setPassword(hostPassword);
                session.setConfig("StrictHostKeyChecking", "no");
                session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
                session.connect(15 * 1000);
                channelExec = (ChannelExec) session.openChannel("exec");
                channelExec.setCommand(operateNodeManager);
                channelExec.setPty(true);
                channelExec.setPtyType("xterm");
                channelExec.connect();
                in2 = channelExec.getInputStream();
                reader = new BufferedReader(new InputStreamReader(in2));

                while ((line = reader.readLine()) != null) {
                    System.out.println("[zdt-qa.info] " + line);
                    if (line.contains("nohup.out")) {
                        Thread.sleep((operation.equals(START_STOP.START) ? 60 : 10) * 1000);
                        //Get nohup.out from host
                        JSch newJsch = new JSch();
                        Session newSession = newJsch.getSession(hostUser, host, 22);
                        newSession.setPassword(hostPassword);
                        newSession.setConfig("StrictHostKeyChecking", "no");
                        newSession.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
                        newSession.connect(15 * 1000);
                        channelSftp = (ChannelSftp) newSession.openChannel("sftp");
                        channelSftp.connect();
                        channelSftp.cd(new File(System.getProperty("user.home")).getCanonicalPath());
                        System.out.println("[zdt-qa.info] Copying nohup.out from host : " + host);
                        channelSftp.get(new File(System.getProperty("user.home"), "nohup.out").getCanonicalPath(),
                                new File(System.getProperty("user.home"), host + ".nohup.out").getCanonicalPath());
                        Thread.sleep(5 * 1000);
                        channelSftp.rm(new File(System.getProperty("user.home"), "nohup.out").getCanonicalPath());
                        channelSftp.disconnect();
                        newSession.disconnect();

                        System.out.println("[zdt-qa.info] Analyzing nohup.out from host : " + host);
                        String scannerLine;
                        Scanner scanner = new Scanner(new FileInputStream(new File(System.getProperty("user.home"), host + ".nohup.out")));
                        while (scanner.hasNext()) {
                            scannerLine = scanner.nextLine();
                            System.out.println("[zdt-qa.info] Scanning line : " + scannerLine);
                            if (scannerLine.contains("Node Manager " + operation.getOperation() + " Failed")) {
                                success = false;
                                break;
                            }
                            if ((scannerLine.contains("Secure socket listener started on port " + AbstractScriptRunner.nmPort)
                                    || scannerLine.contains("NodeManager process is already running"))
                                    && operation.equals(START_STOP.START)) {
                                success = true;
                                break;
                            }
                            if (scannerLine.contains("<StopNodeManager> <Sending signal TERM") && operation.equals(START_STOP.STOP)) {
                                success = true;
                                break;
                            }
                        }
                        scanner.close();
                        FileUtils.forceDelete(new File(System.getProperty("user.home"), host + ".nohup.out"));

                    } else if (!success) {
                        if (line.contains("Node Manager " + operation.getOperation() + " Failed")) {
                            success = false;
                            break;
                        }
                        if ((line.contains("Secure socket listener started on port " + AbstractScriptRunner.nmPort) ||
                                line.contains("The Node Manager is launched") || line.contains("nodemanager.jar")) && operation.equals(START_STOP.START)) {
                            success = true;
                            in2.close();
                            Thread.sleep(60 * 1000);
                            break;
                        }
                        if (line.contains("<StopNodeManager> <Sending signal TERM") && operation.equals(START_STOP.STOP)) {
                            success = true;
                            in2.close();
                            Thread.sleep(15 * 1000);
                            break;
                        }
                    }
                }
                if (success) {
                    System.out.println("[zdt-qa.info] " + operation.getOperation() + " Node Manager Successful");
                    in2.close();
                } else
                    throw new Exception("[error] " + operation.getOperation() + " Node Manager Failed" + " on Host " + host);
            }

            //Kill NM Process
            else if(operation.equals(START_STOP.KILL_NM)){
                final String operateNodeManager = operation.getCommand();
                System.out.println("Executing command : " + operateNodeManager + " on host : " + host);
                jsch = new JSch();
                session = jsch.getSession(hostUser, host, 22);
                session.setPassword(hostPassword);
                session.setConfig("StrictHostKeyChecking", "no");
                session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
                session.connect(15 * 1000);
                channelExec = (ChannelExec) session.openChannel("exec");
                channelExec.setCommand(operateNodeManager);
                channelExec.setPty(true);
                channelExec.setPtyType("xterm");
                channelExec.connect();
                in2 = channelExec.getInputStream();
                reader = new BufferedReader(new InputStreamReader(in2));

                while ((line = reader.readLine()) != null) {
                    System.out.println("[zdt-qa.info] " + line);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }

    }

    public static void operateServer(START_STOP operation, SERVER_TYPE serverType, String serverName, String serverHost, String adminHost, String nmPort, String domainHome) throws Exception {
        printHeader((operation.getOperation() + " " + serverName).toUpperCase() + " on host " + serverHost);
        Session session = null;
        ChannelExec channelExec = null;
        InputStream in = null;
        boolean success = false;
        try {
            Credential sshCredential = AbstractScriptRunner.credentialManager.getCredential(adminHost, "ssh");
            if (sshCredential.equals(null)) {
                throw new Exception("[error] SSH Credential for host " + adminHost + " is not available in Wallet");
            }
            final String hostUser = sshCredential.getUsername();
            final String hostPassword = new String(sshCredential.getPassword());

            Credential wlsCredential = AbstractScriptRunner.credentialManager.getCredential(AbstractScriptRunner.adminHost, "wls");
            if (wlsCredential.equals(null)) {
                throw new Exception("[error] Weblogic Credentials are not available in Wallet");
            }
            final String wlsUser = wlsCredential.getUsername();
            final String wlsPassword = new String(wlsCredential.getPassword());

            System.out.println("Host        : " + adminHost);
            System.out.println("Username    : " + hostUser);
            System.out.println("Password    : " + hostPassword);
            System.out.println("WLS Username: " + wlsUser);
            System.out.println("WLS Password: " + wlsPassword);

            final String serverCommand = (String.format("%1$s -i" +
                            " %2$s" +
                            " %3$s" +
                            " %4$s" +
                            " %5$s" +
                            " \"-Dweblogic.security.SSL.ignoreHostnameVerification=true -Dweblogic.debug.DebugPatchingRuntime=true\"" +
                            " %6$s" +
                            " %7$s" +
                            " %8$s" +
                            " %9$s",
                    new File(AbstractScriptRunner.wlstHome + File.separator + AbstractScriptRunner.wlst).getCanonicalPath(),
                    new File(AbstractScriptRunner.testEnvScriptsHome + File.separator +
                            operation.getOperation() + serverType.getServerType() + ".py").getCanonicalPath(),
                    wlsUser,
                    wlsPassword,
                    serverName,
                    serverHost,
                    nmPort,
                    domainHome.split(File.separator)[(domainHome.split(File.separator).length) - 1],
                    domainHome));

            System.out.println("Executing command : " + serverCommand + " to " + operation.getOperation() + " "
                    + serverName + " on host : " + serverHost);

            JSch jsch = new JSch();
            session = jsch.getSession(hostUser, adminHost, 22);
            session.setPassword(hostPassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            session.connect(15 * 1000);

            channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setCommand(serverCommand);
            channelExec.connect();

            in = channelExec.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[zdt-qa.info] " + line);

                if (line.contains("Successfully killed server " + serverName) ||
                        line.contains("Starting server " + serverName) || line.contains("Killing server " + serverName)) {
                    success = true;
                    Thread.sleep(operation.equals(START_STOP.START) ? 180 * 1000 : 0);
                    break;
                }

                if (line.contains("Server " + operation.getOperation() + " Failed")) {
                    success = false;
                    break;
                }
            }
            if (success)
                System.out.println("[zdt-qa.info] " + operation.getOperation() + " " + serverName + " Successful");
            else
                throw new Exception("[error] " + operation.getOperation() + " " + serverName + " Failed");
        } catch (Exception e) {
            throw e;
        } finally {
        }
    }

    public static void operateCluster(START_STOP operation, String clusterName, String host, String adminPort) throws Exception {
        printHeader((operation.getOperation() + " " + clusterName).toUpperCase() + " on host " + host);
        Session session = null;
        ChannelExec channelExec = null;
        InputStream in = null;
        boolean success = false;
        try {
            Credential sshCredential = AbstractScriptRunner.credentialManager.getCredential(host, "ssh");
            if (sshCredential.equals(null)) {
                throw new Exception("[error] SSH Credential for host " + host + " is not available in Wallet");
            }
            final String hostUser = sshCredential.getUsername();
            final String hostPassword = new String(sshCredential.getPassword());

            Credential wlsCredential = AbstractScriptRunner.credentialManager.getCredential(host, "wls");
            if (wlsCredential.equals(null)) {
                throw new Exception("[error] Weblogic Credentials are not available in Wallet");
            }
            final String wlsUser = wlsCredential.getUsername();
            final String wlsPassword = new String(wlsCredential.getPassword());

            System.out.println("Host        : " + host);
            System.out.println("Username    : " + hostUser);
            System.out.println("Password    : " + hostPassword);
            System.out.println("WLS Username: " + wlsUser);
            System.out.println("WLS Password: " + wlsPassword);

            final String clusterCommand = (String.format("%1$s -i" +
                            " %2$s" +
                            " %3$s" +
                            " %4$s" +
                            " %5$s" +
                            " %6$s" +
                            " %7$s",
                    new File(AbstractScriptRunner.wlstHome + File.separator + AbstractScriptRunner.wlst).getCanonicalPath(),
                    new File(AbstractScriptRunner.testEnvScriptsHome + File.separator +
                            operation.getOperation() + "Cluster.py").getCanonicalPath(),
                    clusterName,
                    wlsUser,
                    wlsPassword,
                    host,
                    adminPort));

            System.out.println("Executing command : " + clusterCommand + " on host : " + host);

            JSch jsch = new JSch();
            session = jsch.getSession(hostUser, host, 22);
            session.setPassword(hostPassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            session.connect(15 * 1000);

            channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setCommand(clusterCommand);
            channelExec.connect();

            in = channelExec.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[zdt-qa.info] " + line);
                if (line.contains(operation.getOperation() + " Cluster Failed")) {
                    success = false;
                    break;
                }
                if ((line.contains("All servers in the cluster " + clusterName + " are started successfully") ||
                        line.contains("servers are already running")) && operation.equals(START_STOP.START)) {
                    success = true;
                    Thread.sleep(10 * 1000);
                    break;
                }
                if (line.contains("Shutting down the cluster with name " + clusterName) && operation.equals(START_STOP.STOP)) {
                    success = true;
                    Thread.sleep(10 * 1000);
                    break;
                }
            }
            if (success)
                System.out.println("[zdt-qa.info] " + operation.getOperation() + " " + clusterName + " Successful");
            else
                throw new Exception("[error] " + operation.getOperation() + " " + clusterName + " Failed");
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                in.close();
                channelExec.disconnect();
                session.disconnect();
            } catch (Exception e) {
                throw new Exception("[error] Stream/Session Close Failed");
            }
        }
    }

}
