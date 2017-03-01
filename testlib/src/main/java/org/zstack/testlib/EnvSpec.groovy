package org.zstack.testlib

import org.codehaus.groovy.runtime.InvokerHelper
import org.springframework.http.*
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import org.zstack.core.CoreGlobalProperty
import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.SQL
import org.zstack.header.identity.AccountConstant
import org.zstack.header.message.Message
import org.zstack.header.rest.RESTConstant
import org.zstack.sdk.LogInByAccountAction
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.ZSClient
import org.zstack.utils.gson.JSONObjectUtil

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by xing5 on 2017/2/12.
 */
class EnvSpec implements Node {
    protected List<ZoneSpec> zones = []
    List<AccountSpec> accounts = []

    SessionInventory session

    Map specsByName = [:]
    Map specsByUuid = [:]

    private boolean hasCreated
    private ConcurrentHashMap<String, Closure> httpHandlers = [:]
    private ConcurrentHashMap<String, Closure> httpPostHandlers = [:]
    private ConcurrentHashMap<String, Closure> defaultHttpHandlers = [:]
    private ConcurrentHashMap<String, Closure> defaultHttpPostHandlers = [:]
    protected ConcurrentHashMap<Class, Tuple> messageHandlers = [:]
    private static RestTemplate restTemplate
    private static Set<Class> simulatorClasses = Platform.reflections.getSubTypesOf(Simulator.class)

    EnvSpec() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory()
        factory.setReadTimeout(CoreGlobalProperty.REST_FACADE_READ_TIMEOUT)
        factory.setConnectTimeout(CoreGlobalProperty.REST_FACADE_CONNECT_TIMEOUT)
        restTemplate = new RestTemplate(factory)

