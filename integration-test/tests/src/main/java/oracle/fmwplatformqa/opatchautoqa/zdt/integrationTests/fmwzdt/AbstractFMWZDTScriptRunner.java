package oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests.fmwzdt;


import oracle.fmwplatform.envspec.model.EnvironmentModel;
import oracle.fmwplatformqa.opatchautoqa.util.StartStopExecutionOrderProcessing;
import oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests.AbstractScriptRunner;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.PumpStreamHandler;
import org.testng.annotations.AfterClass;

import java.io.*;
import java.lang.Exception;
import java.util.LinkedHashMap;

public abstract class AbstractFMWZDTScriptRunner extends AbstractScriptRunner {
    protected static LinkedHashMap<String, String> testPatches;
    protected static String testPatchLocation;
    protected static PATCH_OPERATION patchOperation;
    protected static EnvironmentModel envModel = null;

    protected static String sessionLog = null;
    protected static StartStopExecutionOrderProcessing startStopExecutionOrderProcessing = null;

    public AbstractFMWZDTScriptRunner(){
        startStopExecutionOrderProcessing = new StartStopExecutionOrderProcessing();
    }

    static{
     suiteGrp="FMWZDT";
    }

//    public abstract String getTestPatchHome();


    @AfterClass
    public abstract void cleanUpClass();


    protected enum FMWZDT_OPERATION {
        APPLY("APPLY"),
        APPLYOOP("APPLYOOP"),
        ROLLBACK("ROLLBACK"),
        RESUME("RESUME"),
        INVALID_OPERATION("INVALID_OPERATION");

        private String operation;

        FMWZDT_OPERATION(String op) {
            operation = op;
        }

        public String getFMWZDTOperationType(){
            return this.operation;
        }

        public String getFMWZDTOperation(){
            String operation = "INVALID" ;
            switch(this.operation){
                case "APPLY": operation = "apply";
                    break;

                case "APPLYOOP": operation = "apply";
                    break;

                case "ROLLBACK": operation = "rollback";
                    break;

                case "RESUME": operation = "resume";
                    break;
            }
            return operation;
        }

    }

    protected enum FMWZDT_PLAN {
        INVALID_PLAN("INVALID_PLAN"),
        ROLLING_PLAN("ROLLING_PLAN"),
        PARALLEL_PLAN("PARALLEL_PLAN");

        private String patchplan;

        FMWZDT_PLAN(String pp) {
            patchplan = pp;
        }

        public String getFMWZDTPlanType() {
            return this.patchplan;
        }

        public String getFMWZDTPlan() {
            String patchplan = "INVALID" ;
            switch(this.patchplan){
                case "ROLLING_PLAN": patchplan = "-plan " + "rolling";
                    break;

                case "PARALLEL_PLAN": patchplan = "-plan " + "parallel";
                    break;
            }
            return patchplan;
        }
    }


    public String getTestPatchHome(String patch){
        return (AbstractScriptRunner.patchExternal.equalsIgnoreCase("TRUE") ? "" : "fmwzdtPatches" +File.separator+patch);
    }

    public void getTestPatches(String patch) throws Exception {
        testPatches = processPatches(AbstractScriptRunner.patchHome + File.separator + getTestPatchHome(patch), PATCH_OPERATION.APPLY);
        if (testPatches.size() > 1) {
            patchOperation = PATCH_OPERATION.APPLY_CUMULATIVE;
            testPatchLocation = AbstractScriptRunner.patchStagingDir.getCanonicalPath();
        } else {
            patchOperation = PATCH_OPERATION.APPLY;
            testPatchLocation = testPatches.get(testPatches.keySet().toArray()[0]).toString();
        }
    }

