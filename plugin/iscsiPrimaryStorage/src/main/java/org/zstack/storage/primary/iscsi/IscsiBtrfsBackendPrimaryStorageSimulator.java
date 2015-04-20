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
import org.zstack.kvm.KVMConstant;
import org.zstack.storage.primary.iscsi.IscsiFileSystemBackendPrimaryStorageCommands.DownloadBitsFromSftpBackupStorageCmd;
import org.zstack.storage.primary.iscsi.IscsiFileSystemBackendPrimaryStorageCommands.DownloadBitsFromSftpBackupStorageRsp;
import org.zstack.utils.gson.JSONObjectUtil;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by frank on 4/19/2015.
 */
@Controller
public class IscsiBtrfsBackendPrimaryStorageSimulator {

    @Autowired
    private RESTFacade restf;
    @Autowired
    private IscsiBtrfsBackendPrimaryStorageSimulatorConfig config;

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
    private String attachNic(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        download(entity);
        return null;
    }

}
