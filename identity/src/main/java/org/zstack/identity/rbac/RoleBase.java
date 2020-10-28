package org.zstack.identity.rbac;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.identity.PolicyVO;
import org.zstack.header.identity.role.*;
import org.zstack.header.identity.role.api.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.utils.gson.JSONObjectUtil;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class RoleBase implements Role {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    protected RoleVO self;

    public RoleBase(RoleVO self) {
        this.self = self;
    }

    protected RoleInventory getSelfInventory() {
        return RoleInventory.valueOf(self);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteRoleMsg) {
            handle((APIDeleteRoleMsg) msg);
        } else if (msg instanceof APIAttachRoleToAccountMsg) {
            handle((APIAttachRoleToAccountMsg) msg);
        } else if (msg instanceof APIDetachRoleFromAccountMsg) {
            handle((APIDetachRoleFromAccountMsg) msg);
        } else if (msg instanceof APIAttachPolicyToRoleMsg) {
            handle((APIAttachPolicyToRoleMsg) msg);
        } else if (msg instanceof APIDetachPolicyFromRoleMsg) {
            handle((APIDetachPolicyFromRoleMsg) msg);
        } else if (msg instanceof APIAddPolicyStatementsToRoleMsg) {
            handle((APIAddPolicyStatementsToRoleMsg) msg);
        } else if (msg instanceof APIRemovePolicyStatementsFromRoleMsg) {
            handle((APIRemovePolicyStatementsFromRoleMsg) msg);
        } else if (msg instanceof APIChangeRoleStateMsg) {
            handle((APIChangeRoleStateMsg) msg);
        } else if (msg instanceof APIUpdateRoleMsg) {
            handle((APIUpdateRoleMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateRoleMsg msg) {
        APIUpdateRoleEvent evt = new APIUpdateRoleEvent(msg.getId());
        RoleInventory inv = new SQLBatchWithReturn<RoleInventory>() {
            @Override
            protected RoleInventory scripts() {
                boolean updated = false;

                self = findByUuid(self.getUuid(), RoleVO.class);

                if (msg.getName() != null) {
                    self.setName(msg.getName());
                    updated = true;
                }

                if (msg.getDescription() != null) {
                    self.setDescription(msg.getDescription());
                    updated = true;
                }

                if (updated) {
                    merge(self);
                    self = reload(self);
                }

                if (msg.getStatements() != null) {
                    sql(RolePolicyStatementVO.class).eq(RolePolicyStatementVO_.roleUuid, msg.getRoleUuid()).delete();

                    msg.getStatements().forEach(s -> {
                        RolePolicyStatementVO pvo = new RolePolicyStatementVO();
                        pvo.setRoleUuid(msg.getRoleUuid());
                        pvo.setUuid(Platform.getUuid());
                        pvo.setStatement(JSONObjectUtil.toJsonString(s));
                        persist(pvo);
                    });
                }

                if (msg.getPolicyUuids() != null) {
                    sql(RolePolicyRefVO.class).eq(RolePolicyRefVO_.roleUuid, msg.getRoleUuid()).delete();

                    msg.getPolicyUuids().forEach(puuid -> {
                        RolePolicyRefVO ref = new RolePolicyRefVO();
                        ref.setPolicyUuid(puuid);
                        ref.setRoleUuid(msg.getRoleUuid());
                        persist(ref);
                    });
                }

                flush();
                self = reload(self);
                return RoleInventory.valueOf(self);
            }
        }.execute();
        evt.setInventory(inv);
        bus.publish(evt);
    }

    private void handle(APIChangeRoleStateMsg msg) {
        if (msg.getStateEvent() == RoleStateEvent.enable) {
            self.setState(RoleState.Enabled);
        } else {
            self.setState(RoleState.Disabled);
        }

        self = dbf.updateAndRefresh(self);
        APIChangeRoleStateEvent evt = new APIChangeRoleStateEvent(msg.getId());
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    private void handle(APIRemovePolicyStatementsFromRoleMsg msg) {
        SQL.New(RolePolicyStatementVO.class).eq(RolePolicyStatementVO_.roleUuid, self.getUuid())
                .in(RolePolicyStatementVO_.uuid, msg.getPolicyStatementUuids()).hardDelete();
        bus.publish(new APIRemovePolicyStatementsFromRoleEvent(msg.getId()));
    }

    private void handle(APIAddPolicyStatementsToRoleMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                msg.getStatements().forEach(s -> {
                    RolePolicyStatementVO vo = new RolePolicyStatementVO();
                    vo.setStatement(JSONObjectUtil.toJsonString(s));
                    vo.setUuid(Platform.getUuid());
                    vo.setRoleUuid(self.getUuid());
                    persist(vo);
                });
            }
        }.execute();

        bus.publish(new APIAddPolicyStatementsToRoleEvent(msg.getId()));
    }

    private void handle(APIDetachPolicyFromRoleMsg msg) {
        SQL.New(RolePolicyRefVO.class).eq(RolePolicyRefVO_.policyUuid, msg.getPolicyUuid())
                .eq(RolePolicyRefVO_.roleUuid, msg.getRoleUuid()).hardDelete();
        bus.publish(new APIDetachPolicyFromRoleEvent(msg.getId()));
    }

    private void handle(APIAttachPolicyToRoleMsg msg) {
        RolePolicyRefVO ref = new RolePolicyRefVO();
        ref.setPolicyUuid(msg.getPolicyUuid());
        ref.setRoleUuid(msg.getRoleUuid());
        dbf.persist(ref);
        bus.publish(new APIAttachPolicyToRoleEvent(msg.getId()));
    }

    private void handle(APIDetachRoleFromAccountMsg msg) {
        SQL.New(RoleAccountRefVO.class).eq(RoleAccountRefVO_.accountUuid, msg.getAccountUuid())
                .eq(RoleAccountRefVO_.roleUuid, msg.getRoleUuid()).hardDelete();

        APIDetachRoleFromAccountEvent evt = new APIDetachRoleFromAccountEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIAttachRoleToAccountMsg msg) {
        RoleAccountRefVO ref = new RoleAccountRefVO();
        ref.setAccountUuid(msg.getAccountUuid());
        ref.setRoleUuid(msg.getRoleUuid());
        dbf.persist(ref);
        bus.publish(new APIAttachRoleToAccountEvent(msg.getId()));
    }

    private void handle(APIDeleteRoleMsg msg) {
        new RoleUtils().deleteRole(msg.getUuid());
        bus.publish(new APIDeleteRoleEvent(msg.getId()));
    }
}