    protected CommandLine fmwZDT(FMWZDT_OPERATION fmwzdtop, FMWZDT_PLAN fmwplan, String testPatchLocation, String sessionID, String domain,
                                 String adminHost, String adminPort, String walletLocation, String walletPassword, String extraParams)
            throws Exception{
        String command = "";
        sessionLog = new File(testDir, testName + "_" + fmwzdtop.getFMWZDTOperation() + ".session.log").getCanonicalPath();

        if(walletLocation == "")
            walletLocation = AbstractScriptRunner.walletLocation;
        if(walletPassword == "")
            walletPassword = AbstractScriptRunner.walletPassword;

        if (fmwzdtop.equals(FMWZDT_OPERATION.APPLY)) {

            command = (String.format("%1$s apply %2$s" +
                            " %3$s" +
                            " %4$s" +
                            " -instance %5$s" +
                            " -wallet %6$s"+
                            (walletType.equals("ENCRYPTED") ? " -walletPassword %7$s" : "%7$s")  +
                            " -wls-admin-host %8$s" +
                            " -log %9$s" +
                            " %10$s ",
                    new File(opatchautoHome + File.separator + scriptName).getCanonicalPath(),
                    patchOperation.getOperation(),
                    testPatchLocation,
                    fmwplan.getFMWZDTPlan(),
                    domain,
                    walletLocation,
                    walletPassword,
                    adminHost.trim() + ":" + adminPort.trim(),
                    new File(sessionLog).getCanonicalPath(),
                    extraParams));
        }else  if (fmwzdtop.equals(FMWZDT_OPERATION.APPLYOOP)) {

            command = (String.format("%1$s apply %2$s" +
                            " %3$s" +
                            " %4$s" +
                            " -oop" +
                            " -instance %5$s" +
                            " -wallet %6$s"+
                            (walletType.equals("ENCRYPTED") ? " -walletPassword %7$s" : "%7$s")  +
                            " -wls-admin-host %8$s" +
                            " -log %9$s" +
                            " %10$s ",
                    new File(opatchautoHome + File.separator + scriptName).getCanonicalPath(),
                    patchOperation.getOperation(),
                    testPatchLocation,
                    fmwplan.getFMWZDTPlan(),
                    domain,
                    walletLocation,
                    walletPassword,
                    adminHost.trim() + ":" + adminPort.trim(),
                    new File(sessionLog).getCanonicalPath(),
                    extraParams));
        }else if(fmwzdtop.equals(FMWZDT_OPERATION.ROLLBACK)) {

            command = (String.format("%1$s rollback %2$s" +
                            " %3$s" +
                            " %4$s" +
                            " -wallet %5$s" +
                            (walletType.equals("ENCRYPTED") ? " -walletPassword %6$s" : "%6$s")  +
                            " -wls-admin-host %7$s" +
                            " -log %8$s" +
                            " %9$s ",
                    new File(opatchautoHome + File.separator + scriptName).getCanonicalPath(),
                    patchOperation.getOperation(),
                    testPatchLocation,
                    fmwplan.getFMWZDTPlan(),
                    walletLocation,
                    walletPassword,
                    adminHost.trim() + ":" + adminPort.trim(),
                    new File(sessionLog).getCanonicalPath(),
                    extraParams));
        }else if(fmwzdtop.equals(FMWZDT_OPERATION.RESUME)) {

            command = (String.format("%1$s resume" +
                            " -session %2$s" +
                            " -wallet %3$s" +
                            (walletType.equals("ENCRYPTED") ? " -walletPassword %4$s" : "%4$s")  +
                            " -log %5$s" +
                            " %6$s ",
                    new File(opatchautoHome + File.separator + scriptName).getCanonicalPath(),
                    sessionID,
                    walletLocation,
                    walletPassword,
                    new File(sessionLog).getCanonicalPath(),
                    extraParams));


//        } else if (zdtOperation.equals(ZDT_OPERATION.ROLLOUT)) {
//            command = (String.format("%1$s apply " +
//                            " %2$s" +
//                            " -image-location %3$s" +
//                            " -wls-admin-host %4$s" +
//                            " -wls-target %5$s" +
//                            " -remote-image-location %6$s" +
//                            " -backup-home %7$s" +
//                            " -wallet %8$s" +
//                            (walletType.equals("ENCRYPTED") ? " -walletPassword %9$s" : "%9$s") +
//                            " -log %10$s" +
//                            " %11$s",
//                    new File(opatchautoHome + File.separator + scriptName).getCanonicalPath(),
//                    zdtOperation.getZDTOperation(),
//                    imageLocation,
//                    adminHost.trim() + ":" + adminPort.trim(),
//                    zdtTarget,
//                    new File(remoteImageLocation).getCanonicalPath(),
//                    new File(backupLocation).getCanonicalPath(),
//                    walletLocation,
//                    walletPassword,
//                    new File(testDir, testName + "_" + zdtOperation.getZDTOperationType() + ".session.log").getCanonicalPath(),
//                    extraParams));
        } else {
            throw new Exception("[error] Invalid FMWZDT Operation Specified.");
        }

        setupExecutorLog();
        executor.setWorkingDirectory(new File(opatchautoHome));
        executor.setStreamHandler(new PumpStreamHandler(outputCollector, errorCollector));

        CommandLine cmdString = CommandLine.parse(command);
        System.out.println("Executing Command : " + command);
        return cmdString;
    }

