package org.zstack.testlib

import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.AbstractService
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.message.AbstractBeforeSendMessageInterceptor
import org.zstack.header.message.Event
import org.zstack.header.message.Message
import org.zstack.utils.ShellUtils
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

/**
 * Created by xing5 on 2017/2/12.
 */
abstract class Test implements CreationSpec {
    static final CLogger logger = Utils.getLogger(this.getClass())

    private final int PHASE_NONE = 0
    private final int PHASE_SETUP = 1
    private final int PHASE_ENV = 2
    private final int PHASE_TEST = 3

    static Deployer deployer = new Deployer()

    private int phase = PHASE_NONE

    protected EnvSpec env(@DelegatesTo(strategy=Closure.DELEGATE_FIRST, value=EnvSpec.class) Closure c) {
        def spec = new EnvSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        return spec
    }

    protected void spring(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = SpringSpec.class) Closure c) {
        def code = c.rehydrate(deployer.springSpec, this, this)
        code.resolveStrategy = Closure.DELEGATE_FIRST
        code()
    }

    protected void simulator(String path, Closure c) {
        Deployer.simulator(path, c)
    }

    abstract void setup()
    abstract void environment()
    abstract void test()

    protected boolean DEPLOY_DB = true
    protected boolean NEED_WEB_SERVER = true
    protected boolean API_PORTAL = true
    protected boolean INCLUDE_CORE_SERVICES = true

    protected Map<Class, Tuple> messageHandlers = [:]

    private void deployDB() {
        logger.info("Deploying database ...")
        String home = System.getProperty("user.dir")
        String baseDir = [home, "../"].join("/")
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

        deployer.buildBeanConstructor(NEED_WEB_SERVER)

        nextPhase()

        hijackService()
        environment()
    }

    protected <T> T bean(Class<T> clz) {
        assert phase > PHASE_SETUP : "getBean() can only be called in method environment() or test()"
        return deployer.componentLoader.getComponent(clz)
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
            logger.warn(t.message, t)
            System.exit(1)
        }
    }
}
