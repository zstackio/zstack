package org.zstack.test.identity;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.*;
import org.zstack.header.identity.PolicyStatement;
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
    Map<String, PolicyInventory> policies = new HashMap<String, PolicyInventory>();
    SessionInventory accountSession;

    public SessionInventory getAccountSession() {
        return accountSession;
    }

    public IdentityCreator(Api api) {
        this.api = api;
    }

    public PolicyInventory createPolicy(String name, List<PolicyStatement> s) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createPolicy()");
        PolicyInventory p = api.createPolicy(name, s, accountSession);
        policies.put(p.getName(), p);
        return p;
    }

    public PolicyInventory createPolicy(String name, PolicyStatement s) throws ApiSenderException {
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

    public void resetAccountPassword(String password) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createPolicy()");
        api.resetAccountPassword(account.getUuid(), password, accountSession);
    }

    public void deletePolicy(String policy) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createPolicy()");
        PolicyInventory p = policies.get(policy);
        DebugUtils.Assert(p != null, String.format("cannot find policy[%s]", policy));

        api.deletePolicy(p.getUuid(), accountSession);
    }

    public SessionInventory accountLogin(String name, String password) throws ApiSenderException {
        DebugUtils.Assert(account != null, "please call createAccount() before createPolicy()");
        return api.loginByAccount(name, password);
    }
}
