package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.network.l3.ReturnIpMsg;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmReturnReleaseNicFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VmReturnReleaseNicFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected VmInstanceDeletionPolicyManager deletionPolicyMgr;
    @Autowired
    protected VmInstanceManager vmMgr;
    @Autowired
    protected PluginRegistry pluginRgty;

    @Override
    public void run(FlowTrigger chain, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        if (spec.getVmInventory().getVmNics().isEmpty()) {
            chain.next();
            return;
        }

        returnIpsAndReleaseNics(spec, data, chain);
    }


    private void returnIpsAndReleaseNics(VmInstanceSpec spec, Map data, FlowTrigger chain) {
        List<ReturnIpMsg> msgs = new ArrayList<>(spec.getVmInventory().getVmNics().size());
        for (VmNicInventory nic : spec.getVmInventory().getVmNics()) {
            for (UsedIpInventory ip : nic.getUsedIps()) {
                ReturnIpMsg msg = new ReturnIpMsg();
                msg.setL3NetworkUuid(ip.getL3NetworkUuid());
                msg.setUsedIpUuid(ip.getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, ip.getL3NetworkUuid());
                msgs.add(msg);
            }
        }

        VmInstanceDeletionPolicy deletionPolicy =
                VmInstanceConstant.USER_VM_TYPE.equals(spec.getVmInventory().getType())
                        ? getDeletionPolicy(spec, data)
                        : VmInstanceDeletionPolicy.Direct;

        new While<>(msgs).each((returnIpMsg, completion) -> bus.send(returnIpMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to release ip[usedIpUuid:%s] for vm[uuid:%s], but continue anyway",
                            returnIpMsg.getUsedIpUuid(), spec.getVmInventory().getUuid()));
                }
                completion.done();
            }
        })).run(new WhileDoneCompletion(chain) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                List<VmNicInventory> releasedNics = new ArrayList<>();
                List<VmNicVO> nicsToDelete = new ArrayList<>();
                List<VmNicVO> nicsToReleaseIp = new ArrayList<>();
                for (VmNicInventory nic : spec.getVmInventory().getVmNics()) {
                    VmNicVO vo = dbf.findByUuid(nic.getUuid(), VmNicVO.class);
                    if (VmInstanceConstant.USER_VM_TYPE.equals(spec.getVmInventory().getType())) {
                        if (deletionPolicy == VmInstanceDeletionPolicy.Direct) {
                            nicsToDelete.add(vo);
                        } else {
                            nicsToReleaseIp.add(vo);
                        }
                    } else {
                        nicsToDelete.add(vo);
                    }
                    releasedNics.add(nic);
                }

                Completion releaseDone = new Completion(chain) {
                    @Override
                    public void success() {
                        for (VmNicVO vo : nicsToReleaseIp) {
                            vo.setUsedIpUuid(null);
                            vo.setIp(null);
                            vo.setGateway(null);
                            vo.setNetmask(null);
                            dbf.update(vo);
                        }
                        nicsToDelete.forEach(dbf::remove);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chain.fail(errorCode);
                    }
                };

                if (shouldReleaseSdnNicIps(deletionPolicy)) {
                    callReleaseSdnNicIps(releasedNics, releaseDone);
                } else {
                    callReleaseSdnNics(releasedNics, releaseDone);
                }
            }
        });
    }

    private boolean shouldReleaseSdnNicIps(VmInstanceDeletionPolicy deletionPolicy) {
        return deletionPolicy == VmInstanceDeletionPolicy.Delay
                || deletionPolicy == VmInstanceDeletionPolicy.Never;
    }

    private void callReleaseSdnNicIps(List<VmNicInventory> nics, Completion completion) {
        List<AfterAllocateSdnNicExtensionPoint> exts = pluginRgty.getExtensionList(AfterAllocateSdnNicExtensionPoint.class);
        if (exts.isEmpty() || nics.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(exts).each((ext, wcomp) -> {
            ext.releaseNicIps(nics, new Completion(wcomp) {
                @Override
                public void success() {
                    wcomp.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("releaseNicIps extension failed: %s", errorCode));
                    wcomp.addError(errorCode);
                    wcomp.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errorCodeList.getCauses().get(0));
                }
            }
        });
    }

    private void callReleaseSdnNics(List<VmNicInventory> nics, Completion completion) {
        List<AfterAllocateSdnNicExtensionPoint> exts = pluginRgty.getExtensionList(AfterAllocateSdnNicExtensionPoint.class);
        if (exts.isEmpty() || nics.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(exts).each((ext, wcomp) -> {
            ext.releaseSdnNics(nics, new Completion(wcomp) {
                @Override
                public void success() {
                    wcomp.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("releaseSdnNics extension failed: %s", errorCode));
                    wcomp.addError(errorCode);
                    wcomp.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errorCodeList.getCauses().get(0));
                }
            }
        });
    }

    private VmInstanceDeletionPolicy getDeletionPolicy(VmInstanceSpec spec, Map data) {
        if (data.containsKey(VmInstanceConstant.Params.DeletionPolicy)) {
            return (VmInstanceDeletionPolicy) data.get(VmInstanceConstant.Params.DeletionPolicy);
        }

        return deletionPolicyMgr.getDeletionPolicy(spec.getVmInventory().getUuid());
    }
}
