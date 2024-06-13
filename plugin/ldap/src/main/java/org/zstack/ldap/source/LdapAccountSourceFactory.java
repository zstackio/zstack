package org.zstack.ldap.source;

import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.identity.imports.header.AbstractAccountSourceSpec;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;
import org.zstack.identity.imports.source.AbstractAccountSourceBase;
import org.zstack.identity.imports.source.AccountSourceFactory;
import org.zstack.ldap.LdapConstant;

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
    public ErrorableValue<ThirdPartyAccountSourceVO> createAccountSource(AbstractAccountSourceSpec spec) {
        return null;
    }
}
