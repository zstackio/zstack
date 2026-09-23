package org.zstack.physicalserver;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.physicalserver.PhysicalServerManager;
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentObserver;
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentFactory;
import org.zstack.header.physicalserver.RoleServiceManifest;
import static org.zstack.core.Platform.operr;

public class PhysicalServerManagerImpl extends AbstractService implements PhysicalServerManager, Component {
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
        if (msg instanceof PhysicalServerResourceAssignmentMessage) {
            passThrough((PhysicalServerResourceAssignmentMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void passThrough(PhysicalServerResourceAssignmentMessage request) {
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

        } else if (msg instanceof APIGetPhysicalServerManagedServicesMsg) {
            resourceAssignments(((APIGetPhysicalServerManagedServicesMsg) msg).getServerUuid()).handleMessage(msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIRefreshPhysicalServerResourceAssignmentsFromProfileMsg msg) {
        APIRefreshPhysicalServerResourceAssignmentsFromProfileEvent event =
                new APIRefreshPhysicalServerResourceAssignmentsFromProfileEvent(msg.getId());
        RoleServiceManifest.reloadAll();
        Map<String, RoleServiceManifest> profiles = new LinkedHashMap<>();
        for (PhysicalServerResourceAssignmentFactory factory :
                pluginRgty.getExtensionList(PhysicalServerResourceAssignmentFactory.class)) {
            profiles.put(factory.getRoleType().toString(), factory.roleServices());
        }
        Set<String> targets = new LinkedHashSet<>();
        if (msg.getServerUuids() == null) {
            for (PhysicalServerResourceAssignmentVO assignment : assignments.listAssignments()) {
                targets.add(assignment.getServerUuid());
            }
        } else {
            targets.addAll(msg.getServerUuids());
        }
        assignments.markUnsyncedByServerUuids(targets);
        targets.forEach(serverUuid -> resourceAssignments(serverUuid).applyAssignmentsFromProfile(profiles));
        bus.publish(event);
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
        msg.setTimeout(TimeUnit.MINUTES.toMillis(5));
        bus.makeTargetServiceIdByResourceUuid(
                msg, PhysicalServerConstant.SERVICE_ID, PhysicalServerConstant.CONTROL_OWNER_KEY);
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
        PhysicalServerResourceAssignmentGlobalConfig.ENABLED.installUpdateExtension((oldConfig, newConfig) -> {
            if (!destinationMaker.isManagedByUs(PhysicalServerConstant.CONTROL_OWNER_KEY)) {
                return;
            }
            if (!newConfig.value(Boolean.class)) {
                Map<String, Set<String>> rolesByServer = new LinkedHashMap<>();
                for (PhysicalServerResourceAssignmentVO assignment : assignments.listAssignments()) {
                    if (getFactory(assignment.getRoleType()).roleServices().getSliceName() != null) {
                        rolesByServer.computeIfAbsent(assignment.getServerUuid(), ignored -> new LinkedHashSet<>())
                                .add(assignment.getRoleType());
                    }
                }
                rolesByServer.forEach((serverUuid, roles) -> resourceAssignments(serverUuid).release(roles));
            }
        });
        return true;
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
