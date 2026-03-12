package org.zstack.sdnController.h3cVcfc;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.Message;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.SdnControllerDisableDHCPMsg;
import org.zstack.header.network.l3.SdnControllerDisableDHCPReply;
import org.zstack.header.network.l3.SdnControllerEnableDHCPMsg;
import org.zstack.header.network.l3.SdnControllerEnableDHCPReply;
import org.zstack.header.network.l3.SdnControllerUpdateDHCPMsg;
import org.zstack.header.network.l3.SdnControllerUpdateDHCPReply;
import org.zstack.header.network.sdncontroller.SdnControllerConstant;
import org.zstack.header.network.sdncontroller.SdnControllerDeletionMsg;
import org.zstack.header.network.sdncontroller.SdnControllerInventory;
import org.zstack.header.network.sdncontroller.SdnControllerMessage;
import org.zstack.header.network.sdncontroller.SdnControllerVO;
import org.zstack.header.rest.RESTFacade;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.sdnController.SdnController;
import org.zstack.sdnController.SdnControllerL2;
import org.zstack.sdnController.SdnControllerLog;
import org.zstack.sdnController.SdnControllerPingMsg;
import org.zstack.sdnController.SdnControllerPingReply;
import org.zstack.sdnController.SdnControllerSystemTags;
import org.zstack.sdnController.header.AddSdnControllerMsg;
import org.zstack.sdnController.header.SdnVlanRange;
import org.zstack.sdnController.header.SdnVniRange;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class H3cVcfcSdnController implements SdnController, SdnControllerL2 {
    private static final CLogger logger = Utils.getLogger(H3cVcfcSdnController.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    CloudBus bus;
    @Autowired
    protected RESTFacade restf;

    private SdnControllerVO self;
    private String token;
    private String leaderIp;

    private String buildUrl(String path) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme(H3cVcfcSdnControllerGlobalProperty.H3C_CONTROLLER_SCHEME);
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            ub.host("localhost");
            ub.port(8989);
        } else {
            ub.host(self.getIp());
            ub.port(H3cVcfcSdnControllerGlobalProperty.H3C_CONTROLLER_PORT);
        }

        ub.path(path);
        return ub.build().toUriString();
    }

    public H3cVcfcSdnController(SdnControllerVO self) {
        this.self = self;
    }

    private Map<String, String> getH3cHeaders() {
        return getH3cHeaders(null);
    }

    private Map<String, String> getH3cHeaders(String token) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        headers.put("Cache-Control", "no-cache");
        if (token != null) {
            headers.put("X-Auth-Token", token);
        }
        return headers;
    }

    // from H3cCmd
    public void getH3cVniRanges(Completion completion) {
        H3cVcfcCommands.GetH3cVniRangeCmd cmd = getGetH3cVniRangeCmd();
        try {
            H3cVcfcCommands.GetH3cVniRangeRsp rsp = new H3cVcfcHttpClient<>(getGetH3cVniRangeRspClass())
                    .syncCall(HttpMethod.GET.name(), self.getIp(), getH3cVcfcVniRangesPath(), cmd, getH3cHeaders(token));
            if (rsp == null) {
                completion.fail(operr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10025, "Could not retrieve VNI ranges because the SDN controller [ip:%s] did not respond", self.getIp()));
                return;
            }

            int count = 0;
            for (H3cVcfcCommands.H3cVniRangeStruct d : rsp.domains) {
                for (H3cVcfcCommands.VniRangeStruct v : d.vlan_map_list) {
                    Integer startVni = Integer.valueOf(v.start_vxlan);
                    Integer endVni = Integer.valueOf(v.end_vxlan);
                    SystemTagCreator creator = SdnControllerSystemTags.VNI_RANGE.newSystemTagCreator(self.getUuid());
                    creator.ignoreIfExisting = false;
                    creator.inherent = false;
                    creator.setTagByTokens(
                            map(
                                    e(SdnControllerSystemTags.START_VNI_TOKEN, v.start_vxlan),
                                    e(SdnControllerSystemTags.END_VNI_TOKEN, v.end_vxlan)
                            )
                    );
                    creator.create();
                    count++;
                }
            }

            if (count == 0) {
                completion.fail(operr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10026, "Could not initialize SDN controller because no VNI ranges are configured on controller [ip:%s]", self.getIp()));
                return;
            }

            completion.success();
        } catch (Exception e) {
            completion.fail(operr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10027, "Could not retrieve VNI ranges from SDN controller [ip:%s] because %s", self.getIp(), e.getLocalizedMessage()));
        }
    }

    private void getH3cDefaultTenant(Completion completion) {
        H3cVcfcCommands.GetH3cTenantsCmd cmd = getGetH3cTenantsCmd();
        try {
            H3cVcfcCommands.GetH3cTenantsRsp rsp = new H3cVcfcHttpClient<>(getGetH3cTenantsRspClass())
                    .syncCall(HttpMethod.GET.name(), self.getIp(), getH3cVcfcTenantsPath(), cmd, getH3cHeaders(token));
            if (rsp == null) {
                completion.fail(operr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10028, "Could not retrieve tenants because the SDN controller [ip:%s] did not respond", self.getIp()));
                return;
            }

            boolean found = false;
            for (H3cVcfcCommands.H3cTenantStruct d : rsp.tenants) {
                if (SdnControllerConstant.H3C_VCFC_DEFAULT_TENANT_NAME.equals(d.name)
                        && SdnControllerConstant.H3C_VCFC_DEFAULT_TENANT_TYPE.equals(d.type)) {
                    SystemTagCreator creator = H3cVcfcSdnControllerSystemTags.H3C_TENANT_UUID.newSystemTagCreator(self.getUuid());
                    creator.ignoreIfExisting = false;
                    creator.inherent = false;
                    creator.setTagByTokens(
                            map(
                                    e(H3cVcfcSdnControllerSystemTags.H3C_TENANT_UUID_TOKEN, d.id)
                            )
                    );
                    creator.create();
                    found = true;
                    break;
                }
            }

            if (!found) {
                completion.fail(operr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10029, "Could not initialize SDN controller because no default tenant is configured on controller [ip:%s]", self.getIp()));
                return;
            }

            completion.success();
        } catch (Exception e) {
            completion.fail(operr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10030, "Could not retrieve default tenant from SDN controller [ip:%s] because of a communication error", self.getIp()));
        }
    }

    private void getH3cParameters(AddSdnControllerMsg msg, Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("get-h3c-parameters-%s", self.getIp()));
        // DEBT: NoRollbackFlow — in getH3cParameters
        chain.then(new NoRollbackFlow() {
            String __name__ = "get_h3c_vni_ranges";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                getH3cVniRanges(new Completion(trigger) {
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
        // DEBT: NoRollbackFlow — in getH3cParameters
        }).then(new NoRollbackFlow() {
            String __name__ = "get_h3c_default_tenant";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                for (String systemTag : msg.getSystemTags()) {
                    if (H3cVcfcSdnControllerSystemTags.H3C_TENANT_UUID.isMatch(systemTag)) {
                        trigger.next();
                        return;
                    }
                }
                getH3cDefaultTenant(new Completion(trigger) {
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
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void handleMessage(SdnControllerMessage msg) {
        if (msg instanceof SdnControllerPingMsg) {
            handle((SdnControllerPingMsg)msg);
        } else if (msg instanceof SdnControllerEnableDHCPMsg) {
            handle((SdnControllerEnableDHCPMsg) msg);
        } else if (msg instanceof SdnControllerDisableDHCPMsg) {
            handle((SdnControllerDisableDHCPMsg) msg);
        } else if (msg instanceof SdnControllerUpdateDHCPMsg) {
            handle((SdnControllerUpdateDHCPMsg) msg);
        } else {
            bus.dealWithUnknownMessage((Message) msg);
        }
    }

    @Override
    @SdnControllerLog
    public void preInitSdnController(AddSdnControllerMsg msg, Completion completion) {
        completion.success();
    }

    @Override
    public void createSdnControllerDb(AddSdnControllerMsg msg, SdnControllerVO vo, Completion completion) {
        dbf.persist(vo);
        completion.success();
    }

    @Override
    public void deleteSdnControllerDb(SdnControllerVO vo) {
        dbf.removeByPrimaryKey(vo.getUuid(), SdnControllerVO.class);
    }

    @Override
    @SdnControllerLog
    public void initSdnController(AddSdnControllerMsg msg, Completion completion) {
        getH3cControllerToken(new Completion(completion) {
            @Override
            public void success() {
                getH3cParameters(msg, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    @SdnControllerLog
    public void postInitSdnController(SdnControllerVO vo, Completion completion) {
        completion.success();
    }

    @Override
    @SdnControllerLog
    public void preCreateVxlanNetwork(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion) {
        completion.success();
    }

    private void createVxlanNetworkOnController(L2NetworkInventory vxlan, Completion completion) {
        getH3cControllerLeaderIp(new Completion(completion) {
            @Override
            public void success() {
                doCreateVxlanNetworkOnController(vxlan, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    /* H3C VCFC backup node can not handle the create command  */
    private void doCreateVxlanNetworkOnController(L2NetworkInventory vxlan, Completion completion) {
        VxlanNetworkVO vo = dbf.findByUuid(vxlan.getUuid(), VxlanNetworkVO.class);
        String tenantUuid = H3cVcfcSdnControllerSystemTags.H3C_TENANT_UUID.getTokenByResourceUuid(self.getUuid(), H3cVcfcSdnControllerSystemTags.H3C_TENANT_UUID_TOKEN);
        String vdsUuid = H3cVcfcSdnControllerSystemTags.H3C_VDS_UUID.getTokenByResourceUuid(self.getUuid(), H3cVcfcSdnControllerSystemTags.H3C_VDS_TOKEN);
        H3cVcfcCommands.CreateH3cNetworksCmd cmd = getCreateH3cNetworksCmd();
        H3cVcfcCommands.NetworkCmd networkCmd = getNetworkCmd();
        networkCmd.name = vxlan.getName();
        networkCmd.tenant_id = tenantUuid;
        networkCmd.distributed = true;
        networkCmd.network_type = "VXLAN";
        networkCmd.original_network_type = "VXLAN";
        networkCmd.domain = vdsUuid;
        networkCmd.segmentation_id = vo.getVni();
        networkCmd.external = false;
        networkCmd.force_flat = false;

        cmd.networks.add(networkCmd);
        try {
            H3cVcfcCommands.CreateH3cNetworksRsp rsp = new H3cVcfcHttpClient<>(getCreateH3cNetworksRspClass())
                    .syncCall(HttpMethod.POST.name(), leaderIp, getH3cVcfcL2NetworksPath(), cmd, getH3cHeaders(token));
            if (rsp == null) {
                completion.fail(operr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10031, "Could not create VXLAN network because the SDN controller [ip:%s] did not respond", self.getIp()));
                return;
            }
            H3cVcfcCommands.NetworkCmd network = rsp.networks.get(0);
            SystemTagCreator creator = H3cVcfcSdnControllerSystemTags.H3C_L2_NETWORK_UUID.newSystemTagCreator(vxlan.getUuid());
            creator.ignoreIfExisting = false;
            creator.inherent = false;
            creator.setTagByTokens(
                    map(
                            e(H3cVcfcSdnControllerSystemTags.H3C_L2_NETWORK_UUID_TOKEN, network.id)
                    )
            );
            creator.create();

            completion.success();
        } catch (Exception e) {
            completion.fail(operr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10032, "Could not create VXLAN network on SDN controller [ip:%s] because %s", self.getIp(), e.getMessage()));
        }
    }

    @Override
    @SdnControllerLog
    public void createL2Network(L2NetworkInventory inv, APICreateL2NetworkMsg msg, Completion completion) {
        /* initSdnController get the token */
        getH3cControllerToken(new Completion(completion) {
            @Override
            public void success() {
                createVxlanNetworkOnController(inv, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    @SdnControllerLog
    public void postCreateVxlanNetwork(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion) {
        completion.success();
    }

    @Override
    @SdnControllerLog
    public void preAttachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion) {
        completion.success();
    }

    @Override
    @SdnControllerLog
    public void attachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> clusterUuids, List<String> systemTags, Completion completion) {
        completion.success();
    }

    @Override
    @SdnControllerLog
    public void postAttachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion) {
        completion.success();
    }

    @Override
    @SdnControllerLog
    public void deleteSdnController(SdnControllerDeletionMsg msg, SdnControllerInventory sdn, Completion completion) {
        completion.success();
    }


    @Override
    @SdnControllerLog
    public void detachL2NetworkFromCluster(L2VxlanNetworkInventory vxlan, List<String> clusterUuid, Completion completion) {
        completion.success();
    }

    private void deleteVxlanNetworkOnController(L2NetworkInventory vxlan, Completion completion) {
        getH3cControllerLeaderIp(new Completion(completion) {
            @Override
            public void success() {
                doDeleteVxlanNetworkOnController(vxlan, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void doDeleteVxlanNetworkOnController(L2NetworkInventory vxlan, Completion completion) {
        H3cVcfcCommands.DeleteH3cNetworksCmd cmd = getDeleteH3cNetworksCmd();
        try {
            String h3cL2NetworkUuid = H3cVcfcSdnControllerSystemTags.H3C_L2_NETWORK_UUID.getTokenByResourceUuid(vxlan.getUuid(), H3cVcfcSdnControllerSystemTags.H3C_L2_NETWORK_UUID_TOKEN);
            H3cVcfcCommands.DeleteH3cNetworksRsp rsp = new H3cVcfcHttpClient<>(getDeleteH3cNetworksRspClass())
                    .syncCall(HttpMethod.DELETE.name(), leaderIp, String.format("%s/%s", getH3cVcfcL2NetworksPath(), h3cL2NetworkUuid), cmd, getH3cHeaders(token));
            if (rsp == null) {
                completion.fail(operr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10033, "Could not delete VXLAN network because the SDN controller [ip:%s] did not respond", self.getIp()));
                return;
            }

            completion.success();
        } catch (Exception e) {
            completion.fail(operr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10034, "Could not delete VXLAN network on SDN controller [ip:%s] because %s", self.getIp(), e.getMessage()));
        }
    }

    @Override
    @SdnControllerLog
    public void deleteL2Network(L2NetworkInventory vxlan, Completion completion) {
        /* initSdnController get the token */
        getH3cControllerToken(new Completion(completion) {
            @Override
            public void success() {
                deleteVxlanNetworkOnController(vxlan, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public List<SdnVniRange> getVniRange(SdnControllerInventory controller) {
        List<Map<String, String>> tokenList = SdnControllerSystemTags.VNI_RANGE
                .getTokensOfTagsByResourceUuid(controller.getUuid());
        List<SdnVniRange> vniRanges = new ArrayList<>();
        for (Map<String, String> tokens : tokenList) {
            SdnVniRange range = new SdnVniRange();
            range.startVni = Integer.valueOf(tokens.get(SdnControllerSystemTags.START_VNI_TOKEN));
            range.endVni = Integer.valueOf(tokens.get(SdnControllerSystemTags.END_VNI_TOKEN));
            vniRanges.add(range);
        }
        return vniRanges;
    }

    @Override
    public List<SdnVlanRange> getVlanRange(SdnControllerInventory controller) {
        // H3c: access vlan == vni
        List<Map<String, String>> tokenList = SdnControllerSystemTags.VNI_RANGE
                .getTokensOfTagsByResourceUuid(controller.getUuid());
        List<SdnVlanRange> vlanRanges = new ArrayList<>();
        for (Map<String, String> tokens : tokenList) {
            SdnVlanRange range = new SdnVlanRange();
            range.startVlan = Integer.valueOf(tokens.get(SdnControllerSystemTags.START_VNI_TOKEN));
            range.endVlan = Integer.valueOf(tokens.get(SdnControllerSystemTags.END_VNI_TOKEN));
            vlanRanges.add(range);
        }
        return vlanRanges;
    }

    private void getH3cControllerLeaderIp(Completion completion) {
        H3cVcfcCommands.GetH3cTeamLederIpCmd cmd = getGetH3cTeamLederIpCmd();

        try {
            H3cVcfcCommands.GetH3cTeamLederIpReply rsp = new H3cVcfcHttpClient<>(getGetH3cTeamLederIpReplyClass())
                    .syncCall(HttpMethod.GET.name(), self.getIp(), getH3cVcfcTeamLeaderIpPath(), cmd, getH3cHeaders(token));
            if (rsp == null) {
                completion.fail(operr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10035, "Could not determine cluster leader because the SDN controller [ip:%s] did not respond", self.getIp()));
                return;
            }

            leaderIp = rsp.ip;
            completion.success();
        } catch (Exception e) {
            completion.fail(operr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10036, "Could not determine cluster leader for SDN controller [ip:%s] because %s", self.getIp(), e.getMessage()));
        }
    }

    public void getH3cControllerToken(Completion completion) {
        H3cVcfcCommands.GetH3cTokenCmd cmd = getGetH3cTokenCmd();
        H3cVcfcCommands.LoginCmd loginCmd = getLoginCmd();
        loginCmd.user = self.getUsername();
        loginCmd.password = self.getPassword();
        cmd.login = loginCmd;

        try {
            H3cVcfcCommands.LoginRsp rsp = new H3cVcfcHttpClient<>(getLoginRspClass())
                    .syncCall(HttpMethod.POST.name(), self.getIp(), getH3cVcfcGetTokenPath(), cmd, getH3cHeaders());
            if (rsp == null) {
                completion.fail(operr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10037, "Could not authenticate with SDN controller because controller [ip:%s] did not respond", self.getIp()));
                return;
            }

            token = rsp.record.token;

            completion.success();
        } catch (Exception e) {
            completion.fail(operr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10038, "Could not authenticate with SDN controller [ip:%s] because %s", self.getIp(), e.getMessage()));
        }
    }

    void handle(SdnControllerEnableDHCPMsg msg) {
        SdnControllerEnableDHCPReply reply = new SdnControllerEnableDHCPReply();
        bus.reply(msg, reply);
    }

    void handle(SdnControllerDisableDHCPMsg msg) {
        SdnControllerDisableDHCPReply reply = new SdnControllerDisableDHCPReply();
        bus.reply(msg, reply);
    }

    void handle(SdnControllerUpdateDHCPMsg msg) {
        SdnControllerUpdateDHCPReply reply = new SdnControllerUpdateDHCPReply();
        bus.reply(msg, reply);
    }

    void handle(SdnControllerPingMsg msg) {
        SdnControllerPingReply reply = new SdnControllerPingReply();
        
        getH3cControllerToken(new Completion(msg) {
            @Override
            public void success() {
                reply.setSuccess(true);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    public void reconnectSdnController(Completion completion) {
        getH3cControllerToken(new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    // Protected methods for H3cVcfcCommands access, for subclass override
    protected Class<H3cVcfcCommands.GetH3cVniRangeRsp> getGetH3cVniRangeRspClass() {
        return H3cVcfcCommands.GetH3cVniRangeRsp.class;
    }

    protected H3cVcfcCommands.GetH3cVniRangeCmd getGetH3cVniRangeCmd() {
        return new H3cVcfcCommands.GetH3cVniRangeCmd();
    }

    protected String getH3cVcfcVniRangesPath() {
        return H3cVcfcCommands.H3C_VCFC_VNI_RANGES;
    }

    private Class<? extends H3cVcfcCommands.GetH3cTenantsRsp> getGetH3cTenantsRspClass() {
        return H3cVcfcCommands.GetH3cTenantsRsp.class;
    }

    protected H3cVcfcCommands.GetH3cTenantsCmd getGetH3cTenantsCmd() {
        return new H3cVcfcCommands.GetH3cTenantsCmd();
    }

    protected String getH3cVcfcTenantsPath() {
        return H3cVcfcCommands.H3C_VCFC_TENANTS;
    }

    private H3cVcfcCommands.NetworkCmd getNetworkCmd() {
        return new H3cVcfcCommands.NetworkCmd();
    }

    private H3cVcfcCommands.CreateH3cNetworksCmd getCreateH3cNetworksCmd() {
        return new H3cVcfcCommands.CreateH3cNetworksCmd();
    }

    private Class<? extends H3cVcfcCommands.CreateH3cNetworksRsp> getCreateH3cNetworksRspClass() {
        return H3cVcfcCommands.CreateH3cNetworksRsp.class;
    }

    protected String getH3cVcfcL2NetworksPath() {
        return H3cVcfcCommands.H3C_VCFC_L2_NETWORKS;
    }

    private H3cVcfcCommands.DeleteH3cNetworksCmd getDeleteH3cNetworksCmd() {
        return new H3cVcfcCommands.DeleteH3cNetworksCmd();
    }

    protected Class<H3cVcfcCommands.DeleteH3cNetworksRsp> getDeleteH3cNetworksRspClass() {
        return H3cVcfcCommands.DeleteH3cNetworksRsp.class;
    }

    protected H3cVcfcCommands.GetH3cTeamLederIpCmd getGetH3cTeamLederIpCmd() {
        return new H3cVcfcCommands.GetH3cTeamLederIpCmd();
    }

    protected Class<H3cVcfcCommands.GetH3cTeamLederIpReply> getGetH3cTeamLederIpReplyClass() {
        return H3cVcfcCommands.GetH3cTeamLederIpReply.class;
    }

    protected String getH3cVcfcTeamLeaderIpPath() {
        return H3cVcfcCommands.H3C_VCFC_TEAM_LEADERIP;
    }

    protected H3cVcfcCommands.GetH3cTokenCmd getGetH3cTokenCmd() {
        return new H3cVcfcCommands.GetH3cTokenCmd();
    }

    protected H3cVcfcCommands.LoginCmd getLoginCmd() {
        return new H3cVcfcCommands.LoginCmd();
    }

    protected Class<H3cVcfcCommands.LoginRsp> getLoginRspClass() {
        return H3cVcfcCommands.LoginRsp.class;
    }

    protected String getH3cVcfcGetTokenPath() {
        return H3cVcfcCommands.H3C_VCFC_GET_TOKEN;
    }
}