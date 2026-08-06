package org.zstack.test.integration.core.ansible

import org.junit.Test
import org.zstack.core.ansible.AnsibleGlobalProperty
import org.zstack.core.ansible.AnsibleChecker
import org.zstack.core.ansible.AnsibleFacade
import org.zstack.core.ansible.AnsibleFacadeImpl
import org.zstack.core.ansible.AnsibleBasicArguments
import org.zstack.core.ansible.AnsibleRunner
import org.zstack.core.ansible.CallBackNetworkChecker
import org.zstack.core.ansible.PrepareAnsible
import org.zstack.core.ansible.RunAnsibleMsg
import org.zstack.core.CoreGlobalProperty
import org.zstack.core.Platform
import org.zstack.core.aspect.AsyncSafeAspect
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.core.componentloader.ComponentLoader
import org.zstack.core.errorcode.ErrorFacade
import org.zstack.core.rest.RESTFacadeImpl
import org.zstack.header.core.ReturnValueCompletion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.message.NeedReplyMessage
import org.zstack.header.message.MessageReply
import org.zstack.header.rest.RESTFacade
import org.zstack.kvm.KVMHost

import java.lang.reflect.Field
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicInteger

class AnsibleRunnerTargetAwareArgumentsTest {
    private static final String IPV4 = "192.168.1.10"
    private static final String IPV6 = "2001:db8::1"
    private static final String IPV4_TARGET = "192.168.1.2"
    private static final String IPV6_TARGET = "2001:db8::2"
    private static final String HOST_UUID = "11111111111111111111111111111111"
    private static final int REST_PORT = 8080

    @Test
    void test() {
        testTargetAwareEndpointArgumentsUseTheSelectedIpv6Node()
        testTargetAwareEndpointArgumentsUseTheSelectedIpv4Node()
        testMissingFamilyFailsBeforeRunAnsibleDispatch()
        testMissingFamilyFailsBeforeChecker()
        testHostnameUsesResolvedTargetAndMatchingCallbackIp()
        testAnsibleFacadeKeepsRunnerSelectedManagementNodeAddress()
        testAnsibleLogCallbackUrlUsesTheSelectedManagementNodeAddress()
    }

    void testTargetAwareEndpointArgumentsUseTheSelectedIpv6Node() {
        withManagementServerIpProperties([
                "management.server.ip" : IPV4,
                "management.server.ip6": IPV6,
        ]) {
            withTemporaryAnsibleInventory {
                AtomicReference<RunAnsibleMsg> received = new AtomicReference<>()
                AtomicReference<Boolean> result = new AtomicReference<>()
                AtomicReference<ErrorCode> error = new AtomicReference<>()
                CallBackNetworkChecker checker = new CallBackNetworkChecker()

                runAnsible(IPV6_TARGET, received, result, error, checker)

                assert result.get() == true
                assert error.get() == null
                assert received.get() != null
                def arguments = received.get().deployArguments
                assert arguments.pipUrl == "http://[${IPV6}]:${REST_PORT}/zstack/static/pypi/simple"
                assert arguments.yumServer == "[${IPV6}]:${REST_PORT}"
                assert arguments.trustedHost == IPV6
                assert checker.callbackIp == IPV6
                assert received.get().targetIp == IPV6_TARGET
            }
        }
    }

    void testTargetAwareEndpointArgumentsUseTheSelectedIpv4Node() {
        withManagementServerIpProperties([
                "management.server.ip" : IPV6,
                "management.server.ip4": IPV4,
        ]) {
            withTemporaryAnsibleInventory {
                AtomicReference<RunAnsibleMsg> received = new AtomicReference<>()
                AtomicReference<Boolean> result = new AtomicReference<>()
                AtomicReference<ErrorCode> error = new AtomicReference<>()
                CallBackNetworkChecker checker = new CallBackNetworkChecker()

                runAnsible(IPV4_TARGET, received, result, error, checker)

                assert result.get() == true
                assert error.get() == null
                assert received.get() != null
                def arguments = received.get().deployArguments
                assert arguments.pipUrl == "http://${IPV4}:${REST_PORT}/zstack/static/pypi/simple"
                assert arguments.yumServer == "${IPV4}:${REST_PORT}"
                assert arguments.trustedHost == IPV4
                assert checker.callbackIp == IPV4
                assert received.get().targetIp == IPV4_TARGET
            }
        }
    }

    void testMissingFamilyFailsBeforeRunAnsibleDispatch() {
        withManagementServerIpProperties([
                "management.server.ip": IPV4,
        ]) {
            AtomicReference<RunAnsibleMsg> received = new AtomicReference<>()
            AtomicReference<Boolean> result = new AtomicReference<>()
            AtomicReference<ErrorCode> error = new AtomicReference<>()

            runAnsible(IPV6_TARGET, received, result, error)

            assert result.get() == null
            assert error.get()?.globalErrorCode == "ORG_ZSTACK_CORE_PLATFORM_10001"
            assert received.get() == null
        }
    }

