package org.zstack.network.service.portforwarding;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.network.service.portforwarding.PortForwardingConstant.Params;
import org.zstack.network.service.vip.VipConstant;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipManager;

import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PortForwardingReturnVipFlow extends NoRollbackFlow {
    @Autowired
    private VipManager vipMgr;

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        VipInventory vip = (VipInventory) data.get(VipConstant.Params.VIP.toString());
        boolean needUnlock = data.containsKey(Params.NEED_UNLOCK_VIP.toString());
        boolean needReleasePeerL3 = data.containsKey(VipConstant.Params.RELEASE_PEER_L3NETWORK.toString());
        if (needUnlock) {
            vipMgr.releaseAndUnlockVip(vip, needReleasePeerL3, new Completion(trigger) {
                @Override
                public void success() {
                    trigger.next();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    trigger.fail(errorCode);
                }
            });
        } else {
            vipMgr.releaseVip(vip, needReleasePeerL3, new Completion(trigger) {
                @Override
                public void success() {
                    trigger.next();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    trigger.fail(errorCode);
                }
            });
        }
    }
}