        simulatorClasses.each {
            Simulator sim = it.newInstance() as Simulator
            sim.registerSimulators(this)
        }
    }

    void cleanSimulatorHandlers() {
        httpHandlers.clear()
        httpHandlers.putAll(defaultHttpHandlers)
    }

    void cleanAfterSimulatorHandlers() {
        httpPostHandlers.clear()
        httpPostHandlers.putAll(defaultHttpPostHandlers)
    }

    void cleanMessageHandlers() {
        messageHandlers.clear()
    }

    void cleanSimulatorAndMessageHandlers() {
        cleanSimulatorHandlers()
        cleanAfterSimulatorHandlers()
        cleanMessageHandlers()
    }

    ZoneSpec zone(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ZoneSpec.class) Closure c)  {
        def zspec = new ZoneSpec(this)
        c.delegate = zspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        zones.add(zspec)
        addChild(zspec)
        return zspec
    }

    AccountSpec account(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = AccountSpec.class) Closure c) {
        def aspec = new AccountSpec(this)
        c.delegate = aspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(aspec)
        accounts.add(aspec)
        return aspec
    }

    InstanceOfferingSpec instanceOffering(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = InstanceOfferingSpec.class) Closure c) {
        def spec = new InstanceOfferingSpec(this)
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        return spec
    }

    BackupStorageSpec sftpBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = SftpBackupStorageSpec.class) Closure c) {
        def spec = new SftpBackupStorageSpec(this)
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.delegate = spec
        c()
        addChild(spec)
        return spec
    }

    BackupStorageSpec cephBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = CephBackupStorageSpec.class) Closure c) {
        def spec = new CephBackupStorageSpec(this)
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.delegate = spec
        c()
        addChild(spec)
        return spec
    }

    DiskOfferingSpec diskOffering(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = DiskOfferingSpec.class) Closure c) {
        def spec = new DiskOfferingSpec(this)
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        return spec
    }

    VmSpec vm(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = VmSpec.class) Closure c) {
        def spec = new VmSpec(this)
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        return spec
    }

    void adminLogin() {
        session = login(AccountConstant.INITIAL_SYSTEM_ADMIN_NAME, AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD)
    }

    SessionInventory login(String accountName, String password) {
        LogInByAccountAction a = new LogInByAccountAction()
        a.accountName = accountName
        a.password = password
        def res = a.call()
        assert res.error == null : "Login failure: ${JSONObjectUtil.toJsonString(res.error)}"
        return res.value.inventory
    }

    def specByUuid(String uuid) {
        return specsByUuid[uuid]
    }

    def specByName(String name) {
        return specsByName[name]
    }

    private void deploy() {
        def allNodes = []

        walk {
            if (it instanceof CreateAction) {
                it.preOperations.each { it() }
            }

            allNodes.add(it)
        }

        Set<Node> resolvedNodes = new LinkedHashSet<>()
        allNodes.each {
            resolveDependency(it as Node, resolvedNodes, [])
        }

        def names = resolvedNodes.collect { sn ->
            return sn.hasProperty("name") ? sn.name : sn.toString()
        }

        System.out.println("deploying path: ${names.join(" --> ")} ")

        resolvedNodes.each {
            if (!(it instanceof CreateAction)) {
                return
            }

            def uuid = Platform.getUuid()
            specsByUuid[uuid] = it

            def suuid = session.uuid
            if (it instanceof HasSession) {
                if (it.accountName != null) {
                    AccountSpec aspec = find(it.accountName, AccountSpec.class)
                    assert aspec != null: "cannot find the account[$it.accountName] defined in environment()"
                    suuid = aspec.session.uuid
                } else {
                    def n = it.parent
                    while (n != null) {
                        if (!(n instanceof HasSession) || n.accountName == null) {
                            n = n.parent
                        } else {
                            // one of the parent has the accountName set, use it
                            AccountSpec aspec = find(n.accountName, AccountSpec.class)
                            assert aspec != null: "cannot find the account[$n.accountName] defined in environment()"
                            suuid = aspec.session.uuid
                            break
                        }
                    }
                }
            }

            try {
                SpecID id = (it as CreateAction).create(uuid, suuid)
                if (id != null) {
                    specsByName[id.name] = it
                }
            } catch (Throwable t) {
                String name = null
                if (it.hasProperty("name")) {
                    name = it.name
                } else {
                    // the node doesn't have a name, use its parent name + its class name
                    Node n = it
                    while (n != null) {
                        if (n.hasProperty("name")) {
                            name = "${n.name}->${it.class.simpleName}"
                            break
                        }

                        n = n.parent
                    }
                }

                throw new Exception("failed to create a spec[name: $name, spec type: ${it.class.simpleName}], ${t.message}", t)
            }
        }

        allNodes.each {
            if (it instanceof CreateAction) {
                it.postOperations.each { it() }
            }
        }
    }

    EnvSpec create(Closure cl = null) {
        assert Test.currentEnvSpec == null: "There is another EnvSpec created but not deleted. There can be only one EnvSpec" +
                " in used, you must delete the previous one"

        hasCreated = true
        Test.currentEnvSpec = this

        adminLogin()
        deploy()

        defaultHttpHandlers = [:]
        defaultHttpHandlers.putAll(httpHandlers)
        defaultHttpPostHandlers = [:]
        defaultHttpPostHandlers.putAll(httpPostHandlers)

        if (cl != null) {
            cl.delegate = this
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
        }

        return this
    }

    private void makeSureAllEntitiesDeleted() {
        DatabaseFacade dbf = Test.componentLoader.getComponent(DatabaseFacade.class)
        def entityTypes = dbf.entityManager.metamodel.entities
        entityTypes.each { type ->
            if (type.name in ["ManagementNodeVO", "SessionVO",
                              "GlobalConfigVO", "AsyncRestVO",
                              "AccountVO", "NetworkServiceProviderVO",
                              "NetworkServiceTypeVO", "VmInstanceSequenceNumberVO"]) {
                // those tables will continue having entries during running a test suite
                return
            }

            long count = SQL.New("select count(*) from ${type.name}".toString(), Long.class).find()

            if (count > 0) {
                logger.fatal("[${this.class}] EnvSpec.delete() didn't cleanup the environment, there are still records in the database" +
                        " table ${type.name}, go fix it immediately!!! Abort the system")
                // abort the system
                System.exit(1)
            }
        }
    }

    void delete() {
        try {
            destroy(session.uuid)

            makeSureAllEntitiesDeleted()
        } finally {
            // set the currentEnvSpec to null anyway
            // because the current sub case may fail but
            // it should not effect the following cases
            Test.currentEnvSpec = null
        }
    }

    EnvSpec copy() {
        assert !hasCreated: "copy() can not be called after the create() is called"

        def n = new EnvSpec()
        InvokerHelper.setProperties(n, this.properties)
        return n
    }

    private void replyHttpCall(HttpEntity<String> entity, HttpServletResponse response, Object rsp) {
        String taskUuid = entity.getHeaders().getFirst(RESTConstant.TASK_UUID)
        if (taskUuid == null) {
            response.status = HttpStatus.OK.value()
            response.writer.write(rsp == null ? "" : JSONObjectUtil.toJsonString(rsp))
            return
        }

        String callbackUrl = entity.getHeaders().getFirst(RESTConstant.CALLBACK_URL)
        String rspBody = rsp == null ? "" : JSONObjectUtil.toJsonString(rsp)
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        headers.setContentLength(rspBody.length())
        headers.set(RESTConstant.TASK_UUID, taskUuid)
        HttpEntity<String> rreq = new HttpEntity<String>(rspBody, headers)
        restTemplate.exchange(callbackUrl, HttpMethod.POST, rreq, String.class)
    }

    void simulator(String path, Closure c) {
        httpHandlers[path] = c
    }

    void afterSimulator(String path, Closure c) {
        httpPostHandlers[path] = c
    }

    void handleSimulatorHttpRequests(HttpServletRequest req, HttpServletResponse rsp) {
        def url = req.getRequestURI()
        if (WebBeanConstructor.WEB_HOOK_PATH.toString().contains(url)) {
            ZSClient.webHookCallback(req, rsp)
            return
        }

        def handler = httpHandlers[url]
        if (handler == null) {
            rsp.sendError(HttpStatus.NOT_FOUND.value(), "no handler found for the path $url")
            return
        }

        StringBuilder sb = new StringBuilder()
        String line
        while ((line = req.getReader().readLine()) != null) {
            sb.append(line)
        }
        req.getReader().close()

        HttpHeaders header = new HttpHeaders()
        for (Enumeration e = req.getHeaderNames() ; e.hasMoreElements() ;) {
            String name = e.nextElement().toString()
            header.add(name, req.getHeader(name))
        }

        def entity = new HttpEntity<String>(sb.toString(), header)
        try {
            def ret
            if (handler.maximumNumberOfParameters == 0) {
                ret = handler()
            } else if (handler.maximumNumberOfParameters == 1) {
                ret = handler(entity)
            } else {
                ret = handler(entity, this)
            }

            Closure postHandler = httpPostHandlers[url]
            if (postHandler != null) {
                if (postHandler.maximumNumberOfParameters <= 1) {
                    ret = postHandler(ret)
                } else if (postHandler.maximumNumberOfParameters == 2) {
                    ret = postHandler(ret, entity)
                } else {
                    ret = postHandler(ret, entity, this)
                }
            }

            if (ret == null) {
                ret = [:]
            }

            replyHttpCall(entity, rsp, ret)
        } catch (HttpError he) {
            logger.warn("the simulator[$url] reports a http error[status code:${he.status}, message:${he.message}]", he)
            rsp.sendError(he.status, he.message)
        } catch (Throwable t) {
            logger.warn("error happened when handlign $url", t)
            rsp.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), t.message)
        }
    }

    void message(Class<? extends Message> msgClz, Closure condition, Closure handler) {
        messageHandlers[(msgClz)] = new Tuple(condition, handler)
    }

    void message(Class<? extends Message> msgClz, Closure handler) {
        message(msgClz, null, handler)
    }
}
