package org.zstack.ldap.driver;

import org.apache.commons.lang.StringUtils;
import org.springframework.ldap.AuthenticationException;
import org.springframework.ldap.CommunicationException;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.control.PagedResultsDirContextProcessor;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.NameAwareAttribute;
import org.springframework.ldap.core.support.*;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.ldap.*;
import org.zstack.ldap.entity.LdapServerType;
import org.zstack.ldap.entity.LdapServerVO;
import org.zstack.ldap.entity.LdapServerVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.net.ssl.*;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.function.Predicate;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by miao on 12/19/16.
 */
public class LdapUtil {
    private static final CLogger logger = Utils.getLogger(LdapUtil.class);

    static final String PAGED_RESULTS_CONTROL_OID = "1.2.840.113556.1.4.319";

    private void setSsl() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext ctx = SSLContext.getInstance("TLS");
        X509TrustManager tm;

        if (LdapGlobalConfig.SKIP_ALL_SSL_CERTS_CHECK.value(Boolean.class)) {
            tm = new X509ExtendedTrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {

                }

                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws CertificateException {

                }

                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
        } else {
            tm = new X509TrustManager() {

                public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
        }

        ctx.init(null, new TrustManager[]{tm}, null);
        SSLContext.setDefault(ctx);
    }

    void setTls(LdapContextSource ldapContextSource) {
        // set tls
        logger.debug("Ldap TLS enabled.");
        DefaultTlsDirContextAuthenticationStrategy tls = new DefaultTlsDirContextAuthenticationStrategy();
        tls.setHostnameVerifier((hostname, session) -> true);
        tls.setSslSocketFactory(new DummySSLSocketFactory());
        ldapContextSource.setAuthenticationStrategy(tls);
    }

    LdapContextSource buildLdapContextSource(LdapServerVO vo, Map<String, Object> baseEnvironmentProperties) {
        LdapContextSource ldapContextSource;
        ldapContextSource = new LdapContextSource();

        List<String> urls = new ArrayList<>();
        urls.add(vo.getUrl());

        if (LdapSystemTags.LDAP_URLS.hasTag(vo.getUuid())) {
            String standbyUrls = LdapSystemTags.LDAP_URLS.getTokenByResourceUuid(vo.getUuid(), LdapSystemTags.LDAP_URLS_TOKEN);

            urls.addAll(list(standbyUrls.split(",")));
        }

        ldapContextSource.setUrls(urls.toArray(new String[]{}));
        ldapContextSource.setBase(vo.getBase());
        ldapContextSource.setUserDn(vo.getUsername());
        ldapContextSource.setPassword(vo.getPassword());
        ldapContextSource.setDirObjectFactory(DefaultDirObjectFactory.class);
        if (vo.getEncryption().equals(LdapEncryptionType.TLS.toString())) {
            if (urls.get(0).contains(LdapConstant.LDAP_SSL_PREFIX)) {
                try {
                    setSsl();
                } catch (NoSuchAlgorithmException | KeyManagementException e) {
                    logger.debug("Failed to init ssl skip it and connection failure will occurs");
                }
            } else {
                setTls(ldapContextSource);
            }
        }
        ldapContextSource.setCacheEnvironmentProperties(false);
        ldapContextSource.setPooled(false);
        ldapContextSource.setReferral("follow");

        if (baseEnvironmentProperties != null && !baseEnvironmentProperties.isEmpty()) {
            ldapContextSource.setBaseEnvironmentProperties(baseEnvironmentProperties);
        }

        try {
            ldapContextSource.afterPropertiesSet();
            logger.info("Test LDAP Context Source loaded ");
        } catch (Exception e) {
            logger.error("Test LDAP Context Source not loaded ", e);
            throw new CloudRuntimeException("Test LDAP Context Source not loaded", e);
        }

        return ldapContextSource;
    }

    LdapTemplateContextSource loadRootLdap(LdapServerVO ldap) {
        LdapContextSource ldapContextSource = buildLdapContextSource(ldap, getBaseEnvProperties(ldap));
        ldapContextSource.setBase("");
        return buildLdapTemplateContextSource(ldapContextSource);
    }

    public LdapTemplateContextSource loadLdap(LdapServerVO ldap) {
        return buildLdapTemplateContextSource(buildLdapContextSource(ldap, getBaseEnvProperties(ldap)));
    }

