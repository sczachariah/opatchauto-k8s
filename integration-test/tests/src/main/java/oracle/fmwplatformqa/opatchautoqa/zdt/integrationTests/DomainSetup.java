package oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests;

import oracle.fmwplatformqa.opatchautoqa.util.PortFinder;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests.AbstractScriptRunner.*;

/**
 * Created by pcbajpai on 7/17/2017.
 */
public class DomainSetup {

    // generate ssl keystore
    // get a topology
    // update the topology with a ssl domain object
    // create a domain
    // pack the domain
    // unpack the domain to target host
    DefaultExecutor executor = null;
    CollectingLogOutputStream outputCollector = null;
    CollectingLogOutputStream errorCollector = null;

    public void setupExceutorLog() {
        executor = new DefaultExecutor();
        outputCollector = new CollectingLogOutputStream();
        errorCollector = new CollectingLogOutputStream();
    }

    protected void writeToFile(String filename, String content) {
        File name = new File(testOutput, filename + ".log");
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

    public void createDomain(String oracleHome, String domainID, String script, String userName, String password, boolean enableSSL) throws IOException {
        setupExceutorLog();
        String wlstScriptLoc = oracleHome + File.separator + "oracle_common" + File.separator + "common" + File.separator + "bin" + File.separator + "wlst.sh";
        executor.setWorkingDirectory(new File(wlstScriptLoc).getParentFile());
        executor.setStreamHandler(new PumpStreamHandler(outputCollector, errorCollector));
        adminSSLPort=Integer.toString(PortFinder.setNextAvailableUniquePort());
        server1SSLPort=Integer.toString(PortFinder.setNextAvailableUniquePort());
        server2SSLPort = Integer.toString(PortFinder.setNextAvailableUniquePort());
        sslDomainNMPort =Integer.toString(PortFinder.setNextAvailableUniquePort());
        String command = (String.format("%1$s -i %2$s %3$s %4$s %5$s %6$s %7$s %8$s %9$s %10$s %11$s %12$s %13$s %14$s %15$s",
                new File(wlstScriptLoc).getAbsolutePath(),
                new File(script).getCanonicalPath(),
                new File(domainsDir + File.separator + domainID).getAbsolutePath(),
                domainID,
                userName,
                password,
                adminHost,
                adminSSLPort,
                server1Host,
                PortFinder.setNextAvailableUniquePort(),
                server1SSLPort,
                server2Host,
                PortFinder.setNextAvailableUniquePort(),
                server2SSLPort,
                sslDomainNMPort));

        CommandLine cmdString = CommandLine.parse(command);
        System.out.println("Executing command : " + command);

        try {
            executor.execute(cmdString);
        } catch (Exception e) {
            System.out.println("DOMAIN_CREATION_FAILED : ");
            e.printStackTrace();
            throw e;
        } finally {
            writeToFile("DOMAIN_SETUP_" + domainID, outputCollector.toString());
        }

    }

    public void packDomain(String oracleHome, String domainID, String templateLoc, String templateID, boolean managed) throws IOException{
        setupExceutorLog();
        File templateFile = new File(templateLoc+File.separator+templateID+".jar");
        if(templateFile.exists()){
            FileUtils.forceDelete(templateFile);
        }
        String packScriptLoc = oracleHome + File.separator + "oracle_common" + File.separator + "common" + File.separator + "bin" + File.separator + "pack.sh";
        executor.setWorkingDirectory(new File(packScriptLoc).getParentFile());

        String command = (String.format("%1$s -domain %2$s -template %3$s -template_name %4$s -managed %5$s",
                new File(packScriptLoc).getCanonicalPath(),
                new File(domainsDir + File.separator + domainID).getAbsolutePath(),
                new File(templateLoc+File.separator+templateID+".jar"),
                templateID,
                managed));


        CommandLine cmdString = CommandLine.parse(command);
        System.out.println("Executing command : " + command);

        try {
            executor.execute(cmdString);
            System.out.println("Template Jar Created Successfully : "+ templateFile.getCanonicalPath());
        } catch (Exception e) {
            System.out.println("PACK_DOMAIN_FAILED : ");
            e.printStackTrace();
            throw e;
        } finally {
            writeToFile("PACK_DOMAIN_" + domainID, outputCollector.toString());
        }
    }

    public String unpackDomainComand(String oracleHome, String domainID, String templateJar) throws IOException{
        setupExceutorLog();
        String unpackScriptLoc = oracleHome + File.separator + "oracle_common" + File.separator + "common" + File.separator + "bin" + File.separator + "unpack.sh";
        executor.setWorkingDirectory(new File(unpackScriptLoc).getParentFile());



        String command = (String.format("%1$s -domain %2$s -template %3$s",
                new File(unpackScriptLoc).getCanonicalPath(),
                new File(domainsDir + File.separator + domainID).getAbsolutePath(),
                new File(templateJar)));
return command;
//        CommandLine cmdString = CommandLine.parse(command);
//        System.out.println("Executing command : " + command);

//        try {
//            executor.execute(cmdString);
//        } catch (Exception e) {
//            System.out.println("UNPACK_DOMAIN_FAILED : ");
//            e.printStackTrace();
//            throw e;
//        } finally {
//            writeToFile("UNPACK_DOMAIN_" + domainID, outputCollector.toString());
//        }
    }

}
