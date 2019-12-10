package org.zstack.test.integration.core

import org.zstack.core.Platform
import org.zstack.header.errorcode.ErrorCode
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.string.ElaborationSearchMethod

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
    }

    void testElaboration1() {
        def err = Platform.operr("Fn::Join must be array and contain 2 params, array[0] must be String, array[1] must be array!") as ErrorCode
        assert err.messages != null
        assert err.messages.method == ElaborationSearchMethod.regex
        assert err.messages.message_cn == "Fn::Join后面的参数出错，该参数应包含2个参数，第一个为String,第二个为数组"
    }

    void testElaboration2() {
        def err = Platform.operr("Param [%s] has no value or default value found!", "ImageUuid") as ErrorCode
        assert err.messages != null
        assert err.messages.method == ElaborationSearchMethod.regex
        assert err.messages.method == ElaborationSearchMethod.regex
        assert err.messages.message_cn == "Parameter中的[ImageUuid]字段没有输入，并且也没有缺省值"
    }

    void testElaboration3() {
        def err = Platform.operr("no host having cpu[%s], memory[%s bytes] found", 4, 8589934592) as ErrorCode
        assert err.messages != null
        assert err.messages.method == ElaborationSearchMethod.regex
        assert err.messages.message_cn == "找不到合适的host来启动vm, 因为可以用于分配vm的host都没有足够的资源: cpu [4], 内存 [8589934592]"
    }

    void testElaboration4() {
        def err = Platform.operr("no Connected hosts found in the [%s] candidate hosts having the hypervisor type [%s]", 4, "KVM") as ErrorCode
        assert err.messages != null
        assert err.messages.method == ElaborationSearchMethod.regex
        assert err.messages.message_cn == "找不到合适的host来启动vm, 因为满足分配条件的4个hosts都不是KVM的虚拟化类型"
    }

    void testElaboration5() {
        def err = Platform.operr("no Connected hosts found in the [%s] candidate hosts", 2) as ErrorCode
        assert err.messages != null
        assert err.messages.method == ElaborationSearchMethod.regex
        assert err.messages.message_cn == "找不到合适的host来启动vm, 因为满足分配条件的2个hosts都不处于Connected状态"
    }

    void testElaboration6() {
        def err = Platform.operr("shell command[sudo PYTHONPATH=/usr/local/zstack/ansible/files/zstacklib timeout 1800 python /usr/local/zstack/ansible/kvm.py -i /usr/local/zstack/ansible/hosts --private-key /usr/local/zstack/apache-tomcat-7.0.35/webapps/zstack/WEB-INF/classes/ansible/rsaKeys/id_rsa -e '{\"chrony_servers\":\"\",\"trusted_host\":\"\",\"remote_port\":\"22\",\"update_packages\":\"false\",\"zstack_root\":\"/var/lib/zstack\",\"remote_user\":\"root\",\"hostname\":\"10-0-121-175.zstack.org\",\"pkg_kvmagent\":\"kvmagent-3.2.0.tar.gz\",\"post_url\":\"http://172.20.11.235:8080/zstack/kvm/ansiblelog/%s\\n\",\"remote_pass\":\"******\",\"host\":\"172.20.11.235\",\"pip_url\":\"http://172.20.11.235:8080/zstack/static/pypi/simple\",\"zstack_repo\":\"\\\"zstack-mn,qemu-kvm-ev-mn\\\"\",\"yum_server\":\"172.20.11.235:8080\",\"pkg_zstacklib\":\"zstacklib-3.2.0.tar.gz\"}'] failed\n ret code: 1", Platform.uuid)
        assert err.elaboration != null
        assert err.messages.method == ElaborationSearchMethod.regex
        assert err.messages.message_cn == "pip安装失败。因为pip安装文件不完整，或者pip版本不正确"
    }
}