    public LdapTemplateContextSource loadLdap(LdapServerVO ldap, Map<String, Object> baseEnvironmentProperties) {
        return buildLdapTemplateContextSource(buildLdapContextSource(ldap, baseEnvironmentProperties));
    }

    private LdapTemplateContextSource buildLdapTemplateContextSource(LdapContextSource ldapContextSource) {
        LdapTemplate ldapTemplate = new LdapTemplate();
        ldapTemplate.setIgnorePartialResultException(true);
        ldapTemplate.setContextSource(ldapContextSource);
        return new LdapTemplateContextSource(ldapTemplate, ldapContextSource);
    }

    public Map<String, Object> getBaseEnvProperties(LdapServerVO ldap) {
        LdapServerType type = ldap.getServerType();

        Map<String, Object> properties = new HashMap<>();
        if(type == LdapServerType.WindowsAD){
            properties.put("java.naming.ldap.attributes.binary","objectGUID");
        }
        // add socket timeout
        String timeout = Integer.toString(LdapGlobalProperty.LDAP_ADD_SERVER_CONNECT_TIMEOUT);
        properties.put("com.sun.jndi.ldap.connect.timeout", timeout);
        String readTimeout = Integer.toString(LdapGlobalProperty.LDAP_ADD_SERVER_READ_TIMEOUT );
        properties.put("com.sun.jndi.ldap.read.timeout", readTimeout);
        return properties;
    }

    public String decodeGlobalUuid(NameAwareAttribute attribute, String ldapServerUuid) throws javax.naming.NamingException {
        LdapServerType type = Q.New(LdapServerVO.class)
                .eq(LdapServerVO_.uuid, ldapServerUuid)
                .select(LdapServerVO_.serverType)
                .findValue();

        if (type == LdapServerType.WindowsAD) {
            byte[] GUID = (byte[]) attribute.get();

            String strGUID;
            String byteGUID = "";


            //Convert the GUID into string using the byte format
            for (int c=0;c<GUID.length;c++) {
                byteGUID = byteGUID + "\\" + addLeadingZero((int)GUID[c] & 0xFF);
            }
            strGUID = "{";
            strGUID = strGUID + addLeadingZero((int)GUID[3] & 0xFF);
            strGUID = strGUID + addLeadingZero((int)GUID[2] & 0xFF);
            strGUID = strGUID + addLeadingZero((int)GUID[1] & 0xFF);
            strGUID = strGUID + addLeadingZero((int)GUID[0] & 0xFF);
            strGUID = strGUID + "-";
            strGUID = strGUID + addLeadingZero((int)GUID[5] & 0xFF);
            strGUID = strGUID + addLeadingZero((int)GUID[4] & 0xFF);
            strGUID = strGUID + "-";
            strGUID = strGUID + addLeadingZero((int)GUID[7] & 0xFF);
            strGUID = strGUID + addLeadingZero((int)GUID[6] & 0xFF);
            strGUID = strGUID + "-";
            strGUID = strGUID + addLeadingZero((int)GUID[8] & 0xFF);
            strGUID = strGUID + addLeadingZero((int)GUID[9] & 0xFF);
            strGUID = strGUID + "-";
            strGUID = strGUID + addLeadingZero((int)GUID[10] & 0xFF);
            strGUID = strGUID + addLeadingZero((int)GUID[11] & 0xFF);
            strGUID = strGUID + addLeadingZero((int)GUID[12] & 0xFF);
            strGUID = strGUID + addLeadingZero((int)GUID[13] & 0xFF);
            strGUID = strGUID + addLeadingZero((int)GUID[14] & 0xFF);
            strGUID = strGUID + addLeadingZero((int)GUID[15] & 0xFF);
            strGUID = strGUID + "}";

            return strGUID;
        }

        return attribute.get(0).toString();
    }

    public String addLeadingZero(int k) {
        return (k <= 0xF) ? "0" + Integer.toHexString(k) : Integer
                .toHexString(k);
    }

    private LdapTemplate getRootBaseLdapTemplate(String ldapServerUuid) {
        LdapTemplateContextSource ldapTemplateContextSource = readRootLdapServerConfiguration(ldapServerUuid);
        LdapTemplate ldapTemplate = ldapTemplateContextSource.getLdapTemplate();
        ldapTemplate.setContextSource(new SingleContextSource(ldapTemplateContextSource.getLdapContextSource().getReadOnlyContext()));

        return ldapTemplate;
    }

