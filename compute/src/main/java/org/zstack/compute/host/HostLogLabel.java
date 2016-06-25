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
            "en_US = check if the operating system version on the host matches others in the same cluster",
            "zh_CN = 检查物理机操作系统版本是否与集群中其它物理机一致"
    })
    public static String ADD_HOST_CHECK_OS_VERSION_IN_CLUSTER = "add.host.checkOSVersionInCluster";

    @LogLabel(messages = {
            "en_US = successfully added the host",
            "zh_CN = 添加物理机成功"
    })
    public static String ADD_HOST_SUCCESS = "add.host.success";

    @LogLabel(messages = {
            "en_US = the host[UUID:{0}, name:{1}] status becomes Disconnected, cause: {2}",
            "zh_CN = 物理机[UUID:{0}, name:{1}]失联，请检查链接并重连物理机。失联原因是: {2}"
    })
    public static String HOST_STATUS_DISCONNECTED = "host.status.disconnected";
}
