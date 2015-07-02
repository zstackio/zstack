package org.zstack.storage.primary.local;

import org.zstack.storage.primary.local.LocalStorageKvmBackend.AgentResponse;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.CreateEmptyVolumeCmd;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.CreateEmptyVolumeRsp;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.CreateVolumeFromCacheCmd;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.CreateVolumeFromCacheRsp;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.DeleteBitsCmd;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.DeleteBitsRsp;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.GetPhysicalCapacityCmd;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.InitCmd;
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl.SftpDownloadBitsCmd;
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl.SftpDownloadBitsRsp;
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl.SftpUploadBitsCmd;
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl.SftpUploadBitsRsp;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
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
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 * Created by frank on 7/1/2015.
 */
@Controller
public class LocalStorageSimulator {
    @Autowired
    private RESTFacade restf;
    @Autowired
    private LocalStorageSimulatorConfig config;
    @Autowired
    private DatabaseFacade dbf;

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

    @RequestMapping(value=LocalStorageKvmBackend.INIT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String init(HttpEntity<String> entity) {
        InitCmd cmd = JSONObjectUtil.toObject(entity.getBody(), InitCmd.class);
        config.initCmdList.add(cmd);

        AgentResponse rsp = new AgentResponse();

        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.name);
        q.add(HostVO_.uuid, Op.EQ, cmd.getHostUuid());
        String hname = q.findValue();

        Capacity c = config.capacityMap.get(hname);
        rsp.setTotalCapacity(c.total);
        rsp.setAvailableCapacity(c.avail);
        reply(entity, rsp);

        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.GET_PHYSICAL_CAPACITY_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String getPhysicalCapacity(HttpEntity<String> entity) {
        GetPhysicalCapacityCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetPhysicalCapacityCmd.class);
        config.getPhysicalCapacityCmds.add(cmd);

        AgentResponse rsp = new AgentResponse();

        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.name);
        q.add(HostVO_.uuid, Op.EQ, cmd.getHostUuid());
        String hname = q.findValue();

        Capacity c = config.capacityMap.get(hname);
        rsp.setTotalCapacity(c.total);
        rsp.setAvailableCapacity(c.avail);
        reply(entity, rsp);

        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.CREATE_EMPTY_VOLUME_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String createEmptyVolume(HttpEntity<String> entity) {
        CreateEmptyVolumeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateEmptyVolumeCmd.class);
        config.createEmptyVolumeCmds.add(cmd);
        CreateEmptyVolumeRsp rsp = new CreateEmptyVolumeRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.CREATE_VOLUME_FROM_CACHE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String createVolumeFromCache(HttpEntity<String> entity) {
        CreateVolumeFromCacheCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateVolumeFromCacheCmd.class);
        config.createVolumeFromCacheCmds.add(cmd);
        CreateVolumeFromCacheRsp rsp = new CreateVolumeFromCacheRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=LocalStorageKvmBackend.DELETE_BITS_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String delete(HttpEntity<String> entity) {
        DeleteBitsCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteBitsCmd.class);
        config.deleteBitsCmds.add(cmd);
        DeleteBitsRsp rsp = new DeleteBitsRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=LocalStorageKvmSftpBackupStorageMediatorImpl.DOWNLOAD_BIT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String download(HttpEntity<String> entity) {
        SftpDownloadBitsCmd cmd = JSONObjectUtil.toObject(entity.getBody(), SftpDownloadBitsCmd.class);
        config.downloadBitsCmds.add(cmd);
        reply(entity, new SftpDownloadBitsRsp());
        return null;
    }

    @RequestMapping(value=LocalStorageKvmSftpBackupStorageMediatorImpl.UPLOAD_BIT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String upload(HttpEntity<String> entity) {
        SftpUploadBitsCmd cmd = JSONObjectUtil.toObject(entity.getBody(), SftpUploadBitsCmd.class);
        config.uploadBitsCmds.add(cmd);
        reply(entity, new SftpUploadBitsRsp());
        return null;
    }
}