    protected CommandLine fmwZDT(FMWZDT_OPERATION fmwzdtop, FMWZDT_PLAN fmwplan, String testPatchLocation, String sessionID, String topology,
                                 String walletLocation, String walletPassword, String extraParams)
            throws Exception{
        String command = "";
        sessionLog = new File(testDir, testName + "_" + fmwzdtop.getFMWZDTOperation() + ".session.log").getCanonicalPath();

        if(walletLocation == "")
            walletLocation = AbstractScriptRunner.walletLocation;
        if(walletPassword == "")
            walletPassword = AbstractScriptRunner.walletPassword;

        if (fmwzdtop.equals(FMWZDT_OPERATION.APPLY)) {

            command = (String.format("%1$s apply %2$s" +
                            " %3$s" +
                            " %4$s" +
                            " -topology %5$s" +
                            " -wallet %6$s" +
                            (walletType.equals("ENCRYPTED") ? " -walletPassword %7$s" : "%7$s")  +
                            " -log %8$s" +
                            " %9$s ",
                    new File(opatchautoHome + File.separator + scriptName).getCanonicalPath(),
                    patchOperation.getOperation(),
                    testPatchLocation,
                    fmwplan.getFMWZDTPlan(),
                    topology,
                    walletLocation,
                    walletPassword,
                    new File(sessionLog).getCanonicalPath(),
                    extraParams));
        }else  if (fmwzdtop.equals(FMWZDT_OPERATION.APPLYOOP)) {

            command = (String.format("%1$s apply %2$s" +
                            " %3$s" +
                            " %4$s" +
                            " -oop" +
                            " -topology %5$s" +
                            " -wallet %6$s" +
                            (walletType.equals("ENCRYPTED") ? " -walletPassword %7$s" : "%7$s")  +
                            " -log %8$s" +
                            " %9$s ",
                    new File(opatchautoHome + File.separator + scriptName).getCanonicalPath(),
                    patchOperation.getOperation(),
                    testPatchLocation,
                    fmwplan.getFMWZDTPlan(),
                    topology,
                    walletLocation,
                    walletPassword,
                    new File(sessionLog).getCanonicalPath(),
                    extraParams));
        }else if(fmwzdtop.equals(FMWZDT_OPERATION.ROLLBACK)) {

            command = (String.format("%1$s rollback %2$s" +
                            " %3$s" +
                            " %4$s" +
                            " -topology %5$s" +
                            " -wallet %6$s" +
                            (walletType.equals("ENCRYPTED") ? " -walletPassword %7$s" : "%7$s")  +
                            " -log %8$s" +
                            " %9$s ",
                    new File(opatchautoHome + File.separator + scriptName).getCanonicalPath(),
                    patchOperation.getOperation(),
                    testPatchLocation,
                    fmwplan.getFMWZDTPlan(),
                    topologyLoc,
                    walletLocation,
                    walletPassword,
                    new File(sessionLog).getCanonicalPath(),
                    extraParams));
        }else if(fmwzdtop.equals(FMWZDT_OPERATION.RESUME)) {

            command = (String.format("%1$s resume" +
                            " -session %2$s" +
                            " -log %3$s" +
                            " -wallet %4$s" +
                            (walletType.equals("ENCRYPTED") ? " -walletPassword %5$s" : "%5$s")  +
                            " %6$s ",
                    new File(opatchautoHome + File.separator + scriptName).getCanonicalPath(),
                    sessionID,
                    walletLocation,
                    walletPassword,
                    new File(sessionLog).getCanonicalPath(),
                    extraParams));
        } else {
            throw new Exception("[error] Invalid FMWZDT Operation Specified.");
        }

        setupExecutorLog();
        executor.setWorkingDirectory(new File(opatchautoHome));
        executor.setStreamHandler(new PumpStreamHandler(outputCollector, errorCollector));

        CommandLine cmdString = CommandLine.parse(command);
        System.out.println("Executing Command : " + command);
        return cmdString;
    }


