package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.gc.GCStatus;
import org.zstack.core.gc.GarbageCollectorVO;
import org.zstack.core.gc.GarbageCollectorVO_;
import org.zstack.core.jsonlabel.JsonLabel;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmJustBeforeDeleteFromDbExtensionPoint;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmBeforeStartOnHypervisorExtensionPoint;
import org.zstack.header.vm.VmBeforeCreateOnHypervisorExtensionPoint;
import org.zstack.header.vm.VmInstanceStartNewCreatedVmExtensionPoint;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmStateChangedExtensionPoint;
import org.zstack.tag.SystemTagCreator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_KVM_10159;

public class KVMVmFirmwareFlashCleanup extends AbstractService implements VmJustBeforeDeleteFromDbExtensionPoint,
        VmStateChangedExtensionPoint, VmBeforeStartOnHypervisorExtensionPoint,
        VmBeforeCreateOnHypervisorExtensionPoint, VmInstanceStartNewCreatedVmExtensionPoint,
        VmInstanceMigrateExtensionPoint {
    static final String SERVICE_ID = "kvmVmFirmwareFlash";
    private static final String GENERATION_LABEL_PREFIX = "kvm-vm-firmware-generation-";
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void handleMessage(Message message) {
        if (!(message instanceof KVMVmFirmwareFlashCleanupMsg)) {
            bus.dealWithUnknownMessage(message);
            return;
        }
        KVMVmFirmwareFlashCleanupMsg msg = (KVMVmFirmwareFlashCleanupMsg) message;
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return VmInstanceConstant.VM_SYNC_SIGNATURE_PREFIX + msg.getVmUuid();
            }

            @Override
            public String getName() {
                return "cleanup-vm-firmware-" + msg.getVmUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                MessageReply reply = new MessageReply();
                if (dbf.isExist(msg.getVmUuid(), VmInstanceVO.class)) {
                    reply.setError(operr(ORG_ZSTACK_KVM_10159, "%s operation failed: %s",
                            String.format("firmware cleanup for vm[uuid:%s]", msg.getVmUuid()),
                            "the VM still exists; keep its firmware flash"));
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }
                if (!dbf.isExist(msg.getHostUuid(), HostVO.class)) {
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }
                if (msg.getJobUuid() == null || !Q.New(GarbageCollectorVO.class)
                        .eq(GarbageCollectorVO_.uuid, msg.getJobUuid())
                        .eq(GarbageCollectorVO_.name, firmwareJobPrefix(msg.getVmUuid()) + msg.getHostUuid())
                        .notEq(GarbageCollectorVO_.status, GCStatus.Done)
                        .isExists()
                        || !Objects.equals(getFirmwareGeneration(msg.getVmUuid()), msg.getGeneration())) {
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }
                KVMAgentCommands.CleanVmFirmwareFlashCmd command = new KVMAgentCommands.CleanVmFirmwareFlashCmd();
                command.vmUuid = msg.getVmUuid();
                command.cleanupInactiveDomain = true;
                KVMHostAsyncHttpCallMsg call = new KVMHostAsyncHttpCallMsg();
                call.setHostUuid(msg.getHostUuid());
                call.setPath(KVMConstant.CLEAN_FIRMWARE_FLASH);
                call.setCommand(command);
                bus.makeTargetServiceIdByResourceUuid(call, HostConstant.SERVICE_ID, msg.getHostUuid());
                bus.send(call, new CloudBusCallBack(chain) {
                    @Override
                    public void run(MessageReply response) {
                        try {
                            try {
                                ErrorableValue<KVMAgentCommands.CleanVmFirmwareFlashRsp> result = KVMHostAsyncHttpCallReply.unwrap(
                                        response, KVMAgentCommands.CleanVmFirmwareFlashRsp.class);
                                if (!result.isSuccess()) {
                                    reply.setError(result.error);
                                } else if (!result.result.cleanupCompleted) {
                                    reply.setError(operr(ORG_ZSTACK_KVM_10159, "%s operation failed: %s",
                                            String.format("firmware cleanup for vm[uuid:%s]", msg.getVmUuid()),
                                            "the agent did not confirm firmware cleanup; retry after updating the agent"));
                                }
                            } catch (RuntimeException e) {
                                reply.setError(operr(ORG_ZSTACK_KVM_10159, "%s operation failed: %s",
                                        String.format("firmware cleanup for vm[uuid:%s]", msg.getVmUuid()),
                                        "the agent returned an invalid firmware cleanup response"));
                            }
                            bus.reply(msg, reply);
                        } finally {
                            chain.next();
                        }
                    }
                });
            }
        });
    }

    private String firmwareJobPrefix(String vmUuid) {
        return "gc-vm-firmware-" + vmUuid + "-on-host-";
    }

    private String getFirmwareGeneration(String vmUuid) {
        // Honor revocations persisted by earlier versions of this patch.
        return new JsonLabel().get(GENERATION_LABEL_PREFIX + vmUuid, String.class);
    }

    private void recordFirmwareHosts(String vmUuid, String... hostUuids) {
        Set<String> hosts = new HashSet<>(Arrays.asList(hostUuids));
        hosts.remove(null);
        for (String hostUuid : hosts) {
            SystemTagCreator creator = KVMSystemTags.FIRMWARE_HOST.newSystemTagCreator(vmUuid);
            creator.inherent = true;
            creator.ignoreIfExisting = true;
            creator.setTagByTokens(Collections.singletonMap(KVMSystemTags.FIRMWARE_HOST_UUID_TOKEN, hostUuid));
            creator.create();
        }
    }

    @Override
    public void vmStateChanged(VmInstanceInventory inv, VmInstanceState oldState, VmInstanceState newState) {
        if (KVMConstant.KVM_HYPERVISOR_TYPE.toString().equals(inv.getHypervisorType())
                && (newState == VmInstanceState.Starting || newState == VmInstanceState.Migrating)) {
            recordFirmwareHosts(inv.getUuid(), inv.getHostUuid(), inv.getLastHostUuid());
        }
    }

    @Override
    public void beforeStartVmOnHypervisor(VmInstanceSpec spec) {
        if (spec.getDestHost() == null
                || !KVMConstant.KVM_HYPERVISOR_TYPE.toString().equals(spec.getDestHost().getHypervisorType())) {
            return;
        }
        VmInstanceInventory inv = spec.getVmInventory();
        recordFirmwareHosts(inv.getUuid(), inv.getHostUuid(), inv.getLastHostUuid(), spec.getDestHost().getUuid());
    }

    @Override
    public void beforeCreateVmOnHypervisor(VmInstanceSpec spec) {
        beforeStartVmOnHypervisor(spec);
    }

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        if (KVMConstant.KVM_HYPERVISOR_TYPE.toString().equals(inv.getHypervisorType())) {
            recordFirmwareHosts(inv.getUuid(), inv.getHostUuid(), inv.getLastHostUuid(), destHostUuid);
        }
    }

    @Override
    public void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {
    }

    @Override
    public String preStartNewCreatedVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStartNewCreatedVm(VmInstanceInventory inv) {
    }

    @Override
    public void afterStartNewCreatedVm(VmInstanceInventory inv) {
    }

    @Override
    public void failedToStartNewCreatedVm(VmInstanceInventory inv, ErrorCode reason) {
        vmJustBeforeDeleteFromDb(inv);
    }

    @Override
    @Transactional
    public void vmJustBeforeDeleteFromDb(VmInstanceInventory inv, VmInstanceDeletionPolicy deletionPolicy) {
        if (deletionPolicy == VmInstanceDeletionPolicy.DBOnly) {
            if (inv.getHypervisorType() != null
                    && !KVMConstant.KVM_HYPERVISOR_TYPE.toString().equals(inv.getHypervisorType())) {
                return;
            }
            SQL.New(GarbageCollectorVO.class)
                    .eq(GarbageCollectorVO_.runnerClass, KVMVmFirmwareFlashGC.class.getName())
                    .like(GarbageCollectorVO_.name, firmwareJobPrefix(inv.getUuid()) + "%")
                    .notEq(GarbageCollectorVO_.status, GCStatus.Done)
                    .set(GarbageCollectorVO_.status, GCStatus.Done)
                    .update();
        } else {
            vmJustBeforeDeleteFromDb(inv);
        }
    }

    @Override
    public void vmJustBeforeDeleteFromDb(VmInstanceInventory inv) {
        if (inv.getHypervisorType() != null
                && !KVMConstant.KVM_HYPERVISOR_TYPE.toString().equals(inv.getHypervisorType())) {
            return;
        }

        Set<String> hosts = new HashSet<>();
        for (SystemTagInventory tag : KVMSystemTags.FIRMWARE_HOST.getTagInventories(inv.getUuid())) {
            if (tag.isInherent()) {
                hosts.add(KVMSystemTags.FIRMWARE_HOST.getTokenByTag(tag.getTag(), KVMSystemTags.FIRMWARE_HOST_UUID_TOKEN));
            }
        }
        if (KVMConstant.KVM_HYPERVISOR_TYPE.toString().equals(inv.getHypervisorType())) {
            hosts.add(inv.getHostUuid());
            hosts.add(inv.getLastHostUuid());
        }
        hosts.remove(null);
        for (String hostUuid : hosts) {
            KVMVmFirmwareFlashGC gc = new KVMVmFirmwareFlashGC();
            gc.NAME = firmwareJobPrefix(inv.getUuid()) + hostUuid;
            gc.vmUuid = inv.getUuid();
            gc.hostUuid = hostUuid;
            gc.generation = getFirmwareGeneration(inv.getUuid());
            gc.submit(CoreGlobalProperty.UNIT_TEST_ON ? 2L : 30L, TimeUnit.SECONDS);
        }
    }
}
