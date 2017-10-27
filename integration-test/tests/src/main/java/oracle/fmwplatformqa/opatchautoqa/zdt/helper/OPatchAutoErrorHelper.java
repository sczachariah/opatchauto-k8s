package oracle.fmwplatformqa.opatchautoqa.zdt.helper;

/**
 * Created by sczachar on 10/6/2015.
 */
public class OPatchAutoErrorHelper {

    public enum OPATCHAUTO_ERROR {
        NULL("", "", ""),
        OPATCHAUTO_FAILED("","OPatchAuto failed.","[error] OPatchAuto Execution Failed."),
        ROLLOUT_INVALID_TARGET("", "Please specify a valid target.", "[error] Invalid Rollout Target was not identified by OPatchAuto."),
        ROLLOUT_DIFF_TARGET("", "Target could be \"SERVER\" or \"CLUSTER\".", "[error] Invalid Target Combination was not identified by OPatchAuto."),
        INVALID_IMAGE_LOC("OPATCHAUTO-70022", "Image location invalid.", "[error] Invalid Image Location was not identified by OPatchAuto."),
        INVALID_WALLET_LOC("OPATCHAUTO-68082", "Wallet location did not exist.", "[error] Invalid Wallet Location was not identified by OPatchAuto."),
        INVALID_WALLET_PASS("OPATCHAUTO-68083", "Failed to open wallet.", "[error] Invalid Wallet Password was not identified by OPatchAuto."),
        INVALID_ADMIN_HOST("OPATCHAUTO-71069", "Unable to resolve host.", "[error] Invalid Admin Host was not identified by OPatchAuto."),
        INVALID_ADMIN_PORT("OPATCHAUTO-71070", "Invalid port.", "[error] Invalid Admin Port was not identified by OPatchAuto."),
        INVALID_RESUME_SESSION_ID("OPATCHAUTO-68011", "Invalid resume session id.", "[error] Invalid Session ID was not identified by OPatchAuto."),
        INVALID_PLAN("OPATCHAUTO-68096", "Unknown patch plan.", "[error] Invalid Patch Plan was not identified by OPatchAuto."),
        ZDT_OPTIONS("", "Unable to find migration properties file", "[error] WLS ZDT Options were not processed/identified by OPatchAuto.");

        private String errorCode;
        private String errorMessage;
        private String opatchautoError;
        private String automationError;

        OPATCHAUTO_ERROR(String code, String message, String error) {
            errorCode = code;
            errorMessage = message;
            opatchautoError = this.errorCode + (this.errorCode != "" ? ": " : "") + this.errorMessage;
            automationError = error;
        }

        public String getOPatchAutoError() {
            if(!this.errorMessage.contains("OPatchAuto failed.")) {
                System.out.println("[zdt-qa.info] Checking for error \"" + opatchautoError + "\"");
            }
            return opatchautoError;
        }

        public String getAutomationError() {
            return automationError;
        }
    }
}
