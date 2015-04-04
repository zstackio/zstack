package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.Component;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostStatusChangeNotifyPoint;
import org.zstack.header.host.HostInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.List;

public class HostNotifyPointEmitter implements Component {
    private static final CLogger logger = Utils.getLogger(HostNotifyPointEmitter.class);
    
    @Autowired
    private PluginRegistry pluginRgty;
    
    private List<HostStatusChangeNotifyPoint> connectionStatePoints;

    private void populateNotifyPoints() {
        connectionStatePoints = pluginRgty.getExtensionList(HostStatusChangeNotifyPoint.class);
    }
    
    @Override
    public boolean start() {
        populateNotifyPoints();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
    
    @AsyncThread
    public void notifyHostConnectionChange(final HostInventory host, final HostStatus previousState, final HostStatus currentState) {
        CollectionUtils.safeForEach(connectionStatePoints, new ForEachFunction<HostStatusChangeNotifyPoint>() {
            @Override
            public void run(HostStatusChangeNotifyPoint arg) {
                arg.notifyHostConnectionStateChange(host, previousState, currentState);
            }
        });
    }
}
