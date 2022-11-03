package org.zstack.compute.vm;


import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowException;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.DetachNicFromVmOnHypervisorMsg;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.vm.*;
import org.zstack.identity.Account;
import org.zstack.utils.ExceptionDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.PersistenceException;
import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.err;


public class TfVmNicFactory extends VmNicFactory implements VmJustBeforeDeleteFromDbExtensionPoint {
    private static final CLogger logger = Utils.getLogger(TfVmNicFactory.class);
    private static final VmNicType type = new VmNicType(VmInstanceConstant.TF_VIRTUAL_NIC_TYPE);
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public VmNicType getType() {
        return type;
    }

    @Override
    public VmNicVO createVmNic(VmNicInventory nic, VmInstanceSpec spec, List<UsedIpInventory> ips) {
        String acntUuid = Account.getAccountUuidOfResource(spec.getVmInventory().getUuid());

        VmNicVO vnic = VmInstanceNicFactory.createVmNic(nic);
        vnic.setType(type.toString());
        vnic.setAccountUuid(acntUuid);
        vnic = persistAndRetryIfMacCollision(vnic);
        if (vnic == null) {
            throw new FlowException(err(VmErrors.ALLOCATE_MAC_ERROR, "unable to find an available mac address after re-try 5 times, too many collisions"));
        }

        List<UsedIpVO> ipVOS = new ArrayList<>();
        for (UsedIpInventory ip : ips) {
            /* update usedIpVo */
            UsedIpVO ipVO = dbf.findByUuid(ip.getUuid(), UsedIpVO.class);
            ipVO.setVmNicUuid(vnic.getUuid());
            ipVOS.add(ipVO);
        }
        dbf.updateCollection(ipVOS);

        vnic = dbf.reload(vnic);
        spec.getDestNics().add(VmNicInventory.valueOf(vnic));
        return vnic;
    }

    private VmNicVO persistAndRetryIfMacCollision(VmNicVO vo) {
        int tries = 5;
        while (tries-- > 0) {
            try {
                return dbf.persistAndRefresh(vo);
            } catch (PersistenceException e) {
                if (ExceptionDSL.isCausedBy(e, MySQLIntegrityConstraintViolationException.class, "Duplicate entry")) {
                    logger.debug(String.format("Concurrent mac allocation. Mac[%s] has been allocated, try allocating another one. " +
                            "The error[Duplicate entry] printed by jdbc.spi.SqlExceptionHelper is no harm, " +
                            "we will try finding another mac", vo.getMac()));
                    logger.trace("", e);
                    vo.setMac(NetworkUtils.generateMacWithDeviceId((short) vo.getDeviceId()));
                } else {
                    throw e;
                }
            }
        }
        return null;
    }

    @Override
    public void vmJustBeforeDeleteFromDb(VmInstanceInventory inv) {
        new While<>(inv.getVmNics()).each((nic, wcompl) -> {
            DetachNicFromVmOnHypervisorMsg msg = new DetachNicFromVmOnHypervisorMsg();
            //The host uuid is null, because teh vm is deleted
            msg.setHostUuid(inv.getLastHostUuid());
            msg.setVmInstanceUuid(inv.getUuid());
            msg.setNic(nic);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
            bus.send(msg, new CloudBusCallBack(wcompl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        wcompl.addError(reply.getError());
                        return;
                    }
                    wcompl.done();
                }
            });
        }).run(new WhileDoneCompletion(null) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    logger.info("Notify vrouter success before vm expunged.");
                } else {
                    logger.error("Notify vrouter fail before vm expunged, error code: " + errorCodeList.getCauses().get(0));
                }
            }
        });
    }
}
