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
            return;
        }

        if(!isBindingExist()){
            return;
        }

        try {
            LdapTemplateContextSource ldapTemplateContextSource = readLdapServerConfiguration();
            LdapTemplate ldapTemplate = ldapTemplateContextSource.getLdapTemplate();

            List<LdapAccountRefVO> refs = Q.New(LdapAccountRefVO.class).list();
            for(LdapAccountRefVO ref : refs){
                update(ldapTemplate, ref);
            }

        }catch (Throwable t){
            logger.error("update ldapUid to ldapDn An error occurred", t);
        }
    }

    private void update(LdapTemplate ldapTemplate, LdapAccountRefVO ref){
        String uid = ref.getLdapUid();

        AndFilter filter = new AndFilter();
        filter.and(new EqualsFilter("uid", ref.getLdapUid()));

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
        ref.setLdapUid(dn);
        dbf.update(ref);
        logger.info(String.format("update ldapUid[%s] to ldapDn[%s] success", uid, dn));
    }

    private boolean isBindingExist(){
        if(Q.New(LdapAccountRefVO.class).isExists()){
            logger.warn("update ldapUid to ldapDn fail, There is no bindings in the system");
            return true;
        }

        return false;
    }

    private boolean isLdapServerExist(){
        if(Q.New(LdapServerVO.class).isExists()){
            logger.warn("update ldapUid to ldapDn fail, There is no ldap server in the system");
            return true;
        }

        return false;
    }

    private LdapTemplateContextSource readLdapServerConfiguration() {
        LdapServerVO ldapServerVO = Q.New(LdapServerVO.class).find();
        LdapServerInventory ldapServerInventory = LdapServerInventory.valueOf(ldapServerVO);
        return new LdapUtil().loadLdap(ldapServerInventory);
    }

    @Override
    public boolean stop() {
        return true;
    }
}
