package org.zstack.physicalserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeWhileDoneCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.physicalserver.PhysicalServerManager;
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentObserver;
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentFactory;
import org.zstack.header.physicalserver.RoleServiceManifest;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import static org.zstack.core.Platform.operr;

public class PhysicalServerManagerImpl extends AbstractService implements PhysicalServerManager, Component,
        ManagementNodeReadyExtensionPoint {
    private static final long RESOURCE_CONTROL_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(5);
    private static final int ASSIGNMENT_BATCH_SIZE = 500;
    private static final int ASSIGNMENT_CONCURRENCY = 15;
    private static final CLogger logger = Utils.getLogger(PhysicalServerManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private PhysicalServerAssignmentRepository assignments;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private PhysicalServerIdentityService identity;
    @Autowired
    private ResourceDestinationMaker destinationMaker;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof PhysicalServerMessage) {
            passThrough((PhysicalServerMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void passThrough(PhysicalServerMessage request) {
        resourceAssignments(request.getServerUuid()).handleMessage((Message) request);
    }

    private PhysicalServerResourceAssignmentBase resourceAssignments(String serverUuid) {
        return Platform.New(() -> new PhysicalServerResourceAssignmentBase(serverUuid));
    }

    public PhysicalServerResourceAssignmentFactory getFactory(String roleType) {
        PhysicalServerResourceAssignmentFactory selected = null;
        for (PhysicalServerResourceAssignmentFactory factory :
                pluginRgty.getExtensionList(PhysicalServerResourceAssignmentFactory.class)) {
            if (!factory.getRoleType().toString().equals(roleType)) {
                continue;
            }
            if (selected != null) {
                throw new IllegalStateException(
                        String.format("Duplicate resource assignment factory for role[%s]", roleType));
            }
            selected = factory;
        }
        if (selected == null) {
            throw new OperationFailureException(operr(PhysicalServerConstant.ERROR_CODE,
                    "No resource assignment factory for role[%s]", roleType));
        }
        return selected;
    }

    public PhysicalServerResourceAssignmentObserver getResourceAssignment(String serverUuid, String roleType) {
        PhysicalServerResourceAssignmentFactory factory = getFactory(roleType);
        return Platform.New(() -> factory.getResourceAssignment(serverUuid));
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIRefreshPhysicalServerResourceAssignmentsFromProfileMsg) {
            handle((APIRefreshPhysicalServerResourceAssignmentsFromProfileMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIRefreshPhysicalServerResourceAssignmentsFromProfileMsg msg) {
        APIRefreshPhysicalServerResourceAssignmentsFromProfileEvent event =
                new APIRefreshPhysicalServerResourceAssignmentsFromProfileEvent(msg.getId());
        String profileSha256 = getProfileSha256(loadProfiles());
        if (msg.getServerUuids() != null) {
            msg.setServerUuids(new ArrayList<>(new TreeSet<>(msg.getServerUuids())));
        }
        refreshFromProfile(msg, profileSha256, "", event);
    }

    private void refreshFromProfile(APIRefreshPhysicalServerResourceAssignmentsFromProfileMsg msg, String profileSha256,
                                   String lastUuid, APIRefreshPhysicalServerResourceAssignmentsFromProfileEvent event) {
        List<String> targets;
        if (msg.getServerUuids() == null) {
            targets = assignments.listServerUuids(lastUuid, ASSIGNMENT_BATCH_SIZE);
        } else {
            int start = lastUuid.isEmpty() ? 0 : Collections.binarySearch(msg.getServerUuids(), lastUuid) + 1;
            targets = msg.getServerUuids().subList(start,
                    Math.min(start + ASSIGNMENT_BATCH_SIZE, msg.getServerUuids().size()));
        }
        if (targets.isEmpty()) {
            bus.publish(event);
            return;
        }
        List<RefreshPhysicalServerResourceAssignmentResult> results = targets.stream().map(serverUuid -> {
            RefreshPhysicalServerResourceAssignmentResult result = new RefreshPhysicalServerResourceAssignmentResult();
            result.setServerUuid(serverUuid);
            return result;
        }).collect(Collectors.toList());
        event.getResults().addAll(results);
        new While<>(results).step((result, each) -> {
            RefreshPhysicalServerResourceAssignmentsFromProfileMsg request =
                    new RefreshPhysicalServerResourceAssignmentsFromProfileMsg();
            request.setServerUuid(result.getServerUuid());
            request.setProfileSha256(profileSha256);
            request.setTimeout(RESOURCE_CONTROL_TIMEOUT_MILLIS);
            bus.makeTargetServiceIdByResourceUuid(request, PhysicalServerConstant.SERVICE_ID, result.getServerUuid());
            bus.send(request, new CloudBusCallBack(each) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        result.setError(reply.getError());
                    }
                    each.done();
                }
            });
        }, ASSIGNMENT_CONCURRENCY).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errors) {
                if (event.getError() == null && !errors.getCauses().isEmpty()) {
                    event.setError(errors.getCauses().get(0));
                }
                refreshFromProfile(msg, profileSha256, targets.get(targets.size() - 1), event);
            }
        });
    }

    public Map<String, RoleServiceManifest> loadProfiles() {
        RoleServiceManifest.reloadAll();
        Map<String, RoleServiceManifest> profiles = new LinkedHashMap<>();
        for (PhysicalServerResourceAssignmentFactory factory :
                pluginRgty.getExtensionList(PhysicalServerResourceAssignmentFactory.class)) {
            profiles.put(factory.getRoleType().toString(), factory.roleServices());
        }
        return profiles;
    }

    public String getProfileSha256(Map<String, RoleServiceManifest> profiles) {
        return DigestUtils.sha256Hex(JSONObjectUtil.toJsonString(new TreeMap<>(profiles)));
    }

    @Override
    public Map<String, String> resolveBySerialNumbers(Collection<String> serialNumbers) {
        return identity.resolveBySerialNumbers(serialNumbers);
    }

    @Override
    public void refreshResourceAssignment(String serverUuid, String roleType, Completion completion) {
        if (serverUuid == null || roleType == null) {
            completion.fail(operr(PhysicalServerConstant.ERROR_CODE, "serverUuid and roleType cannot be null"));
            return;
        }
        RefreshPhysicalServerResourceAssignmentMsg msg = new RefreshPhysicalServerResourceAssignmentMsg();
        msg.setServerUuid(serverUuid);
        msg.setRoleType(roleType);
        sendResourceAssignmentMessage(msg, completion);
    }

    @Override
    public void releaseResourceAssignment(String serverUuid, String roleType, Completion completion) {
        ReleasePhysicalServerResourceAssignmentMsg msg = new ReleasePhysicalServerResourceAssignmentMsg();
        msg.setServerUuid(serverUuid);
        msg.setRoleType(roleType);
        sendResourceAssignmentMessage(msg, completion);
    }

    @Override
    public void forgetResourceAssignment(String serverUuid, String roleType, Completion completion) {
        ForgetPhysicalServerResourceAssignmentMsg msg = new ForgetPhysicalServerResourceAssignmentMsg();
        msg.setServerUuid(serverUuid);
        msg.setRoleType(roleType);
        sendResourceAssignmentMessage(msg, completion);
    }

    private void sendResourceAssignmentMessage(NeedReplyMessage msg, Completion completion) {
        msg.setTimeout(RESOURCE_CONTROL_TIMEOUT_MILLIS);
        bus.makeTargetServiceIdByResourceUuid(
                msg, PhysicalServerConstant.SERVICE_ID, ((PhysicalServerMessage) msg).getServerUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                completion.success();
            }
        });
    }

    @Override
    public boolean start() {
        PhysicalServerResourceAssignmentGlobalConfig.ENABLED.installUpdateExtension(
                (oldConfig, newConfig) -> updateResourceAssignments());
        return true;
    }

    @Override
    public void managementNodeReady() {
        if (PhysicalServerResourceAssignmentGlobalConfig.ENABLED.value(Boolean.class)) {
            updateResourceAssignments();
        }
    }

    @AsyncThread
    private void updateResourceAssignments() {
        boolean enabled = PhysicalServerResourceAssignmentGlobalConfig.ENABLED.value(Boolean.class);
        List<PhysicalServerResourceAssignmentFactory> factories =
                pluginRgty.getExtensionList(PhysicalServerResourceAssignmentFactory.class);
        new While<>(factories).each((factory, each) -> {
            String role = factory.getRoleType().toString();
            Completion completion = new Completion(each) {
                @Override
                public void success() {
                    each.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("failed to initialize role[%s] assignments: %s", role, errorCode));
                    each.done();
                }
            };
            if (enabled) {
                factory.initializeAssociations(ASSIGNMENT_BATCH_SIZE,
                        (servers, batch) -> updateResourceAssignments(servers, role, batch), completion);
            } else if (factory.roleServices().getSliceName() != null) {
                updateRoleAssignments(role, "", completion);
            } else {
                each.done();
            }
        }).run(new NopeWhileDoneCompletion());
    }

    @AsyncThread
    private void updateRoleAssignments(String role, String lastUuid, Completion completion) {
        List<String> servers = assignments.listServerUuids(role, lastUuid, ASSIGNMENT_BATCH_SIZE);
        if (servers.isEmpty()) {
            completion.success();
            return;
        }
        updateResourceAssignments(servers, role, new Completion(completion) {
            @Override
            public void success() {
                updateRoleAssignments(role, servers.get(servers.size() - 1), completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void updateResourceAssignments(Collection<String> servers, String role, Completion completion) {
        List<String> owned = servers.stream().distinct().filter(destinationMaker::isManagedByUs)
                .collect(Collectors.toList());
        new While<>(owned).step((serverUuid, each) -> {
            Completion applied = new Completion(each) {
                @Override
                public void success() {
                    each.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("failed to update role[%s] assignment for server[%s]: %s",
                            role, serverUuid, errorCode));
                    each.done();
                }
            };
            if (PhysicalServerResourceAssignmentGlobalConfig.ENABLED.value(Boolean.class)) {
                refreshResourceAssignment(serverUuid, role, applied);
            } else {
                releaseResourceAssignment(serverUuid, role, applied);
            }
        }, ASSIGNMENT_CONCURRENCY).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errors) {
                completion.success();
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(PhysicalServerConstant.SERVICE_ID);
    }
}
