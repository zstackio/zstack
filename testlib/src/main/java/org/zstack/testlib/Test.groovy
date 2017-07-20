package org.zstack.testlib

import okhttp3.OkHttpClient
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusImpl2
import org.zstack.core.componentloader.ComponentLoader
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.AbstractService
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.identity.AccountConstant
import org.zstack.header.message.AbstractBeforeSendMessageInterceptor
import org.zstack.header.message.Event
import org.zstack.header.message.Message
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.ZSClient
import org.zstack.testlib.collectstrategy.SubCaseCollectionStrategyFactory
import org.zstack.testlib.collectstrategy.SubCaseCollectionStrategy
import org.zstack.utils.ShellUtils
import org.zstack.utils.Utils
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.logging.CLogger
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Created by xing5 on 2017/2/12.
 */
abstract class Test implements ApiHelper {
    final CLogger logger = Utils.getLogger(this.getClass())

    static Object deployer
    static Map<String, String> apiPaths = new ConcurrentHashMap<>()

    private final static long DEFAULT_MESSAGE_TIMEOUT_SECS = TimeUnit.SECONDS.toMillis(25)

    static long getMessageTimeoutMillsConfig(){
        String msgTimeoutStr = System.getProperty("msgTimeoutMins")

        if(System.getProperty("maven.surefire.debug") != null && msgTimeoutStr == null){
            return TimeUnit.MINUTES.toMillis(30)
        }

        if(msgTimeoutStr == null || msgTimeoutStr.isEmpty()){
            return DEFAULT_MESSAGE_TIMEOUT_SECS
        }

        long msgTimeout = Long.parseLong(msgTimeoutStr)
        return TimeUnit.MINUTES.toMillis(msgTimeout)
    }

    private final int PHASE_NONE = 0
    private final int PHASE_SETUP = 1
    private final int PHASE_ENV = 2
    private final int PHASE_TEST = 3

    // these are global variables
    static EnvSpec currentEnvSpec
    static ComponentLoader componentLoader

    private int phase = PHASE_NONE
    protected BeanConstructor beanConstructor
    protected SpringSpec _springSpec

