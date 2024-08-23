package org.zstack.identity.rbac;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.rbac.PolicyMatcher;
import org.zstack.header.identity.rbac.RBAC;
import org.zstack.header.identity.role.*;
import org.zstack.header.identity.role.api.APICreateRoleEvent;
import org.zstack.header.identity.role.api.APICreateRoleMsg;
import org.zstack.header.identity.role.api.RoleMessage;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.zstack.core.Platform.err;

public class RBACManagerImpl extends AbstractService implements
        RBACManager, Component, PrepareDbInitialValueExtensionPoint, RolePolicyChecker {
    private static final CLogger logger = Utils.getLogger(RBACManagerImpl.class);

    private static PolicyMatcher matcher = new PolicyMatcher();

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof RoleMessage) {
            passThrough((RoleMessage)msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage(msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void passThrough(RoleMessage msg) {
        RoleVO vo = dbf.findByUuid(msg.getRoleUuid(), RoleVO.class);
        if (vo == null) {
            throw new CloudRuntimeException(String.format("RoleVO[uuid:%s] not existing, it may have been deleted", msg.getRoleUuid()));
        }

        new RoleBase(vo).handleMessage((Message) msg);
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(Message msg) {
        if (msg instanceof APICreateRoleMsg) {
            handle((APICreateRoleMsg) msg);
        } else if (msg instanceof APICheckResourcePermissionMsg) {
            handle((APICheckResourcePermissionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APICheckResourcePermissionMsg msg) {
        throw new CloudRuntimeException("APICheckResourcePermissionMsg not support now"); // TODO
    }

    private void handle(APICreateRoleMsg msg) {
        final RoleSpec spec = RoleSpec.valueOf(msg);

        RoleVO vo = spec.buildVOWithoutPolicies();
        dbf.persist(vo);

        List<RolePolicyVO> policies = spec.buildPoliciesToCreate(vo.getUuid());
        dbf.persistCollection(policies);

        APICreateRoleEvent evt = new APICreateRoleEvent(msg.getId());
        evt.setInventory(RoleInventory.valueOf(dbf.findByUuid(vo.getUuid(), RoleVO.class)));
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }

    @Override
    public void prepareDbInitialValue() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                RBAC.roles.stream().filter(RBAC.Role::isPredefine).forEach(role -> {
                    if (!q(RoleVO.class).eq(RoleVO_.uuid, role.getUuid()).isExists()) {
                        RoleVO rvo = new RoleVO();
                        rvo.setUuid(role.getUuid());
                        rvo.setName(String.format("predefined: %s", role.getName()));
                        rvo.setType(RoleType.Predefined);
                        rvo.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                        persist(rvo);

                        AccountResourceRefVO sh = new AccountResourceRefVO();
                        sh.setResourceType(RoleVO.class.getSimpleName());
                        sh.setResourceUuid(rvo.getUuid());
                        sh.setType(AccessLevel.SharePublic);
                        persist(sh);

                        role.toStatements().forEach(s -> {
                            // TODO
                        });
                    } else {
                        role.toStatements().forEach(s -> {
                            String statementString = JSONObjectUtil.toJsonString(s);

                            // TODO
                        });

                    }
                });
            }
        }.execute();
    }

    @Override
    public ErrorCode checkRolePolicies(List<RolePolicyStatement> policies) {
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9._]+$");

        for (RolePolicyStatement policy : policies) {
            for (String action : policy.actions) {
                String path = action.endsWith("**") ? action.substring(0, action.length() - 2) :
                        action.endsWith("*") ? action.substring(0, action.length() - 1) : action;
                Matcher m = pattern.matcher(path);
                if (!m.matches() || path.contains("..")) {
                    return err(IdentityErrors.INVALID_ROLE_POLICY, "invalid role policy actions: %s", action);
                }
            }
        }

        return null;
    }
}
