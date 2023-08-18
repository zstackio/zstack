package org.zstack.sdnController.h3cVcfc;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.*;
import org.zstack.header.rest.RESTFacade;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.sdnController.SdnController;
import org.zstack.sdnController.header.*;
import org.zstack.sdnController.SdnControllerLog;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class H3cVcfcSdnController implements SdnController {
    private static final CLogger logger = Utils.getLogger(H3cVcfcSdnController.class);

    @Autowired
    private DatabaseFacade dbf;

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
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        headers.put("Cache-Control", "no-cache");

        return headers;
    }

    private Map<String, String> getH3cHeaders(String token) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        headers.put("Cache-Control", "no-cache");
        headers.put("X-Auth-Token", token);

        return headers;
    }

    // from H3cCmd
    private void getH3cVniRanges(Completion completion) {
        H3cVcfcCommands.GetH3cVniRangeCmd cmd = new H3cVcfcCommands.GetH3cVniRangeCmd();
        try {
            H3cVcfcCommands.GetH3cVniRangeRsp rsp = new H3cVcfcHttpClient<>(H3cVcfcCommands.GetH3cVniRangeRsp.class).syncCall("GET", self.getIp(), H3cVcfcCommands.H3C_VCFC_VNI_RANGES, cmd, getH3cHeaders(token));
            if (rsp == null) {
                completion.fail(operr("get vni range on sdn controller [ip:%s] failed", self.getIp()));
                return;
            }

            int count = 0;
            for (H3cVcfcCommands.H3cVniRangeStruct d : rsp.domains) {
                for (H3cVcfcCommands.VniRangeStruct v : d.vlan_map_list) {
                    Integer startVni = Integer.valueOf(v.start_vxlan);
                    Integer endVni = Integer.valueOf(v.end_vxlan);
                    SystemTagCreator creator = H3cVcfcSdnControllerSystemTags.H3C_VNI_RANGE.newSystemTagCreator(self.getUuid());
                    creator.ignoreIfExisting = false;
                    creator.inherent = false;
                    creator.setTagByTokens(
                            map(
                                    e(H3cVcfcSdnControllerSystemTags.H3C_START_VNI_TOKEN, v.start_vxlan),
                                    e(H3cVcfcSdnControllerSystemTags.H3C_END_VNI_TOKEN, v.end_vxlan)
                            )
                    );
                    creator.create();
                    count++;
                }
            }

            if (count == 0) {
                completion.fail(operr("there is no vni range on sdn controller [ip:%s]", self.getIp()));
                return;
            }

            completion.success();
        } catch (Exception e) {
            completion.fail(operr("get sdn controller [ip:%s] vni range failed because %s", self.getIp(), e.getLocalizedMessage()));
        }
    }

    private void getH3cDefaultTenant(Completion completion) {
        H3cVcfcCommands.GetH3cTenantsCmd cmd = new H3cVcfcCommands.GetH3cTenantsCmd();
        try {
            H3cVcfcCommands.GetH3cTenantsRsp rsp = new H3cVcfcHttpClient<>(H3cVcfcCommands.GetH3cTenantsRsp.class).syncCall("GET", self.getIp(), H3cVcfcCommands.H3C_VCFC_TENANTS, cmd, getH3cHeaders(token));
            if (rsp == null) {
                completion.fail(operr("there is no vni range on sdn controller [ip:%s]", self.getIp()));
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
                completion.fail(operr("there is no vni range on sdn controller [ip:%s]", self.getIp()));
                return;
            }

            completion.success();
        } catch (Exception e) {
            completion.fail(operr("there is no default tenant on sdn controller [ip:%s]", self.getIp()));
        }
    }

    private void getH3cParameters(APIAddSdnControllerMsg msg, Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("get-h3c-parameters-%s", self.getIp()));
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
        }).then(new NoRollbackFlow() {
            String __name__ = "get_h3c_default_tenant";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                for(String systemTag : msg.getSystemTags()) {
                    if(H3cVcfcSdnControllerSystemTags.H3C_TENANT_UUID.isMatch(systemTag)) {
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
    @SdnControllerLog
    public void preInitSdnController(APIAddSdnControllerMsg msg, Completion completion) {
        completion.success();
    }

    @Override
    @SdnControllerLog
    public void initSdnController(APIAddSdnControllerMsg msg, Completion completion) {
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
    public void postInitSdnController(APIAddSdnControllerMsg msg, Completion completion) {
        completion.success();
    }

    @Override
    @SdnControllerLog
    public void preCreateVxlanNetwork(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion) {
        completion.success();
    }

    private void createVxlanNetworkOnController(L2VxlanNetworkInventory vxlan, Completion completion) {
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
    private void doCreateVxlanNetworkOnController(L2VxlanNetworkInventory vxlan, Completion completion) {
        String tenantUuid = H3cVcfcSdnControllerSystemTags.H3C_TENANT_UUID.getTokenByResourceUuid(self.getUuid(), H3cVcfcSdnControllerSystemTags.H3C_TENANT_UUID_TOKEN);
        String vdsUuid = H3cVcfcSdnControllerSystemTags.H3C_VDS_UUID.getTokenByResourceUuid(self.getUuid(), H3cVcfcSdnControllerSystemTags.H3C_VDS_TOKEN);
        H3cVcfcCommands.CreateH3cNetworksCmd cmd = new H3cVcfcCommands.CreateH3cNetworksCmd();
        H3cVcfcCommands.NetworkCmd networkCmd = new H3cVcfcCommands.NetworkCmd();
        networkCmd.name = vxlan.getName();
        networkCmd.tenant_id = tenantUuid;
        networkCmd.distributed = true;
        networkCmd.network_type = "VXLAN";
        networkCmd.original_network_type = "VXLAN";
        networkCmd.domain = vdsUuid;
        networkCmd.segmentation_id = vxlan.getVni();
        networkCmd.external = false;
        networkCmd.force_flat = false;

        cmd.networks.add(networkCmd);
        try {
            H3cVcfcCommands.CreateH3cNetworksRsp rsp = new H3cVcfcHttpClient<>(H3cVcfcCommands.CreateH3cNetworksRsp.class).syncCall("POST", leaderIp, H3cVcfcCommands.H3C_VCFC_L2_NETWORKS, cmd, getH3cHeaders(token));
            if (rsp == null) {
                completion.fail(operr("create vxlan network on sdn controller [ip:%s] failed", self.getIp()));
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
            completion.fail(operr("create vxlan network on sdn controller [ip:%s] failed because %s", self.getIp(), e.getMessage()));
        }
    }

    @Override
    @SdnControllerLog
    public void createVxlanNetwork(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion) {
        /* initSdnController get the token */
        getH3cControllerToken(new Completion(completion) {
            @Override
            public void success() {
                createVxlanNetworkOnController(vxlan, completion);
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
    public void attachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion) {
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
    public void detachL2NetworkFromCluster(L2VxlanNetworkInventory vxlan, String clusterUuid, Completion completion) {
        completion.success();
    }

    private void deleteVxlanNetworkOnController(L2VxlanNetworkInventory vxlan, Completion completion) {
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

    private void doDeleteVxlanNetworkOnController(L2VxlanNetworkInventory vxlan, Completion completion) {
        H3cVcfcCommands.DeleteH3cNetworksCmd cmd = new H3cVcfcCommands.DeleteH3cNetworksCmd();
        try {
            String h3cL2NetworkUuid = H3cVcfcSdnControllerSystemTags.H3C_L2_NETWORK_UUID.getTokenByResourceUuid(vxlan.getUuid(), H3cVcfcSdnControllerSystemTags.H3C_L2_NETWORK_UUID_TOKEN);
            H3cVcfcCommands.DeleteH3cNetworksRsp rsp = new H3cVcfcHttpClient<>(H3cVcfcCommands.DeleteH3cNetworksRsp.class).syncCall("DELETE", leaderIp, String.format("%s/%s", H3cVcfcCommands.H3C_VCFC_L2_NETWORKS, h3cL2NetworkUuid), cmd, getH3cHeaders(token));
            if (rsp == null) {
                completion.fail(operr("delete vxlan network on sdn controller [ip:%s] failed", self.getIp()));
                return;
            }

            completion.success();
        } catch (Exception e) {
            completion.fail(operr("delete vxlan network on sdn controller [ip:%s] failed because %s", self.getIp(), e.getMessage()));
        }
    }

    @Override
    @SdnControllerLog
    public void deleteVxlanNetwork(L2VxlanNetworkInventory vxlan, Completion completion) {
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
        List<Map<String, String>> tokenList = H3cVcfcSdnControllerSystemTags.H3C_VNI_RANGE
                .getTokensOfTagsByResourceUuid(controller.getUuid());
        List<SdnVniRange> vniRanges = new ArrayList<>();
        for (Map<String, String> tokens : tokenList) {
            SdnVniRange range = new SdnVniRange();
            range.startVni = Integer.valueOf(tokens.get(H3cVcfcSdnControllerSystemTags.H3C_START_VNI_TOKEN));
            range.endVni = Integer.valueOf(tokens.get(H3cVcfcSdnControllerSystemTags.H3C_END_VNI_TOKEN));
            vniRanges.add(range);
        }
        return vniRanges;
    }

    @Override
    public List<SdnVlanRange> getVlanRange(SdnControllerInventory controller) {
        // H3c: access vlan == vni
        List<Map<String, String>> tokenList = H3cVcfcSdnControllerSystemTags.H3C_VNI_RANGE
                .getTokensOfTagsByResourceUuid(controller.getUuid());
        List<SdnVlanRange> vlanRanges = new ArrayList<>();
        for (Map<String, String> tokens : tokenList) {
            SdnVlanRange range = new SdnVlanRange();
            range.startVlan = Integer.valueOf(tokens.get(H3cVcfcSdnControllerSystemTags.H3C_START_VNI_TOKEN));
            range.endVlan = Integer.valueOf(tokens.get(H3cVcfcSdnControllerSystemTags.H3C_END_VNI_TOKEN));
            vlanRanges.add(range);
        }
        return vlanRanges;
    }

    private void getH3cControllerLeaderIp(Completion completion) {
        H3cVcfcCommands.GetH3cTeamLederIpCmd cmd = new H3cVcfcCommands.GetH3cTeamLederIpCmd();

        try {
            H3cVcfcCommands.GetH3cTeamLederIpReply rsp = new H3cVcfcHttpClient<>(H3cVcfcCommands.GetH3cTeamLederIpReply.class).syncCall("GET", self.getIp(), H3cVcfcCommands.H3C_VCFC_TEAM_LEADERIP, cmd, getH3cHeaders(token));
            if (rsp == null) {
                completion.fail(operr("get leader of sdn controller [ip:%s] failed", self.getIp()));
                return;
            }

            leaderIp = rsp.ip;
            completion.success();
        } catch (Exception e) {
            completion.fail(operr("get token of sdn controller [ip:%s] failed because %s", self.getIp(), e.getMessage()));
        }
    }

    private void getH3cControllerToken(Completion completion) {
        H3cVcfcCommands.GetH3cTokenCmd cmd = new H3cVcfcCommands.GetH3cTokenCmd();
        H3cVcfcCommands.LoginCmd loginCmd = new H3cVcfcCommands.LoginCmd();
        loginCmd.user = self.getUsername();
        loginCmd.password = self.getPassword();
        cmd.login = loginCmd;

        try {
            H3cVcfcCommands.LoginRsp rsp = new H3cVcfcHttpClient<>(H3cVcfcCommands.LoginRsp.class).syncCall("POST", self.getIp(), H3cVcfcCommands.H3C_VCFC_GET_TOKEN, cmd, getH3cHeaders());
            if (rsp == null) {
                completion.fail(operr("get token of sdn controller [ip:%s] failed", self.getIp()));
                return;
            }

            token = rsp.record.token;

            completion.success();
        } catch (Exception e) {
            completion.fail(operr("get token of sdn controller [ip:%s] failed because %s", self.getIp(), e.getMessage()));
        }
    }
}