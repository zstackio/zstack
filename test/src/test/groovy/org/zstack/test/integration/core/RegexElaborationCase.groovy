package org.zstack.test.integration.core

import org.zstack.core.Platform
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorCodeList
import org.zstack.header.storage.primary.PrimaryStorageErrors
import org.zstack.header.vm.VmErrors
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.string.ElaborationSearchMethod

import static org.zstack.core.Platform.err

/**
 * Created by mingjian.deng on 2019/7/13.*/
class RegexElaborationCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
    }

    @Override
    void environment() {
        env = new EnvSpec()
    }

    @Override
    void test() {
        testElaboration1()
        testElaboration2()
        testElaboration3()
        testElaboration4()
        testElaboration5()
        testElaboration6()
        testElaboration7()
        testElaboration8()
        testElaboration7()
    }

    void testElaboration1() {
        def err = Platform.operr("Fn::Join must be array and contain 2 params, array[0] must be String, array[1] must be array!") as ErrorCode
        assert err.messages != null
        assert err.messages.method == ElaborationSearchMethod.regex
        assert err.messages.message_cn == "Fn::Join 后面的参数出错，该参数应包含 2 个参数，第一个为 String，第二个为数组。"
    }

    void testElaboration2() {
        def err = Platform.operr("Param [%s] has no value or default value found!", "ImageUuid") as ErrorCode
        assert err.messages != null
        assert err.messages.method == ElaborationSearchMethod.regex
        assert err.messages.method == ElaborationSearchMethod.regex
        assert err.messages.message_cn == "参数中的 [ImageUuid] 字段没有输入，也没有默认值。"
    }

    void testElaboration3() {
        def err = Platform.operr("no host having cpu[%s], memory[%s bytes] found", 4, 8589934592) as ErrorCode
        assert err.messages != null
        assert err.messages.method == ElaborationSearchMethod.regex
        assert err.messages.message_cn == "找不到合适的物理机来启动云主机，因为可以用于分配云主机的物理机都没有足够的资源：CPU [4]，内存 [8589934592 字节]。"
    }

    void testElaboration4() {
        def err = Platform.operr("no Connected hosts found in the [%s] candidate hosts having the hypervisor type [%s]", 4, "KVM") as ErrorCode
        assert err.messages != null
        assert err.messages.method == ElaborationSearchMethod.regex
        assert err.messages.message_cn == "找不到合适的物理机来启动云主机，因为满足分配条件的 4 个物理机都不是 KVM 的虚拟化类型。"
    }

    void testElaboration5() {
        def err = Platform.operr("no Connected hosts found in the [%s] candidate hosts", 2) as ErrorCode
        assert err.messages != null
        assert err.messages.method == ElaborationSearchMethod.regex
        assert err.messages.message_cn == "找不到合适的物理机来启动云主机，因为满足分配条件的 2 个物理机都不处于 Connected 状态。"
    }

    void testElaboration6() {
        def err = Platform.operr("shell command[sudo PYTHONPATH=/usr/local/zstack/ansible/files/zstacklib timeout 1800 python /usr/local/zstack/ansible/kvm.py -i /usr/local/zstack/ansible/hosts --private-key /usr/local/zstack/apache-tomcat-7.0.35/webapps/zstack/WEB-INF/classes/ansible/rsaKeys/id_rsa -e '{\"chrony_servers\":\"\",\"trusted_host\":\"\",\"remote_port\":\"22\",\"update_packages\":\"false\",\"zstack_root\":\"/var/lib/zstack\",\"remote_user\":\"root\",\"hostname\":\"10-0-121-175.zstack.org\",\"pkg_kvmagent\":\"kvmagent-3.2.0.tar.gz\",\"post_url\":\"http://172.20.11.235:8080/zstack/kvm/ansiblelog/%s\\n\",\"remote_pass\":\"******\",\"host\":\"172.20.11.235\",\"pip_url\":\"http://172.20.11.235:8080/zstack/static/pypi/simple\",\"zstack_repo\":\"\\\"zstack-mn,qemu-kvm-ev-mn\\\"\",\"yum_server\":\"172.20.11.235:8080\",\"pkg_zstacklib\":\"zstacklib-3.2.0.tar.gz\"}'] failed\n ret code: 1", Platform.uuid)
        assert err.elaboration != null
        assert err.messages.method == ElaborationSearchMethod.regex
        assert err.messages.message_cn == "pip 安装失败。可能因为 pip 安装文件不完整，或者 pip 版本不正确。"
    }

    void testElaboration7() {
        ErrorCode errorCodes = new ErrorCodeList()
        List<ErrorCode> causes = Collections.synchronizedList(new ArrayList<>())

        def errCode1 = Platform.operr("operation error, because:%s", ".*can not find vg .* and create vg with forceWipw=.*") as ErrorCode
        def errCode2 = Platform.operr("operation error, because:%s", ".*can not find vg .* and create vg with forceWipw=.*") as ErrorCode

        causes.add(errCode1)
        causes.add(errCode2)
        errorCodes.setCauses(causes)

        ErrorCode result = err(PrimaryStorageErrors.ATTACH_ERROR, errorCodes, errorCodes.getDetails())

        assert result.elaboration.trim().equals("错误信息: .*can not find vg .* and create vg with forceWipw=.*")
        assert result.messages.message_cn.trim().equals("无法将物理机上的共享块主存储加载到集群，因为存储可能断开连接或者共享块上存在原有数据，请检查存储连接状态，然后勾选清理块设备并重试。")
        assert result.messages.message_en.trim().equals("Could not attach shared block storage to cluster, because the storage may be disconnected or there may be existing data on the shared block. Please check the storage connection status and select the checkbox \"Clear LUN\" and try again.")

        errCode1 = Platform.operr("operation error, because:.*can not find vg .* and create vg with forceWipw=.*") as ErrorCode
        errCode2 = Platform.operr("operation error, because:.*can not find vg .* and create vg with forceWipw=.*") as ErrorCode

        causes.clear()
        causes.add(errCode1)
        causes.add(errCode2)
        errorCodes.setCauses(causes)

        result = err(PrimaryStorageErrors.ATTACH_ERROR, errorCodes, errorCodes.getDetails())
        assert result.elaboration.trim().equals("错误信息: 无法将物理机上的共享块主存储加载到集群，因为存储可能断开连接或者共享块上存在原有数据，请检查存储连接状态，然后勾选清理块设备并重试。")
        assert result.messages.message_cn.trim().equals("无法将物理机上的共享块主存储加载到集群，因为存储可能断开连接或者共享块上存在原有数据，请检查存储连接状态，然后勾选清理块设备并重试。")
        assert result.messages.message_en.trim().equals("Could not attach shared block storage to cluster, because the storage may be disconnected or there may be existing data on the shared block. Please check the storage connection status and select the checkbox \"Clear LUN\" and try again.")

    }

    /**
     * test error do not match wrong elaboration
     */
    void testElaboration8() {
        ErrorCode errorCode = Platform.operr("operation error, because:%s", "failed to execute bash")
        ErrorCode result = err(VmErrors.START_ERROR, errorCode, errorCode.getDetails())
        // current error do not match elaboration
        assert result.elaboration == null
    }
}
