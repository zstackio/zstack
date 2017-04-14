package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.springframework.web.util.UriComponentsBuilder
import org.zstack.core.Platform
import org.zstack.core.progress.ProgressCommands
import org.zstack.header.core.progress.ProgressConstants
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
                assert inv.currentStep != 0

                // the second one is downloading user image
                inv = invs[1]
                assert inv.content == rcmd.progress
                assert inv.type == TaskType.Progress.toString()
                assert inv.currentStep != 0

            } else if (cmd.backupStorageInstallPath == vrImagePath) {
                // downloading vr image, 1 sub tasks here

                List<TaskProgressInventory> invs = getTaskProgress {
                    apiId = a.apiId
                }

                assert invs.size() == 3

                // the first one is starting the user vm
                TaskProgressInventory inv = invs[0]
                assert inv.type == TaskType.Task.toString()
                assert inv.currentStep != 0

                // the second one is starting vr
                inv = invs[1]
                assert inv.type == TaskType.Task.toString()
                assert inv.currentStep != 0

                // the third one is downloading vr image
                inv = invs[2]
                assert inv.content == rcmd.progress
                assert inv.type == TaskType.Progress.toString()
                assert inv.currentStep != 0

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
            return { assert vmError == null: "$vmError"}
        }

        // the steps are learned, so the return should have totalSteps now
        List<TaskProgressInventory> invs = getTaskProgress {
            apiId = a.apiId
        }

        invs.each {
            if (it.type != TaskType.Progress.toString()) {
                assert it.totalSteps != null: JSONObjectUtil.toJsonString(it)
                assert it.currentStep != null: JSONObjectUtil.toJsonString(it)
            }
        }

        invs = getTaskProgress {
            apiId = a.apiId
            all = true
        }

        invs.each {
            if (it.type != TaskType.Progress.toString()) {
                assert it.totalSteps != null: JSONObjectUtil.toJsonString(it)
                assert it.currentStep != null: JSONObjectUtil.toJsonString(it)
            }
        }
    }

    @Override
    void test() {
        /* disable this case temporarily, as the progress logic needs to be refined
        env.create {
            testCreateVmProgress()
        }
        */
    }
}