    Test() {
        _springSpec = new SpringSpec()
        Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE)
    }

    static EnvSpec makeEnv(@DelegatesTo(strategy=Closure.DELEGATE_FIRST, value=EnvSpec.class) Closure c) {
        def spec = new EnvSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        return spec
    }

    static SpringSpec makeSpring(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = SpringSpec.class) Closure c) {
        def spec = new SpringSpec()
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
        c.delegate = _springSpec
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

    private void nextPhase() {
        phase ++
    }

    private void hijackService() {
        CloudBus bus = bean(CloudBus.class)
        if(bus instanceof CloudBusImpl2){
            logger.info(String.format("CloudBus message timeout: %s mills", getMessageTimeoutMillsConfig()))
            ((CloudBusImpl2)bus).setDEFAULT_MESSAGE_TIMEOUT(getMessageTimeoutMillsConfig())
        }

        def serviceId = "test.hijack.service"
        def service = new AbstractService() {
            @Override
            void handleMessage(Message msg) {
                if (currentEnvSpec == null) {
                    return
                }

                try {
                    def all = currentEnvSpec.messageHandlers.findAll { k, _ -> k.isAssignableFrom(msg.getClass()) }

                    boolean handled = false
                    boolean conded = false
                    all.values().each { tuples ->
                        // if there is a closure with condition satisfied, skip the mock without conditions
                        tuples.each {
                            Closure cond = it[0]
                            if (cond != null && cond(msg)) {
                                conded = true
                            }
                        }
                        tuples.each {
                            Closure cond = it[0]
                            Closure handler = it[1]

                            if (cond == null && conded) {
                                // skip (cond == null)
                                return
                            }

                            if (cond != null && !cond(msg)) {
                                return
                            }

                            handled = true
                            if (handler.maximumNumberOfParameters <= 1) {
                                handler(msg)
                            } else {
                                handler(msg, bus)
                            }
                        }
                    }

                    if (!handled) {
                        bus.replyErrorByMessageType(msg, "a test case installed message handler for this message, however," +
                                " its condition closure decides not to handle this message. Check your test case")
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

        bus.installBeforeSendMessageInterceptor(new AbstractBeforeSendMessageInterceptor() {
            @Override
            void intercept(Message msg) {
                if (Event.class.isAssignableFrom(msg.class) || currentEnvSpec == null) {
                    return
                }

                def has = currentEnvSpec.messageHandlers.find { k, _ -> k.isAssignableFrom(msg.getClass()) } != null
                if (has) {
                    bus.makeLocalServiceId(msg, serviceId)
                }
            }
        })
    }

    void buildBeanConstructor(boolean useWeb = true) {
        beanConstructor = useWeb ? new WebBeanConstructor() : new BeanConstructor()
        if (_springSpec.all) {
            beanConstructor.loadAll = true
        } else {
            _springSpec.xmls.each { beanConstructor.addXml(it) }
        }

        componentLoader = beanConstructor.build()
    }

    protected String adminSession() {
        return currentEnvSpec.session.uuid
    }

    protected void useSpring(SpringSpec spec) {
        _springSpec = spec
    }

    private void prepare() {
        nextPhase()

        setup()

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

    protected <T> T dbFindById(Long id, Class<T> voClz) {
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        return dbf.findById(id, voClz)
    }

    protected <T> T dbFindByUuid(String uuid, Class<T> voClz) {
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        return dbf.findByUuid(uuid, voClz)
    }

    protected boolean dbIsExists(Object primaryKey, Class clz) {
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        return dbf.isExist(primaryKey, clz)
    }

    @org.junit.Test
    final void doTest() {
        try {
            prepare()
            nextPhase()
            test()

            if ((this instanceof Case) && System.getProperty("clean") != null) {
                clean()
            }

            if (System.getProperty("apipath") != null) {
                def dir = new File([getResultDirBase(), "apipath"].join("/"))
                dir.deleteDir()
                dir.mkdirs()

                apiPaths.each { name, path ->
                    new File([dir.absolutePath, name.replace(".", "_")].join("/")).write(path)
                }
            }
        } catch (AssertionError e) {
            logger.warn("\n${e.message}", e)
            System.exit(1)
        } catch (Throwable t) {
            logger.warn("", t)
            System.exit(1)
        }
    }

    static void handleHttp(HttpServletRequest request, HttpServletResponse response) {
        if (WebBeanConstructor.WEB_HOOK_PATH.toString().contains(request.getRequestURI())) {
            ZSClient.webHookCallback(request, response)
        } else {
            currentEnvSpec.handleSimulatorHttpRequests(request, response)
        }
    }

    static class SubCaseResult {
        Boolean success
        String error
        String name
        transient Class caseType
    }

    static Case CURRENT_SUB_CASE

    protected void beforeRunSubCase() {
    }

    private String getResultDirBase() {
        String resultDir = System.getProperty("resultDir")
        if (resultDir == null) {
            resultDir = [System.getProperty("user.dir"), "zstack-integration-test-result"].join("/")
        }
        return resultDir
    }

    private SubCaseCollectionStrategy getSubCaseCollectionStrategy(){
        String strategyName = System.getProperty("subCaseCollectionStrategy")
        SubCaseCollectionStrategy strategy = SubCaseCollectionStrategyFactory.getSubCaseCollectionStrategy(strategyName)

        assert null != strategy : "can not find SubCaseCollectionStrategy"
        logger.info("input subCaseCollectionStrategy = ${strategyName}, subCaseCollectionStrategy is ${strategy.strategyName}")
        return strategy
    }

    protected void runSubCases() {
        def resultDir = [getResultDirBase(), this.class.name.replace(".", "_")].join("/")
        def dir = new File(resultDir)
        dir.deleteDir()
        dir.mkdirs()

        SubCaseCollectionStrategy strategy = getSubCaseCollectionStrategy()
        def caseTypes = strategy.collectSubCases(this)

        def cases = new File([dir.absolutePath, "cases"].join("/"))
        cases.write(caseTypes.collect {it.name}.join("\n"))

        if (System.getProperty("list") != null) {
            return
        }

        if (caseTypes.isEmpty()) {
            return
        }

        List<SubCaseResult> allCases = caseTypes.collect {
            SubCaseResult ret = new SubCaseResult()
            ret.caseType = it
            ret.name = it.simpleName
            return ret
        }

        boolean hasFailure = false

        for (SubCaseResult r in allCases) {
            def c = r.caseType.newInstance() as Case

            String caseLogStartLine = "case log of ${c.class} starts here"
            String caseLogEndLine = "case log of ${c.class} ends here"

            logger.info("starts running a sub case[${c.class}] of suite[${this.class}]")
            new File([dir.absolutePath, "current-case"].join("/")).write("${c.class}")

            try {
                CURRENT_SUB_CASE = c
                c.metaClass.collectErrorLog = {
                    File failureLogDir = new File([dir.absolutePath, "failureLogs", r.caseType.name.replace(".", "_")].join("/"))
                    failureLogDir.mkdirs()
                    File failureLog = new File([failureLogDir.absolutePath, "case.log"].join("/"))

                    File mgmtLogPath = new File([System.getProperty("user.dir"), "management-server.log"].join("/"))

                    ShellUtils.run("""\
start=`grep -nr "$caseLogStartLine" ${mgmtLogPath.absolutePath} | grep -v ShellUtils | gawk '{print \$1}' FS=":"`
tail -n +\$start ${mgmtLogPath.absolutePath} > ${failureLog.absolutePath}

mysqldump -u root zstack > ${failureLogDir.absolutePath}/dbdump.sql
""", false)
                }

                beforeRunSubCase()

                logger.info(caseLogStartLine)

                c.run()

                r.success = true
                logger.info("a sub case[${c.class}] of suite[${this.class}] completes without any error")
            } catch (StopTestSuiteException e) {
                hasFailure = true
                r.success = false
                r.error = e.message

                logger.error("a sub case[${c.class}] of suite[${this.class}] throws StopTestSuiteException, ${e.message}", e)
                break
            } catch (Throwable t) {
                hasFailure = true
                r.success = false
                r.error = t.message

                logger.error("a sub case[${c.class}] of suite[${this.class}] fails, ${t.message}", t)
            } finally {
                def fname = c.class.name.replace(".", "_") + "." + (r.success ? "success" : "failure")
                def rfile = new File([dir.absolutePath, fname].join("/"))
                rfile.write(JSONObjectUtil.toJsonString(r))

                logger.info("write test result of a sub case[${c.class}] of suite[${this.class}] to $fname")
                logger.info(caseLogEndLine)
            }
        }

        int success = 0
        int failure = 0
        int skipped = 0
        allCases.each {
            if (it.success == null) {
                def fname = it.caseType.name.replace(".", "_") + ".skipped"
                def rfile = new File([dir.absolutePath, fname].join("/"))
                rfile.createNewFile()
                skipped ++
            } else if (it.success) {
                success ++
            } else {
                failure ++
            }
        }

        def summary = new File([dir.absolutePath, "summary"].join("/"))
        summary.write(JSONObjectUtil.toJsonString([
                "total" : caseTypes.size(),
                "success": success,
                "failure": failure,
                "skipped": skipped,
                "passRate": ((float)success / (float)caseTypes.size()) * 100
        ]))

        new File([dir.absolutePath, "done"].join("/")).createNewFile()

        if (hasFailure) {
            // some cases failed, exit with code 1
            System.exit(1)
        }
    }

    protected static <T> T json(String str, Class<T> type) {
        return JSONObjectUtil.toObject(str, type)
    }

    SessionInventory loginAsAdmin() {
        return logInByAccount {
            accountName = AccountConstant.INITIAL_SYSTEM_ADMIN_NAME
            password = AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD
        } as SessionInventory
    }

    private static boolean getRetryReturnValue(ret, boolean throwError = false) {
        boolean judge

        if (ret instanceof Closure) {
            try {
                def r = ret()
                judge = (r != null && r instanceof Boolean) ? r : true
            } catch (Throwable t) {
                if (throwError) {
                    throw t
                } else {
                    judge = false
                }
            }
        } else {
            judge = ret
        }

        return judge
    }

    @Deprecated
    protected static boolean retryInSecs3(int total = 15, int interval = 1, Closure c) {
        int count = 0

        def ret = null

        while (count < total) {
            ret = c()

            if (getRetryReturnValue(ret)) {
                return true
            }

            TimeUnit.SECONDS.sleep(interval)
            count += interval
        }

        return getRetryReturnValue(ret, true)
    }

    protected boolean retryInSecs(int total = 8, int interval = 1, Closure c) {
        int count = 0
        def ret = false

        while (count < total) {
            try {
                def r = c()
                ret = r == null || (r != null && r instanceof Boolean && r)
            } catch (StopTestSuiteException e) {
                throw e
            } catch (Throwable t) {
                logger.debug("[retryInSecs:${count + 1}/${total}]", t)
                if (total - count == 1) {
                    throw t
                }
            }

            if (ret) {
                return ret
            }
            TimeUnit.SECONDS.sleep(interval)
            count ++
        }

        return false
    }

    @Deprecated
    protected static boolean retryInMillis(int total, int interval = 500, Closure c) {
        int count = 0

        def ret = null

        while (count < total) {
            ret = c()

            if (getRetryReturnValue(ret)) {
                return true
            }

            TimeUnit.MILLISECONDS.sleep(interval)
            count += interval
        }

        return getRetryReturnValue(ret, true)
    }

    static class ExpectedException extends Exception {
        ExpectedException() {
        }

        ExpectedException(String var1) {
            super(var1)
        }

        ExpectedException(String var1, Throwable var2) {
            super(var1, var2)
        }

        ExpectedException(Throwable var1) {
            super(var1)
        }

        ExpectedException(String var1, Throwable var2, boolean var3, boolean var4) {
            super(var1, var2, var3, var4)
        }
    }

    protected static void expect(exceptions, Closure c) {
        List<Class> lst = []
        if (exceptions instanceof Collection) {
            lst.addAll(exceptions)
        } else if (exceptions instanceof Class && Throwable.class.isAssignableFrom(exceptions)) {
            lst.add(exceptions)
        } else {
            throw new Exception("the first argument must be a Throwable or a collection of Throwable, but got a ${exceptions.class.name}")
        }

        try {
            c()
            throw new ExpectedException("expect exceptions[${lst.collect { it.name }}] happen, but nothing happens")
        } catch (Throwable t) {
            if (t instanceof ExpectedException) {
                throw t
            }

            for (Class tt : lst) {
                if (tt.isAssignableFrom(t.class)) {
                    return
                }
            }

            throw new Exception("expected to get a Throwable of ${lst.collect { it.name }} but got ${t.class.name}")

        }
    }
}
