package org.zstack.network.service.userdata;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.UserdataBuilder;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.VmAfterAttachNicExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicSpec;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.network.securitygroup.SecurityGroupGetDefaultRuleExtensionPoint;
import org.zstack.network.service.AbstractNetworkServiceExtension;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by frank on 10/13/2015.
 */
public class UserdataExtension extends AbstractNetworkServiceExtension implements Component,
        SecurityGroupGetDefaultRuleExtensionPoint, VmAfterAttachNicExtensionPoint {
    private static final int APPLY_USERDATA_AFTER_ATTACH_NIC_RETRY_TIMES = 3;
    private static final long APPLY_USERDATA_AFTER_ATTACH_NIC_INITIAL_DELAY_SECONDS = 0;
    private static final long APPLY_USERDATA_AFTER_ATTACH_NIC_RETRY_DELAY_SECONDS = 5;

    private CLogger logger = Utils.getLogger(UserdataExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ThreadFacade thdf;

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

    private NetworkServiceProviderInventory findProvider(final L3NetworkInventory defaultL3) {
        if (defaultL3 == null || defaultL3.getNetworkServices() == null) {
            return null;
        }

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
                arg -> arg.getUuid().equals(servedVm.getVmInventory().getDefaultL3NetworkUuid()) ? arg : null);
        VmNicInventory defaultNic = null;
        if (defaultL3 == null && servedVm.getVmInventory().getDefaultL3NetworkUuid() != null && UserdataGlobalProperty.APPLY_WITH_NONE_DEFAULT_NIC) {
            L3NetworkVO l3 = Q.New(L3NetworkVO.class)
                    .eq(L3NetworkVO_.uuid, servedVm.getVmInventory().getDefaultL3NetworkUuid())
                    .find();
            if (l3 != null) {
                defaultL3 = L3NetworkInventory.valueOf(l3);
                final String defaultL3Uuid = defaultL3.getUuid();
                defaultNic = servedVm.getVmInventory().getVmNics().stream()
                        .filter(vmNic -> vmNic.getL3NetworkUuid().equals(defaultL3Uuid))
                        .findFirst()
                        .orElse(null);
            }
        }

        if (defaultL3 == null) {
            // the L3 for operation is not the default L3
            completion.success();
            return;
        }

        if (!defaultL3.getIpVersions().contains(IPv6Constants.IPv4)) {
            // userdata depends on the ipv4 address
            completion.success();
            return;
        }

        NetworkServiceProviderInventory provider = findProvider(defaultL3);
        if (provider == null) {
            completion.success();
            return;
        }

        UserdataStruct struct = new UserdataStruct();
        struct.setL3NetworkUuid(servedVm.getVmInventory().getDefaultL3NetworkUuid());
        struct.setParametersFromVmSpec(servedVm);
        struct.setUserdataList(servedVm.getUserdataList());
        if (defaultNic != null) {
            struct.getVmNics().add(defaultNic);
        }
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

    private boolean isAttachedNicOnDefaultL3(String nicUuid, VmInstanceInventory vm) {
        if (vm.getDefaultL3NetworkUuid() == null) {
            return false;
        }

        String l3NetworkUuid = Q.New(VmNicVO.class)
                .select(VmNicVO_.l3NetworkUuid)
                .eq(VmNicVO_.uuid, nicUuid)
                .eq(VmNicVO_.vmInstanceUuid, vm.getUuid())
                .findValue();

        return vm.getDefaultL3NetworkUuid().equals(l3NetworkUuid);
    }

    @Override
    public void afterAttachNic(String nicUuid, VmInstanceInventory vm, Completion completion) {
        if (!VmInstanceState.Running.toString().equals(vm.getState())) {
            completion.success();
            return;
        }

        if (vm.getDefaultL3NetworkUuid() == null || vm.getHostUuid() == null) {
            completion.success();
            return;
        }

        scheduleApplyUserdataAfterAttachNic(vm.getUuid(), nicUuid,
                APPLY_USERDATA_AFTER_ATTACH_NIC_INITIAL_DELAY_SECONDS, 1);
        completion.success();
    }

    @Override
    public void afterAttachNicRollback(String nicUuid, VmInstanceInventory vmInstanceInventory, NoErrorCompletion completion) {
        completion.done();
    }

    private boolean needRefreshUserdataAfterAttachNic(String nicUuid, VmInstanceInventory vm) {
        if (!VmInstanceState.Running.toString().equals(vm.getState())) {
            return false;
        }

        if (vm.getDefaultL3NetworkUuid() == null || vm.getHostUuid() == null) {
            return false;
        }

        return !isAttachedNicOnDefaultL3(nicUuid, vm);
    }

    private void scheduleApplyUserdataAfterAttachNic(String vmUuid, String nicUuid, long delaySeconds, int currentAttempt) {
        thdf.submitTimeoutTask(() -> thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return String.format("refresh-userdata-metadata-after-attach-nic-vm-%s", vmUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                applyUserdataAfterAttachNic(vmUuid, nicUuid, currentAttempt, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("refresh-userdata-metadata-after-attach-nic-vm-%s", vmUuid);
            }
        }),
                TimeUnit.SECONDS, delaySeconds);
    }

    private void applyUserdataAfterAttachNic(String vmUuid, String nicUuid, int currentAttempt, NoErrorCompletion completion) {
        try {
            VmInstanceVO vmVO = dbf.findByUuid(vmUuid, VmInstanceVO.class);
            if (vmVO == null) {
                completion.done();
                return;
            }

            VmInstanceInventory vm = VmInstanceInventory.valueOf(vmVO);
            if (!needRefreshUserdataAfterAttachNic(nicUuid, vm)) {
                completion.done();
                return;
            }

            L3NetworkVO defaultL3VO = Q.New(L3NetworkVO.class)
                    .eq(L3NetworkVO_.uuid, vm.getDefaultL3NetworkUuid())
                    .find();
            if (defaultL3VO == null) {
                completion.done();
                return;
            }

            L3NetworkInventory defaultL3 = L3NetworkInventory.valueOf(defaultL3VO);
            if (!defaultL3.getIpVersions().contains(IPv6Constants.IPv4)) {
                completion.done();
                return;
            }

            NetworkServiceProviderInventory provider = findProvider(defaultL3);
            if (provider == null) {
                completion.done();
                return;
            }

            UserdataStruct struct = new UserdataStruct();
            struct.setL3NetworkUuid(vm.getDefaultL3NetworkUuid());
            struct.setParametersFromVmInventory(vm);
            struct.setUserdataList(new UserdataBuilder().buildByVmUuid(vm.getUuid()));

            UserdataBackend bkd = getUserdataBackend(provider.getType());
            bkd.applyUserdata(struct, new Completion(null) {
                @Override
                public void success() {
                    completion.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    retryApplyUserdataAfterAttachNic(vmUuid, nicUuid, currentAttempt, errorCode, null);
                    completion.done();
                }
            });
        } catch (Throwable t) {
            if (t instanceof Error) {
                logger.warn(String.format("fatal error happened when refreshing userdata metadata after attaching nic[uuid:%s] to vm[uuid:%s]",
                        nicUuid, vmUuid), t);
                completion.done();
                throw (Error) t;
            }

            retryApplyUserdataAfterAttachNic(vmUuid, nicUuid, currentAttempt, null, t);
            completion.done();
        }
    }

    private void retryApplyUserdataAfterAttachNic(String vmUuid, String nicUuid, int currentAttempt, ErrorCode errorCode, Throwable t) {
        String reason = errorCode == null ? t.toString() : errorCode.toString();
        String warn = String.format("failed to refresh userdata metadata after attaching nic[uuid:%s] to vm[uuid:%s], attempt %s/%s, %s",
                nicUuid, vmUuid, currentAttempt, APPLY_USERDATA_AFTER_ATTACH_NIC_RETRY_TIMES, reason);
        if (t == null) {
            logger.warn(warn);
        } else {
            logger.warn(warn, t);
        }

        if (currentAttempt >= APPLY_USERDATA_AFTER_ATTACH_NIC_RETRY_TIMES) {
            return;
        }

        scheduleApplyUserdataAfterAttachNic(vmUuid, nicUuid,
                APPLY_USERDATA_AFTER_ATTACH_NIC_RETRY_DELAY_SECONDS, currentAttempt + 1);
    }

    @Override
    public void releaseNetworkService(final VmInstanceSpec servedVm, Map<String, Object> data, final NoErrorCompletion completion) {
        L3NetworkInventory defaultL3 = CollectionUtils.find(VmNicSpec.getL3NetworkInventoryOfSpec(servedVm.getL3Networks()),
                arg -> arg.getUuid().equals(servedVm.getVmInventory().getDefaultL3NetworkUuid()) ? arg : null);
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

        NetworkServiceProviderInventory provider = findProvider(defaultL3);
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

    @Override
    public void enableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType, List<String> systemTags, Completion completion) {
        completion.success();
    }

    @Override
    public void disableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType, Completion completion) {
        completion.success();
    }
}
