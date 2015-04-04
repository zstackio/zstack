package org.zstack.header.identity;

import org.springframework.security.authentication.BadCredentialsException;
import org.zstack.header.message.APIMessage;

public interface CredentialChecker {
    void authenticateAndAuthorize(APIMessage msg, AuthorizationInfo info) throws CredentialDeniedException;
    
    SessionInventory authenticateByAccount(String accountName, String password) throws CredentialDeniedException;
    
    SessionInventory authenticateByUser(String accountUuid, String userName, String password) throws CredentialDeniedException;

    void validateSession(APIValidateSessionMsg msg) throws BadCredentialsException;
    
    void logOutSession(String sessionUuid);
}
