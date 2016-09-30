package org.zstack.network.l3;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.network.l3.L3NetworkDeleteExtensionPoint;
import org.zstack.header.network.l3.L3NetworkException;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.List;

public class L3NetworkExtensionPointEmitter implements Component {
    private static final CLogger logger = Utils.getLogger(L3NetworkExtensionPointEmitter.class);
    private List<L3NetworkDeleteExtensionPoint> extensions = null;

    @Autowired
    private PluginRegistry pluginRgty;

    public void preDelete(L3NetworkInventory inv) throws L3NetworkException {
        for (L3NetworkDeleteExtensionPoint ext : extensions) {
            try {
                ext.preDeleteL3Network(inv);
            } catch (L3NetworkException l3e) {
                throw l3e;
            } catch (Exception e) {
                logger.warn(String.format("Unhandled exception happened while calling L3NetworkDeleteExtensionPoint.preDeleteL3Network of %s", ext.getClass()
                        .getCanonicalName()), e);
            }
        }
    }

    public void beforeDelete(final L3NetworkInventory inv) {
        CollectionUtils.safeForEach(extensions, new ForEachFunction<L3NetworkDeleteExtensionPoint>() {
            @Override
            public void run(L3NetworkDeleteExtensionPoint arg) {
                arg.beforeDeleteL3Network(inv);
            }
        });
    }

    public void afterDelete(final L3NetworkInventory inv) {
        CollectionUtils.safeForEach(extensions, new ForEachFunction<L3NetworkDeleteExtensionPoint>() {
            @Override
            public void run(L3NetworkDeleteExtensionPoint arg) {
                arg.afterDeleteL3Network(inv);
            }
        });
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    private void populateExtensions() {
        extensions = pluginRgty.getExtensionList(L3NetworkDeleteExtensionPoint.class);
    }

    @Override
    public boolean stop() {
        return true;
    }
}
