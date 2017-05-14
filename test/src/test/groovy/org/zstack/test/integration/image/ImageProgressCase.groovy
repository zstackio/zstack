package org.zstack.test.integration.image

import org.springframework.http.HttpEntity
import org.springframework.web.util.UriComponentsBuilder
import org.zstack.core.Platform
import org.zstack.core.progress.ProgressCommands
import org.zstack.header.core.progress.ProgressConstants
import org.zstack.header.core.progress.TaskType
import org.zstack.header.rest.RESTConstant
import org.zstack.header.rest.RESTFacade
import org.zstack.sdk.AddImageAction
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.Completion
import org.zstack.sdk.ErrorCode
import org.zstack.sdk.TaskProgressInventory
import org.zstack.storage.ceph.backup.CephBackupStorageBase
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.FuncTrigger
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by xing5 on 2017/3/21.
 */
class ImageProgressCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        spring {
            ceph()
            kvm()
            include("Progress.xml")
        }
    }

    @Override
    void environment() {
        env = Env.oneCephBackupStorageEnv()
    }

    void testAddImageProgress() {
        RESTFacade restf = bean(RESTFacade.class)
        BackupStorageInventory bs = env.inventoryByName("ceph-bk")

        def trigger = new FuncTrigger()

        env.afterSimulator(CephBackupStorageBase.DOWNLOAD_IMAGE_PATH) { rsp, HttpEntity<String> e ->
            Map cmd = JSONObjectUtil.toObject(e.getBody(), LinkedHashMap.class)

            UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(restf.getBaseUrl())
            ub.path(RESTConstant.COMMAND_CHANNEL_PATH)
            String url = ub.build().toUriString()

            int i = 0
            trigger.func = {
                i += 20
                def header = [(RESTConstant.COMMAND_PATH): ProgressConstants.PROGRESS_REPORT_PATH]
                def rcmd = new ProgressCommands.ProgressReportCmd()
                rcmd.progress = String.valueOf(i)
                rcmd.setThreadContextMap(cmd.threadContext)
                rcmd.setThreadContextStack(cmd.threadContextStack)
                restf.syncJsonPost(url, JSONObjectUtil.toJsonString(rcmd), header, ProgressCommands.ProgressReportResponse.class)
            }

            trigger.run()

            return rsp
        }

        String id = Platform.getUuid()
        def a = new AddImageAction()
        a.apiId = id
        a.sessionId = adminSession()
        a.backupStorageUuids = [bs.uuid]
        a.name = "image"
        a.url = "http://zstack.org/download/image.qcow2"
        a.format = "qcow2"

        ErrorCode err = null
        a.call(new Completion<AddImageAction.Result>() {
            @Override
            void complete(AddImageAction.Result ret) {
                err = ret.error
            }
        })

        int num = 5
        for (i in 1..num) {
            trigger.trigger()

            retryInMillis(5000) {
                List<TaskProgressInventory> invs = getTaskProgress {
                    apiId = id
                }

                return {
                    assert invs.size() == 1

                    TaskProgressInventory inv = invs[0]

                    assert inv.content == "${i*20}".toString()
                    assert inv.parentUuid == null
                    assert inv.type == TaskType.Progress.toString()
                }
            }
        }

        trigger.quit()

        retryInSecs(30) {
            assert err == null: "$err"
        }

        List<TaskProgressInventory> invs = getTaskProgress {
            apiId = id
            all = true
        }

        assert invs.size() == num
    }

    @Override
    void test() {
        env.create {
            testAddImageProgress()
        }
    }
}
