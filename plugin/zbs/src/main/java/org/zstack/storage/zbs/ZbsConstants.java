package org.zstack.storage.zbs;

import org.zstack.header.configuration.PythonClass;

/**
 * @author Xingwei Yu
 * @date 2024/3/21 13:10
 */
@PythonClass
public interface ZbsConstants {
    String IDENTITY = "zbs";
    String ZBS_PS_IPTABLES_COMMENTS = "Zbsp.allow.port";
    String ZBS_PS_ALLOW_PORTS = "7763";
    String ZBS_HEARTBEAT_VOLUME_NAME = "zbs_zstack_heartbeat";
    long ZBS_HEARTBEAT_VOLUME_SIZE_IN_GIGABYTE = 1;
    String SCHEME_PREFIX = "zbs://";
    Integer MDS_PING_RETRY_PER_CYCLE = 3;
    Integer MDS_PING_FAIL_CYCLE_THRESHOLD = 3;
    String VOLUME_PHYSICAL_BLOCK_SIZE = "4096";
    String MEGABYTE_SUPPORTED_VERSION = "1.6.1";
    String MEGABYTE_UNIT = "M";
    String DEFAULT_GIGABYTE_UNIT = null;

    // vhost: SPDK target runs as a container on the compute node, exposing a
    // vhost-user-blk unix socket at <VHOST_SOCKET_DIR>/<controller name>.
    String VHOST_SOCKET_DIR = "/var/tmp/vhost-sockets";
    String VHOST_CONTROLLER_NAME_PREFIX = "zbs-vhost-";
    String VHOST_BDEV_NAME_PREFIX = "zbs-bdev-";
}
