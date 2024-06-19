package org.zstack.ldap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefVO;
import org.zstack.ldap.entity.LdapServerInventory;
import org.zstack.ldap.entity.LdapServerVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import java.util.List;

/**
 * Created by lining on 2017/10/13.
 */
public class LdapUpgradeExtension implements Component {
    private static final CLogger logger = Utils.getLogger(LdapUpgradeExtension.class);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public boolean start() {
        if (LdapGlobalProperty.UPDATE_LDAP_UID_TO_LDAP_DN_ON_START) {
            updateLdapUidToLdapDn();
        }

        return true;
    }

    private void updateLdapUidToLdapDn() {
        if(!isLdapServerExist()){
            logger.info("There is no LDAP/AD server in the system, skip ldap updating");
            return;
        }

        if(!isBindingExist()){
            logger.info("There is no bindings in the system, skip ldap updating");
            return;
        }

        try {
            LdapTemplateContextSource ldapTemplateContextSource = readLdapServerConfiguration();
            LdapTemplate ldapTemplate = ldapTemplateContextSource.getLdapTemplate();

            List<AccountThirdPartyAccountSourceRefVO> refs = Q.New(AccountThirdPartyAccountSourceRefVO.class).list();
            for(AccountThirdPartyAccountSourceRefVO ref : refs){
                update(ldapTemplate, ref);
            }

        }catch (Throwable t){
            logger.error("update ldapUid to ldapDn An error occurred", t);
        }
    }

    private void update(LdapTemplate ldapTemplate, AccountThirdPartyAccountSourceRefVO ref){
        String uid = ref.getCredentials();

        AndFilter filter = new AndFilter();
        filter.and(new EqualsFilter("uid", ref.getCredentials()));

        List<Object> result = ldapTemplate.search("", filter.toString(), new AbstractContextMapper<Object>() {
            @Override
            protected Object doMapFromContext(DirContextOperations ctx) {
                return ctx.getNameInNamespace();
            }
        });

        if(result.size() == 0){
            logger.error(String.format("Can not find ldapUid[%s] dn", uid));
            return;
        }

        if(result.size() > 1){
            logger.error(String.format("ldapUid[%s] More than one dn result", uid));
            return;
        }

        String dn = result.get(0).toString();
        ref.setCredentials(dn);
        dbf.update(ref);
        logger.info(String.format("update ldapUid[%s] to ldapDn[%s] success", uid, dn));
    }

    private boolean isBindingExist(){
        return Q.New(AccountThirdPartyAccountSourceRefVO.class).isExists();
    }

    private boolean isLdapServerExist(){
        return Q.New(LdapServerVO.class).isExists();
    }

    private LdapTemplateContextSource readLdapServerConfiguration() {
        LdapServerVO ldapServerVO = Q.New(LdapServerVO.class).find();
        LdapServerInventory ldapServerInventory = LdapServerInventory.valueOf(ldapServerVO);
        return LdapManager.ldapUtil.loadLdap(ldapServerInventory);
    }

    @Override
    public boolean stop() {
        return true;
    }
}
