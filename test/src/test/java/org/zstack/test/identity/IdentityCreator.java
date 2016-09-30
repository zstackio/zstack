package org.zstack.test.identity;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.*;
import org.zstack.header.identity.PolicyInventory.Statement;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.utils.DebugUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 7/10/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class IdentityCreator {
    @Autowired
    private DatabaseFacade dbf;

    Api api;

    AccountInventory account;
    Map<String, UserInventory> users = new HashMap<String, UserInventory>();
    Map<String, UserGroupInventory> groups = new HashMap<String, UserGroupInventory>();
    Map<String, PolicyInventory> policies = new HashMap<String, PolicyInventory>();
    SessionInventory accountSession;

    public SessionInventory getAccountSession() {
        return accountSession;
    }

    public IdentityCreator(Api api) {
        this.api = api;
    }

    public PolicyInventory createPolicy(String name, List<Statement> s) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createPolicy()");
        PolicyInventory p = api.createPolicy(name, s, accountSession);
        policies.put(p.getName(), p);
        return p;
    }

    public PolicyInventory createPolicy(String name, Statement s) throws ApiSenderException {
        return createPolicy(name, list(s));
    }

    public AccountInventory createAccount(String name, String password) throws ApiSenderException {
        account = api.createAccount(name, password);
        accountSession = api.loginByAccount(name, password);
        return account;
    }

    public AccountInventory useAccount(String name) throws ApiSenderException {
        SimpleQuery<AccountVO> q = dbf.createQuery(AccountVO.class);
        q.add(AccountVO_.name, Op.EQ, name);
        AccountVO vo = q.find();
        DebugUtils.Assert(vo != null, String.format("cannot find account[name:%s]", name));
        account = AccountInventory.valueOf(vo);
        accountSession = api.loginByAccount(name, vo.getPassword());
        return account;
    }

    public UserInventory createUser(String name, String password) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createUser()");
        UserInventory u = api.createUser(account.getUuid(), name, password, accountSession);
        users.put(name, u);
        return u;
    }

    public UserGroupInventory createGroup(String name) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createGroup()");
        UserGroupInventory g = api.createGroup(account.getUuid(), name, accountSession);
        groups.put(name, g);
        return g;
    }

    public void addUserToGroup(String user, String group) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createPolicy()");
        UserInventory u = users.get(user);
        UserGroupInventory g = groups.get(group);
        DebugUtils.Assert(u != null, String.format("cannot find user[%s]", user));
        DebugUtils.Assert(g != null, String.format("cannot find group[%s]", group));
        api.addUserToGroup(u.getUuid(), g.getUuid(), accountSession);
    }

    public void removeUserFromGroup(String user, String group) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createPolicy()");
        UserInventory u = users.get(user);
        UserGroupInventory g = groups.get(group);
        DebugUtils.Assert(u != null, String.format("cannot find user[%s]", user));
        DebugUtils.Assert(g != null, String.format("cannot find group[%s]", group));
        api.removeUserFromGroup(u.getUuid(), g.getUuid(), accountSession);
    }

    public void attachPoliciesToUser(String user, List<String> puuids) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createPolicy()");
        UserInventory u = users.get(user);
        DebugUtils.Assert(u != null, String.format("cannot find user[%s]", user));
        api.attachPolicesToUser(u.getUuid(), puuids, accountSession);
    }

    public void attachPolicyToUser(String user, String policy) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createPolicy()");
        UserInventory u = users.get(user);
        DebugUtils.Assert(u != null, String.format("cannot find user[%s]", user));
        PolicyInventory p = policies.get(policy);
        DebugUtils.Assert(p != null, String.format("cannot find policy[%s]", policy));
        api.attachPolicyToUser(u.getUuid(), p.getUuid(), accountSession);
    }

    public void detachPolicyFromUser(String user, String policy) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createPolicy()");
        UserInventory u = users.get(user);
        DebugUtils.Assert(u != null, String.format("cannot find user[%s]", user));
        PolicyInventory p = policies.get(policy);
        DebugUtils.Assert(p != null, String.format("cannot find policy[%s]", policy));
        api.detachPolicyFromUser(u.getUuid(), p.getUuid(), accountSession);
    }

    public void detachPoliciesFromUser(String user, List<String> puuids) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createPolicy()");
        UserInventory u = users.get(user);
        DebugUtils.Assert(u != null, String.format("cannot find user[%s]", user));
        api.detachPoliciesFromUser(u.getUuid(), puuids, accountSession);
    }

    public void attachPolicyToGroup(String group, String policy) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createPolicy()");
        PolicyInventory p = policies.get(policy);
        DebugUtils.Assert(p != null, String.format("cannot find policy[%s]", policy));
        UserGroupInventory g = groups.get(group);
        DebugUtils.Assert(g != null, String.format("cannot find group[%s]", group));
        api.attachPolicyToGroup(g.getUuid(), p.getUuid(), accountSession);
    }

    public void detachPolicyFromGroup(String group, String policy) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createPolicy()");
        PolicyInventory p = policies.get(policy);
        DebugUtils.Assert(p != null, String.format("cannot find policy[%s]", policy));
        UserGroupInventory g = groups.get(group);
        DebugUtils.Assert(g != null, String.format("cannot find group[%s]", group));
        api.detachPolicyFromGroup(g.getUuid(), p.getUuid(), accountSession);
    }

    public void resetUserPassword(String user, String password) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createPolicy()");
        UserInventory u = users.get(user);
        DebugUtils.Assert(u != null, String.format("cannot find user[%s]", user));

        api.resetUserPassword(u.getUuid(), password, accountSession);
    }

    public void resetAccountPassword(String password) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createPolicy()");
        api.resetAccountPassword(account.getUuid(), password, accountSession);
    }

    public void deleteGroup(String group) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createPolicy()");
        UserGroupInventory g = groups.get(group);
        DebugUtils.Assert(g != null, String.format("cannot find group[%s]", group));
        api.deleteGroup(g.getUuid(), accountSession);
    }

    public void deleteUser(String user) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createPolicy()");
        UserInventory u = users.get(user);
        DebugUtils.Assert(u != null, String.format("cannot find user[%s]", user));

        api.deleteUser(u.getUuid(), accountSession);
    }

    public void deletePolicy(String policy) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createPolicy()");
        PolicyInventory p = policies.get(policy);
        DebugUtils.Assert(p != null, String.format("cannot find policy[%s]", policy));

        api.deletePolicy(p.getUuid(), accountSession);
    }

    public SessionInventory userLogin(String name, String password) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createPolicy()");
        return api.loginByUser(name, password, account.getUuid());
    }

    public SessionInventory accountLogin(String name, String password) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createPolicy()");
        return api.loginByAccount(name, password);
    }
}
