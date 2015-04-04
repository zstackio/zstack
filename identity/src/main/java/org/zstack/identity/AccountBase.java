package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.search.SearchQuery;
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AccountBase extends AbstractAccount {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    private AccountVO vo;

    public AccountBase(AccountVO vo) {
        this.vo = vo;
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg instanceof APIMessage) {
                handleApiMessage((APIMessage) msg);
            } else {
                handleLocalMessage(msg);
            }
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg, e);
        }
    }

    @Transactional
    private AccountVO resetAccountPassword(AccountVO avo, UserVO uvo, String password) {
        avo.setPassword(password);
        avo = dbf.getEntityManager().merge(avo);
        uvo.setPassword(password);
        dbf.getEntityManager().merge(uvo);
        return avo;
    }

    private void handle(APIResetAccountPasswordMsg msg) {
        String accountUuidToReset = msg.getAccountUuid().equals(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID) ? msg.getAccountUuidToReset() : vo.getUuid();
        SimpleQuery<UserVO> uq = dbf.createQuery(UserVO.class);
        uq.add(UserVO_.accountUuid, Op.EQ, accountUuidToReset);
        uq.add(UserVO_.name, Op.EQ, vo.getName());
        UserVO uvo = uq.find();

        vo = resetAccountPassword(vo, uvo, msg.getPassword());
        APIResetAccountPasswordEvent evt = new APIResetAccountPasswordEvent(msg.getId());
        evt.setInventory(AccountInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIResetAccountPasswordMsg) {
            handle((APIResetAccountPasswordMsg) msg);
        } else if (msg instanceof APICreateUserMsg) {
            handle((APICreateUserMsg) msg);
        } else if (msg instanceof APICreatePolicyMsg) {
            handle((APICreatePolicyMsg) msg);
        } else if (msg instanceof APIAttachPolicyToUserMsg) {
            handle((APIAttachPolicyToUserMsg)msg);
        } else if (msg instanceof APICreateUserGroupMsg) {
            handle((APICreateUserGroupMsg)msg);
        } else if (msg instanceof APIAttachPolicyToUserGroupMsg) {
            handle((APIAttachPolicyToUserGroupMsg)msg);
        } else if (msg instanceof APIAttachUserToUserGroupMsg) {
            handle((APIAttachUserToUserGroupMsg)msg);
        } else if (msg instanceof APISearchUserMsg) {
            handle((APISearchUserMsg) msg);
        } else if (msg instanceof APISearchUserGroupMsg) {
            handle((APISearchUserGroupMsg) msg);
        } else if (msg instanceof APISearchPolicyMsg) {
            handle((APISearchPolicyMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APISearchPolicyMsg msg) {
        SearchQuery<PolicyInventory> query = SearchQuery.create(msg, PolicyInventory.class);
        query.addAccountAsAnd(msg);
        String content = query.listAsString();
        APISearchPolicyReply reply = new APISearchPolicyReply();
        reply.setContent(content);
        bus.reply(msg, reply);
    }

    private void handle(APISearchUserGroupMsg msg) {
        SearchQuery<UserGroupInventory> query = SearchQuery.create(msg, UserGroupInventory.class);
        query.addAccountAsAnd(msg);
        String content = query.listAsString();
        APISearchUserGroupReply reply = new APISearchUserGroupReply();
        reply.setContent(content);
        bus.reply(msg, reply);
    }

    private void handle(APISearchUserMsg msg) {
        SearchQuery<UserInventory> query = SearchQuery.create(msg, UserInventory.class);
        query.addAccountAsAnd(msg);
        String content = query.listAsString();
        APISearchUserReply reply = new APISearchUserReply();
        reply.setContent(content);
        bus.reply(msg, reply);
    }

    private void handle(APIAttachUserToUserGroupMsg msg) {
        UserGroupUserRefVO ugvo = new UserGroupUserRefVO();
        ugvo.setGroupUuid(msg.getGroupUuid());
        ugvo.setUserUuid(msg.getUserUuid());
        dbf.persist(ugvo);
        APIAttachUserToUserGroupEvent evt = new APIAttachUserToUserGroupEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIAttachPolicyToUserGroupMsg msg) {
        UserGroupPolicyRefVO grvo = new UserGroupPolicyRefVO();
        grvo.setGroupUuid(msg.getGroupUuid());
        grvo.setPolicyUuid(msg.getPolicyUuid());
        dbf.persist(grvo);
        APIAttachPolicyToUserGroupEvent evt = new APIAttachPolicyToUserGroupEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APICreateUserGroupMsg msg) {
        UserGroupVO gvo = new UserGroupVO();
        if (msg.getResourceUuid() != null) {
            gvo.setUuid(msg.getResourceUuid());
        } else {
            gvo.setUuid(Platform.getUuid());
        }
        gvo.setAccountUuid(vo.getUuid());
        gvo.setDescription(msg.getGroupDescription());
        gvo.setName(msg.getGroupName());
        dbf.persistAndRefresh(gvo);
        UserGroupInventory inv = UserGroupInventory.valueOf(gvo);
        APICreateUserGroupEvent evt = new APICreateUserGroupEvent(msg.getId());
        evt.setInventory(inv);
        bus.publish(evt);
    }

    private void handle(APIAttachPolicyToUserMsg msg) {
        UserPolicyRefVO upvo = new UserPolicyRefVO();
        upvo.setPolicyUuid(msg.getPolicyUuid());
        upvo.setUserUuid(msg.getUserUuid());
        dbf.persist(upvo);
        
        APIAttachPolicyToUserEvent evt = new APIAttachPolicyToUserEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APICreatePolicyMsg msg) {
        PolicyVO pvo = new PolicyVO();
        if (msg.getResourceUuid() != null) {
            pvo.setUuid(msg.getResourceUuid());
        } else {
            pvo.setUuid(Platform.getUuid());
        }
        pvo.setAccountUuid(vo.getUuid());
        pvo.setName(msg.getName());
        pvo.setDescription(msg.getDescription());
        pvo.setData(msg.getPolicyData());
        PolicyType type = vo.getType() == AccountType.SystemAdmin ? PolicyType.System : PolicyType.User;
        pvo.setType(type);
        pvo = dbf.persistAndRefresh(pvo);
        PolicyInventory pinv = PolicyInventory.valueOf(pvo);
        APICreatePolicyEvent evt = new APICreatePolicyEvent(msg.getId());
        evt.setInventory(pinv);
        bus.publish(evt);
    }

    private void handle(APICreateUserMsg msg) {
        APICreateUserEvent evt = new APICreateUserEvent(msg.getId());
        UserVO uvo = new UserVO();
        if (msg.getResourceUuid() != null) {
            uvo.setUuid(msg.getResourceUuid());
        } else {
            uvo.setUuid(Platform.getUuid());
        }
        uvo.setAccountUuid(vo.getUuid());
        uvo.setName(msg.getUserName());
        uvo.setPassword(msg.getPassword());
        uvo = dbf.persistAndRefresh(uvo);
        UserInventory inv = UserInventory.valueOf(uvo);
        evt.setInventory(inv);
        bus.publish(evt);
    }
}
