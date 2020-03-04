package org.zstack.compute.vm;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaSystemException;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.header.core.workflow.FlowException;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.l3.UsedIpVO_;
import org.zstack.header.vm.*;
import org.zstack.identity.Account;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import static org.zstack.core.Platform.err;

public class VmNicFactory implements VmInstanceNicFactory {
    private static final CLogger logger = Utils.getLogger(VmNicFactory.class);
    private static final VmNicType type = new VmNicType(VmInstanceConstant.VIRTUAL_NIC_TYPE);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public VmNicType getType() {
        return type;
    }

    @Override
    public VmNicVO createVmNic(VmNicInventory nic, VmInstanceSpec spec) {
        String acntUuid = Account.getAccountUuidOfResource(spec.getVmInventory().getUuid());

        VmNicVO vnic = VmInstanceNicFactory.createVmNic(nic);
        vnic.setType(type.toString());
        vnic.setAccountUuid(acntUuid);
        vnic = persistAndRetryIfMacCollision(vnic);
        if (vnic == null) {
            throw new FlowException(err(VmErrors.ALLOCATE_MAC_ERROR, "unable to find an available mac address after re-try 5 times, too many collisions"));
        }
        /* update usedIpVo */
        SQL.New(UsedIpVO.class).eq(UsedIpVO_.uuid, vnic.getUsedIpUuid()).set(UsedIpVO_.vmNicUuid, nic.getUuid()).update();

        vnic = dbf.reload(vnic);
        spec.getDestNics().add(VmNicInventory.valueOf(vnic));
        return vnic;
    }

    private VmNicVO persistAndRetryIfMacCollision(VmNicVO vo) {
        int tries = 5;
        while (tries-- > 0) {
            try {
                return dbf.persistAndRefresh(vo);
            } catch (JpaSystemException e) {
                if (e.getRootCause() instanceof MySQLIntegrityConstraintViolationException &&
                        e.getRootCause().getMessage().contains("Duplicate entry")) {
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
}
