package org.zstack.sdnController.h3cVcfc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.http.HttpMethod;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.sdncontroller.SdnControllerConstant;
import org.zstack.header.network.sdncontroller.SdnControllerVO;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.sdnController.SdnControllerLog;
import org.zstack.sdnController.SdnControllerSystemTags;
import org.zstack.sdnController.header.*;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.zstack.core.Platform.operr;
import static org.zstack.sdnController.h3cVcfc.H3cVcfcSdnControllerGlobalProperty.H3C_MGT_IP_IS_LEADER_IP;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * H3C VCFC SDN Controller V2 Implementation.
 * Extends the base H3cVcfcSdnController to support newer versions or features.
 */
@Slf4j
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class H3cVcfcV2SdnController extends H3cVcfcSdnController {
    private static final CLogger logger = Utils.getLogger(H3cVcfcV2SdnController.class);

    @Autowired
    private DatabaseFacade dbf;
    private SdnControllerVO self;
    private String token;
    private String leaderIp;

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

    public H3cVcfcV2SdnController(SdnControllerVO self) {
        super(self);
        this.self = self;
        logger.debug(String.format("Instantiated H3C VCFC V2 Controller for VO [uuid:%s, name:%s]", self.getUuid(), self.getName()));
    }

    @Override
    @SdnControllerLog
    public void addL3NetworkIpRange(L3NetworkInventory inv, IpRangeInventory ipr, Completion completion) {
        getH3cControllerToken(new Completion(completion) {
            @Override
            public void success() {
                addL3NetworkIpRangeOnController(inv, ipr, completion);
            }
            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    @SdnControllerLog
    public void deleteL3NetworkIpRange(L3NetworkInventory inv, IpRangeInventory ipr, Completion completion) {
        getH3cControllerToken(new Completion(completion) {
            @Override
            public void success() {
                deleteL3NetworkIpRangeOnController(inv, ipr, completion);
            }
            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    @SdnControllerLog
    public void createL2Network(L2NetworkInventory inv, APICreateL2NetworkMsg msg, Completion completion) {
        /* initSdnController get the token */
        getH3cControllerToken(new Completion(completion) {
            @Override
            public void success() {
                createVxlanNetworkOnController(inv, (APICreateL2HardwareVxlanNetworkMsg) msg, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
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

    private void addL3NetworkIpRangeOnController(L3NetworkInventory inv, IpRangeInventory ipr, Completion completion) {
        try {
            String h3cL2NetworkUuid = H3cVcfcSdnControllerSystemTags.H3C_L2_NETWORK_UUID.getTokenByResourceUuid(inv.getL2NetworkUuid(), H3cVcfcSdnControllerSystemTags.H3C_L2_NETWORK_UUID_TOKEN);
            List<H3cVcfcV2Commands.SubnetCmd> subnets = getSubnetsByNetworkUuid(h3cL2NetworkUuid);
            H3cVcfcV2Commands.SubnetCmd subnet = subnets.stream()
                    .filter(s -> ipr.getNetworkCidr().equals(s.cidr))
                    .findFirst()
                    .orElse(null);
            if (subnet == null) {
                logger.debug(String.format("addL3NetworkIpRangeOnController: subnet %s not exist in controller", ipr.getNetworkCidr()));
                H3cVcfcV2Commands.SubnetCmd newSubnet = new H3cVcfcV2Commands.SubnetCmd();
                newSubnet.cidr = ipr.getNetworkCidr();
                newSubnet.gateway_ip = ipr.getGateway();
                newSubnet.name = ipr.getNetworkCidr();
                newSubnet.network_id = h3cL2NetworkUuid;
                subnet = (H3cVcfcV2Commands.SubnetCmd) createSubnets(asList(newSubnet)).get(0);
            }
            if (subnet == null) {
                completion.fail(operr("Could not add IP range because subnet creation failed on the SDN controller"));
                return;
            }
            if (!Q.New(H3cSdnSubnetIpRangeRefVO.class)
                    .eq(H3cSdnSubnetIpRangeRefVO_.l2NetworkUuid, inv.getL2NetworkUuid())
                    .eq(H3cSdnSubnetIpRangeRefVO_.ipRangeUuid, ipr.getUuid())
                    .eq(H3cSdnSubnetIpRangeRefVO_.sdnControllerUuid, self.getUuid())
                    .eq(H3cSdnSubnetIpRangeRefVO_.subnetUuid, subnet.id)
                    .isExists()) {
                H3cSdnSubnetIpRangeRefVO newRef = new H3cSdnSubnetIpRangeRefVO();
                newRef.setL2NetworkUuid(inv.getL2NetworkUuid());
                newRef.setIpRangeUuid(ipr.getUuid());
                newRef.setSdnControllerUuid(self.getUuid());
                newRef.setSubnetUuid(subnet.id);
                dbf.persist(newRef);
                String h3cL2VRouterUuid = H3cVcfcSdnControllerSystemTags.H3C_L2_VROUTER_UUID.getTokenByResourceUuid(inv.getL2NetworkUuid(), H3cVcfcSdnControllerSystemTags.H3C_L2_VROUTER_UUID_TOKEN);
                if(h3cL2VRouterUuid != null && !inv.getType().equals(L3NetworkConstant.L3_BASIC_NETWORK_TYPE)) {
                    addRouterInterface(h3cL2VRouterUuid, subnet.id);
                }
            }
            completion.success();
        } catch (Exception e) {
            completion.fail(operr("Could not add IP range to L3 network because %s", e.getMessage()));
        }
    }

    private void deleteL3NetworkIpRangeOnController(L3NetworkInventory inv, IpRangeInventory ipr, Completion completion) {
        try {
            String h3cL2NetworkUuid = H3cVcfcSdnControllerSystemTags.H3C_L2_NETWORK_UUID.getTokenByResourceUuid(
                    inv.getL2NetworkUuid(), H3cVcfcSdnControllerSystemTags.H3C_L2_NETWORK_UUID_TOKEN);
            List<H3cVcfcV2Commands.SubnetCmd> subnets = getSubnetsByNetworkUuid(h3cL2NetworkUuid);
            H3cVcfcV2Commands.SubnetCmd subnet = subnets.stream()
                    .filter(s -> ipr.getNetworkCidr().equals(s.cidr))
                    .findFirst()
                    .orElse(null);
            if (subnet == null) {
                dbf.removeCollection(
                        Q.New(H3cSdnSubnetIpRangeRefVO.class).eq(H3cSdnSubnetIpRangeRefVO_.ipRangeUuid, ipr.getUuid()).list(),
                        H3cSdnSubnetIpRangeRefVO.class
                );
            } else {
                List<H3cSdnSubnetIpRangeRefVO> refs = Q.New(H3cSdnSubnetIpRangeRefVO.class)
                        .eq(H3cSdnSubnetIpRangeRefVO_.l2NetworkUuid, inv.getL2NetworkUuid())
                        .eq(H3cSdnSubnetIpRangeRefVO_.sdnControllerUuid, self.getUuid())
                        .eq(H3cSdnSubnetIpRangeRefVO_.subnetUuid, subnet.id).list();
                List<H3cSdnSubnetIpRangeRefVO> refsToDelete = refs.stream()
                        .filter(ref -> ref.getIpRangeUuid().equals(ipr.getUuid()))
                        .collect(Collectors.toList());
                refs.removeAll(refsToDelete);
                if (refs.isEmpty()) {
                    deleteSubnet(subnet.id);
                }
                dbf.removeCollection(refsToDelete, H3cSdnSubnetIpRangeRefVO.class);
            }
            completion.success();
        } catch (Exception e) {
            completion.fail(operr("Could not delete IP range from L3 network because %s", e.getMessage()));
        }
    }

    private void createVxlanNetworkOnController(L2NetworkInventory vxlan, APICreateL2HardwareVxlanNetworkMsg msg, Completion completion) {
        getH3cControllerLeaderIp(new Completion(completion) {
            @Override
            public void success() {
                doCreateVxlanNetworkOnController(vxlan, msg, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void getH3cControllerLeaderIp(Completion completion) {
        if (H3C_MGT_IP_IS_LEADER_IP) {
            leaderIp = self.getIp();
            completion.success();
            return;
        }

        H3cVcfcV2Commands.GetH3cTeamLederIpCmd cmd = getGetH3cTeamLederIpCmd();

        try {
            H3cVcfcV2Commands.GetH3cTeamLederIpReply rsp = new H3cVcfcHttpClient<>(getGetH3cTeamLederIpReplyClass())
                    .syncCall(HttpMethod.GET.name(), self.getIp(), getH3cVcfcTeamLeaderIpPath(), cmd, getH3cHeaders(token));
            if (rsp == null) {
                completion.fail(operr("Could not determine cluster leader because the SDN controller [ip:%s] did not respond", self.getIp()));
                return;
            }

            leaderIp = rsp.ip;
            completion.success();
        } catch (Exception e) {
            completion.fail(operr("Could not determine cluster leader for SDN controller [ip:%s] because %s", self.getIp(), e.getMessage()));
        }
    }

    @Override
    public void getH3cControllerToken(Completion completion) {
        H3cVcfcV2Commands.GetH3cTokenCmd cmd = getGetH3cTokenCmd();
        H3cVcfcV2Commands.LoginCmd loginCmd = getLoginCmd();
        loginCmd.user = self.getUsername();
        loginCmd.password = self.getPassword();
        cmd.login = loginCmd;

        try {
            H3cVcfcV2Commands.LoginRsp rsp = new H3cVcfcHttpClient<>(getLoginRspClass())
                    .syncCall(HttpMethod.POST.name(), self.getIp(), getH3cVcfcGetTokenPath(), cmd, getH3cHeaders());
            if (rsp == null) {
                completion.fail(operr("Could not authenticate with SDN controller because controller [ip:%s] did not respond", self.getIp()));
                return;
            }

            token = rsp.record.token;

            completion.success();
        } catch (Exception e) {
            completion.fail(operr("Could not authenticate with SDN controller [ip:%s] because %s", self.getIp(), e.getMessage()));
        }
    }

    @Override
    public void getH3cVniRanges(Completion completion) {
        H3cVcfcV2Commands.GetH3cVniRangeCmd cmd = getGetH3cVniRangeCmd();
        try {
            H3cVcfcV2Commands.GetH3cVniRangeRsp rsp = new H3cVcfcHttpClient<>(getGetH3cVniRangeRspClass())
                    .syncCall(HttpMethod.GET.name(), self.getIp(), getH3cVcfcVniRangesPath(), cmd, getH3cHeaders(token));
            if (rsp == null) {
                completion.fail(operr("Could not retrieve VNI ranges because the SDN controller [ip:%s] did not respond", self.getIp()));
                return;
            }
            // Delete previous inherent VNI range tags before creating new ones
            SdnControllerSystemTags.VNI_RANGE.delete(self.getUuid());
            int count = 0;
            for (H3cVcfcV2Commands.H3cVniRangeStruct d : rsp.domains) {
                for (H3cVcfcV2Commands.VniRangeStruct v : d.vlan_map_list) {
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
                completion.fail(operr("Could not initialize SDN controller because no VNI ranges are configured on controller [ip:%s]", self.getIp()));
                return;
            }

            completion.success();
        } catch (Exception e) {
            completion.fail(operr("Could not retrieve VNI ranges from SDN controller [ip:%s] because %s", self.getIp(), e.getLocalizedMessage()));
        }
    }

    private void getH3cParameters(APIAddSdnControllerMsg msg, Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("get-h3c-parameters-%s", self.getIp()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "get_h3c_controller_token";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                getH3cControllerToken(new Completion(trigger) {
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

    private void doCreateVxlanNetworkOnController(L2NetworkInventory vxlan, APICreateL2HardwareVxlanNetworkMsg msg, Completion completion) {
        VxlanNetworkVO vo = dbf.findByUuid(vxlan.getUuid(), VxlanNetworkVO.class);
        if (msg.getH3cTenantUuid() == null) {
            completion.fail(operr("Could not create hardware VXLAN network because H3C tenant UUID is a mandatory parameter for the H3C VCFC V2 controller"));
            return;
        }
        H3cSdnControllerTenantVO tenantVO = Q.New(H3cSdnControllerTenantVO.class)
                .eq(H3cSdnControllerTenantVO_.uuid, msg.getH3cTenantUuid())
                .eq(H3cSdnControllerTenantVO_.sdnControllerUuid, self.getUuid())
                .limit(1)
                .find();
        if (tenantVO == null) {
            completion.fail(operr("Could not create hardware VXLAN network because the specified tenant does not exist in the SDN controller"));
            return;
        }

        H3cVcfcV2Commands.CreateH3cNetworksCmd cmd = getCreateH3cNetworksCmd();
        H3cVcfcV2Commands.NetworkCmd networkCmd = getNetworkCmd();
        networkCmd.name = vxlan.getName();
        networkCmd.tenant_id = tenantVO.getTenantUuid();
        networkCmd.distributed = true;
        networkCmd.network_type = "VXLAN";
        networkCmd.original_network_type = "VXLAN";
        networkCmd.domain = tenantVO.getVdsUuid();
        networkCmd.segmentation_id = vo.getVni();
        networkCmd.external = false;
        networkCmd.force_flat = false;

        cmd.networks.add(networkCmd);
        try {
            H3cVcfcV2Commands.CreateH3cNetworksRsp rsp = new H3cVcfcHttpClient<>(getCreateH3cNetworksRspClass())
                    .syncCall(HttpMethod.POST.name(), leaderIp, getH3cVcfcL2NetworksPath(), cmd, getH3cHeaders(token));
            if (rsp == null) {
                completion.fail(operr("Could not create VXLAN network because the SDN controller [ip:%s] did not respond", self.getIp()));
                return;
            }
            H3cVcfcV2Commands.NetworkCmd network = rsp.networks.get(0);
            SystemTagCreator creator = H3cVcfcSdnControllerSystemTags.H3C_L2_NETWORK_UUID.newSystemTagCreator(vxlan.getUuid());
            creator.ignoreIfExisting = false;
            creator.inherent = false;
            creator.setTagByTokens(
                    map(
                            e(H3cVcfcSdnControllerSystemTags.H3C_L2_NETWORK_UUID_TOKEN, network.id)
                    )
            );
            creator.create();

            String routerId = createVirtualRouterOnController(SdnControllerConstant.SDN_CONTROLLER_VROUTER_PREFIX + network.name, tenantVO.getUuid());
            if (routerId != null) {
                SystemTagCreator routerTagCreator = H3cVcfcSdnControllerSystemTags.H3C_L2_VROUTER_UUID.newSystemTagCreator(vxlan.getUuid());
                routerTagCreator.ignoreIfExisting = false;
                routerTagCreator.inherent = false;
                routerTagCreator.setTagByTokens(map(e(H3cVcfcSdnControllerSystemTags.H3C_L2_VROUTER_UUID_TOKEN, routerId)));
                routerTagCreator.create();
            }

            if (tenantVO != null) {
                SystemTagCreator tenantTagCreator = H3cVcfcSdnControllerSystemTags.H3C_L2_TENANT_UUID.newSystemTagCreator(vxlan.getUuid());
                tenantTagCreator.ignoreIfExisting = false;
                tenantTagCreator.inherent = false;
                tenantTagCreator.setTagByTokens(map(e(H3cVcfcSdnControllerSystemTags.H3C_L2_TENANT_UUID_TOKEN, tenantVO.getTenantUuid())));
                tenantTagCreator.create();
            }
            completion.success();
        } catch (Exception e) {
            completion.fail(operr("Could not create VXLAN network on SDN controller [ip:%s] because %s", self.getIp(), e.getMessage()));
        }
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
        H3cVcfcV2Commands.DeleteH3cNetworksCmd cmd = getDeleteH3cNetworksCmd();
        try {
            String h3cL2NetworkUuid = H3cVcfcSdnControllerSystemTags.H3C_L2_NETWORK_UUID.getTokenByResourceUuid(vxlan.getUuid(), H3cVcfcSdnControllerSystemTags.H3C_L2_NETWORK_UUID_TOKEN);
            H3cVcfcV2Commands.DeleteH3cNetworksRsp rsp = new H3cVcfcHttpClient<>(getDeleteH3cNetworksRspClass())
                    .syncCall(HttpMethod.DELETE.name(), leaderIp, String.format("%s/%s", getH3cVcfcL2NetworksPath(), h3cL2NetworkUuid), cmd, getH3cHeaders(token));

            String h3cL2VRouterUuid = H3cVcfcSdnControllerSystemTags.H3C_L2_VROUTER_UUID.getTokenByResourceUuid(vxlan.getUuid(), H3cVcfcSdnControllerSystemTags.H3C_L2_VROUTER_UUID_TOKEN);
            if(h3cL2VRouterUuid != null) {
                deleteVirtualRouterOnController(h3cL2VRouterUuid);
            }

            completion.success();
        } catch (Exception e) {
            completion.fail(operr("Could not delete VXLAN network on SDN controller [ip:%s] because %s", self.getIp(), e.getMessage()));
        }
    }

    private H3cVcfcCommands.DeleteH3cNetworksCmd getDeleteH3cNetworksCmd() {
        return new H3cVcfcV2Commands.DeleteH3cNetworksCmd();
    }

    private H3cVcfcV2Commands.NetworkCmd getNetworkCmd() {
        return new H3cVcfcV2Commands.NetworkCmd();
    }

    private H3cVcfcV2Commands.CreateH3cNetworksCmd getCreateH3cNetworksCmd() {
        return new H3cVcfcV2Commands.CreateH3cNetworksCmd();
    }

    private Class<? extends H3cVcfcV2Commands.CreateH3cNetworksRsp> getCreateH3cNetworksRspClass() {
        return H3cVcfcV2Commands.CreateH3cNetworksRsp.class;
    }

    private Class<? extends H3cVcfcV2Commands.GetH3cTenantsRsp> getGetH3cTenantsRspClass() {
        return H3cVcfcV2Commands.GetH3cTenantsRsp.class;
    }

    private Class<? extends H3cVcfcV2Commands.GetH3cSubnetsRsp> getGetH3cSubnetsRspClass() {
        return H3cVcfcV2Commands.GetH3cSubnetsRsp.class;
    }

    @Override
    protected String getH3cVcfcTenantsPath() {
        return H3cVcfcV2Commands.H3C_VCFC_TENANTS;
    }

    public List<H3cVcfcV2Commands.H3cTenantStruct> getAllH3cTenants() {
        H3cVcfcV2Commands.GetH3cTenantsCmd cmd = new H3cVcfcV2Commands.GetH3cTenantsCmd();
        try {
            H3cVcfcV2Commands.GetH3cTenantsRsp rsp = new H3cVcfcHttpClient<>(getGetH3cTenantsRspClass())
                    .syncCall(HttpMethod.GET.name(), self.getIp(), H3cVcfcV2Commands.H3C_VCFC_TENANTS, cmd, getH3cHeaders(token));
            if (rsp == null || rsp.tenants == null) {
                return new ArrayList<>();
            }
            
            // Validate response by checking for default tenant presence
            boolean hasDefaultTenant = rsp.tenants.stream()
                    .anyMatch(tenant -> SdnControllerConstant.H3C_SDN_CONTROLLER_DEFAULT_TENANT_ID.equals(tenant.id));
            
            if (!hasDefaultTenant) {
                throw new RuntimeException(String.format("No default tenant found in SDN controller [ip:%s]", self.getIp()));
            }
            
            return rsp.tenants;
        } catch (Exception e) {
            logger.error("Failed to get all tenants from SDN controller: " + e.getMessage(), e);
            throw new RuntimeException(String.format("failed to get all tenants from SDN controller [ip:%s], error: %s", self.getIp(), e.getMessage()), e);
        }
    }

    public List<H3cVcfcV2Commands.H3cVdsStruct> getAllH3cVds() {
        H3cVcfcV2Commands.GetH3cVdsCmd cmd = new H3cVcfcV2Commands.GetH3cVdsCmd();
        try {
            H3cVcfcV2Commands.GetH3cVdsRsp rsp = new H3cVcfcHttpClient<>(H3cVcfcV2Commands.GetH3cVdsRsp.class)
                    .syncCall(HttpMethod.GET.name(), self.getIp(), H3cVcfcV2Commands.H3C_VCFC_VDS, cmd, getH3cHeaders(token));
            if (rsp == null || rsp.vds == null) {
                return new ArrayList<>();
            }
            
            return rsp.vds;
        } catch (Exception e) {
            logger.error("Failed to get all VDS from SDN controller: " + e.getMessage(), e);
            throw new RuntimeException(String.format("failed to get all VDS from SDN controller [ip:%s], error: %s", self.getIp(), e.getMessage()), e);
        }
    }

    private List<H3cVcfcV2Commands.SubnetCmd> getSubnetsByNetworkUuid(String networkUuid) {
        H3cVcfcV2Commands.GetH3cSubnetsCmd cmd = new H3cVcfcV2Commands.GetH3cSubnetsCmd();
        cmd.network_id = networkUuid;
        try {
            H3cVcfcV2Commands.GetH3cSubnetsRsp rsp = new H3cVcfcHttpClient<>(getGetH3cSubnetsRspClass())
                    .syncCall(HttpMethod.GET.name(), self.getIp(), H3cVcfcV2Commands.H3C_VCFC_SUBNETS, cmd, getH3cHeaders(token));
            if (rsp == null || rsp.subnets == null) {
                return new ArrayList<>();
            }
            return rsp.subnets;
        } catch (Exception e) {
            logger.error("Failed to get all subnets from SDN controller: " + e.getMessage(), e);
            throw new RuntimeException(String.format("failed to get all subnets from SDN controller [ip:%s], error: %s", self.getIp(), e.getMessage()), e);
        }
    }

    private List<H3cVcfcV2Commands.SubnetCmd> createSubnets(List<H3cVcfcV2Commands.SubnetCmd> subnets) {
        H3cVcfcV2Commands.CreateH3cSubnetsCmd cmd = new H3cVcfcV2Commands.CreateH3cSubnetsCmd();
        cmd.subnets = subnets;
        try {
            H3cVcfcV2Commands.CreateH3cSubnetsRsp rsp = new H3cVcfcHttpClient<>(H3cVcfcV2Commands.CreateH3cSubnetsRsp.class)
                    .syncCall(HttpMethod.POST.name(), self.getIp(), H3cVcfcV2Commands.H3C_VCFC_SUBNETS, cmd, getH3cHeaders(token));
            if (rsp == null || rsp.subnets == null) {
                return new ArrayList<>();
            }
            return rsp.subnets;
        } catch (Exception e) {
            logger.error("Failed to create subnets on SDN controller: " + e.getMessage(), e);
            throw new RuntimeException(String.format("failed to create subnets on SDN controller [ip:%s], error: %s", self.getIp(), e.getMessage()), e);
        }
    }

    private void deleteSubnet(String subnetId) {
        H3cVcfcCommands.H3cCmd cmd = new H3cVcfcCommands.H3cCmd();
        try {
            H3cVcfcCommands.H3cRsp rsp = new H3cVcfcHttpClient<>(H3cVcfcCommands.H3cRsp.class)
                    .syncCall(HttpMethod.DELETE.name(), self.getIp(), String.format("%s/%s", H3cVcfcV2Commands.H3C_VCFC_SUBNETS, subnetId), cmd, getH3cHeaders(token));
        } catch (Exception e) {
            logger.error("Failed to delete subnets on SDN controller: " + e.getMessage(), e);
            throw new RuntimeException(String.format("failed to delete subnets on SDN controller [ip:%s], error: %s", self.getIp(), e.getMessage()), e);
        }
    }

    private String createVirtualRouterOnController(String routerName, String tenantUuid) {
        H3cSdnControllerTenantVO tenantVO = Q.New(H3cSdnControllerTenantVO.class)
                .eq(H3cSdnControllerTenantVO_.uuid, tenantUuid)
                .eq(H3cSdnControllerTenantVO_.sdnControllerUuid, self.getUuid())
                .limit(1)
                .find();
        if (tenantVO == null) {
            logger.warn(String.format("cannot find tenant in sdn controller for virtual router [name:%s]", routerName));
            return null;
        }

        H3cVcfcV2Commands.CreateH3cRoutersCmd cmd = new H3cVcfcV2Commands.CreateH3cRoutersCmd();
        H3cVcfcV2Commands.RouterCmd routerCmd = new H3cVcfcV2Commands.RouterCmd();
        routerCmd.name = routerName;
        routerCmd.tenant_id = tenantVO.getTenantUuid();

        cmd.routers.add(routerCmd);
        try {
            H3cVcfcV2Commands.CreateH3cRoutersRsp rsp = new H3cVcfcHttpClient<>(H3cVcfcV2Commands.CreateH3cRoutersRsp.class)
                    .syncCall(HttpMethod.POST.name(), leaderIp, H3cVcfcV2Commands.H3C_VCFC_ROUTERS, cmd, getH3cHeaders(token));
            if (rsp == null || rsp.routers == null || rsp.routers.isEmpty()) {
                logger.warn(String.format("create virtual router on sdn controller [ip:%s] failed", self.getIp()));
                return null;
            }

            H3cVcfcV2Commands.RouterCmd router = rsp.routers.get(0);
            logger.info(String.format("Successfully created virtual router [name:%s, id:%s] on sdn controller [ip:%s]",
                    router.name, router.id, self.getIp()));
            return router.id;
        } catch (Exception e) {
            logger.warn(String.format("create virtual router on sdn controller [ip:%s] failed because %s", self.getIp(), e.getMessage()));
        }
        return null;
    }

    private void deleteVirtualRouterOnController(String routerId) {
        H3cVcfcV2Commands.DeleteH3cRoutersCmd cmd = new H3cVcfcV2Commands.DeleteH3cRoutersCmd();
        try {
            H3cVcfcV2Commands.DeleteH3cRoutersRsp rsp = new H3cVcfcHttpClient<>(H3cVcfcV2Commands.DeleteH3cRoutersRsp.class)
                    .syncCall(HttpMethod.DELETE.name(), leaderIp, String.format("%s/%s", H3cVcfcV2Commands.H3C_VCFC_ROUTERS, routerId), cmd, getH3cHeaders(token));
            if (rsp == null) {
                logger.warn(String.format("delete virtual router on sdn controller [ip:%s] failed", self.getIp()));
                return;
            }

            logger.info(String.format("Successfully deleted virtual router [id:%s] on sdn controller [ip:%s]",
                    routerId, self.getIp()));

        } catch (Exception e) {
            logger.warn(String.format("delete virtual router on sdn controller [ip:%s] failed because %s", self.getIp(), e.getMessage()));
        }
    }

    private void addRouterInterface(String routerId, String subnetId) {
        H3cVcfcV2Commands.AddRouterInterfaceCmd cmd = new H3cVcfcV2Commands.AddRouterInterfaceCmd();
        cmd.subnet_id = subnetId;

        try {
            H3cVcfcV2Commands.AddRouterInterfaceRsp rsp = new H3cVcfcHttpClient<>(H3cVcfcV2Commands.AddRouterInterfaceRsp.class)
                    .syncCall(HttpMethod.PUT.name(), self.getIp(), String.format("%s/%s/add_router_interface", H3cVcfcV2Commands.H3C_VCFC_ROUTERS, routerId), cmd, getH3cHeaders(token));
            if (rsp == null) {
                logger.warn(String.format("add router interface on sdn controller [ip:%s] failed for router [id:%s] subnet [id:%s]",
                        self.getIp(), routerId, subnetId));
            } else {
                logger.info(String.format("Successfully added router interface for router [id:%s] subnet [id:%s] on sdn controller [ip:%s]",
                        routerId, subnetId, self.getIp()));
            }
        } catch (Exception e) {
            logger.warn(String.format("add router interface on sdn controller [ip:%s] failed for router [id:%s] subnet [id:%s] because %s",
                    self.getIp(), routerId, subnetId, e.getMessage()));
        }
    }

    private void removeRouterInterface(String routerId, String subnetId) {
        H3cVcfcV2Commands.RemoveRouterInterfaceCmd cmd = new H3cVcfcV2Commands.RemoveRouterInterfaceCmd();
        cmd.subnet_id = subnetId;

        try {
            H3cVcfcV2Commands.RemoveRouterInterfaceRsp rsp = new H3cVcfcHttpClient<>(H3cVcfcV2Commands.RemoveRouterInterfaceRsp.class)
                    .syncCall(HttpMethod.PUT.name(), self.getIp(), String.format("%s/%s/remove_router_interface", H3cVcfcV2Commands.H3C_VCFC_ROUTERS, routerId), cmd, getH3cHeaders(token));
            if (rsp == null) {
                logger.warn(String.format("remove router interface on sdn controller [ip:%s] failed for router [id:%s] subnet [id:%s]",
                        self.getIp(), routerId, subnetId));
            } else {
                logger.info(String.format("Successfully removed router interface for router [id:%s] subnet [id:%s] on sdn controller [ip:%s]",
                        routerId, subnetId, self.getIp()));
            }
        } catch (Exception e) {
            logger.warn(String.format("remove router interface on sdn controller [ip:%s] failed for router [id:%s] subnet [id:%s] because %s",
                    self.getIp(), routerId, subnetId, e.getMessage()));
        }
    }
}
