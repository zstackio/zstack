package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.appliancevm.*;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.job.Job;
import org.zstack.core.job.JobContext;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.List;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CreateVirtualRouterJob implements Job {
    private static final CLogger logger = Utils.getLogger(CreateVirtualRouterJob.class);

    @JobContext
    private L3NetworkInventory l3Network;
    @JobContext
    private String accountUuid;
    @JobContext
    private VirtualRouterOfferingInventory offering;

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    private ApplianceVmFacade apvmf;


    private void openFirewall(ApplianceVmSpec aspec, String l3NetworkUuid, int port, ApplianceVmFirewallProtocol protocol) {
        ApplianceVmFirewallRuleInventory r = new ApplianceVmFirewallRuleInventory();
        r.setL3NetworkUuid(l3NetworkUuid);
        r.setStartPort(port);
        r.setEndPort(port);
        r.setProtocol(protocol.toString());
        aspec.getFirewallRules().add(r);
    }

    private void openAdditionalPorts(ApplianceVmSpec aspec, String mgmtNwUuid) {
        final List<String> tcpPorts = VirtualRouterGlobalProperty.TCP_PORTS_ON_MGMT_NIC;
        if (!tcpPorts.isEmpty()) {
            List<Integer> ports = CollectionUtils.transformToList(tcpPorts, new Function<Integer, String>() {
                @Override
                public Integer call(String arg) {
                    return Integer.valueOf(arg);
                }
            });
            for (int p : ports) {
                openFirewall(aspec, mgmtNwUuid, p, ApplianceVmFirewallProtocol.tcp);
            }
        }

        final List<String> udpPorts = VirtualRouterGlobalProperty.UDP_PORTS_ON_MGMT_NIC;
        if (!udpPorts.isEmpty()) {
            List<Integer> ports = CollectionUtils.transformToList(udpPorts, new Function<Integer, String>() {
                @Override
                public Integer call(String arg) {
                    return Integer.valueOf(arg);
                }
            });
            for (int p : ports) {
                openFirewall(aspec, mgmtNwUuid, p, ApplianceVmFirewallProtocol.udp);
            }
        }
    }


    private String makeVirtualRouterName(String l3NetworkUuid) {
        return String.format("virtualRouter.l3.%s", l3NetworkUuid);
    }

    @Override
    public void run(final ReturnValueCompletion<Object> completion) {
        List<String> neededService = l3Network.getNetworkServiceTypesFromProvider(vrMgr.getVirtualRouterProvider().getUuid());
        if (neededService.contains(NetworkServiceType.SNAT.toString()) && offering.getPublicNetworkUuid() == null) {
            String err = String.format("L3Network[uuid:%s, name:%s] requires SNAT service, but default virtual router offering[uuid:%s, name:%s] doesn't have a public network", l3Network.getUuid(), l3Network.getName(), offering.getUuid(), offering.getName());
            logger.warn(err);
            completion.fail(errf.instantiateErrorCode(VirtualRouterErrors.NO_PUBLIC_NETWORK_IN_OFFERING, err));
            return;
        }

        ImageVO imgvo = dbf.findByUuid(offering.getImageUuid(), ImageVO.class);

        final ApplianceVmSpec aspec = new ApplianceVmSpec();
        aspec.setSyncCreate(false);
        aspec.setTemplate(ImageInventory.valueOf(imgvo));
        aspec.setApplianceVmType(VirtualRouterApplianceVmFactory.type);
        aspec.setInstanceOffering(offering);
        aspec.setAccountUuid(accountUuid);
        aspec.setName(makeVirtualRouterName(l3Network.getUuid()));

        L3NetworkInventory mgmtNw = L3NetworkInventory.valueOf(dbf.findByUuid(offering.getManagementNetworkUuid(), L3NetworkVO.class));
        ApplianceVmNicSpec mgmtNicSpec = new ApplianceVmNicSpec();
        mgmtNicSpec.setL3NetworkUuid(mgmtNw.getUuid());
        mgmtNicSpec.setMetaData(VirtualRouterNicMetaData.MANAGEMENT_NIC_MASK.toString());
        aspec.setManagementNic(mgmtNicSpec);

        String mgmtNwUuid = mgmtNw.getUuid();
        String pnwUuid = null;

        // NOTE: don't open 22 port here; 22 port is default opened on mgmt network in virtual router with restricted rules
        // open 22 here will cause a non-restricted rule to be added
        openFirewall(aspec, mgmtNwUuid, 7272, ApplianceVmFirewallProtocol.tcp);
        openAdditionalPorts(aspec, mgmtNwUuid);

        if (offering.getPublicNetworkUuid() != null && !offering.getManagementNetworkUuid().equals(offering.getPublicNetworkUuid())) {
            L3NetworkInventory pnw = L3NetworkInventory.valueOf(dbf.findByUuid(offering.getPublicNetworkUuid(), L3NetworkVO.class));
            ApplianceVmNicSpec pnicSpec = new ApplianceVmNicSpec();
            pnicSpec.setL3NetworkUuid(pnw.getUuid());
            pnicSpec.setMetaData(VirtualRouterNicMetaData.PUBLIC_NIC_MASK.toString());
            aspec.getAdditionalNics().add(pnicSpec);
            pnwUuid = pnicSpec.getL3NetworkUuid();
            aspec.setDefaultRouteL3Network(pnw);
        } else {
            // use management nic for both management and public
            mgmtNicSpec.setMetaData(VirtualRouterNicMetaData.PUBLIC_AND_MANAGEMENT_NIC_MASK.toString());
            pnwUuid = mgmtNwUuid;
            aspec.setDefaultRouteL3Network(mgmtNw);
        }


        if (!l3Network.getUuid().equals(mgmtNwUuid) && !l3Network.getUuid().equals(pnwUuid)) {
            if (neededService.contains(NetworkServiceType.SNAT.toString())) {
                DebugUtils.Assert(!l3Network.getIpRanges().isEmpty(), String.format("how can l3Network[uuid:%s] doesn't have ip range", l3Network.getUuid()));
                IpRangeInventory ipr = l3Network.getIpRanges().get(0);
                ApplianceVmNicSpec nicSpec = new ApplianceVmNicSpec();
                nicSpec.setL3NetworkUuid(l3Network.getUuid());
                nicSpec.setAcquireOnNetwork(false);
                nicSpec.setNetmask(ipr.getNetmask());
                nicSpec.setIp(ipr.getGateway());
                nicSpec.setGateway(ipr.getGateway());
                aspec.getAdditionalNics().add(nicSpec);
            } else {
                ApplianceVmNicSpec nicSpec = new ApplianceVmNicSpec();
                nicSpec.setL3NetworkUuid(l3Network.getUuid());
                aspec.getAdditionalNics().add(nicSpec);
            }
        }

        ApplianceVmNicSpec guestNicSpec = mgmtNicSpec.getL3NetworkUuid().equals(l3Network.getUuid()) ? mgmtNicSpec : CollectionUtils.find(aspec.getAdditionalNics(), new Function<ApplianceVmNicSpec, ApplianceVmNicSpec>() {
            @Override
            public ApplianceVmNicSpec call(ApplianceVmNicSpec arg) {
                return arg.getL3NetworkUuid().equals(l3Network.getUuid()) ? arg : null;
            }
        });

        guestNicSpec.setMetaData(guestNicSpec.getMetaData() == null ? VirtualRouterNicMetaData.GUEST_NIC_MASK.toString()
                : String.valueOf(Integer.valueOf(guestNicSpec.getMetaData()) | VirtualRouterNicMetaData.GUEST_NIC_MASK));

        if (neededService.contains(NetworkServiceType.DHCP.toString())) {
            openFirewall(aspec, l3Network.getUuid(), 68, ApplianceVmFirewallProtocol.udp);
            openFirewall(aspec, l3Network.getUuid(), 67, ApplianceVmFirewallProtocol.udp);
        }
        if (neededService.contains(NetworkServiceType.DNS.toString())) {
            openFirewall(aspec, l3Network.getUuid(), 53, ApplianceVmFirewallProtocol.udp);
        }

        logger.debug(String.format("unable to find running virtual for L3Network[name:%s, uuid:%s], is about to create a new one",  l3Network.getName(), l3Network.getUuid()));
        apvmf.createApplianceVm(aspec, new ReturnValueCompletion<ApplianceVmInventory>(completion) {
            @Override
            public void success(ApplianceVmInventory apvm) {
                String paraDegree = VirtualRouterSystemTags.VR_OFFERING_PARALLELISM_DEGREE.getTokenByResourceUuid(offering.getUuid(), VirtualRouterSystemTags.PARALLELISM_DEGREE_TOKEN);
                if (paraDegree != null) {
                    VirtualRouterSystemTags.VR_PARALLELISM_DEGREE.createTag(apvm.getUuid(), map(e(
                            VirtualRouterSystemTags.PARALLELISM_DEGREE_TOKEN,
                            paraDegree
                    )));
                }

                completion.success(VirtualRouterVmInventory.valueOf(dbf.findByUuid(apvm.getUuid(), VirtualRouterVmVO.class)));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    public L3NetworkInventory getL3Network() {
        return l3Network;
    }

    public void setL3Network(L3NetworkInventory l3Network) {
        this.l3Network = l3Network;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public VirtualRouterOfferingInventory getOffering() {
        return offering;
    }

    public void setOffering(VirtualRouterOfferingInventory offering) {
        this.offering = offering;
    }
}
