package org.zstack.testlib

import org.codehaus.groovy.runtime.StackTraceUtils
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.componentloader.ComponentLoader
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.AbstractService
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.message.AbstractBeforeSendMessageInterceptor
import org.zstack.header.message.Event
import org.zstack.header.message.Message
import org.zstack.utils.ShellUtils
import org.zstack.utils.Utils
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.logging.CLogger

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Created by xing5 on 2017/2/12.
 */
abstract class Test implements ApiHelper {
    static final CLogger logger = Utils.getLogger(this.getClass())

    static Object deployer

    private final int PHASE_NONE = 0
    private final int PHASE_SETUP = 1
    private final int PHASE_ENV = 2
    private final int PHASE_TEST = 3

    // these are global variables
    static EnvSpec currentEnvSpec
    static ComponentLoader componentLoader

    private int phase = PHASE_NONE
    protected BeanConstructor beanConstructor
    protected SpringSpec springSpec

    Test() {
        springSpec = new SpringSpec()
    }

    static EnvSpec makeEnv(@DelegatesTo(strategy=Closure.DELEGATE_FIRST, value=EnvSpec.class) Closure c) {
        def spec = new EnvSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        return spec
    }

    protected EnvSpec env(@DelegatesTo(strategy=Closure.DELEGATE_FIRST, value=EnvSpec.class) Closure c) {
        def spec = new EnvSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        return spec
    }