    private LdapEntrySearchMode getSuitableSearchMode(LdapTemplate ldapTemplate) {
        if (isControlSupported(ldapTemplate, PAGED_RESULTS_CONTROL_OID)) {
            logger.debug("[ldap] paged results control is supported");
            return LdapEntrySearchMode.PAGE;
        }

        logger.debug("[ldap] no control matched, search without processor");
        return LdapEntrySearchMode.NONE;
    }

    private boolean isControlSupported(LdapTemplate ldapTemplate, String dotOid) {
        String[] returningAttributes =
                {
                        "supportedControl",
                        "supportedExtension"
                };

        List<Set<String>> attributesResultList = ldapTemplate.search("", "(objectclass=*)", SearchControls.OBJECT_SCOPE, returningAttributes, new AbstractContextMapper<Set<String>>() {
            @Override
            protected Set<String> doMapFromContext(DirContextOperations ctx) {
                Set<String> controlsSet = new HashSet<>();
                controlsSet.addAll(Arrays.asList(ctx.getStringAttributes("supportedControl")));
                    controlsSet.addAll(Arrays.asList(ctx.getStringAttributes("supportedExtension")));

                return controlsSet;
            }
        });

        if (attributesResultList.isEmpty()) {
            return false;
        }

        return attributesResultList.get(0).remove(dotOid);
    }

    private LdapSearchedResult searchWithoutProcessor(LdapTemplate ldapTemplate, String filter, SearchControls searchCtls, Predicate<String> resultFilter, Integer count) {
        LdapSearchedResult ldapSearchedResult = new LdapSearchedResult();
        ldapSearchedResult.setResult(new ArrayList<>());

        List<Object> searchResult = new ArrayList<>();
        try {
            searchResult = ldapTemplate.search("", filter, searchCtls, new AbstractContextMapper<Object>() {
                @Override
                protected Object doMapFromContext(DirContextOperations ctx) {
                    if (resultFilter != null && !resultFilter.test(ctx.getNameInNamespace())){
                        return null;
                    }

                    Map<String, Object> result = new HashMap<>();
                    result.put(LdapConstant.LDAP_DN_KEY, ctx.getNameInNamespace());

                    List<Object> list = new ArrayList<>();
                    result.put("attributes", list);

                    Attributes attributes = ctx.getAttributes();
                    NamingEnumeration it = attributes.getAll();
                    try {
                        while (it.hasMore()){
                            list.add(it.next());
                        }
                    } catch (javax.naming.NamingException e){
                        logger.error("query ldap entry attributes fail", e.getCause());
                        throw new OperationFailureException(operr("query ldap entry fail, %s", e.toString()));
                    }

                    return result;
                }
            });
        } catch (Exception e){
            logger.error("legacy query ldap entry fail", e);
            ldapSearchedResult.setSuccess(false);
            ldapSearchedResult.setResult(null);
            ldapSearchedResult.setError(e.getMessage());
        }

        if (count != null && searchResult.size() > count){
            ldapSearchedResult.setResult(searchResult.subList(0, count));
        } else {
            ldapSearchedResult.setResult(searchResult);
        }

        return ldapSearchedResult;
    }

