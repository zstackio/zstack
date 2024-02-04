package org.zstack.smStorageEncrypt;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.crypto.securitymachine.secretresourcepool.QuerySecretKeyReply;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.securitymachine.secretresourcepool.QuerySecretKeyMsg;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.opencrypto.securitymachine.SecurityMachineGlobalConfig;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;


public class smBackend implements VmInstanceMigrateExtensionPoint {
    @Autowired
    private CloudBus bus;
    public static final String SET_SECRET_FORWARD_PATH = "/host/createqcow2secret";
    public static final String DEL_SECRET_FORWARD_PATH = "/host/deleteqcow2secret";
    private static final CLogger logger = Utils.getLogger(smBackend.class);

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        List<VolumeInventory> volumeInfo = inv.getAllDiskVolumes();
        if (volumeInfo == null || volumeInfo.isEmpty()) {
            return;
        }

        ErrorCodeList errList = new ErrorCodeList();
        FutureCompletion completion = new FutureCompletion(null);
        List<VolumeInventory> changeList = new ArrayList<>();;

        new While<>(volumeInfo).all((vol, completion1) -> {
            String volumeUuid = vol.getUuid();
            String volumeEncryptKeyidTag = VolumeSystemTags.VOLUME_ENCRYPT_KEYID.getTag(volumeUuid);
            if (volumeEncryptKeyidTag == null) {
                completion1.done();
                logger.debug(String.format("volume[uuid:%s] is not encrypted, do not need set secret in dsthost host",
                        volumeUuid));
                return;
            }
            logger.debug(String.format("volume[uuid:%s] is encrypted, check and set secret in dsthost host",
                    volumeUuid));
            FlowChain chain = FlowChainBuilder.newShareFlowChain();
            chain.setName(String.format("migrate-volume-secret-for-volume-%s", volumeUuid));
            chain.then(new ShareFlow() {
                String key = null;

                @Override
                public void setup() {
                    flow(new NoRollbackFlow() {
                        String __name__ = "get-volume-secret-for-cipherMachine";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            QuerySecretKeyMsg qMsg = new QuerySecretKeyMsg();
                            String resourceId = SecurityMachineGlobalConfig.RESOURCE_POOL_UUID_FOR_DATA_PROTECT.value(String.class);
                            if (resourceId == null) {
                                trigger.next();
                            }
                            qMsg.setKeyId(volumeEncryptKeyidTag.split("::")[1]);
                            qMsg.setSecretResourcePoolUuid(resourceId);
                            bus.makeTargetServiceIdByResourceUuid(qMsg, SecretResourcePoolConstant.SERVICE_ID, resourceId);
                            bus.send(qMsg, new CloudBusCallBack(qMsg) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        logger.warn(reply.getError().toString());
                                        errList.getCauses().add(reply.getError());
                                        trigger.fail(reply.getError());
                                        return;
                                    }
                                    QuerySecretKeyReply cr = reply.castReply();
                                    key = cr.getSecretKey();
                                    trigger.next();
                                }
                            });
                        }
                    });
                    flow(new NoRollbackFlow() {
                        String __name__ = "migrate-volume-secret";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            if(key == null || key.isEmpty()) {
                                trigger.next();
                            }
                            smBackendCommands.setSecretCmd cmd = new smBackendCommands.setSecretCmd();
                            cmd.setKeyValue(key);
                            cmd.setVolumeUuid(volumeUuid);
                            KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
                            kmsg.setCommand(cmd);
                            kmsg.setPath(SET_SECRET_FORWARD_PATH);
                            kmsg.setHostUuid(destHostUuid);
                            bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, destHostUuid);
                            bus.send(kmsg, new CloudBusCallBack(completion1) {
                                @Override
                                public void run(MessageReply reply) {

                                    if (!reply.isSuccess()) {
                                        logger.warn(reply.getError().toString());
                                        errList.getCauses().add(reply.getError());
                                        trigger.fail(reply.getError());
                                        return;
                                    }
                                    changeList.add(vol);
                                    logger.debug(String.format("check and migrate secret volume[uuid: %s] for vm[uuid: %s] successed",
                                            volumeUuid, inv.getUuid()));
                                    trigger.next();
                                }
                            });
                        }
                    });
                    done(new FlowDoneHandler(completion1) {
                        @Override
                        public void handle(Map data) {
                            completion1.done();
                        }
                    });

                    error(new FlowErrorHandler(completion1) {
                        @Override
                        public void handle(ErrorCode errCode, Map data) {
                            completion1.allDone();
                        }
                    });
                }
            }).start();
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errList.getCauses().isEmpty()) {
                    completion.fail(errList.getCauses().get(0));
                    releaseSecret(changeList, inv.getUuid(), destHostUuid, new NoErrorCompletion() {
                        @Override
                        public void done() {
                            // ignore
                        }
                    });
                    return;
                }
                logger.info(String.format("check or init secret for vm[uuid: %s] done", inv.getUuid()));
                completion.success();
            }
        });

        completion.await(TimeUnit.MINUTES.toMillis(30));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(operr("cannot configure secert volume for vm[uuid:%s] on the destination host[uuid:%s]",
                    inv.getUuid(), destHostUuid).causedBy(completion.getErrorCode()));
        }
    }

    @Override
    public void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {

    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        List<VolumeInventory> volumeInfo = inv.getAllDiskVolumes();
        if (volumeInfo == null || volumeInfo.isEmpty()) {
            return;
        }

        releaseSecret(volumeInfo, inv.getUuid(), srcHostUuid, new NoErrorCompletion() {
            @Override
            public void done() {
                // ignore
            }
        });
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
        if (destHostUuid == null) {
            return;
        }

        List<VolumeInventory> volumeInfo = inv.getAllDiskVolumes();
        if (volumeInfo == null || volumeInfo.isEmpty()) {
            return;
        }

        releaseSecret(volumeInfo, inv.getUuid(), destHostUuid, new NoErrorCompletion() {
            @Override
            public void done() {
                // ignore
            }
        });
    }

    private void releaseSecret(List<VolumeInventory> info, final String vmUuid,
                               final String destHostUuid, final NoErrorCompletion completion) {

        ErrorCodeList errList = new ErrorCodeList();

        new While<>(info).all((vol, completion1) -> {
            String volumeUuid = vol.getUuid();
            String volumeEncryptKeyidTag = VolumeSystemTags.VOLUME_ENCRYPT_KEYID.getTag(volumeUuid);
            if (volumeEncryptKeyidTag == null) {
                completion1.done();
            }
            logger.debug(String.format("volume[uuid:%s] is encrypted, check encrypt secret in dsthost host",
                    volumeUuid));
            FlowChain chain = FlowChainBuilder.newShareFlowChain();
            chain.setName(String.format("migrate-volume-secret-for-volume-%s", volumeUuid));
            chain.then(new ShareFlow() {
                @Override
                public void setup() {
                    flow(new NoRollbackFlow() {
                        String __name__ = "release-volume-secret";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            smBackendCommands.delSecretCmd cmd = new smBackendCommands.delSecretCmd();
                            cmd.setVolumeUuid(volumeUuid);
                            KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
                            kmsg.setCommand(cmd);
                            kmsg.setPath(DEL_SECRET_FORWARD_PATH);
                            kmsg.setHostUuid(destHostUuid);
                            bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, destHostUuid);
                            bus.send(kmsg, new CloudBusCallBack(completion1) {
                                @Override
                                public void run(MessageReply reply) {

                                    if (!reply.isSuccess()) {
                                        logger.warn(reply.getError().toString());
                                        errList.getCauses().add(reply.getError());
                                        trigger.fail(reply.getError());
                                        return;
                                    }
                                    trigger.next();
                                    logger.debug(String.format("check or init secret volume[uuid: %s] for vm[uuid: %s] successed",
                                            volumeUuid, vmUuid));
                                }
                            });
                        }
                    });
                    done(new FlowDoneHandler(completion1) {
                        @Override
                        public void handle(Map data) {
                            completion1.done();
                        }
                    });

                    error(new FlowErrorHandler(completion1) {
                        @Override
                        public void handle(ErrorCode errCode, Map data) {
                            completion1.done();
                        }
                    });
                }
            }).start();
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errList.getCauses().isEmpty()) {
                    logger.info(String.format("release secret for vm[uuid: %s] failed,skip", vmUuid));
                    completion.done();
                    return;
                }
                logger.info(String.format("release secret for vm[uuid: %s] done", vmUuid));
                completion.done();
            }
        });
    }
}
