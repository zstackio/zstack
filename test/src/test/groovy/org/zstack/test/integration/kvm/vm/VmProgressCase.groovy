package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.springframework.web.util.UriComponentsBuilder
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.progress.ProgressCommands
import org.zstack.core.progress.ProgressGlobalConfig
import org.zstack.core.progress.ProgressReportService
import org.zstack.header.core.progress.ProgressConstants
import org.zstack.header.core.progress.TaskProgressVO
import org.zstack.header.core.progress.TaskProgressVO_
import org.zstack.header.core.progress.TaskType
import org.zstack.header.rest.RESTConstant
import org.zstack.header.rest.RESTFacade
import org.zstack.sdk.*
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.ApiPathTracker
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.FuncTrigger
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by xing5 on 2017/3/23.
 */
class VmProgressCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.noVmEnv()
    }

    void testProgressDeleteAfterApiDone() {
        bean(ProgressReportService.class).setDELETE_DELAY(1)

        String apiUuid = Platform.getUuid()
        createVmInstance {
            apiId = apiUuid
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [env.inventoryByName("l3").uuid]
            name = "vm"
        }

        retryInSecs {
            assert !Q.New(TaskProgressVO.class).eq(TaskProgressVO_.apiId, apiUuid).isExists()
        }
    }

    void testProgressTTL() {
        // set DELETE_DELAY to a very big value so the progress entries won't be deleted
        // after API completes
        bean(ProgressReportService.class).setDELETE_DELAY(1000)

        String apiUuid = Platform.getUuid()
        createVmInstance {
            apiId = apiUuid
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [env.inventoryByName("l3").uuid]
            name = "vm"
        }

        // confirm the progress entries are still there
        assert Q.New(TaskProgressVO.class).eq(TaskProgressVO_.apiId, apiUuid).isExists()

        // set the TTL to 1s
        ProgressGlobalConfig.PROGRESS_TTL.updateValue(1)

        retryInSecs {
            // confirm the progress entries are deleted
            assert !Q.New(TaskProgressVO.class).eq(TaskProgressVO_.apiId, apiUuid).isExists()
        }
    }

    void testNoProgressWhenProgressIsTurnedOff() {
        ProgressGlobalConfig.PROGRESS_ON.updateValue(false)

        String apiUuid = Platform.getUuid()
        createVmInstance {
            apiId = apiUuid
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [env.inventoryByName("l3").uuid]
            name = "vm"
        }

        assert !Q.New(TaskProgressVO.class).isExists()

        // reopen it
        ProgressGlobalConfig.PROGRESS_ON.updateValue(true)
    }

    void testCreateVmProgress() {
        RESTFacade restf = bean(RESTFacade.class)

        ImageInventory vmImage = env.inventoryByName("image1")
        ImageInventory vrImage = env.inventoryByName("vr-image")

        def vmImagePath = vmImage.backupStorageRefs[0].installPath
        def vrImagePath = vrImage.backupStorageRefs[0].installPath

        def ft = new FuncTrigger()

        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(restf.getBaseUrl())
        ub.path(RESTConstant.COMMAND_CHANNEL_PATH)
        String url = ub.build().toUriString()

        env.afterSimulator(LocalStorageKvmSftpBackupStorageMediatorImpl.DOWNLOAD_BIT_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), LinkedHashMap.class)
            int i = 0
            while (i <= 5) {
                def header = [(RESTConstant.COMMAND_PATH): ProgressConstants.PROGRESS_REPORT_PATH]
                def rcmd = new ProgressCommands.ProgressReportCmd()
                rcmd.progress = String.valueOf(i++)
                rcmd.setThreadContextMap(cmd.threadContext)
                rcmd.setThreadContextStack(cmd.threadContextStack)
                restf.syncJsonPost(url, JSONObjectUtil.toJsonString(rcmd), header, ProgressCommands.ProgressReportResponse.class)

                ft.trigger([cmd, rcmd])
            }

            return rsp
        }

        CreateVmInstanceAction a = new CreateVmInstanceAction()
        a.apiId = Platform.getUuid()
        a.sessionId = adminSession()
        a.imageUuid = vmImage.uuid
        a.instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
        a.l3NetworkUuids = [env.inventoryByName("l3").uuid]
        a.name = "vm"

        ft.func = {
            def (LocalStorageKvmSftpBackupStorageMediatorImpl.SftpDownloadBitsCmd cmd, ProgressCommands.ProgressReportCmd rcmd) = it

            if (cmd.backupStorageInstallPath == vmImagePath) {
                // downloading user vm image, 2 sub tasks here

                List<TaskProgressInventory> invs = getTaskProgress {
                    apiId = a.apiId
                }

                assert invs.size() == 2

                // the first one is starting the user vm
                TaskProgressInventory inv = invs[0]
                assert inv.type == TaskType.Task.toString()

                // the second one is downloading user image
                inv = invs[1]
                assert inv.content == rcmd.progress
                assert inv.type == TaskType.Progress.toString()

            } else if (cmd.backupStorageInstallPath == vrImagePath) {
                // downloading vr image, 1 sub tasks here

                List<TaskProgressInventory> invs = getTaskProgress {
                    apiId = a.apiId
                }

                assert invs.size() == 3

                // the first one is starting the user vm
                TaskProgressInventory inv = invs[0]
                assert inv.type == TaskType.Task.toString()

                // the second one is starting vr
                inv = invs[1]
                assert inv.type == TaskType.Task.toString()

                // the third one is downloading vr image
                inv = invs[2]
                assert inv.content == rcmd.progress
                assert inv.type == TaskType.Progress.toString()

            } else {
                assert false: "should not be here: ${cmd.backupStorageInstallPath}"
            }
        }

        ErrorCode vmError = null
        a.call(new Completion<CreateVmInstanceAction.Result>() {
            @Override
            void complete(CreateVmInstanceAction.Result ret) {
                vmError = ret.error
                ft.quit()
            }
        })

        ft.run()

        retryInSecs(30) {
            assert vmError == null: "$vmError"
        }

        List<TaskProgressInventory> invs = getTaskProgress {
            apiId = a.apiId
            all = true
        }

        assert invs.size() != 0

        SQL.New(TaskProgressVO.class).hardDelete();
    }

    @Override
    void test() {
        env.create {
            int deleteDelay =  bean(ProgressReportService.class).getDELETE_DELAY()
            ProgressGlobalConfig.CLEANUP_THREAD_INTERVAL.updateValue(1)

            testCreateVmProgress()
            testNoProgressWhenProgressIsTurnedOff()
            testProgressDeleteAfterApiDone()
            testProgressTTL()

            // recover the DELETE_DELAY to the default value
            bean(ProgressReportService.class).setDELETE_DELAY(deleteDelay)
        }
    }
}
