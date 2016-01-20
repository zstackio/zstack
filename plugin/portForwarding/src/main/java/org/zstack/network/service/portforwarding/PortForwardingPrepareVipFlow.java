package org.zstack.network.service.portforwarding;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.network.service.portforwarding.PortForwardingConstant.Params;
import org.zstack.network.service.vip.VipConstant;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipManager;

import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PortForwardingPrepareVipFlow implements Flow {
    @Autowired
    private VipManager vipMgr;

    private static final String SUCCESS = PortForwardingPrepareVipFlow.class.getName();

    public void run(final FlowTrigger trigger, final Map data) {
        final VipInventory vip = (VipInventory) data.get(VipConstant.Params.VIP.toString());
        final String serviceProviderType = (String) data.get(VipConstant.Params.VIP_SERVICE_PROVIDER_TYPE.toString());
        final L3NetworkInventory peerL3 = (L3NetworkInventory) data.get(VipConstant.Params.GUEST_L3NETWORK_VIP_FOR.toString());
        boolean needLockVip = data.containsKey(Params.NEED_LOCK_VIP.toString());

        if (needLockVip) {
            vipMgr.lockAndAcquireVip(vip, peerL3, PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE, serviceProviderType, new Completion(trigger) {
                @Override
                public void success() {
                    data.put(SUCCESS, true);
                    trigger.next();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    trigger.fail(errorCode);
                }
            });
        } else {
            vipMgr.acquireVip(vip, peerL3, serviceProviderType, new Completion(trigger) {
                @Override
                public void success() {
                    data.put(SUCCESS, true);
                    trigger.next();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    trigger.fail(errorCode);
                }
            });
        }
    }

    @Override
    public void rollback(final FlowRollback trigger, Map data) {
        final VipInventory vip = (VipInventory) data.get(VipConstant.Params.VIP.toString());

        if (data.containsKey(SUCCESS)) {
            boolean needLockVip = data.containsKey(Params.NEED_LOCK_VIP.toString());
            if (needLockVip) {
                vipMgr.releaseAndUnlockVip(vip, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.rollback();
                    }
                });
            } else {
                vipMgr.releaseVip(vip, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.rollback();
                    }
                });
            }
        } else {
            trigger.rollback();
        }

    }
}
