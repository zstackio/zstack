package org.zstack.ldap.compute;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.identity.imports.header.AbstractAccountSourceSpec;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;
import org.zstack.identity.imports.source.AbstractAccountSourceBase;
import org.zstack.identity.imports.source.AccountSourceFactory;
import org.zstack.ldap.LdapConstant;

import static org.zstack.core.Platform.operr;

/**
 * Created by Wenhao.Zhang on 2024/06/03
 */
public class LdapAccountSourceFactory implements AccountSourceFactory {
    @Override
    public String type() {
        return LdapConstant.LOGIN_TYPE;
    }

    @Override
    public AbstractAccountSourceBase createBase(ThirdPartyAccountSourceVO vo) {
        return null;
    }

    @Override
    public void createAccountSource(AbstractAccountSourceSpec spec, ReturnValueCompletion<ThirdPartyAccountSourceVO> completion) {
        completion.fail(operr("not support"));
    }
}
