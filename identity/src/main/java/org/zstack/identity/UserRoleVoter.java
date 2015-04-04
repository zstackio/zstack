package org.zstack.identity;

import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountConstant.RoleDecision;
import org.zstack.header.identity.AccountConstant.StatementEffect;
import org.zstack.header.identity.AuthorizationInfo;
import org.zstack.header.identity.PolicyVO;
import org.zstack.header.identity.UserGroupVO;
import org.zstack.header.identity.UserVO;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.PolicyDoc.Statement;

import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserRoleVoter implements AccessDecisionVoter<APIMessage> {

    @Override
    public boolean supports(ConfigAttribute attribute) {
        if (attribute instanceof AuthorizationInfo) {
            return true;
        }
        return false;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        if (APIMessage.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
    }

    List<PolicyDoc> getGroupPolicy(UserVO user) throws JAXBException {
        List<PolicyDoc> policyDocs = new ArrayList<PolicyDoc>();
        for (UserGroupVO g : user.getGroups()) {
            for (PolicyVO pvo : g.getPolicies()) {
                PolicyDoc doc = new PolicyDoc(pvo);
                policyDocs.add(doc);
            }
        }
        return policyDocs;
    }
    
    List<PolicyDoc> getUserPolicy(UserVO user) throws JAXBException {
        List<PolicyDoc> policyDocs = new ArrayList<PolicyDoc>();
        for (PolicyVO pvo : user.getPolicies()) {
            PolicyDoc doc = new PolicyDoc(pvo);
            policyDocs.add(doc);
        }
        return policyDocs;
    }
    
    @Override
    public int vote(Authentication authentication, APIMessage msg, Collection<ConfigAttribute> attributes) {
        return AccessDecisionVoter.ACCESS_GRANTED;

        /*
        try {
            UserVO user = (UserVO) authentication.getDetails();
            AuthorizationInfo ainfo = (AuthorizationInfo) attributes.iterator().next();
            List<PolicyDoc> userPolices = getUserPolicy(user);
            RoleDecision d = authorize(userPolices, ainfo.getNeedRoles());
            if (d == RoleDecision.EXPLICIT_DENY) {
                return AccessDecisionVoter.ACCESS_DENIED;
            } else if (d == RoleDecision.ALLOW) {
                return AccessDecisionVoter.ACCESS_GRANTED;
            }
            
            List<PolicyDoc> groupPolices = getGroupPolicy(user);
            d = authorize(groupPolices, ainfo.getNeedRoles());
            if (d == RoleDecision.EXPLICIT_DENY) {
                return AccessDecisionVoter.ACCESS_DENIED;
            } else if (d == RoleDecision.ALLOW) {
                return AccessDecisionVoter.ACCESS_GRANTED;
            } else {
                return AccessDecisionVoter.ACCESS_DENIED;
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(String.mediaType("Cannot authorize message[%s]", msg.getMessageName()), e);
        }
        */
    }
    
    private RoleDecision doAuthorize(Statement s, List<String> apiRoles) {
        boolean isAllowed = false;
        for (String role : apiRoles) {
            for (String userRole : s.getRoles()) {
                Pattern p = Pattern.compile(userRole);
                Matcher m = p.matcher(role);
                boolean ret = m.matches();
                if (ret) {
                    if (StatementEffect.Deny.toString().equalsIgnoreCase(s.getEffect())) {
                        return RoleDecision.EXPLICIT_DENY;
                    } else if (StatementEffect.Allow.toString().equalsIgnoreCase(s.getEffect())){
                        isAllowed = true;
                    } else {
                        assert false : "should not be here";
                    }
                }
            }
        }
        
        return isAllowed ? RoleDecision.ALLOW : RoleDecision.DEFAULT_DENY;
    }
    private RoleDecision authorize(List<PolicyDoc> userPolicy, List<String> apiRoles) {
        boolean isAllowed = false;
        for (PolicyDoc doc : userPolicy) {
            for (Statement s : doc.getStatements()) {
                RoleDecision d = doAuthorize(s, apiRoles);
                if (d == RoleDecision.EXPLICIT_DENY) {
                    return d;
                } else if (d == RoleDecision.ALLOW) {
                    isAllowed = true;
                }
            }
        }
        
        return isAllowed ? RoleDecision.ALLOW : RoleDecision.DEFAULT_DENY;
    }
}
