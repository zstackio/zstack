package org.zstack.test.integration.storage.volume

import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.header.image.ImageConstant
import org.zstack.header.storage.primary.DownloadDataVolumeToPrimaryStorageMsg
import org.zstack.header.storage.primary.DownloadDataVolumeToPrimaryStorageReply
import org.zstack.header.volume.VolumeVO
import org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import static org.zstack.core.Platform.operr

/**
 * Created by MaJin on 2017-08-15.
 */
class CreateDataVolumeFromTemplateCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url = "http://zstack.org/download/test.qcow2"
                    mediaType = ImageConstant.ImageMediaType.DataVolumeTemplate.toString()
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                        totalCpu = 40
                        totalMem = SizeUnit.GIGABYTE.toByte(320)
                    }

                    attachPrimaryStorage("local")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testRollbackWhenCreateFail()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testRollbackWhenCreateFail(){
        ImageInventory image = env.inventoryByName("image1") as ImageInventory
        PrimaryStorageInventory ps = env.inventoryByName("local") as PrimaryStorageInventory

        env.message(DownloadDataVolumeToPrimaryStorageMsg.class){DownloadDataVolumeToPrimaryStorageMsg msg, CloudBus bus ->
            def reply = new DownloadDataVolumeToPrimaryStorageReply()
            reply.setError(operr("on purpose"))
            bus.reply(msg, reply)
        }

        def a = new CreateDataVolumeFromVolumeTemplateAction()
        a.name = "test"
        a.imageUuid = image.uuid
        a.primaryStorageUuid = ps.uuid
        a.sessionId = currentEnvSpec.session.uuid

        assert a.call().error != null

        assert Q.New(VolumeVO.class).count() == 0L
    }
}
