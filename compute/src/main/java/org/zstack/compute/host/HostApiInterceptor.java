package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.upgrade.UpgradeGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.agent.versioncontrol.AgentVersionVO;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.host.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeVO;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        } else if (msg instanceof APIReconnectHostMsg){
            validate((APIReconnectHostMsg) msg);
        }

        return msg;
    }

    private void validate(APIDeleteHostMsg msg) {
        if (!dbf.isExist(msg.getUuid(), HostVO.class)) {
            APIDeleteHostEvent evt = new APIDeleteHostEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }

        validateVmStorageBeforeDeletingHost(msg.getUuid());
    }

    private void validateVmStorageBeforeDeletingHost(String hostUuid) {
        // A stopped VM has no hostUuid. Do not treat the last host of a VM
        // currently running elsewhere as its current owner.
        List<Tuple> vms = SQL.New("select vm.uuid, vm.name from VmInstanceVO vm" +
                        " where (vm.hostUuid = :hostUuid or" +
                        " (vm.hostUuid is null and vm.lastHostUuid = :hostUuid))" +
                        " and vm.state <> :destroyed order by vm.uuid", Tuple.class)
                .param("hostUuid", hostUuid)
                .param("destroyed", VmInstanceState.Destroyed)
                .list();
        if (vms.isEmpty()) {
            return;
        }

        Map<String, String> vmNames = new LinkedHashMap<>();
        vms.forEach(vm -> vmNames.put(vm.get(0, String.class), vm.get(1, String.class)));
        List<Tuple> volumes = SQL.New("select vol, ps from VolumeVO vol, PrimaryStorageVO ps" +
                        " where vol.primaryStorageUuid = ps.uuid and ps.type <> :localStorageType" +
                        " and vol.status <> :deleted" +
                        " and (vol.vmInstanceUuid in (:vmUuids) or vol.isShareable = true)" +
                        " order by vol.uuid", Tuple.class)
                .param("localStorageType", "LocalStorage")
                .param("deleted", VolumeStatus.Deleted)
                .param("vmUuids", vmNames.keySet())
                .list();
        for (Tuple tuple : volumes) {
            VolumeVO volume = tuple.get(0, VolumeVO.class);
            // Use the volume attachment extension for shareable data volumes,
            // whose vmInstanceUuid may be null.
            for (String vmUuid : volume.getAttachedVmUuids()) {
                if (!vmNames.containsKey(vmUuid)) {
                    continue;
                }

                PrimaryStorageVO storage = tuple.get(1, PrimaryStorageVO.class);
                throw new ApiMessageInterceptionException(operr(
                        "cannot delete host[uuid:%s]: vm[name:%s, uuid:%s] uses volume[name:%s, uuid:%s]" +
                                " on non-local primary storage[name:%s, uuid:%s, type:%s]." +
                                " Migrate the VM to another host or delete the VM before deleting this host",
                        hostUuid, vmNames.get(vmUuid), vmUuid, volume.getName(), volume.getUuid(),
                        storage.getName(), storage.getUuid(), storage.getType()));
            }
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

    private void validate(APIReconnectHostMsg msg) {
        String hostUuid = msg.getHostUuid();
        if (UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class)) {
            AgentVersionVO agentVersionVO = dbf.findByUuid(msg.getUuid(), AgentVersionVO.class);
            if (agentVersionVO == null) {
                agentVersionVO = new AgentVersionVO();
                agentVersionVO.setUuid(hostUuid);
                agentVersionVO.setAgentType("kvm-agent");
                dbf.persist(agentVersionVO);
            }
        }
    }
}
