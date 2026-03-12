package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.FlowException;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.VSwitchType;
import org.zstack.header.vm.VmErrors;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceNicFactory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicType;
import org.zstack.header.vm.VmNicVO;
import org.zstack.identity.Account;
import org.zstack.utils.ExceptionDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.PersistenceException;
import java.sql.SQLIntegrityConstraintViolationException;

import static org.zstack.core.Platform.err;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

public class VmNicFactory implements VmInstanceNicFactory {
    private static final CLogger logger = Utils.getLogger(VmNicFactory.class);
    private static final VSwitchType vSwitchType = new VSwitchType(L2NetworkConstant.VSWITCH_TYPE_LINUX_BRIDGE);
    private static final VmNicType type = new VmNicType(VmInstanceConstant.VIRTUAL_NIC_TYPE);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public VmNicType getType() {
        type.setHasAddon(true);
        vSwitchType.addVmNicType(VmNicType.VmNicSubType.NONE, type);

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
            throw new FlowException(err(ORG_ZSTACK_COMPUTE_VM_10089, VmErrors.ALLOCATE_MAC_ERROR, "unable to find an available mac address after re-try 5 times, too many collisions"));
        }
        vnic = dbf.reload(vnic);
        spec.getDestNics().add(VmNicInventory.valueOf(vnic));
        return vnic;
    }

    public VmNicVO persistAndRetryIfMacCollision(VmNicVO vo) {
        int tries = 5;
        while (tries-- > 0) {
            try {
                return dbf.persistAndRefresh(vo);
            } catch (PersistenceException e) {
                if (ExceptionDSL.isCausedBy(e, SQLIntegrityConstraintViolationException.class, "Duplicate entry")) {
                    logger.debug(String.format("Concurrent mac allocation. Mac[%s] has been allocated, try allocating another one. " +
                            "The error[Duplicate entry] printed by jdbc.spi.SqlExceptionHelper is no harm, " +
                            "we will try finding another mac", vo.getMac()));
                    logger.trace("", e);
                    vo.setMac(MacOperator.generateMacWithDeviceId((short) vo.getDeviceId()));
                } else {
                    throw e;
                }
            }
        }
        return null;
    }
}
