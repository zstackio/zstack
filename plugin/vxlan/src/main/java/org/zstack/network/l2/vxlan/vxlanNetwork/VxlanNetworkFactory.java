package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.Component;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.identity.AccountManager;
import org.zstack.network.l2.L2NetworkDefaultMtu;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.AllocateVniMsg;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.AllocateVniReply;
import org.zstack.network.service.NetworkServiceGlobalConfig;
import org.zstack.query.QueryFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

/**
 * Created by weiwang on 02/03/2017.
 */
public class VxlanNetworkFactory implements L2NetworkFactory, Component, VmInstanceMigrateExtensionPoint, L2NetworkDefaultMtu {
    private static CLogger logger = Utils.getLogger(VxlanNetworkFactory.class);
    static L2NetworkType type = new L2NetworkType(VxlanNetworkConstant.VXLAN_NETWORK_TYPE);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private QueryFacade qf;
    @Autowired
    protected AccountManager acntMgr;

    @Override
    public L2NetworkType getType() {
        return type;
    }

    @Override
    public L2NetworkInventory createL2Network(L2NetworkVO ovo, APICreateL2NetworkMsg msg) {
        APICreateL2VxlanNetworkMsg amsg = (APICreateL2VxlanNetworkMsg) msg;

        AllocateVniMsg vniMsg = new AllocateVniMsg();
        vniMsg.setL2NetworkUuid(amsg.getPoolUuid());
        vniMsg.setRequiredVni(amsg.getVni());
        bus.makeTargetServiceIdByResourceUuid(vniMsg, L2NetworkConstant.SERVICE_ID, amsg.getPoolUuid());
        MessageReply reply = bus.call(vniMsg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(reply.getError());
        }
        AllocateVniReply r = reply.castReply();

        VxlanNetworkVO vo = new SQLBatchWithReturn<VxlanNetworkVO>() {
            @Override
            protected VxlanNetworkVO scripts() {
                VxlanNetworkVO vo = new VxlanNetworkVO(ovo);
                String uuid = msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid();
                vo.setUuid(uuid);
                vo.setVni(r.getVni());
                vo.setPoolUuid((amsg.getPoolUuid()));
                if (vo.getPhysicalInterface() == null) {
                    vo.setPhysicalInterface("");
                }
                dbf.getEntityManager().persist(vo);

                SimpleQuery<L2NetworkClusterRefVO> q = dbf.createQuery(L2NetworkClusterRefVO.class);
                q.add(L2NetworkClusterRefVO_.l2NetworkUuid, SimpleQuery.Op.EQ, amsg.getPoolUuid());
                final List<L2NetworkClusterRefVO> refs = q.list();
                for (L2NetworkClusterRefVO ref : refs) {
                    L2NetworkClusterRefVO rvo = new L2NetworkClusterRefVO();
                    rvo.setClusterUuid(ref.getClusterUuid());
                    rvo.setL2NetworkUuid(uuid);
                    dbf.getEntityManager().persist(rvo);
                    dbf.getEntityManager().flush();
                    dbf.getEntityManager().refresh(rvo);
                }

                acntMgr.createAccountResourceRef(msg.getSession().getAccountUuid(), vo.getUuid(), VxlanNetworkVO.class);

                dbf.getEntityManager().flush();
                dbf.getEntityManager().refresh(vo);

                return vo;
            }
        }.execute();

        L2VxlanNetworkInventory inv = L2VxlanNetworkInventory.valueOf(vo);
        String info = String.format("successfully create L2VxlanNetwork, %s", JSONObjectUtil.toJsonString(inv));
        logger.debug(info);
        return inv;
    }

    @Override
    public L2Network getL2Network(L2NetworkVO vo) {
        return new VxlanNetwork(vo);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }


    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        List<VmNicInventory> nics = inv.getVmNics();
        List<L3NetworkVO> l3vos = new ArrayList<>();
        nics.stream().forEach((nic -> l3vos.add(Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid ,nic.getL3NetworkUuid()).find())));

        List<String> vxlanUuids = new ArrayList<>();
        for (L3NetworkVO l3 : l3vos) {
            String type = Q.New(L2NetworkVO.class).select(L2NetworkVO_.type).eq(L2NetworkVO_.uuid, l3.getL2NetworkUuid()).findValue();
            if (type.equals(VxlanNetworkConstant.VXLAN_NETWORK_TYPE)) {
                vxlanUuids.add(l3.getL2NetworkUuid());
            }
        }

        if (vxlanUuids.isEmpty()) {
            return;
        }

        ErrorCodeList errList = new ErrorCodeList();
        FutureCompletion completion = new FutureCompletion(null);

        new While<>(vxlanUuids).all((uuid, completion1) -> {
            PrepareL2NetworkOnHostMsg msg = new PrepareL2NetworkOnHostMsg();
            msg.setL2NetworkUuid(uuid);
            msg.setHost(HostInventory.valueOf((HostVO) Q.New(HostVO.class).eq(HostVO_.uuid, destHostUuid).find()));
            bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, uuid);
            bus.send(msg, new CloudBusCallBack(completion1) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().toString());
                        errList.getCauses().add(reply.getError());
                    } else {
                        logger.debug(String.format("check and realize vxlan network[uuid: %s] for vm[uuid: %s] successed", uuid, inv.getUuid()));
                    }
                    completion1.done();

                }
            });
        }).run(new NoErrorCompletion(completion) {
            @Override
            public void done() {
                if (!errList.getCauses().isEmpty()) {
                    completion.fail(errList.getCauses().get(0));
                    return;
                }
                logger.info(String.format("check and realize vxlan networks[uuid: %s] for vm[uuid: %s] done", vxlanUuids, inv.getUuid()));
                completion.success();
            }
        });

        completion.await(TimeUnit.MINUTES.toMillis(30));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(operr("cannot configure vxlan network for vm[uuid:%s] on the destination host[uuid:%s]",
                    inv.getUuid(), destHostUuid).causedBy(completion.getErrorCode()));
        }
    }

    @Override
    public void  beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {
    }

    @Override
    public void  afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
    }

    @Override
    public void  failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
    }

    @Override
    public String getL2NetworkType() {
        return VxlanNetworkConstant.VXLAN_NETWORK_TYPE;
    }

    @Override
    public Integer getDefaultMtu() {
        return Integer.valueOf(NetworkServiceGlobalConfig.DHCP_MTU_VXLAN.getDefaultValue());
    }
}
