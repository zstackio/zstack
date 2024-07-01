package org.zstack.identity.imports.source;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.identity.AccountInventory;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefInventory;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefVO;
import org.zstack.identity.imports.header.CreateAccountSpec;
import org.zstack.identity.imports.header.ImportAccountResult;

/**
 * Created by Wenhao.Zhang on 2024/05/31
 */
public class ImportThirdPartyAccountContext {
    public AccountInventory account;
    public CreateAccountSpec spec;
    public AccountThirdPartyAccountSourceRefVO ref;
    public boolean readyToCreateAccount;
    public boolean accountExisting;
    public boolean bindingExisting;
    public ErrorCode errorForValidation;
    public ErrorCode errorForCreatingAccount;

    public boolean hasError() {
        return errorForValidation != null || errorForCreatingAccount != null;
    }

    public ImportAccountResult makeResult() {
        ImportAccountResult result = new ImportAccountResult();
        if (hasError()) {
            result.setError(errorForValidation == null ? errorForCreatingAccount : errorForValidation);
        } else {
            result.setRef(AccountThirdPartyAccountSourceRefInventory.valueOf(this.ref));
        }
        return result;
    }
}