    private LdapSearchedResult pagedSearch(LdapTemplate ldapTemplate, String filter, SearchControls searchCtls, Predicate<String> resultFilter, Integer count) {
        LdapSearchedResult ldapSearchedResult = new LdapSearchedResult();
        ldapSearchedResult.setResult(new ArrayList<>());

        PagedResultsDirContextProcessor processor = new PagedResultsDirContextProcessor(1000);
        try {
            do {
                List<Object> subResult = ldapTemplate.search("", filter, searchCtls, new AbstractContextMapper<Object>() {
                    @Override
                    protected Object doMapFromContext(DirContextOperations ctx) {
                        if (resultFilter != null && !resultFilter.test(ctx.getNameInNamespace())){
                            return null;
                        }

                        Map<String, Object> result = new HashMap<>();
                        result.put(LdapConstant.LDAP_DN_KEY, ctx.getNameInNamespace());

                        List<Object> list = new ArrayList<>();
                        result.put("attributes", list);

                        Attributes attributes = ctx.getAttributes();
                        NamingEnumeration it = attributes.getAll();
                        try {
                            while (it.hasMore()){
                                list.add(it.next());
                            }
                        } catch (javax.naming.NamingException e){
                            logger.error("query ldap entry attributes fail", e.getCause());
                            throw new OperationFailureException(operr("query ldap entry fail, %s", e.toString()));
                        }

                        return result;
                    }
                }, processor);

                subResult.removeIf(Objects::isNull);
                ldapSearchedResult.getResult().addAll(subResult);

            } while (processor.hasMore() && (count == null || count > ldapSearchedResult.getResult().size()));

            if (count != null && ldapSearchedResult.getResult().size() > count){
                ldapSearchedResult.setResult(ldapSearchedResult.getResult().subList(0, count));
            }
        } catch (Exception e) {
            logger.error("query ldap entry with paged processor fail", e);
            ldapSearchedResult.setSuccess(false);
            ldapSearchedResult.setResult(null);
            ldapSearchedResult.setError(e.getMessage());
        }

        return ldapSearchedResult;
    }

    public List<Object> searchLdapEntry(LdapSearchSpec spec) {
        String ldapServerUuid = spec.getLdapServerUuid();
        String filter = spec.getFilter();
        Integer count = spec.getCount();
        String[] returningAttributes = spec.getReturningAttributes();
        Predicate<String> resultFilter = spec.getResultFilter();
        boolean searchAllAttributes = spec.isSearchAllAttributes();

        LdapServerVO ldap = Q.New(LdapServerVO.class)
                .eq(LdapServerVO_.uuid, ldapServerUuid)
                .find();

        LdapTemplateContextSource ldapTemplateContextSource = readLdapServerConfiguration(ldap);
        LdapTemplate ldapTemplate = ldapTemplateContextSource.getLdapTemplate();
        ldapTemplate.setContextSource(new SingleContextSource(ldapTemplateContextSource.getLdapContextSource().getReadOnlyContext()));

        SearchControls searchCtls = new SearchControls();
        searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        if (searchAllAttributes) {
            searchCtls.setReturningAttributes(null);
        } else {
            searchCtls.setReturningAttributes(returningAttributes == null ? getReturningAttributes(ldap) : returningAttributes);
        }

        String errorMessage = "";

        LdapSearchedResult ldapSearchedResult;
        LdapEntrySearchMode ldapEntrySearchMode = LdapEntrySearchMode.valueOf(LdapGlobalConfig.LDAP_ENTRY_SEARCH_MODE.value());
        if (ldapEntrySearchMode == LdapEntrySearchMode.AUTO) {
            ldapEntrySearchMode = getSuitableSearchMode(getRootBaseLdapTemplate(ldapServerUuid));
        }

        if (ldapEntrySearchMode == LdapEntrySearchMode.PAGE) {
            ldapSearchedResult = pagedSearch(ldapTemplate, filter, searchCtls, resultFilter, count);
        } else if (ldapEntrySearchMode == LdapEntrySearchMode.NONE) {
            ldapSearchedResult = searchWithoutProcessor(ldapTemplate, filter, searchCtls, resultFilter, count);
        } else {
            throw new CloudRuntimeException(String.format("unexpected LdapEntrySearchMode: %s", ldapEntrySearchMode));
        }

        if (ldapSearchedResult.isSuccess()) {
            return ldapSearchedResult.getResult();
        }

        if (ldapSearchedResult.getError() != null) {
            errorMessage += String.format("\n paged query ldap entry failed, because %s", ldapSearchedResult.getError());
        }

        // Paged search may not supported by some ldap server
        // Add external search for compatibility
        List<LdapExternalSearchExtensionPoint> exts = Platform.getComponentLoader().getPluginRegistry().getExtensionList(LdapExternalSearchExtensionPoint.class);
        for (LdapExternalSearchExtensionPoint ext : exts) {
            ldapSearchedResult = ext.trySearch(ldapTemplate, filter, searchCtls, resultFilter, count);

            if (ldapSearchedResult.isSuccess()) {
                return ldapSearchedResult.getResult();
            }

            if (ldapSearchedResult.getError() != null) {
                errorMessage += String.format("\n external query ldap entry failed, because %s", ldapSearchedResult.getError());
            }
        }

        throw new OperationFailureException(operr("query ldap entry[filter: %s] fail, because %s", filter, errorMessage));
    }

