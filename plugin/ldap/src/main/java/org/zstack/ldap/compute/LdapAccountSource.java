package org.zstack.ldap.compute;

import org.zstack.header.core.Completion;
import org.zstack.identity.imports.header.SyncTaskSpec;
import org.zstack.identity.imports.source.AbstractAccountSourceBase;
import org.zstack.ldap.LdapConstant;
import org.zstack.ldap.entity.LdapServerVO;

import static org.zstack.core.Platform.operr;

/**
 * Created by Wenhao.Zhang on 2024/06/03
 */
public class LdapAccountSource extends AbstractAccountSourceBase {
    protected LdapAccountSource(LdapServerVO self) {
        super(self);
    }

    @Override
    public String type() {
        return LdapConstant.LOGIN_TYPE;
    }

    @Override
    protected void syncAccountsFromSource(SyncTaskSpec spec, Completion completion) {
        completion.fail(operr("TODO"));
    }

    @Override
    protected void destroySource(Completion completion) {
        completion.fail(operr("TODO"));
    }
}
