package org.zstack.identity;

import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.vote.AbstractAccessDecisionManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AuthorizationInfo;
import org.zstack.header.identity.UserVO;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.PolicyDoc.Statement;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collection;
import java.util.List;

public class UserRoleDecisionManager extends AbstractAccessDecisionManager {
    private static final CLogger logger = Utils.getLogger(UserRoleDecisionManager.class);
    
    private void printDebugInfo(AccessDecisionVoter voter, Authentication auth, APIMessage msg, Collection<ConfigAttribute> attrs) {
        try {
            UserVO user = (UserVO) auth.getDetails();
            StringBuilder sb = new StringBuilder(String.format("\nUser[name:%s, uuid:%s] has no permission to execute message[%s]", user.getName(),
                    user.getUuid(), msg.getMessageName()));
            UserRoleVoter uv = (UserRoleVoter) voter;
            sb.append("\nGroup policy:");
            List<PolicyDoc> groupDocs = uv.getGroupPolicy(user);
            for (PolicyDoc d : groupDocs) {
                for (Statement s : d.getStatements()) {
                    sb.append("\n\teffect: " + s.getEffect());
                    for (String role : s.getRoles()) {
                        sb.append("\n\trole: " + role);
                    }
                }
            }
            sb.append("\nUser policy:");
            List<PolicyDoc> userDocs = uv.getUserPolicy(user);
            for (PolicyDoc d : userDocs) {
                for (Statement s : d.getStatements()) {
                    sb.append("\n\teffect: " + s.getEffect());
                    for (String role : s.getRoles()) {
                        sb.append("\n\trole: " + role);
                    }
                }
            }
            AuthorizationInfo ainfo = (AuthorizationInfo) attrs.iterator().next();
            sb.append("\nMessage needs policy:");
            for (String r : ainfo.getNeedRoles()) {
                sb.append("\n\trole: " + r);
            }
            logger.debug(sb.toString());
        } catch (Exception e) {
            throw new CloudRuntimeException("", e);
        }
    }

    @Override
    public void decide(Authentication authentication, Object object, Collection<ConfigAttribute> configAttributes) throws AccessDeniedException,
            InsufficientAuthenticationException {
        for (AccessDecisionVoter voter : this.getDecisionVoters()) {
            if (voter.vote(authentication, object, configAttributes) == AccessDecisionVoter.ACCESS_DENIED) {
                printDebugInfo(voter, authentication, (APIMessage) object, configAttributes);
                throw new AccessDeniedException("Access is denied");
            }
        }
    }
}
