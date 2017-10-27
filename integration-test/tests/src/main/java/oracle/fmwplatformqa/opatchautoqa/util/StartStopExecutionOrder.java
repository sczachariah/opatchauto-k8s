package oracle.fmwplatformqa.opatchautoqa.util;

/**
 * Class that is used to store the INFO message of opatchauto session log in their execution order
 */
public class StartStopExecutionOrder implements Comparable<StartStopExecutionOrder> {
    private int serialNo;
    private String statusCode;
    private String actionCode;
    private String targetSource;
    private String targetValues;

    public StartStopExecutionOrder(){
        this.serialNo=0;
        this.statusCode = null;
        this.actionCode = null;
        this.targetSource = null;
        this.targetValues = null;
    }

    public StartStopExecutionOrder(int serialNo, String statusCode, String actionCode, String targetSource, String targetValues){
        this.serialNo = serialNo;
        this.statusCode = statusCode;
        this.actionCode = actionCode;
        this.targetSource = targetSource;
        this.targetValues = targetValues;
    }

    /**
     * Getter Method
     */
    public int getSerialNo() {
        return serialNo;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public String getActionCode() {
        return actionCode;
    }

    public String getTargetSource() {
        return targetSource;
    }

    public String getTargetValues() {
        return targetValues;
    }

    /**
     * Setter Method
     */
    public void setSerialNo(int serialNo) {
        this.serialNo = serialNo;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public void setTargetSource(String targetSource) {
        this.targetSource = targetSource;
    }

    public void setTargetValues(String targetValues) {
        this.targetValues = targetValues;
    }

    @Override
    public String toString() {
        return "{" + serialNo + ", " + statusCode + ", " + actionCode + ", " + targetSource + ", " + targetValues + "}";
    }

    @Override
    public int compareTo(StartStopExecutionOrder object) {
        int result = 1;

        /** succeed success-stop compared with success_no_action-stop for same targets */
        if (this.getStatusCode().equalsIgnoreCase("SUCCESS_NO_ACTION")) {
            if (this.getActionCode().equalsIgnoreCase("STOP") && this.getActionCode().equals(object.getActionCode()) &&
                    this.targetSource.equalsIgnoreCase(object.targetSource) && this.targetValues.equalsIgnoreCase(object.targetValues))
                return 0;
        }

        if ((this.getStatusCode().compareToIgnoreCase(object.getStatusCode()) < 0) || (this.getActionCode().compareTo(object.getActionCode()) < 0) ||
                (this.getTargetSource().compareToIgnoreCase(object.getTargetSource()) < 0) || (this.getTargetValues().compareToIgnoreCase(object.getTargetValues()) < 0)) {
            result = -1;
        } else if (this.getStatusCode().compareToIgnoreCase(object.getStatusCode()) == 0) {
            if (this.getActionCode().compareTo(object.getActionCode()) == 0) {
                if (this.getTargetSource().compareToIgnoreCase(object.getTargetSource()) == 0) {
                    if (this.getTargetValues().compareToIgnoreCase(object.getTargetValues()) == 0) {
                        result = 0;
                    }
                }
            }
        }
        return result;
    }
}
