package org.zstack.testlib

import org.codehaus.groovy.runtime.InvokerHelper
import org.springframework.http.*
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.CoreGlobalProperty
import org.zstack.core.Platform
import org.zstack.core.asyncbatch.While
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.SQL
import org.zstack.core.notification.NotificationVO
import org.zstack.header.core.NoErrorCompletion
import org.zstack.header.core.progress.TaskProgressVO
import org.zstack.header.identity.AccountConstant
import org.zstack.header.image.ImageDeletionPolicyManager
import org.zstack.header.message.Message
import org.zstack.header.rest.RESTConstant
import org.zstack.header.vm.VmInstanceDeletionPolicyManager
import org.zstack.header.vo.EO
import org.zstack.header.volume.VolumeDeletionPolicyManager
import org.zstack.image.ImageGlobalConfig
import org.zstack.sdk.AddCephBackupStorageAction
import org.zstack.sdk.AddCephPrimaryStorageAction
import org.zstack.sdk.AddCephPrimaryStoragePoolAction
import org.zstack.sdk.AddImageAction
import org.zstack.sdk.AddImageStoreBackupStorageAction
import org.zstack.sdk.AddIpRangeByNetworkCidrAction
import org.zstack.sdk.AddKVMHostAction
import org.zstack.sdk.AddLocalPrimaryStorageAction
import org.zstack.sdk.AddNfsPrimaryStorageAction
import org.zstack.sdk.AddSftpBackupStorageAction
import org.zstack.sdk.AddSharedMountPointPrimaryStorageAction
import org.zstack.sdk.ApiResult
import org.zstack.sdk.CreateAccountAction
import org.zstack.sdk.CreateBaremetalChessisAction
import org.zstack.sdk.CreateBaremetalHostCfgAction
import org.zstack.sdk.CreateBaremetalPxeServerAction
import org.zstack.sdk.CreateClusterAction
import org.zstack.sdk.CreateDataVolumeAction
import org.zstack.sdk.CreateDataVolumeFromVolumeSnapshotAction
import org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction
import org.zstack.sdk.CreateDataVolumeTemplateFromVolumeAction
import org.zstack.sdk.CreateDiskOfferingAction
import org.zstack.sdk.CreateEipAction
import org.zstack.sdk.CreateInstanceOfferingAction
import org.zstack.sdk.CreateL2NoVlanNetworkAction
import org.zstack.sdk.CreateL2VlanNetworkAction
import org.zstack.sdk.CreateL3NetworkAction
import org.zstack.sdk.CreateLoadBalancerAction
import org.zstack.sdk.CreatePolicyAction
import org.zstack.sdk.CreatePortForwardingRuleAction
import org.zstack.sdk.CreateRebootVmInstanceSchedulerAction
import org.zstack.sdk.CreateRootVolumeTemplateFromRootVolumeAction
import org.zstack.sdk.CreateSecurityGroupAction
import org.zstack.sdk.CreateStartVmInstanceSchedulerAction
import org.zstack.sdk.CreateStopVmInstanceSchedulerAction
import org.zstack.sdk.CreateUserAction
import org.zstack.sdk.CreateUserGroupAction
import org.zstack.sdk.CreateVipAction
import org.zstack.sdk.CreateVirtualRouterOfferingAction
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.CreateVolumeSnapshotAction
import org.zstack.sdk.CreateVolumeSnapshotSchedulerAction
import org.zstack.sdk.CreateWebhookAction
import org.zstack.sdk.CreateZoneAction
import org.zstack.sdk.DeleteAccountAction
import org.zstack.sdk.DeleteBackupStorageAction
import org.zstack.sdk.DeleteBaremetalChessisAction
import org.zstack.sdk.DeleteBaremetalHostCfgAction
import org.zstack.sdk.DeleteBaremetalPxeServerAction
import org.zstack.sdk.DeleteCephPrimaryStoragePoolAction
import org.zstack.sdk.DeleteClusterAction
import org.zstack.sdk.DeleteDataVolumeAction
import org.zstack.sdk.DeleteDiskOfferingAction
import org.zstack.sdk.DeleteEipAction
import org.zstack.sdk.DeleteHostAction
import org.zstack.sdk.DeleteImageAction
import org.zstack.sdk.DeleteInstanceOfferingAction
import org.zstack.sdk.DeleteIpRangeAction
import org.zstack.sdk.DeleteL2NetworkAction
import org.zstack.sdk.DeleteL3NetworkAction
import org.zstack.sdk.DeleteLoadBalancerAction
import org.zstack.sdk.DeletePolicyAction
import org.zstack.sdk.DeletePortForwardingRuleAction
import org.zstack.sdk.DeletePrimaryStorageAction
import org.zstack.sdk.DeleteSchedulerAction
import org.zstack.sdk.DeleteSecurityGroupAction
import org.zstack.sdk.DeleteUserAction
import org.zstack.sdk.DeleteUserGroupAction
import org.zstack.sdk.DeleteVipAction
import org.zstack.sdk.DeleteVolumeSnapshotAction
import org.zstack.sdk.DeleteWebhookAction
import org.zstack.sdk.DeleteZoneAction
import org.zstack.sdk.DestroyVmInstanceAction
import org.zstack.sdk.ErrorCode
import org.zstack.sdk.GlobalConfigInventory
import org.zstack.sdk.LogInByAccountAction
import org.zstack.sdk.QueryGlobalConfigAction
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.UpdateGlobalConfigAction
import org.zstack.sdk.ZSClient
import org.zstack.storage.volume.VolumeGlobalConfig
import org.zstack.utils.DebugUtils
import org.zstack.utils.gson.JSONObjectUtil

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
    protected ConcurrentHashMap<Class, List<Tuple>> messageHandlers = [:]
    private ConcurrentHashMap<Class, List<Tuple>> defaultMessageHandlers = [:]
    private static RestTemplate restTemplate
    private static Set<Class> simulatorClasses = Platform.reflections.getSubTypesOf(Simulator.class)

    static List deletionMethods = [
            [CreateZoneAction.metaClass, CreateZoneAction.Result.metaClass, DeleteZoneAction.class],
            [AddCephBackupStorageAction.metaClass, AddCephBackupStorageAction.Result.metaClass, DeleteBackupStorageAction.class],
            [AddCephPrimaryStorageAction.metaClass, AddCephPrimaryStorageAction.Result.metaClass, DeletePrimaryStorageAction.class],
            [AddCephPrimaryStoragePoolAction.metaClass, AddCephPrimaryStoragePoolAction.Result.metaClass, DeleteCephPrimaryStoragePoolAction.class],
            [CreateEipAction.metaClass, CreateEipAction.Result.metaClass, DeleteEipAction.class],
            [CreateClusterAction.metaClass, CreateClusterAction.Result.metaClass, DeleteClusterAction.class],
            [CreateDiskOfferingAction.metaClass, CreateDiskOfferingAction.Result.metaClass, DeleteDiskOfferingAction.class],
            [CreateInstanceOfferingAction.metaClass, CreateInstanceOfferingAction.Result.metaClass, DeleteInstanceOfferingAction.class],
            [CreateAccountAction.metaClass, CreateAccountAction.Result.metaClass, DeleteAccountAction.class],
            [CreatePolicyAction.metaClass, CreatePolicyAction.Result.metaClass, DeletePolicyAction.class],
            [CreateUserGroupAction.metaClass, CreateUserGroupAction.Result.metaClass, DeleteUserGroupAction.class],
            [CreateUserAction.metaClass, CreateUserAction.Result.metaClass, DeleteUserAction.class],
            [AddImageAction.metaClass, AddImageAction.Result.metaClass, DeleteImageAction.class],
            [CreateDataVolumeTemplateFromVolumeAction.metaClass, CreateDataVolumeTemplateFromVolumeAction.Result.metaClass, DeleteImageAction.class],
            [CreateRootVolumeTemplateFromRootVolumeAction.metaClass, CreateRootVolumeTemplateFromRootVolumeAction.Result.metaClass, DeleteImageAction.class],
            [CreateL2NoVlanNetworkAction.metaClass, CreateL2NoVlanNetworkAction.Result.metaClass, DeleteL2NetworkAction.class],
            [CreateL2VlanNetworkAction.metaClass, CreateL2VlanNetworkAction.Result.metaClass, DeleteL2NetworkAction.class],
            [AddIpRangeByNetworkCidrAction.metaClass, AddIpRangeByNetworkCidrAction.Result.metaClass, DeleteIpRangeAction.class],
            [CreateL3NetworkAction.metaClass, CreateL3NetworkAction.Result.metaClass, DeleteL3NetworkAction.class],
            [CreateRebootVmInstanceSchedulerAction.metaClass, CreateRebootVmInstanceSchedulerAction.Result.metaClass, DeleteSchedulerAction.class],
            [CreateStartVmInstanceSchedulerAction.metaClass, CreateStartVmInstanceSchedulerAction.Result.metaClass, DeleteSchedulerAction.class],
            [CreateStopVmInstanceSchedulerAction.metaClass, CreateStopVmInstanceSchedulerAction.Result.metaClass, DeleteSchedulerAction.class],
            [CreateVmInstanceAction.metaClass, CreateVmInstanceAction.Result.metaClass, DestroyVmInstanceAction.class],
            [CreateDataVolumeFromVolumeSnapshotAction.metaClass, CreateDataVolumeFromVolumeSnapshotAction.Result.metaClass, DeleteDataVolumeAction.class],
            [CreateDataVolumeFromVolumeTemplateAction.metaClass, CreateDataVolumeFromVolumeTemplateAction.Result.metaClass, DeleteDataVolumeAction.class],
            [CreateDataVolumeAction.metaClass, CreateDataVolumeAction.Result.metaClass, DeleteDataVolumeAction.class],
            [CreateVolumeSnapshotAction.metaClass, CreateVolumeSnapshotAction.Result.metaClass, DeleteVolumeSnapshotAction.class],
            [CreateVolumeSnapshotSchedulerAction.metaClass, CreateVolumeSnapshotSchedulerAction.Result.metaClass, DeleteSchedulerAction.class],
            [AddKVMHostAction.metaClass, AddKVMHostAction.Result.metaClass, DeleteHostAction.class],
            [CreateLoadBalancerAction.metaClass, CreateLoadBalancerAction.Result.metaClass, DeleteLoadBalancerAction.class],
            [AddLocalPrimaryStorageAction.metaClass, AddLocalPrimaryStorageAction.Result.metaClass, DeletePrimaryStorageAction.class],
            [AddImageStoreBackupStorageAction.metaClass, AddImageStoreBackupStorageAction.Result.metaClass, DeleteBackupStorageAction.class],
            [AddNfsPrimaryStorageAction.metaClass, AddNfsPrimaryStorageAction.Result.metaClass, DeletePrimaryStorageAction.class],
            [CreatePortForwardingRuleAction.metaClass, CreatePortForwardingRuleAction.Result.metaClass, DeletePortForwardingRuleAction.class],
            [CreateSecurityGroupAction.metaClass, CreateSecurityGroupAction.Result.metaClass, DeleteSecurityGroupAction.class],
            [AddSftpBackupStorageAction.metaClass, AddSftpBackupStorageAction.Result.metaClass, DeleteBackupStorageAction.class],
            [AddSharedMountPointPrimaryStorageAction.metaClass, AddSharedMountPointPrimaryStorageAction.Result.metaClass, DeletePrimaryStorageAction.class],
            [CreateVipAction.metaClass, CreateVipAction.Result.metaClass, DeleteVipAction.class],
            [CreateVirtualRouterOfferingAction.metaClass, CreateVirtualRouterOfferingAction.Result.metaClass, DeleteInstanceOfferingAction.class],
            [CreateWebhookAction.metaClass, CreateWebhookAction.Result.metaClass, DeleteWebhookAction.class],
            [CreateBaremetalPxeServerAction.metaClass, CreateBaremetalPxeServerAction.Result.metaClass, DeleteBaremetalPxeServerAction.class],
            [CreateBaremetalChessisAction.metaClass, CreateBaremetalChessisAction.Result.metaClass, DeleteBaremetalChessisAction.class],
            [CreateBaremetalHostCfgAction.metaClass, CreateBaremetalHostCfgAction.Result.metaClass, DeleteBaremetalHostCfgAction.class],
    ]

    static Closure GLOBAL_DELETE_HOOK

    protected List resourcesNeedDeletion = []

    private void installDeletionMethods() {
        deletionMethods.each { it ->
            def (actionMeta, resultMeta, deleteClass) = it

            actionMeta.call = {
                ApiResult res = ZSClient.call(delegate)
                def ret = delegate.makeResult(res)
                resourcesNeedDeletion.add(ret)
                return ret
            }

            resultMeta.delete = {
                if (delegate.error != null) {
                    return false
                }

                def action = (deleteClass as Class).newInstance()
                action.uuid = delegate.value.inventory.uuid
                action.sessionId = session.uuid
                def res = action.call()
                assert res.error == null: "API failure: ${JSONObjectUtil.toJsonString(res.error)}"
            }
        }
    }

    EnvSpec() {
        installDeletionMethods()

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory()
        factory.setReadTimeout(CoreGlobalProperty.REST_FACADE_READ_TIMEOUT)
        factory.setConnectTimeout(CoreGlobalProperty.REST_FACADE_CONNECT_TIMEOUT)
        restTemplate = new RestTemplate(factory)
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
        messageHandlers.putAll(defaultMessageHandlers)
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

    def inventoryByName(String name) {
        def spec = specByName(name)

        assert spec.hasProperty("inventory"): "${spec.class} doesn't have inventory"
        return spec.inventory
    }

    protected String retrieveSessionUuid(Node it) {
        String suuid = session.uuid

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

        return suuid
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


            def suuid = retrieveSessionUuid(it)

            try {
                def id
                logger.debug(String.format("create resource of class %s", it.getClass().getName()))
                id = (it as CreateAction).create(uuid, suuid) as SpecID

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

    void resetAllGlobalConfig() {
        def a = new QueryGlobalConfigAction()
        a.sessionId = session.uuid
        QueryGlobalConfigAction.Result res = a.call()
        assert res.error == null: res.error.toString()
        CountDownLatch latch = new CountDownLatch(1)
        List<ErrorCode> errors = []
        new While<GlobalConfigInventory>(res.value.inventories).all(new While.Do<GlobalConfigInventory>() {
            @Override
            void accept(GlobalConfigInventory config, NoErrorCompletion completion) {
                def ua = new UpdateGlobalConfigAction()
                ua.category = config.category
                ua.name = config.name
                ua.value = config.defaultValue
                ua.sessionId = session.uuid
                ua.call { UpdateGlobalConfigAction.Result r ->
                    if (r.error != null) {
                        errors.add(r.error)
                    }

                    completion.done()
                }
            }
        }).run(new NoErrorCompletion() {
            @Override
            void done() {
                latch.countDown()
            }
        })

        def ret = latch.await(1, TimeUnit.MINUTES)
        if (!ret) {
            DebugUtils.dumpAllThreads()
        }

        assert ret: "global configs not all updated after 1 minutes timeout"
        assert errors.isEmpty(): "some global configs fail to update, see ${errors.collect {it.toString()}}"
    }

    def recreate(String specName) {
        def spec = specByName(specName)
        assert spec != null: "cannot find the spec[name:$specName]"

        walkNode(spec) {
            if (!(it instanceof CreateAction)) {
                return
            }

            String uuid = Platform.getUuid()
            specsByUuid[uuid] = it

            SpecID id = it.create(uuid, retrieveSessionUuid(it as Node))
            if (id != null) {
                specsByName[id.name] = it
            }
        }

        return spec
    }

    EnvSpec create(Closure cl = null) {
        assert Test.currentEnvSpec == null: "There is another EnvSpec created but not deleted. There can be only one EnvSpec" +
                " in used, you must delete the previous one"

        hasCreated = true
        Test.currentEnvSpec = this

        adminLogin()
        resetAllGlobalConfig()

        simulatorClasses.each {
            Simulator sim = it.newInstance() as Simulator
            sim.registerSimulators(this)
        }

        deploy()

        defaultHttpHandlers = [:]
        defaultHttpHandlers.putAll(httpHandlers)
        defaultHttpPostHandlers = [:]
        defaultHttpPostHandlers.putAll(httpPostHandlers)
        defaultMessageHandlers = [:]
        defaultMessageHandlers.putAll(messageHandlers)

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
                              "NetworkServiceTypeVO", "VmInstanceSequenceNumberVO",
                              "GarbageCollectorVO",
                              "TaskProgressVO", "NotificationVO", "TaskStepVO",
                              "DataVolumeUsageVO", "RootVolumeUsageVO", "VmUsageVO", "ResourceVO","SecurityGroupSequenceNumberVO"]) {
                // those tables will continue having entries during running a test suite
                return
            }

            long count = SQL.New("select count(*) from ${type.name}".toString(), Long.class).find()

            if (count > 0) {
                def err = "[${Test.CURRENT_SUB_CASE != null ? Test.CURRENT_SUB_CASE.class : this.class}] EnvSpec.delete() didn't cleanup the environment, there are still records in the database" +
                        " table ${type.name}, go fix it immediately!!! Abort the system"
                logger.fatal(err)

                // abort the test suite
                throw new StopTestSuiteException()
            }
        }
    }

    private void cleanupEO() {
        DatabaseFacade dbf = Test.componentLoader.getComponent(DatabaseFacade.class)

        Platform.reflections.getTypesAnnotatedWith(EO.class).findAll { it.isAnnotationPresent(EO.class) }.each {
            dbf.eoCleanup(it)
        }
    }

    void delete() {
        try {
            ImageGlobalConfig.DELETION_POLICY.updateValue(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString())
            VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString())
            VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Direct.toString())

            cleanSimulatorAndMessageHandlers()

            destroy(session.uuid)

            resourcesNeedDeletion.each {
                logger.info("run delete() method on ${it.class}")
                it.delete()
            }

            SQL.New(NotificationVO.class).hardDelete()
            SQL.New(TaskProgressVO.class).hardDelete()

            if (GLOBAL_DELETE_HOOK != null) {
                GLOBAL_DELETE_HOOK()
            }

            cleanupEO()

            makeSureAllEntitiesDeleted()
        } catch (StopTestSuiteException e) {
            throw e
        } catch (Throwable t) {
            logger.fatal("an error happened when running EnvSpec.delete() for" +
                    " the case ${Test.CURRENT_SUB_CASE?.class}, we must stop the test suite, ${t.getMessage()}")
            throw new StopTestSuiteException()
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
            logger.warn("error happened when handling $url", t)
            rsp.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), t.message)
        }
    }

    void message(Class<? extends Message> msgClz, Closure condition, Closure handler) {
        def lst = messageHandlers[(msgClz)]
        if (lst == null) {
            lst = []
            messageHandlers[(msgClz)] = lst
        }

        lst.add(new Tuple(condition, handler))
    }

    void message(Class<? extends Message> msgClz, Closure handler) {
        message(msgClz, null, handler)
    }
}