    protected CommandLine fmwZDT(FMWZDT_OPERATION fmwzdtop, FMWZDT_PLAN fmwplan,String sessionID, String testPatchLocation,
                                 String orahome, String domain, String host1, String host2,
                                 String adminHost, String adminPort, String walletLocation, String walletPassword,
                                 String extraParams)throws Exception {
        String command = "";
        sessionLog = new File(testDir, testName + "_" + fmwzdtop.getFMWZDTOperation() + ".session.log").getCanonicalPath();

        if(walletLocation == "")
            walletLocation = AbstractScriptRunner.walletLocation;
        if(walletPassword == "")
            walletPassword = AbstractScriptRunner.walletPassword;

        if (fmwzdtop.equals(FMWZDT_OPERATION.APPLY)) {

            command = (String.format("%1$s apply %2$s" +
                            " %3$s" +
                            " %4$s" +
                            " -oh %5$s" +
                            " -instance %6$s" +
                            " -host %7$s" +
                            " -wallet %8$s" +
                            (walletType.equals("ENCRYPTED") ? " -walletPassword %9$s" : "%9$s")  +
                            " -wls-admin-host %10$s" +
                            " -log %11$s" +
                            " %12$s ",
                    new File(opatchautoHome + File.separator + scriptName).getCanonicalPath(),
                    patchOperation.getOperation(),
                    testPatchLocation,
                    fmwplan.getFMWZDTPlan(),
                    orahome,
                    domain,
                    adminHost.trim()+","+host1.trim()+"," +host2.trim(),
                    walletLocation,
                    walletPassword,
                    adminHost.trim() + ":" + adminPort.trim(),
                    new File(sessionLog).getCanonicalPath(),
                    extraParams));
        }else if (fmwzdtop.equals(FMWZDT_OPERATION.APPLYOOP)) {

            command = (String.format("%1$s apply %2$s" +
                            " %3$s" +
                            " %4$s" +
                            " -oh %5$s" +
                            " -instance %6$s" +
                            " -oop" +
                            " -host %7$s" +
                            " -wallet %8$s" +
                            (walletType.equals("ENCRYPTED") ? " -walletPassword %9$s" : "%9$s") +
                            " -wls-admin-host %10$s" +
                            " -log %11$s" +
                            " %12$s ",
                    new File(opatchautoHome + File.separator + scriptName).getCanonicalPath(),
                    patchOperation.getOperation(),
                    testPatchLocation,
                    fmwplan.getFMWZDTPlan(),
                    orahome,
                    domain,
                    adminHost.trim() + "," + host1.trim() + "," + host2.trim(),
                    walletLocation,
                    walletPassword,
                    adminHost.trim() + ":" + adminPort.trim(),
                    new File(sessionLog).getCanonicalPath(),
                    extraParams));
        }else if(fmwzdtop.equals(FMWZDT_OPERATION.ROLLBACK)) {
            command = (String.format("%1$s rollback %2$s" +
                            " %3$s" +
                            " %4$s" +
                            " -oh %5$s" +
                            " -instance %6$s" +
                            "-host %7$s" +
                            " -wallet %8$s"  +
                            (walletType.equals("ENCRYPTED") ? " -walletPassword %9$s" : "%9$s")  +
                            " -wls-admin-host %10$s" +
                            " -log %11$s" +
                            " %12$s ",
                    new File(opatchautoHome + File.separator + scriptName).getCanonicalPath(),
                    patchOperation.getOperation(),
                    testPatchLocation,
                    fmwplan.getFMWZDTPlan(),
                    orahome,
                    domain,
                    adminHost.trim()+","+host1.trim()+"," +host2.trim(),
                    walletLocation,
                    walletPassword,
                    adminHost.trim() + ":" + adminPort.trim(),
                    new File(sessionLog).getCanonicalPath(),
                    extraParams));
        }else if(fmwzdtop.equals(FMWZDT_OPERATION.RESUME)) {

            command = (String.format("%1$s resume" +
                            " -session %2$s" +
                            " -wallet %3$s"  +
                            (walletType.equals("ENCRYPTED") ? " -walletPassword %4$s" : "%4$s")  +
                            " -log %5$s" +
                            " %6$s ",
                    new File(opatchautoHome + File.separator + scriptName).getCanonicalPath(),
                    sessionID,
                    walletLocation,
                    walletPassword,
                    new File(sessionLog).getCanonicalPath(),
                    extraParams));
        } else {
            throw new Exception("[error] Invalid FMWZDT Operation Specified.");
        }

        setupExecutorLog();
        executor.setWorkingDirectory(new File(opatchautoHome));
        executor.setStreamHandler(new PumpStreamHandler(outputCollector, errorCollector));

        CommandLine cmdString = CommandLine.parse(command);
        System.out.println("Executing Command : " + command);
        return cmdString;
    }

