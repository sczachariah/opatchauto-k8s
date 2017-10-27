package oracle.fmwplatformqa.opatchautoqa.zdt.helper;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import oracle.fmwplatformqa.opatchautoqa.zdt.credential.Credential;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Vector;


/**
 * Created by gakhuran on 8/4/2016.
 */
public class CopyResource {

        public void copysrcFileToRemote(String srcFile, Credential credential, String host, String destLocation) throws Exception{
        Session session;
        ChannelExec channelExec = null;
        ChannelSftp channelSftp = null;
        InputStream in1 = null;
        InputStream in2 = null;
        boolean success = false;
        boolean fileExist = false;

        try {            
            if (credential.equals(null)) {
                throw new Exception("[error] SSH Credential for host " + host + " is not available in Wallet");
            }
            final String hostUser = credential.getUsername();
            final String hostPassword = new String(credential.getPassword());
            System.out.println("Host        : " + host);
//            System.out.println("Username    : " + hostUser);
//            System.out.println("Password    : " + hostPassword);

            //Copy customActionJar srcFile to Host
            JSch jsch = new JSch();
            session = jsch.getSession(hostUser, host, 22);
            session.setPassword(hostPassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            session.connect(15 * 1000);

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            channelSftp.cd(destLocation);
            File f = new File(srcFile);
            Vector<ChannelSftp.LsEntry> list = channelSftp.ls("*.jar");
            for(ChannelSftp.LsEntry entry : list) {

                 if (entry.getFilename().equals(f.getName())) {
                    fileExist=true;

                }
            }if(fileExist){
                System.out.println(f.getName()+" file already exist on host : "+host+" - "+destLocation);
            }
            else {
                System.out.println("[CopyResource.info] Copying " + srcFile + " to host : " + host);
                channelSftp.put(new FileInputStream(f), f.getName());
//            channelSftp.put(new FileInputStream(new File(srcFile)),destLocation);
            }

            Thread.sleep(5 * 1000);
            channelSftp.chmod(Integer.parseInt("755", 8), f.getName());
            channelSftp.disconnect();
            session.disconnect();
        }catch (Exception e) {
            e.printStackTrace();
        }
        }
}
