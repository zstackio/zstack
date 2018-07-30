package org.zstack.identity.rbac;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.core.StaticInit;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.InternalPolicy;
import org.zstack.header.identity.SharedResourceVO;
import org.zstack.header.identity.rbac.*;
import org.zstack.header.identity.role.*;
import org.zstack.header.identity.role.api.APICreateRoleEvent;
import org.zstack.header.identity.role.api.APICreateRoleMsg;
import org.zstack.header.identity.role.api.RoleMessage;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RBACManagerImpl extends AbstractService implements RBACManager, Component, PrepareDbInitialValueExtensionPoint {
    private static final CLogger logger = Utils.getLogger(RBACManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

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
        } else {
            bus.dealWithUnknownMessage(msg);
        }
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
                vo.setAccountUuid(msg.getSession().getAccountUuid());
                persist(vo);

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
    public void prepareDbInitialValue() {
        new SQLBatch() {
            @Override
            protected void scripts() {
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
