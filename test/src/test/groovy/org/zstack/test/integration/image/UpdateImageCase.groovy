package org.zstack.test.integration.image

import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.image.ImageArchitecture
import org.zstack.header.image.ImageVO
import org.zstack.sdk.SystemTagInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.ImageSpec
import org.zstack.testlib.SubCase

/**
 * Created by xingwei.yu on 2020-11-12.
 */
class UpdateImageCase extends SubCase{
    EnvSpec env
    CloudBus bus

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneSftpEnv
    }

    @Override
    void test() {
        env.create {
            testSetImageBootMode()
            testUpdateImageBootMode()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testSetImageBootMode(){
        def thisImageUuid = (env.specByName("image") as ImageSpec).inventory.uuid
        updateImage {
            uuid = thisImageUuid
            architecture = "aarch64"
        }
        ImageVO vo = dbFindByUuid(thisImageUuid, ImageVO.class)
        assert vo.getArchitecture() == ImageArchitecture.aarch64.toString()

        def tags = querySystemTag {
            conditions = [
                    "resourceUuid=${thisImageUuid}",
                    "tag=bootMode::UEFI"
            ]
        } as List<SystemTagInventory>
        assert tags.size() == 1
    }

    void testUpdateImageBootMode(){

        def thisImageUuid = (env.specByName("image") as ImageSpec).inventory.uuid
        expect(AssertionError.class){
            setImageBootMode {
                uuid = thisImageUuid
                bootMode = "Legacy"
            }
        }

        def tags = querySystemTag {
            conditions = [
                    "resourceUuid=${thisImageUuid}",
                    "tag=bootMode::UEFI"
            ]
        } as List<SystemTagInventory>
        assert tags.size() == 1

    }
}
