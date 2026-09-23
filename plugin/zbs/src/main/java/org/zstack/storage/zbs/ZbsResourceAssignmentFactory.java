package org.zstack.storage.zbs;

import java.util.Collections;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.physicalserver.*;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO_;
import org.zstack.physicalserver.PhysicalServerConstant;
import org.zstack.physicalserver.PhysicalServerResourceAssignmentGlobalConfig;
import org.zstack.physicalserver.PhysicalServerVO;
import org.zstack.physicalserver.PhysicalServerVO_;
import org.zstack.storage.addon.primary.ExternalPrimaryStorageCanonicalEvent;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class ZbsResourceAssignmentFactory implements PhysicalServerResourceAssignmentFactory,
        Component, ManagementNodeReadyExtensionPoint {
    public static final PhysicalServerRoleType type = new PhysicalServerRoleType("ZBS");
    public static final String ROLE_SERVICE_MANIFEST_PATH = "physical-server-roles/zbs.yaml";
    private static final CLogger logger = Utils.getLogger(ZbsResourceAssignmentFactory.class);

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired(required = false)
    private PhysicalServerManager physicalServerManager;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private ZbsNodeRefContributorImpl nodeRefs;

    @Override
    public PhysicalServerRoleType getRoleType() {
        return type;
    }

    @Override
    public RoleServiceManifest roleServices() {
        return RoleServiceManifest.loadObservation(ROLE_SERVICE_MANIFEST_PATH, type.toString());
    }

    @Override
    public PhysicalServerResourceAssignmentObserver getResourceAssignment(String serverUuid) {
        return new ZbsResourceAssignmentObserver(serverUuid);
    }

    public void refreshAssignments() {
        if (!canRefresh()) {
            return;
        }
        Set<String> serialNumbers = new LinkedHashSet<>();
        for (ZbsNodeRefContributor contributor : pluginRgty.getExtensionList(ZbsNodeRefContributor.class)) {
            serialNumbers.addAll(contributor.getAllNodesBySerialNumber().keySet());
        }
        registerServersAndRefreshAssignments(serialNumbers);
    }

    private boolean canRefresh() {
        return physicalServerManager != null
                && PhysicalServerResourceAssignmentGlobalConfig.ENABLED.value(Boolean.class)
                && destinationMaker.isManagedByUs(PhysicalServerConstant.CONTROL_OWNER_KEY);
    }

    private void refreshAssignments(Collection<String> serverUuids) {
        for (String serverUuid : serverUuids) {
            physicalServerManager.refreshResourceAssignment(serverUuid, type.toString(), logCompletion(serverUuid));
        }
    }

    private void registerServersAndRefreshAssignments(Collection<String> serialNumbers) {
        refreshAssignments(physicalServerManager.resolveBySerialNumbers(serialNumbers).values());
    }

    void forgetAssignments(Collection<String> serialNumbers, Completion completion) {
        if (physicalServerManager == null || serialNumbers.isEmpty()) {
            completion.success();
            return;
        }
        List<String> serverUuids = Q.New(PhysicalServerVO.class).select(PhysicalServerVO_.uuid)
                .in(PhysicalServerVO_.serialNumber, serialNumbers).listValues();
        new While<>(serverUuids).each((serverUuid, each) -> physicalServerManager.forgetResourceAssignment(
                serverUuid, type.toString(), new Completion(each) {
                    @Override
                    public void success() {
                        each.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        each.addError(errorCode);
                        each.done();
                    }
                })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errors) {
                if (!errors.getCauses().isEmpty()) {
                    completion.fail(errors.getCauses().get(0));
                } else {
                    completion.success();
                }
            }
        });
    }

    private Completion logCompletion(String serverUuid) {
        return new Completion(null) {
            @Override
            public void success() {
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to update ZBS assignment for server[%s]: %s", serverUuid, errorCode));
            }
        };
    }

    @Override
    public void managementNodeReady() {
        refreshAssignments();
    }

    @Override
    public boolean start() {
        PhysicalServerResourceAssignmentGlobalConfig.ENABLED.installUpdateExtension((oldConfig, newConfig) -> {
            if (newConfig.value(Boolean.class)) {
                refreshAssignments();
            }
        });
        evtf.on(ExternalPrimaryStorageCanonicalEvent.ADDON_INFO_CHANGED_PATH,
                new EventCallback<ExternalPrimaryStorageCanonicalEvent.AddonInfoChangedData>() {
            @Override
            protected void run(Map tokens, ExternalPrimaryStorageCanonicalEvent.AddonInfoChangedData data) {
                if (!canRefresh()) {
                    return;
                }
                ExternalPrimaryStorageVO primaryStorage = Q.New(ExternalPrimaryStorageVO.class)
                        .eq(ExternalPrimaryStorageVO_.uuid, data.getUuid())
                        .eq(ExternalPrimaryStorageVO_.identity, ZbsConstants.IDENTITY).find();
                if (primaryStorage != null) {
                    registerServersAndRefreshAssignments(
                            nodeRefs.getNodesBySerialNumber(Collections.singleton(primaryStorage)).keySet());
                }
            }
        });
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
