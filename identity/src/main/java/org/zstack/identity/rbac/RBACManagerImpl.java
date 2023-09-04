package org.zstack.identity.rbac;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.rbac.PolicyMatcher;
import org.zstack.header.identity.rbac.RBAC;
import org.zstack.header.identity.role.*;
import org.zstack.header.identity.role.api.APICreateRoleEvent;
import org.zstack.header.identity.role.api.APICreateRoleMsg;
import org.zstack.header.identity.role.api.RoleMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.identity.IdentityResourceGenerateExtensionPoint;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RBACManagerImpl extends AbstractService implements RBACManager, Component, IdentityResourceGenerateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(RBACManagerImpl.class);

    private static PolicyMatcher matcher = new PolicyMatcher();

    Map<String, RoleIdentityFactory> roleIdentityFactoryMap = new HashMap<>();

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public boolean start() {
        for (RoleIdentityFactory factory : pluginRgty.getExtensionList(RoleIdentityFactory.class)) {
            RoleIdentityFactory old = roleIdentityFactoryMap.get(factory.getIdentity().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate RoleIdentityFactory[%s, %s] with the same type[%s]", factory.getClass(), old.getClass(), factory.getIdentity()));
            }

            roleIdentityFactoryMap.put(factory.getIdentity().toString(), factory);
        }

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

    static {
        BeanUtils.reflections.getSubTypesOf(InternalPolicy.class).forEach(clz -> {
            try {
                InternalPolicy p = clz.getConstructor().newInstance();
                internalPolices.addAll(p.getPolices());
            } catch (CloudRuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        });
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
        APICheckResourcePermissionReply reply = new APICheckResourcePermissionReply();

        List<RBAC.Permission> permissions = RBAC.permissions.stream().filter(p -> p.getTargetResources().stream().anyMatch(resource -> resource.getSimpleName().equals(msg.getResourceType()))).collect(Collectors.toList());

        List<PolicyInventory> policies = RBACManager.getPoliciesByAPI(msg);
        Map<PolicyInventory, List<PolicyStatement>> denyStatements = RBACManager.collectDenyStatements(policies);
        Map<PolicyInventory, List<PolicyStatement>> allowStatements = RBACManager.collectAllowedStatements(policies);

        List<String> apis = new ArrayList<>();
        APIMessage.apiMessageClasses.forEach(apiClz -> {
            boolean deny = denyStatements.values().stream().anyMatch(states -> states.stream().anyMatch(s -> s.getActions().stream().anyMatch(action -> matcher.match(PolicyUtils.apiNamePatternFromAction(action), apiClz.getName()))));
            boolean allow = allowStatements.values().stream().anyMatch(states -> states.stream().anyMatch(s -> s.getActions().stream().anyMatch(action -> matcher.match(PolicyUtils.apiNamePatternFromAction(action), apiClz.getName()))));

            boolean matched = permissions.stream().anyMatch(p -> p.getNormalAPIs().stream().anyMatch(api -> matcher.match(api, apiClz.getName())));

            if (allow && !deny && matched) {
                apis.add(apiClz.getSimpleName());
            }
        });

        reply.setApis(apis);
        bus.reply(msg, reply);
    }

    private void handle(APICreateRoleMsg msg) {
        APICreateRoleEvent evt = new APICreateRoleEvent(msg.getId());

        new SQLBatch() {
            @Override
            protected void scripts() {
                RoleVO vo = new RoleVO();
                vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
                vo.setName(msg.getName());
                vo.setDescription(msg.getDescription());
                vo.setType(RoleType.Customized);
                vo.setIdentity(msg.getIdentity());
                vo.setRootUuid(msg.getRootUuid());
                vo.setAccountUuid(msg.getSession().getAccountUuid());

                if (msg.getIdentity() == null) {
                    persist(vo);
                } else {
                    RoleIdentityFactory factory = roleIdentityFactoryMap.get(msg.getIdentity());
                    if (factory == null) {
                        persist(vo);
                    } else {
                        vo = factory.createRole(vo, msg.getSession());
                    }
                }

                String roleUuid = vo.getUuid();
                if (msg.getStatements() != null) {
                    msg.getStatements().forEach(s -> {
                        RolePolicyStatementVO pvo = new RolePolicyStatementVO();
                        pvo.setRoleUuid(roleUuid);
                        pvo.setUuid(Platform.getUuid());
                        pvo.setStatement(JSONObjectUtil.toJsonString(s));
                        persist(pvo);
                    });
                }

                if (msg.getPolicyUuids() != null) {
                    msg.getPolicyUuids().forEach(puuid -> {
                        RolePolicyRefVO ref = new RolePolicyRefVO();
                        ref.setPolicyUuid(puuid);
                        ref.setRoleUuid(roleUuid);
                        persist(ref);
                    });
                }

                vo = reload(vo);

                evt.setInventory(RoleInventory.valueOf(vo));
            }
        }.execute();

        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }

    @Override
    public String getIdentityType() {
        return AccountConstant.identityType.toString();
    }

    @Override
    public void prepareResources() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                List<NewPredefinedRoleExtensionPoint> exts = pluginRgty.getExtensionList(NewPredefinedRoleExtensionPoint.class);

                RBAC.roles.stream().filter(RBAC.Role::isPredefine).forEach(role -> {
                    if (!q(SystemRoleVO.class).eq(SystemRoleVO_.uuid, role.getUuid()).isExists()) {
                        SystemRoleVO rvo = new SystemRoleVO();
                        rvo.setUuid(role.getUuid());
                        rvo.setName(String.format("predefined: %s", role.getName()));
                        rvo.setSystemRoleType(role.isAdminOnly() ? SystemRoleType.Admin : SystemRoleType.Normal);
                        rvo.setType(RoleType.Predefined);
                        rvo.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                        persist(rvo);

                        SharedResourceVO sh = new SharedResourceVO();
                        sh.setOwnerAccountUuid(rvo.getAccountUuid());
                        sh.setResourceType(RoleVO.class.getSimpleName());
                        sh.setResourceUuid(rvo.getUuid());
                        sh.setToPublic(true);
                        persist(sh);

                        role.toStatements().forEach(s -> {
                            RolePolicyStatementVO rp = new RolePolicyStatementVO();
                            rp.setRoleUuid(rvo.getUuid());
                            rp.setUuid(Platform.getUuid());
                            rp.setStatement(JSONObjectUtil.toJsonString(s));
                            persist(rp);
                        });

//                        for (NewPredefinedRoleExtensionPoint ext : exts) {
//                            ext.predefinedNewRole(reload(rvo));
//                        }
                    } else {
                        role.toStatements().forEach(s -> {
                            String statementString = JSONObjectUtil.toJsonString(s);

                            if (q(RolePolicyStatementVO.class)
                                    .eq(RolePolicyStatementVO_.roleUuid, role.getUuid())
                                    .eq(RolePolicyStatementVO_.statement, statementString).isExists()) {
                                return;
                            }

                            String uuid = q(RolePolicyStatementVO.class).select(RolePolicyStatementVO_.uuid)
                                    .eq(RolePolicyStatementVO_.roleUuid, role.getUuid()).findValue();

                            sql(RolePolicyStatementVO.class).eq(RolePolicyStatementVO_.uuid, uuid)
                                    .set(RolePolicyStatementVO_.statement, statementString).update();
                        });

                    }
                });
            }
        }.execute();
    }
}
