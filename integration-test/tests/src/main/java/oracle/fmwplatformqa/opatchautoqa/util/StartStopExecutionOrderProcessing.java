package oracle.fmwplatformqa.opatchautoqa.util;

import oracle.fmwplatform.credentials.exception.FMWCredentialsException;
import oracle.fmwplatform.envspec.exception.FMWEnvSpecException;
import oracle.fmwplatform.envspec.helper.EnvironmentModelHelper;
import oracle.fmwplatform.envspec.model.EnvironmentModel;
import oracle.fmwplatform.envspec.model.EnvironmentModelBuilder;
import oracle.fmwplatform.envspec.model.topology.Host;
import oracle.fmwplatform.envspec.model.topology.MappedDomain;
import oracle.fmwplatform.envspec.model.topology.MappedOracleHome;
import oracle.fmwplatform.envspec.model.topology.MappedServer;
import oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests.AbstractScriptRunner;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Print all the strings that match a given pattern from a file.
 */
public class StartStopExecutionOrderProcessing {

    public static int sequenceNo = 0;
    public static String outputFileLocation = "";
    public static boolean isOnlineMode = false;
    public static EnvironmentModelBuilder environmentModelBuilder = new EnvironmentModelBuilder();

    /**
     * ENUM for StatusCode values
     */
    public enum StatusCode {
        SUCCESS, SUCCESS_NO_ACTION, FAILURE, ABORT;
    }

    /**
     * ENUM for ActionCode values
     */
    public enum ActionCode {
        START("Start"),
        STOP("Stop");

        private String actionCode;

        ActionCode() {
        }

        ActionCode(String actionCode) {
            this.actionCode = actionCode;
        }

        public String getActionCode() {
            return actionCode;
        }
    }

    /**
     * ENUM for TargetValues defined
     */
    public enum TargetValues {
        SERVER("Server"),
        NODEMANAGER("NodeManager"),
        ADMINSERVER("AdminServer");

        private String targetValue;

        TargetValues() {
        }

        TargetValues(String targetValue) {
            this.targetValue = targetValue;
        }

        public String getTargetValue() {
            return targetValue;
        }
    }