    void testMissingFamilyFailsBeforeChecker() {
        withManagementServerIpProperties([
                "management.server.ip": IPV4,
        ]) {
            AtomicReference<ErrorCode> error = new AtomicReference<>()
            AtomicInteger checkerCalls = new AtomicInteger()
            AnsibleRunner runner = new AnsibleRunner()
            setField(runner, "restf", [
                    getBaseUrl : { String.format("http://127.0.0.1:%d", REST_PORT) },
                    getHostName: { "fallback-management-host" as String },
            ] as RESTFacade)
            setField(runner, "asf", [
                    isModuleChanged: { String ignored -> false },
            ] as AnsibleFacade)
            runner.targetIp = IPV6_TARGET
            runner.playBookName = "imagestorebackupstorage.py"
            runner.installChecker([
                    stopAnsible  : { checkerCalls.incrementAndGet(); null },
                    needDeploy   : { checkerCalls.incrementAndGet(); false },
                    deleteDestFile: { },
            ] as AnsibleChecker)

            withErrorFacade {
                runner.run(new ReturnValueCompletion<Boolean>(null) {
                    @Override
                    void success(Boolean returnValue) {
                    }

                    @Override
                    void fail(ErrorCode errorCode) {
                        error.set(errorCode)
                    }
                })
            }

            assert checkerCalls.get() == 0
            assert error.get()?.globalErrorCode == "ORG_ZSTACK_CORE_PLATFORM_10001"
        }
    }

    void testHostnameUsesResolvedTargetAndMatchingCallbackIp() {
        withManagementServerIpProperties([
                "management.server.ip": IPV4,
        ]) {
            AtomicReference<Boolean> result = new AtomicReference<>()
            CallBackNetworkChecker checker = new CallBackNetworkChecker()
            AnsibleRunner runner = new AnsibleRunner()
            setField(runner, "restf", [
                    getBaseUrl : { String.format("http://127.0.0.1:%d", REST_PORT) },
                    getHostName: { "fallback-management-host" as String },
            ] as RESTFacade)
            setField(runner, "bus", [
                    makeTargetServiceIdByResourceUuid: { NeedReplyMessage msg, String serviceId, String resourceUuid -> },
                    send                              : { NeedReplyMessage msg, CloudBusCallBack callback ->
                        callback.run(new MessageReply())
                        return null
                    }
            ] as CloudBus)
            boolean oldUnitTestOn = CoreGlobalProperty.UNIT_TEST_ON
            try {
                CoreGlobalProperty.UNIT_TEST_ON = true
                runner.forceRun = true
                runner.targetIp = "localhost"
                runner.targetUuid = Platform.uuid
                runner.playBookName = "kvm.yml"
                runner.username = "root"
                runner.password = "password"
                runner.installChecker(checker)
                runner.run(new ReturnValueCompletion<Boolean>(null) {
                    @Override
                    void success(Boolean returnValue) {
                        result.set(returnValue)
                    }

                    @Override
                    void fail(ErrorCode errorCode) {
                        assert false: errorCode
                    }
                })
            } finally {
                CoreGlobalProperty.UNIT_TEST_ON = oldUnitTestOn
            }

            assert result.get() == true
            assert checker.callbackIp == IPV4
        }
    }

    void testAnsibleFacadeKeepsRunnerSelectedManagementNodeAddress() {
        withManagementServerIpProperties([
                "management.server.ip" : IPV4,
                "management.server.ip6": IPV6,
        ]) {
            AnsibleBasicArguments deployArguments = new AnsibleBasicArguments()
            deployArguments.trustedHost = IPV6
            deployArguments.remoteUser = "root"
            deployArguments.remotePass = "password"
            deployArguments.remotePort = "22"
            RunAnsibleMsg msg = new RunAnsibleMsg()
            msg.targetIp = IPV6_TARGET
            msg.playBookPath = "kvm.yml"
            msg.deployArguments = deployArguments

            def method = AnsibleFacadeImpl.getDeclaredMethod("collectArguments", RunAnsibleMsg.class)
            method.accessible = true
            Map<String, Object> arguments = method.invoke(new AnsibleFacadeImpl(), msg) as Map<String, Object>

            assert arguments.mn_ip == IPV6
        }
    }

    void testAnsibleLogCallbackUrlUsesTheSelectedManagementNodeAddress() {
        RESTFacade restf = [
                buildBaseUrl: { String host -> RESTFacadeImpl.buildBaseUrl(host, REST_PORT, "zstack") },
        ] as RESTFacade
        String ipv4Url = String.format("http://%s:%d/zstack/kvm/ansiblelog/%s", IPV4, REST_PORT, HOST_UUID)
        String ipv6Url = String.format("http://[%s]:%d/zstack/kvm/ansiblelog/%s", IPV6, REST_PORT, HOST_UUID)

        assert KVMHost.buildAnsibleLogCallbackUrl(HOST_UUID, IPV4, restf).equals(ipv4Url)
        assert KVMHost.buildAnsibleLogCallbackUrl(HOST_UUID, IPV6, restf).equals(ipv6Url)
    }

