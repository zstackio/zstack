package org.zstack.test.integration.authentication.checkfile
import org.zstack.utils.Linux;
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.sdk.FileVerificationInventory

class FileVerificationCase extends SubCase{
    EnvSpec env

    private String fileUuid;

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
            testRemoveVerificationFile()
            testRecoverVerificationFile()
            testDeleteVerificationFile()
        }
    }

    void testAddVerificationFile(){
        addVerificationFile{
            path=FileVerificationForTest.LOCALFILE
            node=FileVerificationForTest.LOCALNODE
            hexType=FileVerificationForTest.DEFAULTHEXTYPE
            category=FileVerificationForTest.DEFAULTCATEGORY
        }
        FileVerificationInventory fvi = queryVerificationFile {
            conditions = ["path=${FileVerificationForTest.LOCALFILE}", "node=${FileVerificationForTest.LOCALNODE}", "hexType=${FileVerificationForTest.DEFAULTHEXTYPE}"]
        }[0]
        fileUuid = fvi.getUuid()
        assert fvi != null
        assert fvi.state == "Enabled"
        assert fvi.path == FileVerificationForTest.LOCALFILE
        assert fvi.node == FileVerificationForTest.LOCALNODE
        assert fvi.hexType == FileVerificationForTest.DEFAULTHEXTYPE
        assert fvi.category == FileVerificationForTest.DEFAULTCATEGORY

    }


    void testRemoveVerificationFile(){
        removeVerificationFile{
            uuid=fileUuid
        }
        FileVerificationInventory fvi = queryVerificationFile {}[0]
        assert fvi.state == "Disabled"
    }

    void testRecoverVerificationFile(){
        recoverVerificationFile{
            uuid=fileUuid
        }
        FileVerificationInventory fvi = queryVerificationFile {}[0]
        assert fvi.state == "Enabled"
    }

    void testDeleteVerificationFile(){
        deleteVerificationFile{
            uuid=fileUuid
        }
        FileVerificationInventory fvi = queryVerificationFile {
            conditions = ["uuid=${fileUuid}"]
        }[0]
        assert fvi == null
    }

}
