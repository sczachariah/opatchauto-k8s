package oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests;

import org.apache.commons.exec.LogOutputStream;

import java.util.LinkedList;
import java.util.List;

public class CollectingLogOutputStream extends LogOutputStream {
    private static List<String> lines;


    public CollectingLogOutputStream() {
        lines = new LinkedList<String>();
        lines.clear();
    }

    @Override
    protected void processLine(String line, int level) {
        lines.add(line);
        //Output the log info to console
        System.out.println("[zdt-qa.info] " + line);
        if(line.contains("The id for this session is")){
            AbstractScriptRunner.sessionIDs.add(line.split(" ")[(line.split(" ").length) - 1].trim());
            System.out.println("[zdt-qa.info] " + "Session ID : " +
                    AbstractScriptRunner.sessionIDs.get(AbstractScriptRunner.sessionIDs.size()-1));
        }
    }

    public List<String> getLines() {
        return lines;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (String line : getLines()) {
            sb.append(line);
            sb.append(System.getProperty("line.separator"));
        }
        return sb.toString();
    }
}