package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.identity.*;
import org.zstack.header.identity.Quota.QuotaPair;
import org.zstack.header.message.APIMessage;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by frank on 7/30/2015.
 */
public class QuotaChecker implements GlobalApiMessageInterceptor {
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public List<Class> getMessageClassToIntercept() {
        return null;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        // login, logout
        if (msg.getSession() == null) {
            return msg;
        }

        List<Quota> quotas = acntMgr.getMessageQuotaMap().get(msg.getClass());
        if (quotas == null || quotas.size() == 0) {
            return msg;
        }
        for (Quota q : quotas) {
            check(msg, q);
        }
        return msg;
    }

    private Map<String, QuotaPair> makeQuotaPairs(Quota quota, SessionInventory session) {
        List<String> names = new ArrayList<>();
        for (QuotaPair p : quota.getQuotaPairs()) {
            names.add(p.getName());
        }

        SimpleQuery<QuotaVO> q = dbf.createQuery(QuotaVO.class);
        q.select(QuotaVO_.name, QuotaVO_.value);
        q.add(QuotaVO_.identityType, Op.EQ, AccountVO.class.getSimpleName());
        q.add(QuotaVO_.identityUuid, Op.EQ, session.getAccountUuid());
        q.add(QuotaVO_.name, Op.IN, names);
        List<Tuple> ts = q.listTuple();

        Map<String, QuotaPair> pairs = new HashMap<>();
        for (Tuple t : ts) {
            String name = t.get(0, String.class);
            long value = t.get(1, Long.class);
            QuotaPair p = new QuotaPair();
            p.setName(name);
            p.setValue(value);
            pairs.put(name, p);
        }

        return pairs;
    }

    private void check(APIMessage msg, Quota quota) {
        Map<String, QuotaPair> pairs = makeQuotaPairs(quota, msg.getSession());
        quota.getOperator().checkQuota(msg, pairs);

        if (quota.getQuotaValidators() != null) {
            for (Quota.QuotaValidator q : quota.getQuotaValidators()) {
                q.checkQuota(msg, pairs);
            }
        }
    }
}
