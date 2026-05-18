package org.zstack.test.integration.kvm.host

import org.zstack.core.CoreGlobalProperty
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.header.core.Completion
import org.zstack.header.core.workflow.Flow
import org.zstack.header.core.workflow.FlowTrigger
import org.zstack.header.core.workflow.NoRollbackFlow
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.host.ConnectHostInfo
import org.zstack.header.host.ReconnectHostMsg
import org.zstack.header.host.ReconnectHostReply
import org.zstack.kvm.KVMGlobalProperty
import org.zstack.kvm.KVMHost
import org.zstack.kvm.KVMHostConnectExtensionPoint
import org.zstack.kvm.KVMHostConnectedContext
import org.zstack.kvm.KVMHostFactory
import org.zstack.kvm.KVMHostVO
import org.zstack.kvm.KVMHostVO_
import org.zstack.sdk.HostInventory
import org.zstack.sdk.KVMHostInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.ssh.SshResult
import org.zstack.utils.ssh.SshShell

import java.lang.reflect.InvocationTargetException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class KVMHostRequirettySudoCase extends SubCase {
    EnvSpec env
    HostInventory host
    KVMHostFactory factory
    final List<String> commandsWithoutPty = Collections.synchronizedList(new ArrayList<String>())
    final List<String> commandsWithPty = Collections.synchronizedList(new ArrayList<String>())

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.noVmEnv()
    }

    @Override
    void test() {
        env.create {
            host = env.inventoryByName("kvm")
            factory = bean(KVMHostFactory.class)
            env.mockFactory(SshShell.class) { SshShell shell ->
                return new RecordingSshShell(commandsWithoutPty, commandsWithPty)
            }

            testRequirettySudoCommandsUsePseudoTty()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testRequirettySudoCommandsUsePseudoTty() {
        KVMHostInventory updated = updateKVMHost {
            uuid = host.uuid
            username = "zstac77375"
            password = "password"
            sshPort = 22
        } as KVMHostInventory
        assert updated.username == "zstac77375"

        KVMHost kvmHost = getKvmHost()
        testRestartKvmAgent(kvmHost)
        testDeleteTakeOverFlag(kvmHost)
        testWriteTakeOverFlag(kvmHost)

        assert commandsWithPty.contains("sudo service zstack-kvmagent restart")
        assert commandsWithPty.any { it == String.format("sudo /bin/sh -c \"rm -rf %s\"", KVMGlobalProperty.TAKEVOERFLAGPATH) }
        assert commandsWithPty.any { it == String.format("sudo /bin/sh -c \"echo uuid:%s > %s\"", host.uuid, KVMGlobalProperty.TAKEVOERFLAGPATH) }

        assert !commandsWithoutPty.contains("sudo service zstack-kvmagent restart")
        assert !commandsWithoutPty.any { it.startsWith("sudo /bin/sh -c \"rm -rf ") }
        assert !commandsWithoutPty.any { it.startsWith("sudo /bin/sh -c \"echo uuid:") }
    }

    void testRestartKvmAgent(KVMHost kvmHost) {
        env.message(ReconnectHostMsg.class) { ReconnectHostMsg msg, CloudBus bus ->
            ReconnectHostReply reply = new ReconnectHostReply()
            bus.reply(msg, reply)
        }

        ErrorCode error
        try {
            error = runWithUnitTestOff { Completion completion ->
                invokeKVMHost(kvmHost, "restartKvmAgentOnHost", [Boolean.TYPE, Completion.class] as Class[], [true, completion] as Object[])
            }
        } finally {
            env.cleanMessageHandlers()
        }

        assert error == null
    }

    void testDeleteTakeOverFlag(KVMHost kvmHost) {
        ErrorCode error = runWithUnitTestOff { Completion completion ->
            invokeKVMHost(kvmHost, "deleteTakeOverFlag", [Completion.class] as Class[], [completion] as Object[])
        }

        assert error == null
    }

    void testWriteTakeOverFlag(KVMHost kvmHost) {
        ConnectHostInfo info = new ConnectHostInfo()
        info.setNewAdded(false)

        List extensions = new ArrayList(factory.getConnectExtensions())
        factory.getConnectExtensions().clear()
        factory.getConnectExtensions().add(noOpConnectExtension())
        try {
            ErrorCode error = runWithUnitTestOff { Completion completion ->
                invokeKVMHost(kvmHost, "continueConnect", [ConnectHostInfo.class, Completion.class] as Class[], [info, completion] as Object[])
            }

            assert error == null
        } finally {
            factory.getConnectExtensions().clear()
            factory.getConnectExtensions().addAll(extensions)
        }
    }

    static KVMHostConnectExtensionPoint noOpConnectExtension() {
        return new KVMHostConnectExtensionPoint() {
            @Override
            Flow createKvmHostConnectingFlow(KVMHostConnectedContext context) {
                return new NoRollbackFlow() {
                    String __name__ = "no-op kvm host connect extension"

                    @Override
                    void run(FlowTrigger trigger, Map data) {
                        trigger.next()
                    }
                }
            }
        }
    }

    KVMHost getKvmHost() {
        KVMHostVO vo = Q.New(KVMHostVO.class).eq(KVMHostVO_.uuid, host.uuid).find()
        return factory.getHost(vo) as KVMHost
    }

    ErrorCode runWithUnitTestOff(Closure caller) {
        boolean oldUnitTestOn = CoreGlobalProperty.UNIT_TEST_ON
        CountDownLatch latch = new CountDownLatch(1)
        ErrorCode[] error = new ErrorCode[1]
        Throwable[] thrown = new Throwable[1]

        Completion completion = new Completion(null) {
            @Override
            void success() {
                latch.countDown()
            }

            @Override
            void fail(ErrorCode errorCode) {
                error[0] = errorCode
                latch.countDown()
            }
        }

        boolean completed
        CoreGlobalProperty.UNIT_TEST_ON = false
        try {
            caller.call(completion)
            completed = latch.await(60, TimeUnit.SECONDS)
        } catch (Throwable t) {
            thrown[0] = t
            latch.countDown()
            completed = true
        } finally {
            CoreGlobalProperty.UNIT_TEST_ON = oldUnitTestOn
        }

        assert completed
        if (thrown[0] != null) {
            throw thrown[0]
        }

        return error[0]
    }

    static void invokeKVMHost(KVMHost kvmHost, String methodName, Class[] parameterTypes, Object[] args) {
        def method = KVMHost.class.getDeclaredMethod(methodName, parameterTypes)
        method.setAccessible(true)
        try {
            method.invoke(kvmHost, args)
        } catch (InvocationTargetException e) {
            throw e.getTargetException()
        }
    }

    static class RecordingSshShell extends SshShell {
        final List<String> commandsWithoutPty
        final List<String> commandsWithPty

        RecordingSshShell(List<String> commandsWithoutPty, List<String> commandsWithPty) {
            this.commandsWithoutPty = commandsWithoutPty
            this.commandsWithPty = commandsWithPty
        }

        @Override
        SshResult runCommand(String cmd) {
            commandsWithoutPty.add(cmd)
            return result(cmd, false)
        }

        @Override
        SshResult runCommandWithPseudoTty(String cmd) {
            commandsWithPty.add(cmd)
            return result(cmd, true)
        }

        SshResult result(String cmd, boolean withPty) {
            SshResult ret = new SshResult()
            ret.setCommandToExecute(cmd)
            ret.setReturnCode((isRequirettySudoCommand(cmd) && !withPty) ? 1 : 0)
            ret.setStdout(cmd == "uname -m" ? "x86_64\n" : "")
            ret.setStderr(ret.getReturnCode() == 0 ? "" : "sudo: sorry, you must have a tty to run sudo")
            ret.setExitErrorMessage(ret.getStderr())
            return ret
        }

        boolean isRequirettySudoCommand(String cmd) {
            return cmd == "sudo service zstack-kvmagent restart" ||
                    cmd.startsWith("sudo /bin/sh -c \"rm -rf ") ||
                    cmd.startsWith("sudo /bin/sh -c \"echo uuid:")
        }
    }
}