    private String[] getReturningAttributes(LdapServerVO ldap) {
        Set<String> returnedAttSet = new HashSet<>();

        String queryLdapEntryReturnAttributes = LdapGlobalConfig.QUERY_LDAP_ENTRY_RETURN_ATTRIBUTES.value();
        if(StringUtils.isNotEmpty(queryLdapEntryReturnAttributes)){
            String separator = LdapGlobalConfig.QUERY_LDAP_ENTRY_RETURN_ATTRIBUTE_SEPARATOR.value();
            separator = separator != null ? separator : LdapConstant.QUERY_LDAP_ENTRY_RETURN_ATTRIBUTE_SEPARATOR;
            String[] attributes = queryLdapEntryReturnAttributes.split(separator);

            returnedAttSet.addAll(Arrays.asList(attributes));
        }

        returnedAttSet.addAll(Arrays.asList(LdapConstant.QUERY_LDAP_ENTRY_MUST_RETURN_ATTRIBUTES));

        returnedAttSet.add(ldap.getUsernameProperty());

        return returnedAttSet.toArray(new String[]{});
    }

    public LdapTemplateContextSource readLdapServerConfiguration(LdapServerVO ldap) {
        return loadLdap(ldap);
    }

    // set LdapContextSource base as empty
    public LdapTemplateContextSource readRootLdapServerConfiguration(String ldapServerUuid) {
        LdapServerVO ldapServerVO = Q.New(LdapServerVO.class)
                .eq(LdapServerVO_.uuid, ldapServerUuid)
                .find();
        return loadRootLdap(ldapServerVO);
    }

    @Transactional(readOnly = true)
    public LdapServerVO getLdapServer() {
        final List<LdapServerVO> ldapServers = Q.New(LdapServerVO.class).list();
        if (ldapServers.isEmpty()) {
            throw new CloudRuntimeException("No LDAP/AD server record in database.");
        }
        if (ldapServers.size() > 1) {
            throw new CloudRuntimeException("More than one LDAP/AD server record in database.");
        }
        return ldapServers.get(0);
    }

    private String LdapEscape(String ldapDn) {
        return ldapDn.replace("/", "\\2f");
    }

    public ErrorCode validateDnExist(String fullDn, LdapServerVO ldap) {
        LdapTemplateContextSource context = readLdapServerConfiguration(ldap);

        try {
            String dn = fullDn.replace("," + context.getLdapContextSource().getBaseLdapPathAsString(), "");
            dn = LdapEscape(dn);
            Object result = context.getLdapTemplate().lookup(dn, new AbstractContextMapper<Object>() {
                @Override
                protected Object doMapFromContext(DirContextOperations ctx) {
                    return ctx.getAttributes();
                }
            });

            if (result != null) {
                return null;
            }
            return err(LdapErrors.UNABLE_TO_GET_SPECIFIED_LDAP_UID,
                    "user[%s] is not exists on LDAP/AD server[address:%s, baseDN:%s]",
                    fullDn,
                    String.join(", ", context.getLdapContextSource().getUrls()),
                    context.getLdapContextSource().getBaseLdapPathAsString());
        } catch (Exception e){
            return err(LdapErrors.UNABLE_TO_GET_SPECIFIED_LDAP_UID,
                    "failed to find dn[%s] on LDAP/AD server[address:%s, baseDN:%s]: %s",
                    fullDn,
                    String.join(", ", context.getLdapContextSource().getUrls()),
                    context.getLdapContextSource().getBaseLdapPathAsString(),
                    e.getMessage());
        }
    }

    public String getFullUserDn(String uid, LdapServerVO ldap) {
        LdapTemplateContextSource ldapTemplateContextSource = readLdapServerConfiguration(ldap);
        String fullUserDn = null;
        try {
            fullUserDn = getFullUserDn(ldapTemplateContextSource.getLdapTemplate(), ldap.getUsernameProperty(), uid);

            if (fullUserDn.equals("")) {
                return null;
            }
        } catch (Exception e) {
            logger.debug(String.format("Get user dn of uid[%s] failed ", uid));
        }

        return fullUserDn;
    }

