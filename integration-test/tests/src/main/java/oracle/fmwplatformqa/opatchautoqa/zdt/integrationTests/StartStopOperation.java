package oracle.fmwplatformqa.opatchautoqa.zdt.integrationTests;

import oracle.fmwplatform.actionframework.api.v2.Action;
import oracle.fmwplatform.actionframework.api.v2.ActionFactory;
import oracle.fmwplatform.actionframework.api.v2.ActionResult;
import oracle.fmwplatform.actionframework.api.v2.DefaultActionFactoryLocator;
import oracle.fmwplatform.envspec.model.EnvironmentModel;
import oracle.fmwplatform.envspec.model.targets.ModelTarget;

import java.util.List;
import java.util.Properties;

/**
 * Created by gakhuran on 6/6/2016.
 */
public class StartStopOperation {
    protected ActionFactory actionFactory;
    protected EnvironmentModel envModel;
    protected List<ModelTarget> envTargets;
    protected String oracleHome;

    public enum SERVEROP {
        START("start"),
        STOP("stop");

        private String operation;

        SERVEROP(String op) {
            this.operation = op;
        }

        public String getOperation() {
            return this.operation;
        }
    }


    public ActionResult operate(StartStopOperation.SERVEROP operation, EnvironmentModel environmentModel, List<ModelTarget> targets, String oh) throws Exception {
        envModel = environmentModel;
        envTargets = targets;
        oracleHome = oh;

        actionFactory = DefaultActionFactoryLocator.locateActionFactory();
        Action action = actionFactory.getAction(operation.getOperation(), this.oracleHome);
//        HashMap<String,String> hm= new HashMap<String, String>();
        Properties properties = new Properties();
        ActionResult actionResult = action.run(this.envModel, envTargets, properties);

        return actionResult;
    }


    public ActionResult operateWithoutNodeManager(StartStopOperation.SERVEROP operation, EnvironmentModel environmentModel, List<ModelTarget> targets, String oh) throws Exception {
        envModel = environmentModel;
        envTargets = targets;
        oracleHome = oh;

//        Object extraParameters = new HashMap();
//        ((Map) extraParameters).put("NoNodeManager", "True");
        Properties extraParameters = new Properties();
        extraParameters.setProperty("NoNodeManager", "True");

        actionFactory = DefaultActionFactoryLocator.locateActionFactory();
        Action action = actionFactory.getAction(operation.getOperation(), oracleHome);
        ActionResult actionResult = action.run(envModel, envTargets, extraParameters);
        return actionResult;
    }
}
