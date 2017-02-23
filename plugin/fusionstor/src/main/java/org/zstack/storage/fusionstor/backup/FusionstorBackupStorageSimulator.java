package org.zstack.storage.fusionstor.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.storage.fusionstor.backup.FusionstorBackupStorageBase.*;
import org.zstack.storage.fusionstor.backup.FusionstorBackupStorageSimulatorConfig.FusionstorBackupStorageConfig;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

/**
 * Created by frank on 7/28/2015.
 */
@Controller
public class FusionstorBackupStorageSimulator {
    CLogger logger = Utils.getLogger(FusionstorBackupStorageSimulator.class);

    @Autowired
    private FusionstorBackupStorageSimulatorConfig config;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private RESTFacade restf;

    public void reply(HttpEntity<String> entity, Object rsp) {
        String taskUuid = entity.getHeaders().getFirst(RESTConstant.TASK_UUID);
        String callbackUrl = entity.getHeaders().getFirst(RESTConstant.CALLBACK_URL);
        String rspBody = JSONObjectUtil.toJsonString(rsp);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentLength(rspBody.length());
        headers.set(RESTConstant.TASK_UUID, taskUuid);
        HttpEntity<String> rreq = new HttpEntity<String>(rspBody, headers);
        restf.getRESTTemplate().exchange(callbackUrl, HttpMethod.POST, rreq, String.class);
    }

    private FusionstorBackupStorageConfig getConfig(AgentCommand cmd) {
        SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
        q.select(BackupStorageVO_.name);
        q.add(BackupStorageVO_.uuid, Op.EQ, cmd.getUuid());
        String name = q.findValue();

        FusionstorBackupStorageConfig c = config.config.get(name);
        if (c == null) {
            throw new CloudRuntimeException(String.format("cannot find FusionstorBackupStorageConfig by name[%s], uuid[%s]", name, cmd.getUuid()));
        }

        c.name = name;

        return c;
    }

    @RequestMapping(value=FusionstorBackupStorageBase.INIT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String initialize(HttpEntity<String> entity) {
        InitCmd cmd = JSONObjectUtil.toObject(entity.getBody(), InitCmd.class);
        FusionstorBackupStorageConfig cbc = getConfig(cmd);
        config.initCmds.add(cmd);

        DebugUtils.Assert(cbc.fsid != null, String.format("fsid for fusionstor backup storage[%s] is null", cbc.name));

        InitRsp rsp = new InitRsp();

        if (!config.monInitSuccess) {
            rsp.error = "on purpose";
            rsp.success = false;
        } else {
            rsp.fsid = cbc.fsid;
            rsp.totalCapacity = cbc.totalCapacity;
            rsp.availableCapacity = cbc.availCapacity;
        }

        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=FusionstorBackupStorageBase.DOWNLOAD_IMAGE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String download(HttpEntity<String> entity) {
        DownloadCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DownloadCmd.class);
        config.downloadCmds.add(cmd);

        DownloadRsp rsp = new DownloadRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=FusionstorBackupStorageBase.DELETE_IMAGE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String doDelete(HttpEntity<String> entity) {
        DeleteCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteCmd.class);
        config.deleteCmds.add(cmd);
        DeleteRsp rsp = new DeleteRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=FusionstorBackupStorageBase.PING_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String ping(HttpEntity<String> entity) {
        PingCmd cmd = JSONObjectUtil.toObject(entity.getBody(), PingCmd.class);
        config.pingCmds.add(cmd);
        PingRsp rsp = new PingRsp();
        reply(entity, rsp);
        return null;
    }
}