    public String getFullUserDn(LdapServerVO ldap, String key, String val) {
        LdapTemplateContextSource context = readLdapServerConfiguration(ldap);
        return getFullUserDn(context.getLdapTemplate(), key, val);
    }

    private String getFullUserDn(LdapTemplate ldapTemplate, String key, String val) {
        EqualsFilter f = new EqualsFilter(key, val);
        String filter = f.toString();

        String dn;
        try {
            List<Object> result = ldapTemplate.search("", filter, new AbstractContextMapper<Object>() {
                @Override
                protected Object doMapFromContext(DirContextOperations ctx) {
                    return ctx.getNameInNamespace();
                }
            });
            if (result.size() == 1) {
                dn = result.get(0).toString();
            } else if (result.size() > 1) {
                throw new OperationFailureException(err(
                        LdapErrors.UNABLE_TO_GET_SPECIFIED_LDAP_UID, "More than one ldap search result"));
            } else {
                return "";
            }
            logger.info(String.format("getDn success filter:%s, dn:%s", filter, dn));
        } catch (NamingException e) {
            LdapServerVO ldapServerVO = getLdapServer();
            throw new OperationFailureException(err(
                    LdapErrors.UNABLE_TO_GET_SPECIFIED_LDAP_UID, "You'd better check the LDAP/AD server[url:%s, baseDN:%s, encryption:%s, username:%s, password:******]" +
                            " configuration and test connection first.getDn error filter:%s",
                    ldapServerVO.getUrl(), ldapServerVO.getBase(),
                    ldapServerVO.getEncryption(), ldapServerVO.getUsername(), filter));
        }
        return dn;
    }

    public boolean isValid(String uid, String password, LdapServerVO ldap) {
        LdapTemplateContextSource ldapTemplateContextSource = readLdapServerConfiguration(ldap);
        String ldapUseAsLoginName = ldap.getUsernameProperty();
        try {
            boolean valid;
            String fullUserDn = getFullUserDn(ldapTemplateContextSource.getLdapTemplate(), ldapUseAsLoginName, uid);
            if (fullUserDn.equals("") || password.equals("")) {
                return false;
            }

            LdapServerVO ldapServerVO = getLdapServer();
            LdapTemplateContextSource ldapTemplateContextSource2 = loadLdap(ldapServerVO);

            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter(ldapUseAsLoginName, uid));
            valid = ldapTemplateContextSource2.getLdapTemplate().
                    authenticate("", filter.toString(), password);
            logger.info(String.format("isValid[%s:%s, dn:%s, valid:%s]", ldapUseAsLoginName, uid, fullUserDn, valid));
            return valid;
        } catch (NamingException e) {
            logger.info("isValid fail userName:" + uid, e);
            return false;
        } catch (Exception e) {
            logger.info("isValid error userName:" + uid, e);
            return false;
        }
    }

    public ErrorCode testLdapServerConnection(LdapServerVO ldap) {
        Map<String, Object> properties = new HashMap<>();
        String timeout = Integer.toString(LdapGlobalProperty.LDAP_ADD_SERVER_CONNECT_TIMEOUT);
        properties.put("com.sun.jndi.ldap.connect.timeout", timeout);
        LdapTemplateContextSource ldapTemplateContextSource = loadLdap(ldap, properties);

        try {
            AndFilter filter = new AndFilter();
            // Any search conditions
            filter.and(new EqualsFilter(LdapConstant.LDAP_UID_KEY, ""));
            ldapTemplateContextSource.getLdapTemplate().authenticate("", filter.toString(), "");
            logger.info("LDAP connection was successful");
        } catch (AuthenticationException e) {
            logger.debug("Cannot connect to LDAP/AD server, Invalid Credentials, please checkout User DN and password", e);
            return operr("Cannot connect to LDAP/AD server, Invalid Credentials, please checkout User DN and password");
        } catch (CommunicationException e) {
            logger.debug("Cannot connect to LDAP/AD server, communication false, please checkout IP, port and Base DN", e);
            return operr("Cannot connect to LDAP/AD server, communication false, please checkout IP, port and Base DN");
        } catch (Exception e) {
            logger.debug("Cannot connect to LDAP/AD server", e);
            return operr("Cannot connect to LDAP/AD server, %s", e.toString());
        }

        return null;
    }
}
