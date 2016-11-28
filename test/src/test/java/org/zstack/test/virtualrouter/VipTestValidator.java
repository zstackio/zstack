package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.VipTO;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipVO;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.List;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VipTestValidator {
    private static final CLogger logger = Utils.getLogger(VipTestValidator.class);

    @Autowired
    private DatabaseFacade dbf;

    public static boolean compareWithoutCheckOwnerEthernetMac(VipTO to, VipInventory inv) {
        return (to.getGateway().equals(inv.getGateway()) && to.getIp().equals(inv.getIp()) && to.getNetmask().equals(inv.getNetmask()));
    }

    public static void validateWithoutCheckOwnerEthernetMac(List<VipTO> actual, VipInventory expected) {
        for (VipTO to : actual) {
            if (compareWithoutCheckOwnerEthernetMac(to, expected)) {
                return;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n========================== Can't find VIP =====================");
        sb.append(String.format("\nexpected: \n%s", JSONObjectUtil.toJsonString(expected)));
        sb.append(String.format("\nactual: \n%s", JSONObjectUtil.toJsonString(actual)));
        sb.append("\n===============================================================");
        logger.warn(sb.toString());
        Assert.fail();
    }

    public boolean compare(VipTO to, VipInventory inv) {
        VirtualRouterVipVO vipvo = dbf.findByUuid(inv.getUuid(), VirtualRouterVipVO.class);
        assert vipvo != null;
        SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
        q.select(VmNicVO_.mac);
        q.add(VmNicVO_.vmInstanceUuid, Op.EQ, vipvo.getVirtualRouterVmUuid());
        q.add(VmNicVO_.l3NetworkUuid, Op.EQ, inv.getL3NetworkUuid());
        String mac = q.findValue();
        assert mac != null;

        return (to.getGateway().equals(inv.getGateway()) && to.getIp().equals(inv.getIp()) && to.getNetmask().equals(inv.getNetmask()) && mac.equals(to.getOwnerEthernetMac()));
    }

    public void validate(List<VipTO> actual, VipInventory expected) {
        for (VipTO to : actual) {
            if (compare(to, expected)) {
                return;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n========================== Can't find VIP =====================");
        sb.append(String.format("\nexpected: \n%s", JSONObjectUtil.toJsonString(expected)));
        sb.append(String.format("\nactual: \n%s", JSONObjectUtil.toJsonString(actual)));
        sb.append("\n===============================================================");
        logger.warn(sb.toString());
        Assert.fail();
    }
}