    /**
     * Method 1 -> read the provided session log and writing to an outputFile for logging
     *
     * @param inputSessionLogLocation
     */
    private void readSessionLogAndWriteToNewOutputLogFile(String inputSessionLogLocation) {
        BufferedReader readInputFile = null; /** to read input file */
        BufferedWriter writeOutputFile = null; /** to write to new file */
        String patternMatchingLine; /** line that matched pattern and that will be written to new output file */

        /** Pattern to Search in opatchauto session log for checking the execution order of start stop of servers during opatchauto execution */
        Pattern patternToSearch = Pattern.compile(".*\\bINFO\\b.*(SUCCESS|SUCCESS_NO_ACTION|FAILURE|ABORT):\\sJAVA_WLST_ACTION:\\sInvoke:\\s(?!.*(nodemanager|NODEMANAGER|NodeManager|Nodemanager).*)");
        try {
            /** reads the input provided opatchauto session log */
            readInputFile = new BufferedReader(new FileReader(inputSessionLogLocation));

            /** writes the pattern available into the new log file outputFile */
            writeOutputFile = new BufferedWriter(new FileWriter(new File(outputFileLocation)));

            /** For each line of input, try matching pattern in it.*/
            while ((patternMatchingLine = readInputFile.readLine()) != null) {
                /** For each match in the line, extract, print it & write to new outputFile. */
                Matcher matcher = patternToSearch.matcher(patternMatchingLine);
                while (matcher.find()) {
                    writeOutputFile.write(patternMatchingLine + "\n");
                }
            }
            /** Close open filehandlers */
            writeOutputFile.flush();
            writeOutputFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to fetch the StatusCode from the matched pattern lines and add value retrieved to startStopExecutionOrder object
     *
     * @param newToken
     * @param startStopExecutionOrder
     */
    private void fetchStatusCodeForExecutionOrder(String newToken, StartStopExecutionOrder startStopExecutionOrder) {
        if (newToken.contains("SUCCESS"))
            startStopExecutionOrder.setStatusCode(String.valueOf(StatusCode.SUCCESS));
        else if (newToken.contains("SUCCESS_NO_ACTION"))
            startStopExecutionOrder.setStatusCode(String.valueOf(StatusCode.SUCCESS_NO_ACTION));
        else if (newToken.contains("FAILURE"))
            startStopExecutionOrder.setStatusCode(String.valueOf(StatusCode.FAILURE));
        else if (newToken.contains("ABORT"))
            startStopExecutionOrder.setStatusCode(String.valueOf(StatusCode.ABORT));
    }

    /**
     * Method to fetch the ActionCode from the matched pattern lines and add value to startStopExecutionOrder object
     *
     * @param newToken
     * @param startStopExecutionOrder
     */
    private void fetchActionCodeForExecutionOrder(String newToken, StartStopExecutionOrder startStopExecutionOrder) {
        if ((newToken.contains("Started")) || (newToken.contains("started")))
            startStopExecutionOrder.setActionCode(String.valueOf(ActionCode.START));
        else if ((newToken.contains("Stopped")) || (newToken.contains("stopped")))
            startStopExecutionOrder.setActionCode(String.valueOf(ActionCode.STOP));
    }

    /**
     * Method to fetch the TargetSource and TargetValues from the matched pattern lines and add value to startStopExecutionOrder object
     *
     * @param newToken
     * @param startStopExecutionOrder
     */
    private void fetchTargetSourceAndValueForExecutionOrder(String newToken, StartStopExecutionOrder startStopExecutionOrder) {
        int index = 0; /** to keep track index where neToken exists */
        String searchString = null; /** string to search */
        String newTokenString = newToken.replace("\"", ""); /** newTokenString after removing double quotes of newToken string */
        /** tokens has the values resulted from split which is done so as to get the target in form <target, name of target> *
         * Eg : Already Stopped server1 clusterdomain will be split to Stopped, server1, clusterdomain into tokens *
         * Eg : Already Stopped ManagedServer_1 base_domain will be split to Stopped, ManagedServer_1, base_domain *
         * Eg : Already Stopped NodeManager Machine_1 will be split to Stopped, NodeManager, Machine1 *
         * Eg : Started AdminServer "AdminServer" will be split to Started, AdminServer, AdminServer */
        String[] tokens = null; /** placeholder to store tokens after split to find consecutive words following searchString */

        if (newToken.contains("Already Started") || (newToken.contains("Already Stopped"))) {
            searchString = "Already";
            index = newTokenString.indexOf(searchString);
            index += searchString.length();
            tokens = newTokenString.substring(index, newTokenString.length()).trim().split(" ");
            if (tokens[1].contains("server") || tokens[1].contains("AdminServer") || tokens[1].contains("Server") || tokens[1].contains("ManagedServer")) {
                startStopExecutionOrder.setTargetSource(String.valueOf(TargetValues.SERVER).toUpperCase());
                startStopExecutionOrder.setTargetValues(tokens[1].toString()); /** will  contain name of the server */
            }
        } else if (newToken.contains("Admin Server") || newToken.contains("AdminServer")) {
            startStopExecutionOrder.setTargetSource(String.valueOf(TargetValues.SERVER).toUpperCase());
            startStopExecutionOrder.setTargetValues(String.valueOf(TargetValues.ADMINSERVER.getTargetValue()));
        } else if (newToken.contains("Server") || newToken.contains("server")) {
            searchString = "Server";
            index = newTokenString.indexOf(searchString);
            index += searchString.length();
            tokens = newTokenString.substring(index - searchString.length(), newTokenString.length()).trim().split(" ");
            startStopExecutionOrder.setTargetSource(String.valueOf(TargetValues.SERVER).toUpperCase());
            startStopExecutionOrder.setTargetValues(tokens[1].toString()); /** will  contain name of the server */
        }
    }

    /**
     * Method 2 -> to read the newly written outputFile.log and create an object of startStopExecutionOrder based on matching patterns
     * the created object of startStopExecutionOrder should be of format
     * StartStopExecutionOrder object :: {"SerialNo" , "StatucCode => SUCCESS|SUCCESS_NO_ACTION|FAILURE|ABORT", "ActionCode", "TargetSource", "TargetValues}
     *
     * @return
     */
    private List<StartStopExecutionOrder> readNewOutputLogFileAndCreateStartStopExecutionOrderObject() {
        int serialNo = 0; /** to store serialNo for ease of debugging */
        BufferedReader readOutputFile = null; /** to read newly written outputFile.log */
        List<StartStopExecutionOrder> startStopExecutionOrderList = new ArrayList<>(); /** to store list of startStopExecutionOrder objects created */

        try {
            readOutputFile = new BufferedReader(new FileReader(outputFileLocation));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Scanner scanner = new Scanner(readOutputFile);
        while (scanner.hasNextLine()) {
            StartStopExecutionOrder startStopExecutionOrder = new StartStopExecutionOrder();
            String input = scanner.nextLine();
            /** Line is split based on pattern "JAVA_WLST_ACTION: Invoke:" if this is found so that tokenizing gets easier */
            String[] inputs = input.split(":\\s+JAVA_WLST_ACTION:\\s+Invoke:\\s");

            /** to add line no or serial no for debugging */
            startStopExecutionOrder.setSerialNo(serialNo++);
            for (String tempString : inputs) {
                /** Method to fetch the StatusCode from the matched pattern lines and add to startStopExecutionOrder object */
                fetchStatusCodeForExecutionOrder(tempString, startStopExecutionOrder);
                /** Method to fetch the ActionCode from the matched pattern lines and add to startStopExecutionOrder object */
                fetchActionCodeForExecutionOrder(tempString, startStopExecutionOrder);
                /** Method to fetch the TargetSource and TargetValues from the matched pattern lines and add to startStopExecutionOrder object */
                fetchTargetSourceAndValueForExecutionOrder(tempString, startStopExecutionOrder);
            }
            /** Add the startStopExecutionOrder object fetched to startStopExecutionOrderList */
            startStopExecutionOrderList.add(startStopExecutionOrder);
        }
        return startStopExecutionOrderList;
    }

    /**
     * Compares two lists
     *
     * @param inputStartStopSequenceList
     * @param startStopExecutionOrderList
     * @return
     * @throws Exception
     */
    private boolean compareListsHelper(List<StartStopExecutionOrder> inputStartStopSequenceList, List<StartStopExecutionOrder> startStopExecutionOrderList) throws Exception {
        boolean diffFound = false;

        /** identity which server start to ignore */
        List<String> ignoreStart = new ArrayList<>();
        for (int i = 0; i < startStopExecutionOrderList.size(); i++) {
            if (startStopExecutionOrderList.get(i).getActionCode().equalsIgnoreCase("STOP") &&
                    startStopExecutionOrderList.get(i).getStatusCode().equalsIgnoreCase("SUCCESS_NO_ACTION") &&
                    startStopExecutionOrderList.get(i).getTargetSource().equalsIgnoreCase("Server")) {
                ignoreStart.add(startStopExecutionOrderList.get(i).getTargetValues());
            }
        }

        /** modify input list to remove those server whose start is to be ignored */
        List<StartStopExecutionOrder> expectedStartStopSequenceList = new ArrayList<>();
        for (StartStopExecutionOrder object : inputStartStopSequenceList) {
            if (ignoreStart.contains(object.getTargetValues()) &&
                    object.getActionCode().equalsIgnoreCase("START"))
                continue;
            expectedStartStopSequenceList.add(object);
        }

        System.out.println("Expected List : " + expectedStartStopSequenceList.toString());

        /** ideally list size should be same. Throw exception if otherwise */
        if (startStopExecutionOrderList.size() != expectedStartStopSequenceList.size()) {
            System.out.println("inputStartStopSequenceList contents     :  " + inputStartStopSequenceList.toString());
            System.out.println("expectedStartStopSequenceList contents  :  " + expectedStartStopSequenceList.toString());
            System.out.println("startStopExecutionOrderList contents    :  " + startStopExecutionOrderList.toString());
            throw new Exception("\n[INFO] : Sizes of provided lists is not same. Please check the above list sequences!!");
        }

        /** size of lists for comparision */
        int listSize = expectedStartStopSequenceList.size();

        /** loop through two lists for comparison and finding out the diff set from lists */
        for (int i = 0; i < listSize; i++) {
            if (!(startStopExecutionOrderList.get(i).compareTo(expectedStartStopSequenceList.get(i)) == 0)) {
                diffFound = true;
                System.out.println("[INFO] : There is a mismatch in the below listed sequences. Please check!!!");
                System.out.println("inputStartStopSequenceList.get(" + i + ")     :    " + inputStartStopSequenceList.get(i).toString());
                System.out.println("expectedStartStopSequenceList.get(" + i + ")  :    " + expectedStartStopSequenceList.get(i).toString());
                System.out.println("startStopExecutionOrderList.get(" + i + ")    :    " + startStopExecutionOrderList.get(i).toString());
                System.out.println("inputStartStopSequenceList contents     :  " + inputStartStopSequenceList.toString());
                System.out.println("expectedStartStopSequenceList contents  :  " + expectedStartStopSequenceList.toString());
                System.out.println("startStopExecutionOrderList contents    :  " + startStopExecutionOrderList.toString());
                break;
            }
        }
        if (!diffFound) {
            System.out.println("[INFO] : Both Lists contain same contents (There is no mismatch found in the provided lists for comparison)");
            System.out.println("inputStartStopSequenceList contents     :  " + inputStartStopSequenceList.toString());
            System.out.println("expectedStartStopSequenceList contents  :  " + expectedStartStopSequenceList.toString());
            System.out.println("startStopExecutionOrderList contents    :  " + startStopExecutionOrderList.toString());
        }
        return diffFound;
    }

    /**
     * Method to derive topology section from session log that gets created during runtime opatchauto execution only in case of offline discovery case,
     * in case its online discovery done for parallel cases, or for remote host then this parsing to fetch topology from session log should be avoided.
     *
     * @param domainId
     * @param sessionLog
     * @param testName
     * @return
     * @throws IOException
     * @throws FMWEnvSpecException
     * @throws FMWCredentialsException
     */
    private EnvironmentModel getEnvModelByParsingSessionLogToFetchDiscoveredTopology(String domainId, String sessionLog, String testName) throws IOException, FMWEnvSpecException, FMWCredentialsException {
        /** TOPOLOGY_START_TAG & TOPOLOGY_END_TAG are two parameters required to be extracted data from their limits */
        final String TOPOLOGY_START_TAG = "<?xml version='1.1' encoding='UTF-8'?>";
        final String TOPOLOGY_END_TAG = "</topology>";
        StringBuilder topologyString = new StringBuilder();
        /** Path to the topology file to be saved which will be fetched once session log is parsed , if path not found then we need to make dirs*/
        File topologyLocation = new File(AbstractScriptRunner.testOutput + File.separator + "runtime" + File.separator + "models" + File.separator + "topologies");
        if (!topologyLocation.exists()) {
            topologyLocation.mkdirs();
        }
        /** Output file name where parsed topology file will be saved */
        String outputTopologyFileName = AbstractScriptRunner.testOutput + File.separator + "runtime" + File.separator + "models" + File.separator + "topologies" + File.separator + testName + "-1.0.xml";
        File topologyFile = new File(outputTopologyFileName);

        String scan;
        EnvironmentModel environmentModel = new EnvironmentModel();
        try {
            BufferedReader br = new BufferedReader(new FileReader(sessionLog)); /** reader to read session log for parsing */
            Writer writer = null; /** fileHandler to write to topology file */
            while ((scan = br.readLine()) != null) {
                /** if the discovery done in session log offline mode then only construct/fetch the topology
                 * created on runtime from session log and write it to new topologyFile */
                if (scan.contains("Try to discover a WebLogic Domain in offline mode")) {
                    isOnlineMode = false;
                } else if (scan.contains("Try to discover a WebLogic Domain in online mode")) {
                    /** if the discovery done in session log is online mode then only isOnlineMode flag set value to true */
                    isOnlineMode = true;
                    break;
                }
                if (scan.contains(TOPOLOGY_START_TAG)) {
                    /** if start tag of topology is found then initialise the writer handler and append the line to topologyString as its first line */
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputTopologyFileName)));
                    topologyString.append(scan + "\n");
                } else if (scan.contains(TOPOLOGY_END_TAG)) {
                    /** if end tag of topology is found append the line to topologyString as its last line of topology and exit from parsing */
                    topologyString.append(scan + "\n");
                    break;
                } else {
                    /** if topology writer handler is not null that means if the toplogyString is having contents then update name tag with testname and save in desired topologyLocation*/
                    if (writer != null) {
                        String temp = scan.toString();
                        if ((scan.trim().equals("<name>" + domainId + "-topology</name>")) || (scan.trim().equals("<name>modelOutput</name>"))) {
                            String str = scan.toString().replaceAll("(?<=<name>).*?(?=</name>)", testName);
                            topologyString.append(str + "\n");
                        } else {
                            topologyString.append(temp + "\n");
                        }
                    }
                }
            }
            if (writer != null) {
                /** write the toplogy contents extracted from session log to new topologyFile */
                writer.write(topologyString.toString());
                /** Close open filehandlers */
                writer.flush();
                writer.close();
            }
            br.close();
            if (environmentModelBuilder != null) {
                environmentModelBuilder.appendModelSearchLocation(AbstractScriptRunner.testOutput + File.separator + "runtime" + File.separator + "models");
            }
            if (topologyFile.exists()) {
                /** build environmentModel from newly created topologyFile */
                environmentModel = environmentModelBuilder.buildFromTopologyFile(topologyFile, null, null);
            } else {
                throw new FileNotFoundException("Unable to locate topology file.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return environmentModel;
    }

    /**
     * Method to parse SessionLog and Compare Input list with output lists of start stop sequence derived from sessionLog
     *
     * @param inputSessionLogLocation
     * @param inputStartStopSequenceList
     * @return
     * @throws Exception
     */
    private boolean parseSessionLogAndCompareInputSequenceOfStartStopOperations(String inputSessionLogLocation, List<StartStopExecutionOrder> inputStartStopSequenceList) throws Exception {
        boolean diffFound = false;
        outputFileLocation = inputSessionLogLocation + ".startstop";
        /** Method to read the provided session log and writing [INFO] JAVA_WLST_ACTION statements/lines to an outputFile for logging */
        readSessionLogAndWriteToNewOutputLogFile(inputSessionLogLocation);
        /** Method to read the newly written outputFile.log and create an object of startStopExecutionOrder based on matching patterns *
         * the created object of startStopExecutionOrder should be of format
         * ExecutionOrder object :: {"SerialNo" , "StatucCode => SUCCESS|SUCCESS_NO_ACTION|FAILURE|ABORT", "ActionCode", "TargetSource", "TargetValues} */
        List<StartStopExecutionOrder> startStopExecutionOrderList = readNewOutputLogFileAndCreateStartStopExecutionOrderObject();

        /** Compare the retrieved startStopExecutionOrderList with input executionList of each test case to see the difference in sequencing of start stop operations *
         * Method to read the input executionList sequencing of start stop operations with startStopExecutionOrderList*/
        try {
            diffFound = compareListsHelper(inputStartStopSequenceList, startStopExecutionOrderList);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return diffFound;
    }

    /**
     * A method to iterate through the Host objects trying to find a match to a value in the supplied list.
     *
     * @param model         an EnvironmentModel object which contains the topology to be queried.
     * @param hostAddresses a list of host address stings to use to find the correct host.
     * @return a Host object which matches one of the addresses in the hostAddresses list.
     */
    private Host findHostByHostAddress(EnvironmentModel model, List<String> hostAddresses) {
        Host foundHost = null;
        List<Host> hosts = model.getHosts();
        for (Host host : hosts) {
            if (hostAddresses.contains(host.substitute(host.getAddress()))) {
                foundHost = host;
            }
        }
        return foundHost;
    }

    /**
     * For parsing and comparing of lists we will need a environmentModel either in offline mode or online mode based on opatchauto execution done
     * If online discovery is done during runtime by opatchauto then we need to derive at environmentModel using offline discovery done as in above method.
     * If Offline discovery is done during runtime by opatchauto then we need to parse sesssionLog to derive at the environmentModel.
     *
     * @param oracleHome
     * @param domainHome
     * @param domainName
     * @param sessionLog
     * @param testName
     * @param topologyFile
     * @return
     * @throws Exception
     */
    private EnvironmentModel getNewEnvironmentModelForOfflineOrOnlineMode(String oracleHome, String domainHome, String domainName, String sessionLog, String testName, String topologyFile) throws Exception {
        /** Variable initialised required for this method */
        EnvironmentModel localEnvironmentModel = null;
        try {
            if (topologyFile != null) {
                /** If topology file is provided by user then we should use the same topologyFile to derive at localEnvironmentModel */
                if (environmentModelBuilder != null) {
                    String topologyLocationStr = AbstractScriptRunner.testOutput + File.separator + "modelOutput" + File.separator + "models";
                    environmentModelBuilder.appendModelSearchLocation(new File(topologyLocationStr));
                }
                localEnvironmentModel = environmentModelBuilder.buildFromTopologyFile(new File(topologyFile), null, null);
            } else if (topologyFile == null && isOnlineMode) {
                /** If online discovery is done during runtime by opatchauto then we need to derive at environmentModel using offline discovery done as given
                 *  for opatchauto execn from remote host or for multinode parallel cases. All these cases we will need the offline discovery as online discovery has issues and is not yet stable. */
                localEnvironmentModel = AbstractScriptRunner.getEnvironmentModel(oracleHome,domainHome, domainName,"modelOutput");
            } else if (topologyFile == null && !isOnlineMode) {
                /**  If Offline discovery is done during runtime by opatchauto then we need to parse sesssionLog to derive at the environmentModel.*/
                localEnvironmentModel = getEnvModelByParsingSessionLogToFetchDiscoveredTopology(domainName, sessionLog, testName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return localEnvironmentModel;
    }

    /**
     * Method to retrieve the input sequence of start stop operations of server based on the patchPlan = rolling given by user for execution of opatchauto.
     * @param localEnvironmentModel
     * @param domainName
     * @param listOfHosts
     * @return
     * @throws FMWEnvSpecException
     */
    private List<StartStopExecutionOrder> getInputSequenceOfStartStopOperationForRollingPatchPlan(EnvironmentModel localEnvironmentModel, String domainName, List<Host> listOfHosts) throws FMWEnvSpecException {
        List<StartStopExecutionOrder> inputStartStopSequenceList = new ArrayList<>(); /** input sequence of startstop operations */
        sequenceNo = 0; /** sequenceNo of input sequence */

        for (Host host : listOfHosts) {
            for (MappedOracleHome mappedOracleHome : host.getMappedOracleHomes()) {
                if (mappedOracleHome.getMappedDomains() != null) {
                    for (MappedDomain mappedDomain : mappedOracleHome.getMappedDomains()) {
                        if (mappedDomain != null) {
                            /** for servers - stop */
                            for (MappedServer mappedServer : mappedDomain.getMappedServers()) {
                                /** If mappedServer is AdminServer then stop is done after all managed server of that host is stopped */
                                if (mappedServer.getServerBinding().getServerRef().equalsIgnoreCase(String.valueOf(TargetValues.ADMINSERVER))) {
                                    continue;
                                }
                                inputStartStopSequenceList.add(new StartStopExecutionOrder(sequenceNo++, String.valueOf(StatusCode.SUCCESS), String.valueOf(ActionCode.STOP), String.valueOf(TargetValues.SERVER), mappedServer.getServerBinding().getServerRef()));
                            }
                            /** for adminserver - stop*/
                            for (MappedServer mappedServer : mappedDomain.getMappedServers()) {
                                /** If mappedServer is AdminServer then stop is done after all managed server of that host is stopped */
                                if (mappedServer.getServerBinding().getServerRef().equalsIgnoreCase(String.valueOf(TargetValues.ADMINSERVER))) {
                                    inputStartStopSequenceList.add(new StartStopExecutionOrder(sequenceNo++, String.valueOf(StatusCode.SUCCESS), String.valueOf(ActionCode.STOP), String.valueOf(TargetValues.SERVER), localEnvironmentModel.getDomainById(domainName).getAdminServerId()));
                                }
                            }
                            /** for adminserver -start */
                            for (MappedServer mappedServer : mappedDomain.getMappedServers()) {
                                /** If mappedServer is AdminServer then stop is done after all managed server of that host is stopped */
                                if (mappedServer.getServerBinding().getServerRef().equalsIgnoreCase(String.valueOf(TargetValues.ADMINSERVER))) {
                                    inputStartStopSequenceList.add(new StartStopExecutionOrder(sequenceNo++, String.valueOf(StatusCode.SUCCESS), String.valueOf(ActionCode.START), String.valueOf(TargetValues.SERVER), localEnvironmentModel.getDomainById(domainName).getAdminServerId()));
                                }
                            }
                            /** for adminserver - start */
                            for (MappedServer mappedServer : mappedDomain.getMappedServers()) {
                                /** If mappedServer is AdminServer then stop is done after all managed server of that host is stopped */
                                if (mappedServer.getServerBinding().getServerRef().equalsIgnoreCase(String.valueOf(TargetValues.ADMINSERVER))) {
                                    continue;
                                }
                                inputStartStopSequenceList.add(new StartStopExecutionOrder(sequenceNo++, String.valueOf(StatusCode.SUCCESS), String.valueOf(ActionCode.START), String.valueOf(TargetValues.SERVER), mappedServer.getServerBinding().getServerRef()));
                            }
                        }
                    }
                }
            }
        }

        return inputStartStopSequenceList;
    }

    /**
     * Method to retrieve the input sequence of start stop operations of server based on the patchPlan = parallel given by user for execution of opatchauto.
     * @param localEnvironmentModel
     * @param domainName
     * @param serverIdList
     * @return
     * @throws FMWEnvSpecException
     */
    private List<StartStopExecutionOrder> getInputSequenceOfStartStopOperationForParallelPatchPlan(EnvironmentModel localEnvironmentModel, String domainName, List<String> serverIdList) throws FMWEnvSpecException {
        List<StartStopExecutionOrder> inputStartStopSequenceList = new ArrayList<>(); /** input sequence of startstop operations */
        sequenceNo = 0; /** sequenceNo of input sequence */

        if (localEnvironmentModel.getDomainById(domainName) != null) {
            /** for servers - stop */
            for (String serverStr : serverIdList) {
                /** If mappedServer is AdminServer then stop is done after all managed server of that host is stopped */
                if (serverStr.equalsIgnoreCase(String.valueOf(TargetValues.ADMINSERVER))) {
                    continue;
                }
                inputStartStopSequenceList.add(new StartStopExecutionOrder(sequenceNo++, String.valueOf(StatusCode.SUCCESS), String.valueOf(ActionCode.STOP), String.valueOf(TargetValues.SERVER), serverStr));
            }
            /** for adminserver - stop*/
            for (String serverStr : serverIdList) {
                /** If mappedServer is AdminServer then stop is done after all managed server of that host is stopped */
                if (serverStr.equalsIgnoreCase(String.valueOf(TargetValues.ADMINSERVER))) {
                    inputStartStopSequenceList.add(new StartStopExecutionOrder(sequenceNo++, String.valueOf(StatusCode.SUCCESS), String.valueOf(ActionCode.STOP), String.valueOf(TargetValues.SERVER), localEnvironmentModel.getDomainById(domainName).getAdminServerId()));
                }
            }
            /** for adminserver - start */
            for (String serverStr : serverIdList) {
                /** If mappedServer is AdminServer then stop is done after all managed server of that host is stopped */
                if (serverStr.equalsIgnoreCase(String.valueOf(TargetValues.ADMINSERVER))) {
                    continue;
                }
                inputStartStopSequenceList.add(new StartStopExecutionOrder(sequenceNo++, String.valueOf(StatusCode.SUCCESS), String.valueOf(ActionCode.START), String.valueOf(TargetValues.SERVER), serverStr));
            }
            /** for adminserver -start */
            for (String serverStr : serverIdList) {
                /** If mappedServer is AdminServer then stop is done after all managed server of that host is stopped */
                if (serverStr.equalsIgnoreCase(String.valueOf(TargetValues.ADMINSERVER))) {
                    inputStartStopSequenceList.add(new StartStopExecutionOrder(sequenceNo++, String.valueOf(StatusCode.SUCCESS), String.valueOf(ActionCode.START), String.valueOf(TargetValues.SERVER), localEnvironmentModel.getDomainById(domainName).getAdminServerId()));
                }
            }
        }
        return inputStartStopSequenceList;
    }

    /**
     * Method to be used/called from OPatchAuto automation code to read sessionLog, parse the log file and compare the lists of input sequence of startstop operations
     * with sessionlog parsed output sequence of start stop operations of server.
     *
     * @param oracleHome
     * @param domainHome
     * @param hostAddresses
     * @param topologyFile
     * @param sessionLog
     * @param patchPlan
     * @param testName
     * @return
     * @throws Exception
     */
    public boolean readSessionLogToCheckStartStopSequence(String oracleHome, String domainHome, List<String> hostAddresses, String topologyFile, String sessionLog, String patchPlan, String testName) throws Exception {
        boolean diffFoundInLists = false; /** boolean variable to check if diffFound in the two lists while comparing */
        List<StartStopExecutionOrder> inputSequenceOfStartStopList = new ArrayList<>(); /** input sequence of startstop operations */
        String domainName = domainHome.split(File.separator)[domainHome.split(File.separator).length - 1]; /** name or id of the domain */
        EnvironmentModelHelper environmentModelHelper = new EnvironmentModelHelper(); /** helper used to get list of serverIds from environmentModel */

        /** Fetch or get environmentModel either in offline mode/online mode or when user has supplied topplogy file for opatchauto execution */
        EnvironmentModel localEnvironmentModel = getNewEnvironmentModelForOfflineOrOnlineMode(oracleHome, domainHome, domainName, sessionLog, testName, topologyFile);

        /** below code lines checks if its singleNode setup or multiNode setup, if singleNode and patchplan is parallel then execution of serverstart stop should be similar to rolling plan
         * so this tweak is done to handle this corner case */
        boolean isSingleNode = false;
        int noOfHosts = localEnvironmentModel.getHosts().size();
        if(noOfHosts == 1){
            isSingleNode = true;
        }else if(noOfHosts > 1){
            isSingleNode = false;
        }
        /** based on the patchPlan given by user for execution of opatchauto the input sequence lists of start stop operations is retrieved by traversing through
         * environmentModel derived from above mentioned method.
         */
        if (patchPlan.equalsIgnoreCase("rolling_plan") || isSingleNode) {
            /** fetch listOfHosts from either model or list of hostaddresses provided by user for execution of opatchauto */
            List<Host> listOfHosts = new ArrayList<>();
            if (hostAddresses.isEmpty()) {
                listOfHosts = localEnvironmentModel.getHosts();
            } else {
                for (String hostStr : hostAddresses) {
                    List<String> hostList = new ArrayList<>();
                    hostList.add(hostStr);
                    listOfHosts.add(findHostByHostAddress(localEnvironmentModel, hostList));
                }
            }
            /** retrieve the input sequence of start stop operations of server based on the patchPlan = rolling given by user for execution of opatchauto. */
            inputSequenceOfStartStopList = getInputSequenceOfStartStopOperationForRollingPatchPlan(localEnvironmentModel, domainName, listOfHosts);
        } else if (patchPlan.equalsIgnoreCase("parallel_plan") && !isSingleNode) {
            /** fetch list of serverIds from helper based on if hostAddresses is populated by user of not */
            List<String> serverIdList;
            if (hostAddresses.isEmpty()) {
                serverIdList = environmentModelHelper.getServerIds(localEnvironmentModel, domainName);
            } else {
                serverIdList = environmentModelHelper.getServerIds(localEnvironmentModel, hostAddresses, domainHome, oracleHome);
            }
            /** retrieve the input sequence of start stop operations of server based on the patchPlan = parallel given by user for execution of opatchauto. */
            inputSequenceOfStartStopList = getInputSequenceOfStartStopOperationForParallelPatchPlan(localEnvironmentModel, domainName, serverIdList);
        }

        /** Method to parse session log to create output list of sequence of start stop operations of server and comparsion of two lists retrieved is done here. */
        diffFoundInLists = parseSessionLogAndCompareInputSequenceOfStartStopOperations(sessionLog, inputSequenceOfStartStopList);

        return diffFoundInLists;
    }
}