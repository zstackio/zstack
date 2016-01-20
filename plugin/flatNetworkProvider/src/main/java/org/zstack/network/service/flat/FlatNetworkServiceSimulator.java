package org.zstack.network.service.flat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.network.service.flat.FlatDhcpBackend.*;
import org.zstack.network.service.flat.FlatDnsBackend.SetDnsCmd;
import org.zstack.network.service.flat.FlatDnsBackend.SetDnsRsp;
import org.zstack.network.service.flat.FlatUserdataBackend.ApplyUserdataCmd;
import org.zstack.network.service.flat.FlatUserdataBackend.ApplyUserdataRsp;
import org.zstack.network.service.flat.FlatUserdataBackend.ReleaseUserdataCmd;
import org.zstack.network.service.flat.FlatUserdataBackend.ReleaseUserdataRsp;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 * Created by frank on 9/19/2015.
 */
@Controller
public class FlatNetworkServiceSimulator {
    @Autowired
    private RESTFacade restf;
    @Autowired
    private FlatNetworkServiceSimulatorConfig config;

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

    @RequestMapping(value = FlatDhcpBackend.APPLY_DHCP_PATH, method = RequestMethod.POST)
    public @ResponseBody String setDhcp(HttpEntity<String> entity) {
        ApplyDhcpCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ApplyDhcpCmd.class);
        config.applyDhcpCmdList.add(cmd);
        ApplyDhcpRsp rsp = new ApplyDhcpRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = FlatDhcpBackend.PREPARE_DHCP_PATH, method = RequestMethod.POST)
    public @ResponseBody String prepareDhcp(HttpEntity<String> entity) {
        PrepareDhcpCmd cmd = JSONObjectUtil.toObject(entity.getBody(), PrepareDhcpCmd.class);
        config.prepareDhcpCmdList.add(cmd);
        reply(entity, new PrepareDhcpRsp());
        return null;
    }

    @RequestMapping(value = FlatDhcpBackend.RELEASE_DHCP_PATH, method = RequestMethod.POST)
    public @ResponseBody String releaseDhcp(HttpEntity<String> entity) {
        ReleaseDhcpCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ReleaseDhcpCmd.class);
        config.releaseDhcpCmds.add(cmd);
        ReleaseDhcpRsp rsp = new ReleaseDhcpRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = FlatDnsBackend.SET_DNS_PATH, method = RequestMethod.POST)
    public @ResponseBody String setDns(HttpEntity<String> entity) {
        SetDnsCmd cmd = JSONObjectUtil.toObject(entity.getBody(), SetDnsCmd.class);
        config.setDnsCmds.add(cmd);
        SetDnsRsp rsp = new SetDnsRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = FlatUserdataBackend.APPLY_USER_DATA, method = RequestMethod.POST)
    public @ResponseBody String applyUserdata(HttpEntity<String> entity) {
        ApplyUserdataCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ApplyUserdataCmd.class);
        config.applyUserdataCmds.add(cmd);
        ApplyUserdataRsp rsp = new ApplyUserdataRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = FlatUserdataBackend.RELEASE_USER_DATA, method = RequestMethod.POST)
    public @ResponseBody String releaseUserdata(HttpEntity<String> entity) {
        ReleaseUserdataCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ReleaseUserdataCmd.class);
        config.releaseUserdataCmds.add(cmd);
        ReleaseUserdataRsp rsp = new ReleaseUserdataRsp();
        reply(entity, rsp);
        return null;
    }
}
