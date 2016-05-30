package org.zstack.compute.host;

import org.zstack.core.logging.LogLabel;

/**
 * Created by xing5 on 2016/6/1.
 */
public class HostLogLabel {
    @LogLabel(messages = {
            "en_US = save information to the database",
            "zh_CN = 保存信息到数据库"
    })
    public static String ADD_HOST_WRITE_DB = "add.host.writeDb";

    @LogLabel(messages = {
            "en_US = connecting the host",
            "zh_CN = 连接物理机"
    })
    public static String ADD_HOST_CONNECT = "add.host.connect";

    @LogLabel(messages = {
            "en_US = successfully added the host",
            "zh_CN = 添加物理机成功"
    })
    public static String ADD_HOST_CHECK_OS_VERSION_IN_CLUSTER = "add.host.checkOSVersionInCluster";

    public static String ADD_HOST_SUCCESS = "add.host.success";
}
