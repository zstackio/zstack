package org.zstack.ldap.compute;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.identity.imports.header.AbstractAccountSourceSpec;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;
import org.zstack.identity.imports.source.AbstractAccountSourceBase;
import org.zstack.identity.imports.source.AccountSourceFactory;
import org.zstack.ldap.LdapConstant;
import org.zstack.ldap.entity.LdapServerVO;

/**
 * Created by Wenhao.Zhang on 2024/06/03
 */
public class LdapAccountSourceFactory implements AccountSourceFactory {
    @Autowired
    private DatabaseFacade databaseFacade;

    @Override
    public String type() {
        return LdapConstant.LOGIN_TYPE;
    }

    @Override
    public AbstractAccountSourceBase createBase(ThirdPartyAccountSourceVO vo) {
        final LdapServerVO ldapServer = (vo instanceof LdapServerVO) ?
                (LdapServerVO) vo :
                databaseFacade.findByUuid(vo.getUuid(), LdapServerVO.class);
        return new LdapAccountSource(ldapServer);
    }

    @Override
    public ErrorableValue<ThirdPartyAccountSourceVO> createAccountSource(AbstractAccountSourceSpec spec) {
        return null;
    }
}
