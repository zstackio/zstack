package org.zstack.network.l3;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.SdnControllerDhcp;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NormalIpRangeFactory implements IpRangeFactory {
    @Autowired
    DatabaseFacade dbf;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected L3NetworkManager l3Mgr;

    @Override
    public IpRangeType getType() {
        return IpRangeType.Normal;
    }

    @Override
    public void createIpRange(List<IpRangeInventory> iprs, APICreateMessage msg, ReturnValueCompletion<List<IpRangeInventory>> completion) {
        L3NetworkVO l3vo = dbf.findByUuid(iprs.get(0).getL3NetworkUuid(), L3NetworkVO.class);

        FlowChain chain = new SimpleFlowChain();
        chain.setName(String.format("add-iprange-to-l3-%s", iprs.get(0).getL3NetworkUuid()));
        chain.then(new Flow() {
            String __name__ = "save-db";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<IpRangeVO> vos = new ArrayList<>();
                for (IpRangeInventory ipr : iprs) {
                    NormalIpRangeVO vo = new SQLBatchWithReturn<NormalIpRangeVO>() {
                        @Override
                        protected NormalIpRangeVO scripts() {
                            NormalIpRangeVO vo = (NormalIpRangeVO) IpRangeHelper
                                    .fromIpRangeInventory(ipr, msg.getSession().getAccountUuid());
                            dbf.getEntityManager().persist(vo);
                            dbf.getEntityManager().flush();
                            dbf.getEntityManager().refresh(vo);

                            return vo;
                        }
                    }.execute();

                    IpRangeHelper.updateL3NetworkIpversion(vo);

                    List<UsedIpVO> usedIpVos = Q.New(UsedIpVO.class)
                            .eq(UsedIpVO_.l3NetworkUuid, vo.getL3NetworkUuid())
                            .eq(UsedIpVO_.ipVersion, vo.getIpVersion()).list();
                    List<UsedIpVO> updateVos = new ArrayList<>();
                    for (UsedIpVO ipvo : usedIpVos) {
                        if (ipvo.getIpVersion() == IPv6Constants.IPv4) {
                            if (NetworkUtils.isInRange(ipvo.getIp(), vo.getStartIp(), vo.getEndIp())) {
                                ipvo.setIpRangeUuid(vo.getUuid());
                                updateVos.add(ipvo);
                            }
                        } else {
                            if (IPv6NetworkUtils.isIpv6InRange(ipvo.getIp(), vo.getStartIp(), vo.getEndIp())) {
                                ipvo.setIpRangeUuid(vo.getUuid());
                                updateVos.add(ipvo);
                            }
                        }
                    }

                    if (!updateVos.isEmpty()) {
                        dbf.updateCollection(updateVos);
                    }

                    CollectionUtils.safeForEach(pluginRgty.getExtensionList(AfterAddIpRangeExtensionPoint.class), new ForEachFunction<AfterAddIpRangeExtensionPoint>() {
                        @Override
                        public void run(AfterAddIpRangeExtensionPoint ext) {
                            ext.afterAddIpRange(IpRangeInventory.valueOf(vo), msg.getSystemTags());
                        }
                    });

                    vos.add(vo);
                }

                data.put("IpRangeVO", vos);
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                List<IpRangeVO> vos = (List<IpRangeVO>) data.get("IpRangeVO");
                dbf.removeCollection(vos, IpRangeVO.class);
                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "enable-sdn-dhcp";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (!l3vo.enableIpAddressAllocation()) {
                    trigger.next();
                    return;
                }

                SdnControllerDhcp sdnDhcp = l3Mgr.getSdnControllerDhcp(l3vo.getUuid());
                if (sdnDhcp == null) {
                    trigger.next();
                    return;
                }

                sdnDhcp.enableDhcp(Collections.singletonList(L3NetworkInventory.valueOf(l3vo)), false, new Completion(trigger) {
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
        }).then(new NoRollbackFlow() {
            String __name__ = "add-sdn-subnet";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                SdnControllerL3 sdnL3 = l3Mgr.getSdnControllerL3(l3vo.getL2NetworkUuid());
                if (sdnL3 == null) {
                    trigger.next();
                    return;
                }

                List<IpRangeVO> vos = (List<IpRangeVO>) data.get("IpRangeVO");
                sdnL3.createIpRange(IpRangeInventory.valueOf(vos.get(0)), new Completion(trigger) {
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
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                List<IpRangeVO> vos = (List<IpRangeVO>) data.get("IpRangeVO");
                completion.success(IpRangeInventory.valueOf(vos));
            }
        }).start();
    }
}
