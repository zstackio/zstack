package org.zstack.test.integration.image

import org.zstack.testlib.EnvSpec
import org.zstack.testlib.Test

/**
 * Created by xing5 on 2017/3/5.
 */
class Env {
    static EnvSpec oneSftpEnv = Test.makeEnv {
        zone {
            name = "zone"
        }

        sftpBackupStorage {
            name = "sftp"
            url = "/sftp"

            image {
                name = "image"
                url = "http://zstack.org/download/image.qcow2"
            }
        }
    }
}
