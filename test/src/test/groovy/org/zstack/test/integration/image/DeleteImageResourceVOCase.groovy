package org.zstack.test.integration.image

import org.zstack.core.db.Q
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.identity.AccountResourceRefVO_
import org.zstack.header.identity.SharedResourceVO
import org.zstack.header.identity.SharedResourceVO_
import org.zstack.sdk.ImageInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by MaJin on 2017-07-07.
 */
class DeleteImageResourceVOCase extends SubCase{
    EnvSpec env
    ImageInventory img
    String adminUuid = "36c27e8ff05c4780bf6d2fa65700f22e"

    @Override
    void setup() {
        useSpring(ImageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneSftpEnv
    }

    @Override
    void test() {
        env.create {
            testDeleteRecuvaImage()
            testDeleteExpungeImage()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testDeleteRecuvaImage(){
        img = env.inventoryByName("image") as ImageInventory

        shareResource {
            resourceUuids = [img.uuid]
            toPublic = true
        }

        deleteImage {
            uuid = img.uuid
        }
        assert Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.accountUuid, adminUuid)
                .eq(AccountResourceRefVO_.resourceUuid, img.uuid)
                .isExists()
        assert Q.New(SharedResourceVO.class)
                .eq(SharedResourceVO_.ownerAccountUuid, adminUuid)
                .eq(SharedResourceVO_.resourceUuid, img.uuid)
                .eq(SharedResourceVO_.toPublic, true)
                .isExists()

        recoverImage {
            imageUuid = img.uuid
        }

        assert Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.accountUuid, adminUuid)
                .eq(AccountResourceRefVO_.resourceUuid, img.uuid)
                .isExists()
        assert Q.New(SharedResourceVO.class)
                .eq(SharedResourceVO_.ownerAccountUuid, adminUuid)
                .eq(SharedResourceVO_.resourceUuid, img.uuid)
                .eq(SharedResourceVO_.toPublic, true)
                .isExists()
    }

    void testDeleteExpungeImage(){
        deleteImage {
            uuid = img.uuid
        }

        expungeImage {
            imageUuid = img.uuid
        }

        assert !Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.accountUuid, adminUuid)
                .eq(AccountResourceRefVO_.resourceUuid, img.uuid)
                .isExists()
        assert !Q.New(SharedResourceVO.class)
                .eq(SharedResourceVO_.ownerAccountUuid, adminUuid)
                .eq(SharedResourceVO_.resourceUuid, img.uuid)
                .eq(SharedResourceVO_.toPublic, true)
                .isExists()
    }
}
