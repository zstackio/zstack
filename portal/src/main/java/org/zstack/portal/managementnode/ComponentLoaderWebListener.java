package org.zstack.portal.managementnode;

import org.springframework.web.context.support.WebApplicationContextUtils;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import static org.zstack.utils.ExceptionDSL.throwableSafe;

public class ComponentLoaderWebListener implements ServletContextListener {
    private static final CLogger logger = Utils.getLogger(ComponentLoaderWebListener.class);
    private static boolean isInit = false;
    private ManagementNodeManager node;
    private CloudBus bus;

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        logger.warn(String.format("web listener issued context destroy event, start stropping process"));
        if (isInit) {
            throwableSafe(new Runnable() {
                @Override
                public void run() {
                    node.stop();
                }
            });
        }

    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            if (!isInit) {
                Platform.createComponentLoaderFromWebApplicationContext(WebApplicationContextUtils.getWebApplicationContext(event.getServletContext()));
                node = Platform.getComponentLoader().getComponent(ManagementNodeManager.class);
                bus = Platform.getComponentLoader().getComponent(CloudBus.class);
                node.startNode();
                isInit = true;
            }
        } catch (Throwable t) {
            logger.warn("failed to start management server", t);
            // have to call bus.stop() because its init has been called by spring
            if (bus != null) {
                bus.stop();
            }
            throw new CloudRuntimeException(t);
        }
    }
}