    protected void spring(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = SpringSpec.class) Closure c) {
        c.delegate = springSpec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
    }

    abstract void setup()
    abstract void environment()
    abstract void test()

    protected boolean DEPLOY_DB = true
    protected boolean NEED_WEB_SERVER = true
    protected boolean API_PORTAL = true
    protected boolean INCLUDE_CORE_SERVICES = true

    protected Map<Class, Tuple> messageHandlers = [:]

    protected String getDeployDBScriptBaseDir() {
        String home = System.getProperty("user.dir")
        return  [home, "../"].join("/")
    }

    private void deployDB() {
        logger.info("Deploying database ...")
        String baseDir = getDeployDBScriptBaseDir()
        Properties prop = new Properties()

        try {
            prop.load(this.getClass().getClassLoader().getResourceAsStream("zstack.properties"))

            String user = System.getProperty("DB.user")
            if (user == null) {
                user = prop.getProperty("DB.user")
                if (user == null) {
                    user = prop.getProperty("DbFacadeDataSource.user")
                }
                if (user == null) {
                    throw new CloudRuntimeException("cannot find DB user in zstack.properties, please set either DB.user or DbFacadeDataSource.user")
                }
            }

            String password = System.getProperty("DB.password")
            if (password == null) {
                password = prop.getProperty("DB.password")
                if (password == null) {
                    password = prop.getProperty("DbFacadeDataSource.password")
                }
                if (password == null) {
                    throw new CloudRuntimeException("cannot find DB user in zstack.properties, please set either DB.password or DbFacadeDataSource.password")
                }
            }

            ShellUtils.run("build/deploydb.sh $user $password", baseDir, false)
            logger.info("Deploying database successfully")
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to deploy zstack database for testing", e)
        }
    }

    protected void message(Class<? extends Message> msgClz, Closure condition, Closure handler) {
        messageHandlers[(msgClz)] = new Tuple(condition, handler)
    }

    protected void message(Class<? extends Message> msgClz, Closure handler) {
        message(msgClz, null, handler)
    }

    private void nextPhase() {
        phase ++
    }

    private void hijackService() {
        CloudBus bus = bean(CloudBus.class)

        def serviceId = "test.hijack.service"
        def service = new AbstractService() {
            @Override
            void handleMessage(Message msg) {
                try {
                    def entry = messageHandlers.find { k, _ -> k.isAssignableFrom(msg.getClass()) }
                    if (entry != null) {
                        Tuple t = entry.value
                        Closure handler = t[1]
                        if (handler.maximumNumberOfParameters <= 1) {
                            handler(msg)
                        } else {
                            handler(msg, bus)
                        }
                    }
                } catch (Exception ex) {
                    bus.replyErrorByMessageType(msg, ex)
                }
            }

            @Override
            String getId() {
                return bus.makeLocalServiceId(serviceId)
            }

            @Override
            boolean start() {
                return true
            }

            @Override
            boolean stop() {
                return true
            }
        }

        bus.registerService(service)

        messageHandlers.each { Class clz, Tuple t ->
            if (!Event.isAssignableFrom(clz)) {
                bus.installBeforeSendMessageInterceptor(new AbstractBeforeSendMessageInterceptor() {
                    @Override
                    void intercept(Message msg) {
                        Closure condition = t[0]

                        if (condition != null && !condition(msg)) {
                            // the condition closure tells us not to hijack this message
                            return
                        }

                        bus.makeLocalServiceId(msg, serviceId)
                    }
                }, clz)
            }
        }
    }

    void buildBeanConstructor(boolean useWeb = true) {
        beanConstructor = useWeb ? new WebBeanConstructor() : new BeanConstructor()
        if (springSpec.all) {
            beanConstructor.loadAll = true
        } else {
            springSpec.xmls.each { beanConstructor.addXml(it) }
        }

        componentLoader = beanConstructor.build()
    }

    protected String adminSession() {
        return currentEnvSpec.session.uuid
    }

    private void prepare() {
        nextPhase()
        if (API_PORTAL) {
            spring {
                include("ManagementNodeManager.xml")
                include("ApiMediator.xml")
                include("AccountManager.xml")
            }
        }

        if (INCLUDE_CORE_SERVICES) {
            spring {
                includeCoreServices()
            }
        }

        setup()

        if (DEPLOY_DB) {
            deployDB()
        }

        buildBeanConstructor(NEED_WEB_SERVER)

        nextPhase()

        hijackService()
        environment()
    }

    protected <T> T bean(Class<T> clz) {
        assert componentLoader != null : "componentLoader is null!!!"
        return componentLoader.getComponent(clz)
    }

    protected <T> T dbFindByUuid(String uuid, Class<T> voClz) {
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        return dbf.findByUuid(uuid, voClz)
    }

    @org.junit.Test
    final void doTest() {
        try {
            prepare()
            nextPhase()
            test()
        } catch (AssertionError e) {
            logger.warn("\n${e.message}", e)
            System.exit(1)
        } catch (Throwable t) {
            logger.warn("", t)
            System.exit(1)
        }
    }

    static void handleHttp(HttpServletRequest request, HttpServletResponse response) {
        currentEnvSpec.handleSimulatorHttpRequests(request, response)
    }

    static class SubCaseResult {
        boolean success
        String error
        String name
    }

    protected void runSubCases(List<SubCase> cases) {
        String resultDir = System.getProperty("resultDir")
        if (resultDir == null) {
            resultDir = [System.getProperty("user.dir"), "zstack-integration-test-result"].join("/")
        }

        resultDir = [resultDir, this.class.name.replace(".", "_")].join("/")

        def dir = new File(resultDir)
        dir.deleteDir()
        dir.mkdirs()

        List<SubCaseResult> allResults = []

        for (SubCase c in cases) {
            def caseResult = new SubCaseResult()
            caseResult.name = c.class.simpleName

            allResults.add(caseResult)

            logger.info("starts running a sub case[${c.class}] of suite[${this.class}]")
            try {
                c.run()

                caseResult.success = true
                logger.info("a sub case[${c.class}] of suite[${this.class}] completes without any error")
            } catch (Throwable t) {
                caseResult.success = false
                caseResult.error = t.message

                logger.error("a sub case [${c.class}] of suite[${this.class}] fails, ${t.message}", t)
            } finally {
                def fname = c.class.name.replace(".", "_") + "." + (caseResult.success ? "success" : "failure")
                def rfile = new File([dir.absolutePath, fname].join("/"))
                rfile.write(JSONObjectUtil.toJsonString(caseResult))

                logger.info("write test result of a sub case [${c.class}] of suite[${this.class}] to $fname")
            }
        }

        int success = 0
        int failure = 0
        allResults.each {
            if (it.success) {
                success ++
            } else {
                failure ++
            }
        }

        def summary = new File([dir.absolutePath, "summary"].join("/"))
        summary.write(JSONObjectUtil.toJsonString([
                "total" : allResults.size(),
                "success": success,
                "failure": failure,
                "passRate": ((float)success / (float)allResults.size()) * 100
        ]))

        if (failure != 0) {
            // some cases failed, exit with code 1
            System.exit(1)
        }
    }
}
