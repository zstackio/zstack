package org.zstack.externalservice.licenseserver;

import org.zstack.core.Platform;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.externalservice.AbstractLocalExternalSystemdService;
import org.zstack.core.externalservice.ExternalService;
import org.zstack.core.externalservice.ExternalServiceManager;
import org.zstack.core.externalservice.ExternalServiceCapabilitiesBuilder;
import org.zstack.header.core.external.service.ExternalServiceCapabilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.File;

public class LicenseServerExternalService extends AbstractLocalExternalSystemdService implements ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(LicenseServerExternalService.class);

    public static final String SYSTEMD_SERVICE_NAME = "zstack-license-server.service";
    public static final String SERVICE_TYPE = "LicenseServer";

    @Autowired
    private ExternalServiceManager externalServiceManager;

    private final ExternalServiceCapabilities capabilities = ExternalServiceCapabilitiesBuilder
            .build()
            .reloadConfig(false);

    @Override
    public void managementNodeReady() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        if (!isInstalled()) {
            logger.warn(String.format("[External Service] %s is not installed, skip registering it", getName()));
            return;
        }

        try {
            ExternalService service = externalServiceManager.getService(getName(), () -> this);
            service.start();
        } catch (Exception t) {
            logger.warn(String.format("[External Service] failed to start %s", getName()), t);
        }
    }

    @Override
    protected String[] getCommandLineKeywords() {
        return new String[] {"zstack-license-server", "--config"};
    }

    @Override
    public String getName() {
        return String.format("license-server-on-machine-%s", Platform.getManagementServerIp());
    }

    @Override
    public String getServiceType() {
        return SERVICE_TYPE;
    }

    @Override
    public String getSystemdServiceName() {
        return SYSTEMD_SERVICE_NAME;
    }

    @Override
    public void start() {
        if (!isInstalled()) {
            logger.warn(String.format("[External Service] %s is not installed, skip starting it", getName()));
            return;
        }

        if (isAlive()) {
            logger.debug(String.format("[External Service] %s is already running", getName()));
            return;
        }

        sysctl("start");
        logger.debug(String.format("[External Service] started %s", getName()));
    }

    @Override
    public void stop() {
        logger.debug(String.format("[External Service] skip stopping %s, it is managed independently", getName()));
    }

    @Override
    public void restart() {
        start();
    }

    @Override
    public boolean isAlive() {
        return getPID() != null;
    }

    @Override
    public ExternalServiceCapabilities getExternalServiceCapabilities() {
        return capabilities;
    }

    @Override
    public void reload() {
        logger.debug(String.format("[External Service] %s does not support reload config", getName()));
    }

    private boolean isInstalled() {
        return new File("/usr/lib/systemd/system/" + SYSTEMD_SERVICE_NAME).exists()
                || new File("/etc/systemd/system/" + SYSTEMD_SERVICE_NAME).exists();
    }
}
