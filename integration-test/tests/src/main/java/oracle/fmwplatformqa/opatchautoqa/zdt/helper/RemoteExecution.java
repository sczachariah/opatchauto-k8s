package oracle.fmwplatformqa.opatchautoqa.zdt.helper;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import oracle.fmwplatformqa.opatchautoqa.zdt.credential.Credential;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;

public class RemoteExecution {
    public static void executeCommand(Credential credential, String host, String cmd) throws Exception{
        if (credential.equals(null)) {
            throw new Exception("[error] SSH Credential for host " + host + " is not available in Wallet");
        }
        final String hostUser = credential.getUsername();
        final String hostPassword = new String(credential.getPassword());
        System.out.println("Host        : " + host);
        JSch jsch=new JSch();
        Session session=jsch.getSession(hostUser, host, 22);
        session.setPassword(hostPassword);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("PreferredAuthentications", "publickey,keyboard-interactive,password");
        session.setConfig(config);
        session.connect();

        ChannelExec channel=(ChannelExec) session.openChannel("exec");
        BufferedReader in=new BufferedReader(new InputStreamReader(channel.getInputStream()));
        channel.setCommand(cmd+";");
        channel.connect();

        String msg=null;
        while((msg=in.readLine())!=null){
            System.out.println(msg);
        }

        channel.disconnect();
        session.disconnect();
    }
}
