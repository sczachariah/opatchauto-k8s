package oracle.fmwplatformqa.opatchautoqa.action;

import oracle.fmwplatform.actionframework.api.v2.ActionMode;
import oracle.fmwplatform.actionframework.api.v2.ActionResult;
import oracle.fmwplatform.actionframework.v2.annotations.ActionImpl;
import oracle.fmwplatform.actionframework.v2.internal.AbstractWLSTActionImpl;
import oracle.fmwplatform.envspec.model.EnvironmentModel;
import oracle.fmwplatform.envspec.model.targets.ModelTarget;

import java.util.List;
import java.util.Properties;


@ActionImpl(name = ZdtCustActionHowAreYou.ACTION_NAME)
public class ZdtCustActionHowAreYou extends AbstractWLSTActionImpl {
    public static final String ACTION_NAME = "opatchautozdtqa_customactions.sayHowAreYou";
    private static final String SCRIPT_AND_METHOD = "opatchautozdtqa_customactions.sayHowAreYou";

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
