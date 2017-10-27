package oracle.fmwplatformqa.opatchautoqa.action;

import oracle.fmwplatform.actionframework.api.v2.ActionMode;
import oracle.fmwplatform.actionframework.api.v2.ActionResult;
import oracle.fmwplatform.actionframework.v2.annotations.ActionImpl;
import oracle.fmwplatform.actionframework.v2.internal.AbstractWLSTActionImpl;
import oracle.fmwplatform.envspec.model.EnvironmentModel;
import oracle.fmwplatform.envspec.model.targets.ModelTarget;

import java.util.List;
import java.util.Properties;


@ActionImpl(name = EchoVersionStandardAction.ACTION_NAME)
public class EchoVersionStandardAction extends AbstractWLSTActionImpl {
    public static final String ACTION_NAME = "newaf_echo_version.echoTopologyVersion_impl";
    private static final String SCRIPT_AND_METHOD = "newaf_echo_version.echoTopologyVersion_impl";

    @Override
    protected ActionResult doInit() {
        setMethodName(SCRIPT_AND_METHOD);
        return getActionResult();
    }

    @Override
    public boolean doCanInvoke() {
        return true;
    }

    @Override
    protected ActionMode getMode() {
        return ActionMode.REQUIRE_OFFLINE;
    }

    @Override
    protected boolean doVerify(EnvironmentModel model, List<ModelTarget> targets, Properties extra) {
        // TODO: Update this method to implement verify logic
        return true;
    }
}
