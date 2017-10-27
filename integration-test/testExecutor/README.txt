## testExecutor to execute  OCP/OPatchAuto tests in standalone mode##

Prerequisites To execute execute.pl

1. Should have simple WebLogic domain created.
2. Export/Set below Environment Variables.
   2.1 JAVA_HOME ex. export JAVA_HOME=<JDK location>
   2.2 ORACLE_HOME ex. export ORACLE_HOME=<Location to Oracle Home>
   2.3 DOMAIN_HOME ex. export Domain_HOME=<Location to Domain Home>
   2.4 M2_HOME (Optional) if set gives priority
 
execute:  perl execute.pl

Optional parameters that can be passed to execute.pl are PATCH_HOME and TEST_PARAMS as below:

PATCH_HOME  -   Path to External patches that can be used for running mats suite.
TEST_PARAMS -   -DsuiteXmlFile for choosing which suite to run.
                Available suites are:
                                    
                                    OPatchAuto  :   mats.xml,
                                                    opatchautoqa.xml,
                                                    opatchautoqa.mats.xml,
                                                    opatchautoqa.xml,
                                                    opatchautoqa.generic.xml,
                                                    opatchautoqa.customAction.xml,
                                                    opatchautoqa.useCase.xml,
                                                    opatchautoqa.standardAction.xml


perl execute.pl -PATCH_HOME <external patches locations> -TEST_PARAMS <extra parameters for maven as mentioned above>
   
   