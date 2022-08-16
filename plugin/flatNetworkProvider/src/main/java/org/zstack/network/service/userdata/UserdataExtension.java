package org.zstack.network.service.userdata;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.*;
import org.zstack.network.securitygroup.SecurityGroupGetDefaultRuleExtensionPoint;
import org.zstack.network.securitygroup.SecurityGroupRuleProtocolType;
import org.zstack.network.securitygroup.SecurityGroupRuleType;
import org.zstack.network.securitygroup.SecurityGroupRuleVO;
import org.zstack.network.service.AbstractNetworkServiceExtension;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by frank on 10/13/2015.
 */
public class UserdataExtension extends AbstractNetworkServiceExtension implements Component, SecurityGroupGetDefaultRuleExtensionPoint {
    private CLogger logger = Utils.getLogger(UserdataExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;

    private Map<String,  UserdataBackend> backends = new HashMap<String, UserdataBackend>();

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    private void populateExtensions() {
        for (UserdataBackend bkd : pluginRgty.getExtensionList(UserdataBackend.class)) {
            UserdataBackend old = backends.get(bkd.getProviderType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicated UserdataBackend[%s, %s] for type[%s]", bkd, old, old.getProviderType()));
            }

            backends.put(bkd.getProviderType().toString(), bkd);
        }
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public NetworkServiceType getNetworkServiceType() {
        return UserdataConstant.USERDATA_TYPE;
    }

    private NetworkServiceProviderInventory findProvider(final VmInstanceSpec spec) {
        L3NetworkInventory defaultL3 = CollectionUtils.find(VmNicSpec.getL3NetworkInventoryOfSpec(spec.getL3Networks()),
                new Function<L3NetworkInventory, L3NetworkInventory>() {
            @Override
            public L3NetworkInventory call(L3NetworkInventory arg) {
                return arg.getUuid().equals(spec.getVmInventory().getDefaultL3NetworkUuid()) ? arg : null;
            }
        });

        for (NetworkServiceL3NetworkRefInventory ref : defaultL3.getNetworkServices()) {
            if (UserdataConstant.USERDATA_TYPE_STRING.equals(ref.getNetworkServiceType())) {
                return NetworkServiceProviderInventory.valueOf(dbf.findByUuid(ref.getNetworkServiceProviderUuid(), NetworkServiceProviderVO.class));
            }
        }

        return null;
    }

    private UserdataBackend getUserdataBackend(String providerType) {
        UserdataBackend bkd = backends.get(providerType);
        if (bkd == null) {
            throw new CloudRuntimeException(String.format("cannot find UserdataBackend for provider[type:%s]", providerType));
        }

        return bkd;
    }

    @Override
    public void applyNetworkService(final VmInstanceSpec servedVm, Map<String, Object> data, Completion completion) {
        L3NetworkInventory defaultL3 = CollectionUtils.find(VmNicSpec.getL3NetworkInventoryOfSpec(servedVm.getL3Networks()),
                new Function<L3NetworkInventory, L3NetworkInventory>() {
            @Override
            public L3NetworkInventory call(L3NetworkInventory arg) {
                return arg.getUuid().equals(servedVm.getVmInventory().getDefaultL3NetworkUuid()) ? arg : null;
            }
        });

        if (defaultL3 == null) {
            // the L3 for operation is not the default L3
            completion.success();
            return;
        }

        // TODO: vDPA do not support Userdata service yet;
        boolean isOvsDpdk = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, defaultL3.getL2NetworkUuid())
                .eq(L2NetworkVO_.vSwitchType, L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK)
                .isExists();
        if (isOvsDpdk) {
            completion.success();
            return;
        }

        if (!defaultL3.getIpVersions().contains(IPv6Constants.IPv4)) {
            // userdata depends on the ipv4 address
            completion.success();
            return;
        }

        NetworkServiceProviderInventory provider = findProvider(servedVm);
        if (provider == null) {
            completion.success();
            return;
        }

        UserdataStruct struct = new UserdataStruct();
        struct.setL3NetworkUuid(servedVm.getVmInventory().getDefaultL3NetworkUuid());
        struct.setParametersFromVmSpec(servedVm);
        struct.setUserdataList(servedVm.getUserdataList());

        UserdataBackend bkd = getUserdataBackend(provider.getType());
        bkd.applyUserdata(struct, completion);
    }

    @Override
    public List<String> getGroupMembers(String sgUuid, int ipVersion) {
        List<String> members = new ArrayList<>();
        if (ipVersion != IPv6Constants.IPv4) {
            return members;
        }

        members.add(NetworkServiceConstants.METADATA_HOST_PREFIX.split("/")[0]);
        return members;
    }

    @Override
    public void releaseNetworkService(final VmInstanceSpec servedVm, Map<String, Object> data, final NoErrorCompletion completion) {
        L3NetworkInventory defaultL3 = CollectionUtils.find(VmNicSpec.getL3NetworkInventoryOfSpec(servedVm.getL3Networks()),
                new Function<L3NetworkInventory, L3NetworkInventory>() {
            @Override
            public L3NetworkInventory call(L3NetworkInventory arg) {
                return arg.getUuid().equals(servedVm.getVmInventory().getDefaultL3NetworkUuid()) ? arg : null;
            }
        });
        if (!Optional.ofNullable(servedVm.getDestHost()).isPresent()){
            completion.done();
            return;
        }
        if (defaultL3 == null) {
            // the L3 for operation is not the default L3
            completion.done();
            return;
        }

        if (!defaultL3.getIpVersions().contains(IPv6Constants.IPv4)) {
            // userdata depends on the ipv4 address
            completion.done();
            return;
        }

        NetworkServiceProviderInventory provider = findProvider(servedVm);
        if (provider == null) {
            completion.done();
            return;
        }

        UserdataStruct struct = new UserdataStruct();
        struct.setL3NetworkUuid(servedVm.getVmInventory().getDefaultL3NetworkUuid());
        struct.setParametersFromVmSpec(servedVm);
        struct.setUserdataList(servedVm.getUserdataList());

        UserdataBackend bkd = getUserdataBackend(provider.getType());
        bkd.releaseUserdata(struct, new Completion(completion) {
            @Override
            public void success() {
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                //TODO add GC
                logger.warn(String.format("unable to release user data for vm[uuid:%s], %s", servedVm.getVmInventory().getUuid(), errorCode));
                completion.done();
            }
        });
    }
}
