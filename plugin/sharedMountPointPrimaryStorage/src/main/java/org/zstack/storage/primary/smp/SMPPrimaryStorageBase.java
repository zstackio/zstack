package org.zstack.storage.primary.smp;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.AsyncLatch;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.VolumeSnapshotCapability.VolumeSnapshotArrangementType;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.storage.primary.PrimaryStorageBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xing5 on 2016/3/26.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SMPPrimaryStorageBase extends PrimaryStorageBase {
    @Autowired
    private PluginRegistry pluginRgty;

    public SMPPrimaryStorageBase(PrimaryStorageVO self) {
        super(self);
    }

    private HypervisorFactory getHypervisorFactoryByHypervisorType(String hvType) {
        for (HypervisorFactory f : pluginRgty.getExtensionList(HypervisorFactory.class)) {
            if (hvType.equals(f.getHypervisorType())) {
                return f;
            }
        }

        throw new CloudRuntimeException(String.format("cannot find HypervisorFactory[type = %s]", hvType));
    }

    private HypervisorFactory getHypervisorFactoryByHostUuid(String huuid) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.hypervisorType);
        q.add(HostVO_.uuid, Op.EQ, huuid);
        String hvType = q.findValue();
        return getHypervisorFactoryByHypervisorType(hvType);
    }

    private HypervisorFactory getHypervisorFactoryByClusterUuid(String cuuid) {
        SimpleQuery<ClusterVO> q = dbf.createQuery(ClusterVO.class);
        q.select(ClusterVO_.hypervisorType);
        q.add(ClusterVO_.uuid, Op.EQ, cuuid);
        String hvType = q.findValue();

        return getHypervisorFactoryByHypervisorType(hvType);
    }

    @Override
    public void attachHook(String clusterUuid, final Completion completion) {
        HypervisorBackend bkd = getHypervisorFactoryByClusterUuid(clusterUuid).getHypervisorBackend(self);
        bkd.attachHook(clusterUuid, completion);
    }

    @Override
    protected void handle(final InstantiateVolumeMsg msg) {
        HypervisorFactory f = getHypervisorFactoryByHostUuid(msg.getDestHost().getUuid());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<InstantiateVolumeReply>(msg) {
            @Override
            public void success(InstantiateVolumeReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                InstantiateVolumeReply reply = new InstantiateVolumeReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DeleteVolumeOnPrimaryStorageMsg msg) {
        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getVolume().getFormat());
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        final HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DeleteVolumeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(DeleteVolumeOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getVolumeInventory().getFormat());
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<CreateTemplateFromVolumeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CreateTemplateFromVolumeOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateTemplateFromVolumeOnPrimaryStorageReply reply = new CreateTemplateFromVolumeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DownloadDataVolumeToPrimaryStorageMsg msg) {
        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getImage().getFormat());
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DownloadDataVolumeToPrimaryStorageReply>(msg) {
            @Override
            public void success(DownloadDataVolumeToPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DownloadDataVolumeToPrimaryStorageReply reply = new DownloadDataVolumeToPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DeleteBitsOnPrimaryStorageMsg msg) {
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(msg.getHypervisorType());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DeleteBitsOnPrimaryStorageReply>(msg) {
            @Override
            public void success(DeleteBitsOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DeleteBitsOnPrimaryStorageReply reply = new DeleteBitsOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DownloadIsoToPrimaryStorageMsg msg) {
        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getIsoSpec().getInventory().getFormat());
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DownloadIsoToPrimaryStorageReply>(msg) {
            @Override
            public void success(DownloadIsoToPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode error) {
                DownloadIsoToPrimaryStorageReply reply = new DownloadIsoToPrimaryStorageReply();
                reply.setError(error);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DeleteIsoFromPrimaryStorageMsg msg) {
        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getIsoSpec().getInventory().getFormat());
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DeleteIsoFromPrimaryStorageReply>(msg) {
            @Override
            public void success(DeleteIsoFromPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode error) {
                DeleteIsoFromPrimaryStorageReply reply = new DeleteIsoFromPrimaryStorageReply();
                reply.setError(error);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(AskVolumeSnapshotCapabilityMsg msg) {
        AskVolumeSnapshotCapabilityReply reply = new AskVolumeSnapshotCapabilityReply();
        VolumeSnapshotCapability capability = new VolumeSnapshotCapability();
        capability.setSupport(true);
        capability.setArrangementType(VolumeSnapshotArrangementType.CHAIN);
        reply.setCapability(capability);
        bus.reply(msg, reply);
    }

    @Override
    protected void connectHook(ConnectPrimaryStorageMsg msg, final Completion completion) {
        SimpleQuery<PrimaryStorageClusterRefVO> q = dbf.createQuery(PrimaryStorageClusterRefVO.class);
        q.select(PrimaryStorageClusterRefVO_.clusterUuid);
        q.add(PrimaryStorageClusterRefVO_.primaryStorageUuid, Op.EQ, msg.getPrimaryStorageUuid());
        final List<String> clusterUuids = q.listValue();

        if (clusterUuids.isEmpty()) {
            completion.success();
            return;
        }

        class Result {
            boolean success;
            List<ErrorCode> errors = new ArrayList<ErrorCode>();
        }

        final Result ret = new Result();

        final AsyncLatch latch = new AsyncLatch(clusterUuids.size(), new NoErrorCompletion(completion) {
            @Override
            public void done() {
                if (ret.success) {
                    completion.success();
                } else {
                    completion.fail(errf.stringToOperationError(
                            String.format("failed to connect to all clusters%s, errors are %s", clusterUuids, ret.errors)
                    ));
                }
            }
        });

        for (String cuuid : clusterUuids) {
            HypervisorBackend bkd = getHypervisorFactoryByClusterUuid(cuuid).getHypervisorBackend(self);
            bkd.connectByClusterUuid(cuuid, new Completion(latch) {
                @Override
                public void success() {
                    ret.success = true;
                    latch.ack();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    ret.errors.add(errorCode);
                    latch.ack();
                }
            });
        }
    }

    @Override
    protected void syncPhysicalCapacity(ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        completion.fail(errf.stringToOperationError("no supported operation"));
    }
}
