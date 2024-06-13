package org.zstack.ldap.compute;

import org.zstack.header.core.ReturnValueCompletion;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.identity.imports.header.AbstractAccountSourceSpec;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;
import org.zstack.identity.imports.source.AccountSourceFactory;
import org.zstack.ldap.LdapConstant;
import org.zstack.ldap.entity.LdapServerVO;

import static org.zstack.core.Platform.operr;

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
    public LdapAccountSource createBase(ThirdPartyAccountSourceVO vo) {
        final LdapServerVO ldapServer = (vo instanceof LdapServerVO) ?
                (LdapServerVO) vo :
                databaseFacade.findByUuid(vo.getUuid(), LdapServerVO.class);
        return new LdapAccountSource(ldapServer);
    }

    @Override
    public void createAccountSource(AbstractAccountSourceSpec spec, ReturnValueCompletion<ThirdPartyAccountSourceVO> completion) {
        completion.fail(operr("not support"));
    }
}
