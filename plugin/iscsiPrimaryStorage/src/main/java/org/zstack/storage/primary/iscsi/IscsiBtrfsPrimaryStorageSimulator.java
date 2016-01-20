package org.zstack.storage.primary.iscsi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.storage.primary.iscsi.IscsiFileSystemBackendPrimaryStorageCommands.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by frank on 4/19/2015.
 */
@Controller
public class IscsiBtrfsPrimaryStorageSimulator {
    private static CLogger logger = Utils.getLogger(IscsiBtrfsPrimaryStorageSimulator.class);

    @Autowired
    private RESTFacade restf;
    @Autowired
    private IscsiBtrfsPrimaryStorageSimulatorConfig config;

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

    @AsyncThread
    private void download(HttpEntity<String> entity) {
        DownloadBitsFromSftpBackupStorageRsp rsp = new DownloadBitsFromSftpBackupStorageRsp();
        DownloadBitsFromSftpBackupStorageCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DownloadBitsFromSftpBackupStorageCmd.class);
        if (config.downloadFromSftpSuccess) {
            config.downloadBitsFromSftpBackupStorageCmdList.add(cmd);
        } else {
            rsp.setError("fail on purpose");
            rsp.setSuccess(false);
        }

        reply(entity, rsp);
    }

    @RequestMapping(value="/btrfs/image/sftp/download",  method= RequestMethod.POST)
    @ResponseBody
    private String download(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        download(entity);
        return null;
    }

    @RequestMapping(value="/btrfs/bits/checkifexists",  method= RequestMethod.POST)
    @ResponseBody
    private String checkBits(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        checkBits(entity);
        return null;
    }

    @AsyncThread
    private void checkBits(HttpEntity<String> entity) {

        CheckBitsExistenceCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CheckBitsExistenceCmd.class);
        CheckBitsExistenceRsp rsp = new CheckBitsExistenceRsp();

        if (config.checkBitsSuccess) {
            config.checkBitsExistenceCmds.add(cmd);
            rsp.setExisting(true);
        } else {
            rsp.setSuccess(false);
            rsp.setError("fail on purpose");
        }

        reply(entity, rsp);
    }

    @RequestMapping(value="/btrfs/bits/delete",  method= RequestMethod.POST)
    @ResponseBody
    private String delete(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        delete(entity);
        return null;
    }

    private void delete(HttpEntity<String> entity) {
        DeleteBitsCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteBitsCmd.class);
        DeleteBitsRsp rsp = new DeleteBitsRsp();
        if (config.deleteBitsSuccess) {
            config.deleteBitsCmds.add(cmd);
        } else {
            rsp.setError("fail on purpose");
        }

        reply(entity, rsp);
    }

    @RequestMapping(value="/btrfs/volumes/createrootfromtemplate",  method= RequestMethod.POST)
    @ResponseBody
    private String createRootVolume(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        createRootVolume(entity);
        return null;
    }

    private void createRootVolume(HttpEntity<String> entity) {
        CreateRootVolumeFromTemplateCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateRootVolumeFromTemplateCmd.class);
        CreateRootVolumeFromTemplateRsp rsp = new CreateRootVolumeFromTemplateRsp();
        if (config.createRootVolumeSuccess) {
            config.createRootVolumeFromTemplateCmds.add(cmd);
            rsp.setIscsiPath("iqn.1994-05.com.redhat:3b93b069cc1");
        } else {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        }

        reply(entity, rsp);
    }

    @RequestMapping(value="/btrfs/volumes/createempty",  method= RequestMethod.POST)
    @ResponseBody
    private String createEmptyVolume(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        createEmptyVolume(entity);
        return null;
    }

    private void createEmptyVolume(HttpEntity<String> entity) {
        CreateEmptyVolumeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateEmptyVolumeCmd.class);
        CreateEmptyVolumeRsp rsp = new CreateEmptyVolumeRsp();
        if (config.createEmptyVolumeSuccess) {
            config.createEmptyVolumeCmds.add(cmd);
            rsp.setIscsiPath("iqn.1994-05.com.redhat:3b93b069cc1");
        } else {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        }
        reply(entity, rsp);
    }

    @RequestMapping(value="/btrfs/init",  method= RequestMethod.POST)
    @ResponseBody
    private String init(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        init(entity);
        return null;
    }

    private void init(HttpEntity<String> entity) {
        InitCmd cmd = JSONObjectUtil.toObject(entity.getBody(), InitCmd.class);
        InitRsp rsp = new InitRsp();
        if (config.initSuccess) {
            rsp.setTotalCapacity(config.totalCapacity);
            rsp.setAvailableCapacity(config.availableCapacity);
        } else {
            rsp.setError("fail on purpose");
            rsp.setSuccess(false);
        }

        reply(entity, rsp);
    }

    @RequestMapping(value="/btrfs/bits/upload",  method= RequestMethod.POST)
    @ResponseBody
    private String upload(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        upload(entity);
        return null;
    }

    private void upload(HttpEntity<String> entity) {
        UploadToSftpCmd cmd = JSONObjectUtil.toObject(entity.getBody(), UploadToSftpCmd.class);
        UploadToSftpRsp rsp = new UploadToSftpRsp();
        if (!config.uploadSuccess) {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        } else {
            config.uploadToSftpCmds.add(cmd);
        }

        reply(entity, rsp);
    }

    @RequestMapping(value="/btrfs/target/create",  method= RequestMethod.POST)
    @ResponseBody
    private String createTarget(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        createTarget(entity);
        return null;
    }

    private void createTarget(HttpEntity<String> entity) {
        CreateIscsiTargetCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateIscsiTargetCmd.class);
        CreateIscsiTargetRsp rsp = new CreateIscsiTargetRsp();
        if (config.createTargetSuccess) {
            synchronized (config.createIscsiTargetCmds) {
                config.createIscsiTargetCmds.add(cmd);
            }
            rsp.setTarget("iqn.1994-05.com.redhat:3b93b069cc1");
            rsp.setLun(1);
        } else {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        }

        reply(entity, rsp);
    }

    @RequestMapping(value="/btrfs/target/delete",  method= RequestMethod.POST)
    @ResponseBody
    private String deleteTarget(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        deleteTarget(entity);
        return null;
    }

    private void deleteTarget(HttpEntity<String> entity) {
        DeleteIscsiTargetCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteIscsiTargetCmd.class);
        synchronized (config.deleteIscsiTargetCmds) {
            config.deleteIscsiTargetCmds.add(cmd);
        }
        logger.debug(String.format("delete iscsi target[name:%s, uuid:%s]", cmd.getTarget(), cmd.getUuid()));
        DeleteIscsiTargetRsp rsp = new DeleteIscsiTargetRsp();
        reply(entity, rsp);
    }

    @RequestMapping(value="/btrfs/subvolume/create",  method= RequestMethod.POST)
    @ResponseBody
    private String createSubvolume(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        createSubvolume(entity);
        return null;
    }

    private void createSubvolume(HttpEntity<String> entity) {
        CreateSubVolumeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateSubVolumeCmd.class);
        CreateSubVolumeRsp rsp = new CreateSubVolumeRsp();
        if (config.createSubVolumeSuccess) {
            config.createSubVolumeCmds.add(cmd);
            rsp.setPath(PathUtil.join("/fake_path/", cmd.getSrc()));
        } else {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        }

        reply(entity, rsp);
    }
}
