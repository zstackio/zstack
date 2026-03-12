package org.zstack.identity.rbac;

import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.PolicyInventory;
import org.zstack.header.identity.PolicyStatement;
import org.zstack.header.identity.PolicyStatementEffect;
import org.zstack.header.identity.PolicyVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.UserGroupPolicyRefVO;
import org.zstack.header.identity.UserGroupUserRefVO;
import org.zstack.header.identity.UserGroupVO;
import org.zstack.header.identity.UserPolicyRefVO;
import org.zstack.header.message.APIMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface RBACManager {
    String SERVICE_ID = "rbac";

    List<PolicyInventory> internalPolices = new ArrayList<>();

    static List<PolicyInventory> getPoliciesByAPI(APIMessage message) {
        return getPoliciesBySession(message.getSession());
    }

    static List<PolicyInventory> getPoliciesBySession(final SessionInventory session) {
        if (session.getAccountUuid().equals(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID) || session.isAccountSession()) {
            return new ArrayList<>(internalPolices);

        }

        return new SQLBatchWithReturn<List<PolicyInventory>>() {
            @Override
            protected List<PolicyInventory> scripts() {
                // polices attached to the user
                List<PolicyVO> vos = sql("select p from PolicyVO p, UserPolicyRefVO r where r.policyUuid = p.uuid" +
                        " and r.userUuid = :uuid", PolicyVO.class).param("uuid", session.getUserUuid()).list();

                // polices attached to user groups the user is in
                vos.addAll(sql("select p from PolicyVO p, UserGroupVO g, UserGroupPolicyRefVO up, UserGroupUserRefVO ugu where p.uuid = up.policyUuid" +
                        " and g.uuid = up.groupUuid and g.uuid = ugu.groupUuid and ugu.userUuid = :uuid", PolicyVO.class).param("uuid", session.getUserUuid()).list());

                return PolicyInventory.valueOf(vos);
            }
        }.execute();
    }

    static Map<PolicyInventory, List<PolicyStatement>> collectDenyStatements(List<PolicyInventory> polices) {
        Map<PolicyInventory, List<PolicyStatement>> ret = new HashMap<>();
        polices.forEach(p -> {
            List<PolicyStatement> ss = p.getStatements().stream().filter(s->s.getEffect() == PolicyStatementEffect.Deny).collect(Collectors.toList());
            if (!ss.isEmpty()) {
                ret.put(p, ss);
            }
        });

        return ret;
    }

    static Map<PolicyInventory, List<PolicyStatement>> collectAllowedStatements(List<PolicyInventory> polices) {
        Map<PolicyInventory, List<PolicyStatement>> ret = new HashMap<>();
        polices.forEach(p -> {
            List<PolicyStatement> ss = p.getStatements().stream().filter(s->s.getEffect() == PolicyStatementEffect.Allow).collect(Collectors.toList());
            if (!ss.isEmpty()) {
                ret.put(p, ss);
            }
        });

        return ret;
    }
}
