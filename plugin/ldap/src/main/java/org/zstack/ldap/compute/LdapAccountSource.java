package org.zstack.ldap.compute;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.identity.imports.header.SyncTaskSpec;
import org.zstack.identity.imports.source.AbstractAccountSourceBase;
import org.zstack.ldap.LdapConstant;
import org.zstack.ldap.entity.LdapServerVO;
import org.zstack.ldap.header.LdapSyncTaskSpec;
import org.zstack.resourceconfig.ResourceConfigFacade;

import static org.zstack.core.Platform.operr;
import static org.zstack.ldap.LdapGlobalConfig.*;

/**
 * Created by Wenhao.Zhang on 2024/06/03
 */
public class LdapAccountSource extends AbstractAccountSourceBase {
    protected LdapAccountSource(LdapServerVO self) {
        super(self);
    }

    public LdapServerVO getSelf() {
        return (LdapServerVO) self;
    }

    @Autowired
    private ResourceConfigFacade resourceConfigFacade;

    @Override
    public String type() {
        return LdapConstant.LOGIN_TYPE;
    }

    @Override
    protected void syncAccountsFromSource(SyncTaskSpec spec, Completion completion) {
        final LdapServerVO vo = getSelf();

        final LdapSyncTaskSpec ldapSpec = new LdapSyncTaskSpec(spec);
        ldapSpec.setFilter(vo.getFilter());
        ldapSpec.setUsernameProperty(vo.getUsernameProperty());
        ldapSpec.setServerType(vo.getServerType());
        ldapSpec.setMaxAccountCount(resourceConfigFacade.getResourceConfigValue(
                LDAP_MAXIMUM_SYNC_USERS, self.getUuid(), Integer.class));

        new LdapSyncHelper(ldapSpec).run(new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    protected void destroySource(Completion completion) {
        completion.fail(operr("TODO"));
    }
}
