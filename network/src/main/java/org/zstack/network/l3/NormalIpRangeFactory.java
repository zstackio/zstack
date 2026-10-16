package org.zstack.network.l3;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;

import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.sdncontroller.SdnControllerConstant;
import org.zstack.header.network.service.SdnControllerDhcp;
import org.zstack.utils.CollectionUtils;

import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORK_L3_10092;

public class NormalIpRangeFactory implements IpRangeFactory {
    @Autowired
    DatabaseFacade dbf;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected L3NetworkManager l3Mgr;
    @Autowired
    private CloudBus bus;


    @Override
    public IpRangeType getType() {
        return IpRangeType.Normal;
    }

    @Override
    public void createIpRange(List<IpRangeInventory> iprs, AddIpRangeMsg msg, ReturnValueCompletion<List<IpRangeInventory>> completion) {
        String resolvedAccountUuid = msg.getSession() == null ? msg.getAccountUuid() : msg.getSession().getAccountUuid();
        if (resolvedAccountUuid == null) {
            completion.fail(argerr(ORG_ZSTACK_NETWORK_L3_10092,
                    "account uuid is required for normal IP range creation"));
            return;
        }
        String accountUuid = resolvedAccountUuid;
        List<NormalIpRangeVO> preparedVos = new ArrayList<>();
        List<IpRangeInventory> preparedIprs = new ArrayList<>();
        for (IpRangeInventory ipr : iprs) {
            NormalIpRangeVO vo = (NormalIpRangeVO) IpRangeHelper.fromIpRangeInventory(ipr, accountUuid);
            ipr.setUuid(vo.getUuid());
            preparedVos.add(vo);
            preparedIprs.add(IpRangeInventory.valueOf(vo));
        }
        msg.putHeaderEntry("znsAddingIpRanges", preparedIprs);
        IpRangeInventory primaryIpr = preparedIprs.get(0);

        L3NetworkVO initialL3Vo = dbf.findByUuid(primaryIpr.getL3NetworkUuid(), L3NetworkVO.class);
        final SdnControllerL3 sdnL3 = initialL3Vo == null ? null : l3Mgr.getSdnControllerL3(initialL3Vo.getL2NetworkUuid());
        final boolean remoteWriteFirst = sdnL3 != null && sdnL3.isRemoteWriteFirst();

        Flow createInSdnFlow = new NoRollbackFlow() {
            String __name__ = "create-ip-range-in-sdn";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (sdnL3 == null) {
                    trigger.next();
                    return;
                }

                sdnL3.createIpRange(primaryIpr, msg.getSystemTags(), msg.getHeaders(), new Completion(trigger) {
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
        };

        Flow notifySdnDhcpFlow = new NoRollbackFlow() {
            String __name__ = "notify-sdn-dhcp-ip";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if ("ZNS_PROJECTION".equals(msg.getHeaderEntry("znsOperationOrigin"))) {
                    trigger.next();
                    return;
                }
                String sdnControllerUuid = L3NetworkHelper.getSdnControllerUuidFromL3Uuid(primaryIpr.getL3NetworkUuid());
                if (sdnControllerUuid == null) {
                    trigger.next();
                    return;
                }

                SdnControllerUpdateDHCPMsg dmsg = new SdnControllerUpdateDHCPMsg();
                dmsg.setL3NetworkUuid(primaryIpr.getL3NetworkUuid());
                dmsg.setIpVersion(primaryIpr.getIpVersion());
                dmsg.setSdnControllerUuid(sdnControllerUuid);
                if (msg.getHeaders() != null && !msg.getHeaders().isEmpty()) {
                    dmsg.setHeaders(new LinkedHashMap<>(msg.getHeaders()));
                }
                bus.makeTargetServiceIdByResourceUuid(dmsg, SdnControllerConstant.SERVICE_ID, sdnControllerUuid);
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                        } else {
                            Object pendingDhcp = reply.getHeaderEntry("znsPendingDhcpDataJson");
                            if (pendingDhcp != null) {
                                msg.putHeaderEntry("znsPendingDhcpDataJson", pendingDhcp);
                            }
                            trigger.next();
                        }
                    }
                });
            }
        };

        Flow saveDbFlow = new Flow() {
            String __name__ = "save-db";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<IpRangeVO> vos = new SQLBatchWithReturn<List<IpRangeVO>>() {
                    @Override
                    protected List<IpRangeVO> scripts() {
                        if (sdnL3 != null) {
                            sdnL3.beforeCommitIpRangeInDb(primaryIpr, msg.getHeaders());
                        }
                        List<IpRangeVO> persisted = new ArrayList<>();
                        for (NormalIpRangeVO vo : preparedVos) {
                            persist(vo);
                            flush();
                            reload(vo);
                            IpRangeHelper.updateL3NetworkIpversion(vo);

                            List<UsedIpVO> usedIpVos = Q.New(UsedIpVO.class)
                                    .eq(UsedIpVO_.l3NetworkUuid, vo.getL3NetworkUuid())
                                    .eq(UsedIpVO_.ipVersion, vo.getIpVersion())
                                    .isNull(UsedIpVO_.ipRangeUuid).list();
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
                            updateVos.forEach(this::merge);
                            persisted.add(vo);
                        }
                        return persisted;
                    }
                }.execute();

                for (IpRangeVO vo : vos) {
                    CollectionUtils.safeForEach(pluginRgty.getExtensionList(AfterAddIpRangeExtensionPoint.class), new ForEachFunction<AfterAddIpRangeExtensionPoint>() {
                        @Override
                        public void run(AfterAddIpRangeExtensionPoint ext) {
                            ext.afterAddIpRange(IpRangeInventory.valueOf(vo), msg.getSystemTags());
                        }
                    });
                }

                data.put("IpRangeVO", vos);
                if (sdnL3 == null) {
                    trigger.next();
                    return;
                }
                sdnL3.afterCommitIpRangeInDb(IpRangeInventory.valueOf(vos.get(0)), msg.getHeaders(), new Completion(trigger) {
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

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                List<IpRangeVO> vos = (List<IpRangeVO>) data.get("IpRangeVO");
                if (vos != null && !vos.isEmpty()) {
                    new SQLBatch() {
                        @Override
                        protected void scripts() {
                            List<String> rangeUuids = new ArrayList<>();
                            for (IpRangeVO vo : vos) {
                                rangeUuids.add(vo.getUuid());
                            }
                            SQL.New(UsedIpVO.class).in(UsedIpVO_.ipRangeUuid, rangeUuids)
                                    .set(UsedIpVO_.ipRangeUuid, null).update();
                            dbf.removeByPrimaryKeysInCurrentTransaction(rangeUuids, IpRangeVO.class);
                            IpRangeHelper.updateL3NetworkIpversion(vos.get(0));
                        }
                    }.execute();
                }
                trigger.rollback();
            }
        };

        FlowChain chain = new SimpleFlowChain();
        chain.setName(String.format("add-iprange-to-l3-%s", primaryIpr.getL3NetworkUuid()));
        if (remoteWriteFirst) {
            chain.then(createInSdnFlow).then(notifySdnDhcpFlow).then(saveDbFlow);
        } else {
            chain.then(saveDbFlow).then(notifySdnDhcpFlow).then(createInSdnFlow);
        }
        chain.error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                if (sdnL3 != null) {
                    sdnL3.afterIpRangeFinally(primaryIpr, msg.getHeaders());
                }
                completion.fail(errCode);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                if (sdnL3 != null) {
                    sdnL3.afterIpRangeFinally(primaryIpr, msg.getHeaders());
                }
                List<IpRangeVO> vos = (List<IpRangeVO>) data.get("IpRangeVO");
                completion.success(IpRangeInventory.valueOf(vos));
            }
        }).start();
    }
}
