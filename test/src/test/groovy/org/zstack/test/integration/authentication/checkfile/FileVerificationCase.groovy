package org.zstack.test.integration.authentication.checkfile
import org.zstack.utils.Linux;
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.sdk.FileVerificationInventory
class FileVerificationCase extends SubCase{
    EnvSpec env


    @Override
    void clean() {
        env.delete()
        Linux.shell("rm -rf ${FileVerificationForTest.LOCALFILE}")
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = "zone"
            }
        }
        Linux.shell("touch ${FileVerificationForTest.LOCALFILE}")
    }
    @Override
    void test() {
        env.create {
            testAddVerificationFile()
            testDeleteVerificationFile()
        }
    }

    void testAddVerificationFile(){
        addVerificationFile{
            path=FileVerificationForTest.LOCALFILE
            node=FileVerificationForTest.LOCALNODE
            type=FileVerificationForTest.DEFAULTTYPE
        }
        FileVerificationInventory fvi = queryVerificationFile {
            conditions = ["path=${FileVerificationForTest.LOCALFILE}", "node=${FileVerificationForTest.LOCALNODE}", "type=${FileVerificationForTest.DEFAULTTYPE}"]
        }[0]
        assert fvi != null

    }

    void testDeleteVerificationFile(){
        deleteVerificationFile{
            path=FileVerificationForTest.LOCALFILE
            node=FileVerificationForTest.LOCALNODE
        }
        FileVerificationInventory fvi = queryVerificationFile {
            conditions = ["path=${FileVerificationForTest.LOCALFILE}", "node=${FileVerificationForTest.LOCALNODE}"]
        }[0]
        assert fvi == null
    }

}