    protected CommandLine fmwZDTHostOrder(FMWZDT_OPERATION fmwzdtop, FMWZDT_PLAN fmwplan,String sessionID, String testPatchLocation,
                                 String orahome, String domain, String host1, String host2,
                                 String adminHost, String adminPort, String walletLocation, String walletPassword,
                                 String extraParams)throws Exception {
        String command = "";
        sessionLog = new File(testDir, testName + "_" + fmwzdtop.getFMWZDTOperation() + ".session.log").getCanonicalPath();

        if(walletLocation == "")
            walletLocation = AbstractScriptRunner.walletLocation;
        if(walletPassword == "")
            walletPassword = AbstractScriptRunner.walletPassword;

        if (fmwzdtop.equals(FMWZDT_OPERATION.APPLY)) {

            command = (String.format("%1$s apply %2$s" +
                            " %3$s" +
                            " %4$s" +
                            " -oh %5$s" +
                            " -instance %6$s" +
                            " -host %7$s" +
                            " -wallet %8$s" +
                            (walletType.equals("ENCRYPTED") ? " -walletPassword %9$s" : "%9$s")  +
                            " -wls-admin-host %10$s" +
                            " -log %11$s" +
                            " %12$s ",
                    new File(opatchautoHome + File.separator + scriptName).getCanonicalPath(),
                    patchOperation.getOperation(),
                    testPatchLocation,
                    fmwplan.getFMWZDTPlan(),
                    orahome,
                    domain,
                    host1.trim()+","+adminHost.trim()+"," +host2.trim(),
                    walletLocation,
                    walletPassword,
                    adminHost.trim() + ":" + adminPort.trim(),
                    new File(sessionLog).getCanonicalPath(),
                    extraParams));
        } else {
            throw new Exception("[error] Invalid FMWZDT Operation Specified.");
        }

        setupExecutorLog();
        executor.setWorkingDirectory(new File(opatchautoHome));
        executor.setStreamHandler(new PumpStreamHandler(outputCollector, errorCollector));

        CommandLine cmdString = CommandLine.parse(command);
        System.out.println("Executing Command : " + command);
        return cmdString;
    }

    public static void delayExecution(int t) throws InterruptedException{
        // introduce a delay of t second
        Thread.sleep(t*1000);
    }
}
