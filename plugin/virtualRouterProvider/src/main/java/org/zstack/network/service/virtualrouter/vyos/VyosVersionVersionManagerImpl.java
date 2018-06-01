package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

public class VyosVersionVersionManagerImpl implements VyosVersionManager {
    private final static CLogger logger = Utils.getLogger(VyosVersionVersionManagerImpl.class);

    @Autowired
    private ApiTimeoutManager apiTimeoutManager;
    @Autowired
    private CloudBus bus;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void vyosRouterVersionCheck(String vrUuid, Completion completion) {
        String managementVersion = getManagementVersion();
        if (managementVersion == null) {
            completion.success();
            return;
        }

        VirtualRouterCommands.PingCmd cmd = new VirtualRouterCommands.PingCmd();
        cmd.setUuid(vrUuid);
        VirtualRouterVmInventory vrinv = VirtualRouterVmInventory.valueOf(dbf.findByUuid(vrUuid, VirtualRouterVmVO.class));
        restf.asyncJsonPost(vrMgr.buildUrl(vrinv.getManagementNic().getIp(), VirtualRouterConstant.VR_PING), cmd, null, new JsonAsyncRESTCallback<VirtualRouterCommands.PingRsp>(completion) {
            @Override
            public void fail(ErrorCode err) {
                logger.warn(String.format("virtual router[uuid: %s] get version failed because %s", vrUuid, err.getDetails()));
                completion.success();
            }

            @Override
            public void success(VirtualRouterCommands.PingRsp ret) {
                if (!ret.isSuccess()){
                    ErrorCode err = operr("virtual router[uuid: %s] failed to get version because %s ",
                            vrUuid, ret.getError());
                    completion.fail(err);
                    return;
                }

                if (ret.getVersion() == null) {
                    ErrorCode err = operr("virtual router[uuid: %s] doesn't have version", vrUuid);
                    completion.fail(err);
                    return;
                }

                if (!versionFormatCheck(ret.getVersion())) {
                    ErrorCode err = operr("virtual router[uuid: %s] version [%s] format error", vrUuid, ret.getVersion());
                    completion.fail(err);
                    return;
                }

                if (versionCompare(managementVersion, ret.getVersion()) > 0) {
                    ErrorCode err = operr("virtual router[uuid: %s] version [%s] is older than management node version [%s]",vrUuid, ret.getVersion(), managementVersion);
                    completion.fail(err);
                } else {
                    logger.debug(String.format("virtual router[uuid: %s] successfully finish the version check", vrUuid));
                    completion.success();
                }
            }

            @Override
            public Class<VirtualRouterCommands.PingRsp> getReturnClass() {
                return VirtualRouterCommands.PingRsp.class;
            }
        });
    }

    private String getManagementVersion() {
        String managementVersion = null;
        String path = null;
        try {
            path = PathUtil.findFileOnClassPath(VyosConstants.VYOS_VERSION_PATH, true).getAbsolutePath();
        } catch (RuntimeException e) {
            logger.error(String.format("vyos version file find file because %s", e.getMessage()));
            return null;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            managementVersion = br.readLine();
        } catch (IOException e) {
            logger.error(String.format("vyos version file %s read error: %s", path, e.getMessage()));
            return null;
        }

        if (!versionFormatCheck(managementVersion)) {
            logger.error(String.format("vyos version file format error: %s", managementVersion));
            return null;
        }

        return managementVersion;
    }

    private boolean versionFormatCheck(String version) {
        return version.split("\\.").length == VyosConstants.VYOS_VERSION_LENGTH;
    }

    private int versionCompare(String ver1, String ver2) {
        String array1[] = ver1.split("\\.");
        String array2[] = ver2.split("\\.");

        for (int i = 0; i < VyosConstants.VYOS_VERSION_LENGTH; i++) {
            int ret = array1[i].compareTo(array2[i]);
            if (ret != 0) {
                return ret;
            }
        }

        return 0;
    }
}
