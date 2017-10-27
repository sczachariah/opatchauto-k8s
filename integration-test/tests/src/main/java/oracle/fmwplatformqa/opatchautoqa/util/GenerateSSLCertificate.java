package oracle.fmwplatformqa.opatchautoqa.util;

import oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests.AbstractScriptRunner;
import oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests.CollectingLogOutputStream;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.File;
import java.io.IOException;


public class GenerateSSLCertificate {

    protected static DefaultExecutor executor;
    protected static CollectingLogOutputStream outputCollector;
    protected static CollectingLogOutputStream errorCollector;


    public static void createSSLCertificates() {
        String certLocation = AbstractScriptRunner.testOutput + File.separator + "certs";
        File certDir = new File(certLocation);
        if (!certDir.exists()) {
            certDir.mkdir();
        }
        File sslCerificate = new File(certLocation + File.separator + "selfsign.cer");
        try {
            if (!sslCerificate.exists()) {
                String scriptLocation = AbstractScriptRunner.testEnvScriptsHome + File.separator + "create-ssl-cert.sh";
                generateCertificate(scriptLocation.toString(), certLocation);
            }
        } catch (Throwable t) {
            System.out.println("[error] Error Creating SSL Certificates");
            t.printStackTrace();
        }
    }


    private static void generateCertificate(String scriptLocation, String certificateDirectory) throws IOException {
        executor = new DefaultExecutor();
        outputCollector = new CollectingLogOutputStream();
        errorCollector = new CollectingLogOutputStream();

        executor.setWorkingDirectory(new File(certificateDirectory).getCanonicalFile());
        executor.setStreamHandler(new PumpStreamHandler(outputCollector, errorCollector));

        String command = (String.format("sh %1$s %2$s",
                new File(scriptLocation).getCanonicalPath(),
                AbstractScriptRunner.javaHome));

        CommandLine cmdString = CommandLine.parse(command);
        System.out.println("Executing command : " + command);

        try {
            executor.execute(cmdString);
        } catch (Exception ex) {
            System.out.println("[error] Failure in executing script " + new File(scriptLocation).getCanonicalPath() + ".\n");
            throw ex;
        } finally {
            System.out.println("[info] " + outputCollector.toString());
        }

    }
}