    private void runAnsible(String targetIp, AtomicReference<RunAnsibleMsg> received,
                            AtomicReference<Boolean> result, AtomicReference<ErrorCode> error,
                            AnsibleChecker... checkers) {
        AnsibleRunner runner = new AnsibleRunner()
        setField(runner, "restf", [
                getBaseUrl : { String.format("http://127.0.0.1:%d", REST_PORT) },
                getHostName: { "fallback-management-host" as String },
        ] as RESTFacade)
        setField(runner, "bus", [
                makeTargetServiceIdByResourceUuid: { NeedReplyMessage msg, String serviceId, String resourceUuid -> },
                send                              : { NeedReplyMessage msg, CloudBusCallBack callback ->
                    received.set(msg as RunAnsibleMsg)
                    callback.run(new MessageReply())
                    return null
                }
        ] as CloudBus)

        boolean oldUnitTestOn = CoreGlobalProperty.UNIT_TEST_ON
        try {
            CoreGlobalProperty.UNIT_TEST_ON = true
            runner.forceRun = true
            runner.targetIp = targetIp
            runner.targetUuid = Platform.uuid
            runner.playBookName = "kvm.yml"
            runner.username = "root"
            runner.password = "password"
            checkers.each { runner.installChecker(it) }
            withErrorFacade {
                runner.run(new ReturnValueCompletion<Boolean>(null) {
                    @Override
                    void success(Boolean returnValue) {
                        result.set(returnValue)
                    }

                    @Override
                    void fail(ErrorCode errorCode) {
                        error.set(errorCode)
                    }
                })
            }
        } finally {
            CoreGlobalProperty.UNIT_TEST_ON = oldUnitTestOn
        }
    }

    private void withManagementServerIpProperties(Map<String, String> properties, Closure closure) {
        List<String> managedKeys = [
                "management.server.ip",
                "management.server.ip4",
                "management.server.ip6",
        ]
        Map<String, String> oldValues = [:]
        managedKeys.each { key -> oldValues[key] = System.getProperty(key) }

        try {
            resetCachedManagementServerIp()
            managedKeys.each { key -> System.clearProperty(key) }
            properties.each { key, value -> System.setProperty(key, value) }
            closure.call()
        } finally {
            managedKeys.each { key ->
                if (oldValues[key] == null) {
                    System.clearProperty(key)
                } else {
                    System.setProperty(key, oldValues[key])
                }
            }
            resetCachedManagementServerIp()
        }
    }

    private void withTemporaryAnsibleInventory(Closure closure) {
        Field hostsFileField = PrepareAnsible.class.getDeclaredField("hostsFile")
        Field hostIpsField = PrepareAnsible.class.getDeclaredField("hostIPs")
        hostsFileField.setAccessible(true)
        hostIpsField.setAccessible(true)
        File oldHostsFile = hostsFileField.get(null) as File
        List<String> oldHostIps = hostIpsField.get(null) as List<String>
        boolean oldKeepHostsFileInMemory = AnsibleGlobalProperty.KEEP_HOSTS_FILE_IN_MEMORY
        File temporaryHostsFile = File.createTempFile("zstack-ansible-hosts", ".tmp")

        try {
            hostsFileField.set(null, temporaryHostsFile)
            hostIpsField.set(null, [])
            AnsibleGlobalProperty.KEEP_HOSTS_FILE_IN_MEMORY = true
            closure.call()
        } finally {
            AnsibleGlobalProperty.KEEP_HOSTS_FILE_IN_MEMORY = oldKeepHostsFileInMemory
            hostsFileField.set(null, oldHostsFile)
            hostIpsField.set(null, oldHostIps)
            temporaryHostsFile.delete()
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        Field field = AnsibleRunner.class.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(target, value)
    }

    private void resetCachedManagementServerIp() {
        Field field = Platform.class.getDeclaredField("managementServerIp")
        field.setAccessible(true)
        field.set(null, null)
    }

    private void withErrorFacade(Closure closure) {
        ErrorFacade errorFacade = [
                instantiateErrorCode : { Enum code, String details, ErrorCode cause -> new ErrorCode(code.name(), "test", details) },
                stringToInternalError: { String details -> new ErrorCode("TEST", "test", details) }
        ] as ErrorFacade

        Field loaderField = Platform.class.getDeclaredField("loader")
        loaderField.setAccessible(true)
        Object oldLoader = loaderField.get(null)

        AsyncSafeAspect aspect = AsyncSafeAspect.aspectOf()
        Field errorFacadeField = AsyncSafeAspect.class.getDeclaredField("errf")
        errorFacadeField.setAccessible(true)
        ErrorFacade oldErrorFacade = errorFacadeField.get(aspect) as ErrorFacade
        try {
            loaderField.set(null, [
                    getComponent: { Class componentClass -> componentClass == ErrorFacade.class ? errorFacade : null }
            ] as ComponentLoader)
            errorFacadeField.set(aspect, errorFacade)
            closure.call()
        } finally {
            errorFacadeField.set(aspect, oldErrorFacade)
            loaderField.set(null, oldLoader)
        }
    }
}
