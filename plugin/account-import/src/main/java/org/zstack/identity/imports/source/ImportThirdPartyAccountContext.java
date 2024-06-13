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
    public boolean bindToExistingAccount;
    public boolean skipBinding;
    public ErrorCode errorForValidation;
    public ErrorCode errorForCreatingAccount;

    public boolean hasError() {
        return errorForValidation != null || errorForCreatingAccount != null;
    }

    public ImportAccountResult makeResult() {
        ImportAccountResult result = new ImportAccountResult();
        result.setRef(AccountThirdPartyAccountSourceRefInventory.valueOf(this.ref));
        result.setErrorForCreatingAccount(this.errorForCreatingAccount);
        return result;
    }
}
