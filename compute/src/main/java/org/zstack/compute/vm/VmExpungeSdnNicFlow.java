package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.vm.AfterAllocateSdnNicExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmExpungeSdnNicFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VmExpungeSdnNicFlow.class);

    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<VmNicInventory> nics = spec.getVmInventory().getVmNics();
        if (nics == null || nics.isEmpty()) {
            trigger.next();
            return;
        }

        releaseSdnNics(nics, new Completion(trigger) {
            @Override
            public void success() {
                trigger.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("releaseSdnNics failed during vm expunge: %s", errorCode));
                trigger.fail(errorCode);
            }
        });
    }

    private void releaseSdnNics(List<VmNicInventory> nics, Completion completion) {
        List<AfterAllocateSdnNicExtensionPoint> exts = pluginRgty.getExtensionList(AfterAllocateSdnNicExtensionPoint.class);
        if (exts.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(exts).each((ext, wcomp) -> ext.releaseSdnNics(nics, new Completion(wcomp) {
            @Override
            public void success() {
                wcomp.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("releaseSdnNics extension failed during vm expunge: %s", errorCode));
                wcomp.addError(errorCode);
                wcomp.allDone();
            }
        })).run(new WhileDoneCompletion(completion) {
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
}
