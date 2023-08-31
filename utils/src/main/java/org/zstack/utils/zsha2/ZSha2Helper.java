package org.zstack.utils.zsha2;

import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.util.Arrays;


/**
 * Created by mingjian.deng on 2020/4/2.
 */
public class ZSha2Helper {
    private static final CLogger logger = Utils.getLogger(ZSha2Helper.class);

    public static boolean isMNHaEnvironment() {
        return PathUtil.exists("/usr/local/bin/zsha2");
    }

    public static ZSha2Info getInfo() {
        return getInfo(true);
    }

    public static ZSha2Info getInfo(boolean checkZSha2Status) {
        ShellResult result;
        String zsha2StatusOut = "";
        if (checkZSha2Status) {
            result = ShellUtils.runAndReturn("sudo -i /usr/local/bin/zsha2 status", false);
            if (!result.isReturnCode(0)) {
                throw new RuntimeException(String.format("cannot get zsha2 status, because %s", result.getStderr()));
            }
            zsha2StatusOut = result.getStdout();
        }

        result = ShellUtils.runAndReturn("/usr/local/bin/zsha2 show-config");
        if (!result.isReturnCode(0)) {
            throw new RuntimeException(String.format("cannot get zsha2 config, because %s, maybe you need upgrade zsha2", result.getStderr()));
        }

        ZSha2Info info = JSONObjectUtil.toObject(result.getStdout(), ZSha2Info.class);

        info.setMaster(ShellUtils.runAndReturn(String.format(
                "ip addr show %s | grep -q '[^0-9]%s[^0-9]'", info.getNic(), info.getDbvip())).isReturnCode(0));
        setCheckDbStatusResult(zsha2StatusOut, info);
        return info;
    }

    private static void setCheckDbStatusResult(String zsha2StatusOut, ZSha2Info info) {
        if (zsha2StatusOut == null || zsha2StatusOut.isEmpty()) return;
        boolean dbStatusError = Arrays.stream(zsha2StatusOut.split("\\\\n"))
                .anyMatch(ZSha2Helper::matchZsha2StatusOutError);
        info.setDbStatusError(dbStatusError);
        if (dbStatusError) info.setDbErrorDetail(zsha2StatusOut);
    }

    private static boolean matchZsha2StatusOutError(String zsha2StatusOut) {
        if (zsha2StatusOut.contains("MySQL status:"))
            return !zsha2StatusOut.contains("mysqld is alive");
        if (zsha2StatusOut.contains("Slave_IO_Running:") || zsha2StatusOut.contains("Slave_SQL_Running:"))
            return !zsha2StatusOut.contains("Yes");
        if (zsha2StatusOut.contains("Last_IO_Error:")
                || zsha2StatusOut.contains("Last_SQL_Error:") || zsha2StatusOut.contains("Last_Error"))
            return !zsha2StatusOut.substring(zsha2StatusOut.indexOf(":") + 1).trim().isEmpty();
        return false;
    }
}
