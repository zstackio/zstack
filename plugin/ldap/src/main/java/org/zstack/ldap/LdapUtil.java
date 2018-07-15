package org.zstack.ldap;

import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.DefaultDirObjectFactory;
import org.springframework.ldap.core.support.DefaultTlsDirContextAuthenticationStrategy;
import org.springframework.ldap.core.support.LdapContextSource;
import org.zstack.core.db.Q;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * Created by miao on 12/19/16.
 */
public class LdapUtil {
    private static final CLogger logger = Utils.getLogger(LdapUtil.class);

    void setTls(LdapContextSource ldapContextSource) {
        // set tls
        logger.debug("Ldap TLS enabled.");
        DefaultTlsDirContextAuthenticationStrategy tls = new DefaultTlsDirContextAuthenticationStrategy();
        tls.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
        tls.setSslSocketFactory(new DummySSLSocketFactory());
        ldapContextSource.setAuthenticationStrategy(tls);
    }

    LdapTemplateContextSource loadLdap(LdapServerInventory inv) {
        LdapContextSource ldapContextSource;
        ldapContextSource = new LdapContextSource();
        ldapContextSource.setUrl(inv.getUrl());
        ldapContextSource.setBase(inv.getBase());
        ldapContextSource.setUserDn(inv.getUsername());
        ldapContextSource.setPassword(inv.getPassword());
        ldapContextSource.setDirObjectFactory(DefaultDirObjectFactory.class);
        if (inv.getEncryption().equals(LdapEncryptionType.TLS.toString())) {
            setTls(ldapContextSource);
        }
        ldapContextSource.setCacheEnvironmentProperties(false);
        ldapContextSource.setPooled(false);
        ldapContextSource.setReferral("follow");

        LdapTemplate ldapTemplate;
        ldapTemplate = new LdapTemplate();
        ldapTemplate.setIgnorePartialResultException(true);
        ldapTemplate.setContextSource(ldapContextSource);

        try {
            ldapContextSource.afterPropertiesSet();
            logger.info("Test LDAP Context Source loaded ");
        } catch (Exception e) {
            logger.error("Test LDAP Context Source not loaded ", e);
            throw new CloudRuntimeException("Test LDAP Context Source not loaded", e);
        }

        return new LdapTemplateContextSource(ldapTemplate, ldapContextSource);
    }

    public static String getMemberKey(){
        String ldapServerUuid = Q.New(LdapServerVO.class).select(LdapServerVO_.uuid).findValue();
        String type = LdapSystemTags.LDAP_SERVER_TYPE.getTokenByResourceUuid(ldapServerUuid, LdapSystemTags.LDAP_SERVER_TYPE_TOKEN);

        if(LdapConstant.WindowsAD.TYPE.equals(type)){
            return LdapConstant.WindowsAD.MEMBER_KEY;
        }

        if(LdapConstant.OpenLdap.TYPE.equals(type)){
            return LdapConstant.OpenLdap.MEMBER_KEY;
        }

        // default WindowsAD
        return LdapConstant.WindowsAD.MEMBER_KEY;
    }

    public static String getLdapUseAsLoginName(){
        String ldapServerUuid = Q.New(LdapServerVO.class).select(LdapServerVO_.uuid).findValue();

        PatternedSystemTag tag = LdapSystemTags.LDAP_USE_AS_LOGIN_NAME;
        if(!tag.hasTag(ldapServerUuid)){
            return LdapConstant.LDAP_UID_KEY;
        }

        return tag.getTokenByResourceUuid(ldapServerUuid, LdapSystemTags.LDAP_USE_AS_LOGIN_NAME_TOKEN);
    }

    public static String getDnKey(){
        String ldapServerUuid = Q.New(LdapServerVO.class).select(LdapServerVO_.uuid).findValue();
        String type = LdapSystemTags.LDAP_SERVER_TYPE.getTokenByResourceUuid(ldapServerUuid, LdapSystemTags.LDAP_SERVER_TYPE_TOKEN);

        if(LdapConstant.WindowsAD.TYPE.equals(type)){
            return LdapConstant.WindowsAD.DN_KEY;
        }

        if(LdapConstant.OpenLdap.TYPE.equals(type)){
            return LdapConstant.OpenLdap.DN_KEY;
        }

        // default WindowsAD
        return LdapConstant.WindowsAD.DN_KEY;
    }
}
