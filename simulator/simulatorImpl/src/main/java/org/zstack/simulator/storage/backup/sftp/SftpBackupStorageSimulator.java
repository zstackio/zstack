package org.zstack.simulator.storage.backup.sftp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands.*;
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant;
import org.zstack.simulator.AsyncRESTReplyer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@Controller
public class SftpBackupStorageSimulator {
    CLogger logger = Utils.getLogger(SftpBackupStorageSimulator.class);
    
    @Autowired
    private SftpBackupStorageSimulatorConfig config;
    @Autowired
    private RESTFacade restf;
    
    private AsyncRESTReplyer replyer;
    
    @RequestMapping(value=SftpBackupStorageConstant.CONNECT_PATH, method=RequestMethod.POST)
    public @ResponseBody String connect(@RequestBody String body) {
        ConnectCmd cmd = JSONObjectUtil.toObject(body, ConnectCmd.class);
        ConnectResponse rsp = new ConnectResponse();
        if (!config.connectSuccess) {
            rsp.setSuccess(false);
            rsp.setError("Fail connect on purpose");
        } else {
            rsp.setTotalCapacity(config.totalCapacity);
            rsp.setAvailableCapacity(config.availableCapacity);
            logger.debug(String.format("Connect to path[%s], %s", cmd.getStoragePath(), JSONObjectUtil.toJsonString(rsp)));
        }
        return JSONObjectUtil.toJsonString(rsp);
    }
    
    @RequestMapping(value=SftpBackupStorageConstant.ECHO_PATH, method=RequestMethod.POST)
    public @ResponseBody String echo(@RequestBody String body) {
        return "";
    }
    
    private void reply(HttpEntity<String> entity, AgentResponse rsp) {
        if (replyer == null) {
            replyer = new AsyncRESTReplyer();
        }
        replyer.reply(entity, rsp);
    }
    
    @AsyncThread
    private void doDownload(HttpEntity<String> entity) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(500);
        DownloadCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DownloadCmd.class);
        DownloadResponse rsp = new DownloadResponse();
        if (!config.downloadSuccess2) {
            rsp.setSuccess(false);
            rsp.setError("Fail download on purpose");
        } else {
            Long size = config.imageSizes.get(cmd.getUuid());
            rsp.setSize(size == null ? 0 : size);
            rsp.setMd5Sum(config.imageMd5sum);
            rsp.setTotalCapacity(config.totalCapacity);
            long usedSize = 0;
            for (Long s : config.imageSizes.values()) {
                usedSize += s;
            }
            rsp.setAvailableCapacity(config.totalCapacity-usedSize);
            logger.debug(String.format("Download %s", cmd.getUrl()));
        }
        
        reply(entity, rsp);
    }
    
    @RequestMapping(value=SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH, method=RequestMethod.POST)
    public @ResponseBody String download(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        String ret = null;
        if (!config.downloadSuccess1) {
            throw new CloudRuntimeException("Fail download on purpose");
        } else {
            doDownload(entity);
        }
        return ret;
    }
    
    @AsyncThread
    private void doDelete(HttpEntity<String> entity) {
        AgentResponse rsp = null;
        DeleteCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteCmd.class);
        if (!config.deleteSuccess) {
            rsp = new AgentResponse();
            rsp.setError("Fail delete on purpose");
            rsp.setSuccess(false);
        } else {
            config.deleteCmds.add(cmd);
            logger.debug(String.format("Deleted %s", cmd.getInstallUrl()));
            rsp = (AgentResponse)(new DeleteResponse());
        }
        
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
    
    @RequestMapping(value=SftpBackupStorageConstant.DELETE_PATH, method=RequestMethod.POST)
    public @ResponseBody String delete(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doDelete(entity);
        return null;
    }

    @RequestMapping(value=SftpBackupStorageConstant.PING_PATH, method=RequestMethod.POST)
    public @ResponseBody String ping(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        if (config.pingException) {
            throw new CloudRuntimeException("on purpose");
        }
        doPing(entity);
        return null;
    }

    @AsyncThread
    private void doPing(HttpEntity<String> entity) {
        PingResponse rsp = new PingResponse();
        if (!config.pingSuccess) {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        }
        reply(entity, rsp);
    }
}
