package org.zstack.compute.vm;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class VmBootTimeUtils {
    private static final String STOPPED_UPTIME = "0";
    private static final DateTimeFormatter BOOT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String getUptime(String vmUuid, VmInstanceState state) {
        if (StringUtils.isBlank(vmUuid) || state != VmInstanceState.Running) {
            return STOPPED_UPTIME;
        }

        String bootTime = getBootTime(vmUuid);
        if (StringUtils.isNotEmpty(bootTime)) {
            return bootTime;
        }

        return resetBootTime(vmUuid);
    }

    public String getBootTime(String vmUuid) {
        if (StringUtils.isBlank(vmUuid)) {
            return null;
        }

        Timestamp bootTime = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.bootTime)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .findValue();
        return formatBootTime(bootTime);
    }

    public String resetBootTime(String vmUuid) {
        if (!vmExists(vmUuid)) {
            return STOPPED_UPTIME;
        }

        Timestamp bootTime = Timestamp.valueOf(LocalDateTime.now());
        setBootTime(vmUuid, bootTime);
        return formatBootTime(bootTime);
    }

    public void clearBootTime(String vmUuid) {
        if (StringUtils.isBlank(vmUuid)) {
            return;
        }

        SQL.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .set(VmInstanceVO_.bootTime, (Timestamp) null)
                .update();
    }

    private static boolean vmExists(String vmUuid) {
        return StringUtils.isNotBlank(vmUuid)
                && Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).isExists();
    }

    private static void setBootTime(String vmUuid, Timestamp bootTime) {
        SQL.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .set(VmInstanceVO_.bootTime, bootTime)
                .update();
    }

    private static String formatBootTime(Timestamp bootTime) {
        if (bootTime == null) {
            return null;
        }

        return bootTime.toLocalDateTime().format(BOOT_TIME_FORMATTER);
    }
}