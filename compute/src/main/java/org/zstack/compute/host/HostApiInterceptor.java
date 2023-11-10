package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleConstant;
import org.zstack.core.upgrade.UpgradeGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.agent.versioncontrol.AgentVersionVO;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.host.*;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 3:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class HostApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof HostMessage) {
            HostMessage hmsg = (HostMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hmsg.getHostUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        setServiceId(msg);

        if (msg instanceof APIAddHostMsg) {
            validate((APIAddHostMsg) msg);
        } else if (msg instanceof APIUpdateHostMsg) {
            validate((APIUpdateHostMsg) msg);
        } else if (msg instanceof APIDeleteHostMsg) {
            validate((APIDeleteHostMsg) msg);
        } else if (msg instanceof APIChangeHostStateMsg){
            validate((APIChangeHostStateMsg) msg);
        } else if (msg instanceof APIGetHostWebSshUrlMsg) {
            validate((APIGetHostWebSshUrlMsg) msg);
        }

        return msg;
    }

    private void validate(APIGetHostWebSshUrlMsg msg) {
        String ZOPS_CONTAINER_NAME = "zops-controller";
        ShellResult ret;
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            ret = ShellUtils.runAndReturn(String.format("docker exec %s systemctl is-active webssh", ZOPS_CONTAINER_NAME));
        } else {
            ret = new ShellResult();
            ret.setCommand(String.format("docker exec %s systemctl is-active webssh", ZOPS_CONTAINER_NAME));
            ret.setRetCode(0);
        }
        if (!ret.isReturnCode(0)) {
            throw new ApiMessageInterceptionException(operr("webssh server is not running."));
        }
    }

    private void validate(APIDeleteHostMsg msg) {
        if (!dbf.isExist(msg.getUuid(), HostVO.class)) {
            APIDeleteHostEvent evt = new APIDeleteHostEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(APIUpdateHostMsg msg) {
        if (msg.getManagementIp() != null) {
            SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
            q.add(HostVO_.managementIp, Op.EQ, msg.getManagementIp());
            if (q.isExists()) {
                throw new ApiMessageInterceptionException(argerr("there has been a host having managementIp[%s]", msg.getManagementIp()));
            }
        }
    }

    private void validate(APIAddHostMsg msg) {
        if (!NetworkUtils.isIpv4Address(msg.getManagementIp()) && !NetworkUtils.isHostname(msg.getManagementIp())) {
            throw new ApiMessageInterceptionException(argerr("managementIp[%s] is neither an IPv4 address nor a valid hostname", msg.getManagementIp()));
        }
    }

    private void validate(APIChangeHostStateMsg msg){
        HostStatus hostStatus = Q.New(HostVO.class)
                .select(HostVO_.status)
                .eq(HostVO_.uuid,msg.getHostUuid())
                .findValue();
        if (hostStatus != HostStatus.Connected && msg.getStateEvent().equals(HostStateEvent.maintain.toString())){
            throw new ApiMessageInterceptionException(operr("can not maintain host[uuid:%s, status:%s]which is not Connected", msg.getHostUuid(), hostStatus));
        }
    }
}
