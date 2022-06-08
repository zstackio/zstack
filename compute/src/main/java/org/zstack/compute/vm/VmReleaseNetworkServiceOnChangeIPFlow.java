package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNetworkServiceOnChangeIPExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmReleaseNetworkServiceOnChangeIPFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmReleaseNetworkServiceOnChangeIPFlow.class);

    @Autowired
    private PluginRegistry pluginRgty;

    private static List<VmNetworkServiceOnChangeIPExtensionPoint> extensions = null;

    public VmReleaseNetworkServiceOnChangeIPFlow() {
        if (extensions == null) {
            extensions = pluginRgty.getExtensionList(VmNetworkServiceOnChangeIPExtensionPoint.class);
        }
    }

    private void runExtensions(final Iterator<VmNetworkServiceOnChangeIPExtensionPoint> it, final VmInstanceSpec spec, final Map<String, Object> ctx, final FlowTrigger trigger) {
        if (!it.hasNext()) {
            trigger.next();
            return;
        }

        VmNetworkServiceOnChangeIPExtensionPoint ext = it.next();
        logger.debug(String.format("run VmReleaseNetworkServiceOnChangeIPFlow[%s]", ext.getClass()));
        ext.releaseNetworkServiceOnChangeIP(spec, null, new Completion(trigger) {
            @Override
            public void success() {
                runExtensions(it, spec, ctx, trigger);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        runExtensions(extensions.iterator(), spec, data, trigger);
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        trigger.rollback();
    }
}
