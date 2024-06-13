package org.zstack.identity.imports.source;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.identity.imports.header.UnbindThirdPartyAccountSpecItem;
import org.zstack.identity.imports.header.UnbindThirdPartyAccountResult;

public class UnbindThirdPartyAccountsContext {
    public UnbindThirdPartyAccountSpecItem specItem;
    public ErrorCode errorForDeleteAccount;
    public String sourceUuid;

    public UnbindThirdPartyAccountResult makeResult() {
        UnbindThirdPartyAccountResult result = new UnbindThirdPartyAccountResult();
        result.setAccountUuid(specItem.getAccountUuid());
        result.setErrorForDeleteAccount(errorForDeleteAccount);
        return result;
    }
}
