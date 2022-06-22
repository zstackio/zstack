package org.zstack.ldap;

import org.apache.commons.lang.StringUtils;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.control.PagedResultsDirContextProcessor;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.NameAwareAttribute;
import org.springframework.ldap.core.support.*;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.Filter;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.tag.PatternedSystemTag;
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
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by miao on 12/19/16.
 */
public class LdapUtil {
    private static final CLogger logger = Utils.getLogger(LdapUtil.class);

    static final String PAGED_RESULTS_CONTROL_OID = "1.2.840.113556.1.4.319";

    public LdapUtil() {

    }

    public LdapUtil(String scope) {
        this.scope = scope;
    }

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

    private String scope;

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

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

    LdapContextSource buildLdapContextSource(LdapServerInventory inv, Map<String, Object> baseEnvironmentProperties) {
        LdapContextSource ldapContextSource;
        ldapContextSource = new LdapContextSource();

        List<String> urls = new ArrayList<>();
        urls.add(inv.getUrl());

        if (LdapSystemTags.LDAP_URLS.hasTag(inv.getUuid())) {
            String standbyUrls = LdapSystemTags.LDAP_URLS.getTokenByResourceUuid(inv.getUuid(), LdapSystemTags.LDAP_URLS_TOKEN);

            urls.addAll(list(standbyUrls.split(",")));
        }

        ldapContextSource.setUrls(urls.toArray(new String[]{}));
        ldapContextSource.setBase(inv.getBase());
        ldapContextSource.setUserDn(inv.getUsername());
        ldapContextSource.setPassword(inv.getPassword());
        ldapContextSource.setDirObjectFactory(DefaultDirObjectFactory.class);
        if (inv.getEncryption().equals(LdapEncryptionType.TLS.toString())) {
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

    LdapTemplateContextSource doLoadLdap(LdapServerInventory inv, boolean emptyBase) {
        LdapContextSource ldapContextSource = buildLdapContextSource(inv, getBaseEnvProperties());

        if (emptyBase) {
            ldapContextSource.setBase("");
        }

        LdapTemplate ldapTemplate;
        ldapTemplate = new LdapTemplate();
        ldapTemplate.setIgnorePartialResultException(true);
        ldapTemplate.setContextSource(ldapContextSource);

        return new LdapTemplateContextSource(ldapTemplate, ldapContextSource);
    }

    LdapTemplateContextSource loadRootLdap(LdapServerInventory inv) {
        return doLoadLdap(inv, true);
    }

    LdapTemplateContextSource loadLdap(LdapServerInventory inv) {
        return doLoadLdap(inv, false);
    }

    LdapTemplateContextSource loadLdap(LdapServerInventory inv, Map<String, Object> baseEnvironmentProperties) {
        LdapContextSource ldapContextSource = buildLdapContextSource(inv, baseEnvironmentProperties);

        LdapTemplate ldapTemplate;
        ldapTemplate = new LdapTemplate();
        ldapTemplate.setIgnorePartialResultException(true);
        ldapTemplate.setContextSource(ldapContextSource);

        return new LdapTemplateContextSource(ldapTemplate, ldapContextSource);
    }

    public String getMemberKey(){
        String ldapServerUuid = Q.New(LdapServerVO.class)
                .select(LdapServerVO_.uuid)
                .eq(LdapServerVO_.scope, scope)
                .findValue();
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

    public String getLdapUseAsLoginName(){
        String ldapServerUuid = Q.New(LdapServerVO.class).select(LdapServerVO_.uuid)
                .eq(LdapServerVO_.scope, scope)
                .findValue();

        PatternedSystemTag tag = LdapSystemTags.LDAP_USE_AS_LOGIN_NAME;
        if(!tag.hasTag(ldapServerUuid)){
            return LdapConstant.LDAP_UID_KEY;
        }

        return tag.getTokenByResourceUuid(ldapServerUuid, LdapSystemTags.LDAP_USE_AS_LOGIN_NAME_TOKEN);
    }

    public String getDnKey(){
        String ldapServerUuid = Q.New(LdapServerVO.class)
                .select(LdapServerVO_.uuid)
                .eq(LdapServerVO_.scope, scope)
                .findValue();
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

    public Map<String, Object> getBaseEnvProperties() {
        String ldapServerUuid = Q.New(LdapServerVO.class)
                .select(LdapServerVO_.uuid)
                .eq(LdapServerVO_.scope, scope)
                .findValue();
        String type = LdapSystemTags.LDAP_SERVER_TYPE.getTokenByResourceUuid(ldapServerUuid, LdapSystemTags.LDAP_SERVER_TYPE_TOKEN);

        Map<String, Object> properties = new HashMap<>();
        if(LdapConstant.WindowsAD.TYPE.equals(type)){
            properties.put("java.naming.ldap.attributes.binary","objectGUID");
        }

        return properties;
    }

    public String decodeGlobalUuid(NameAwareAttribute attribute) throws javax.naming.NamingException {
        String ldapServerUuid = Q.New(LdapServerVO.class)
                .select(LdapServerVO_.uuid)
                .eq(LdapServerVO_.scope, scope)
                .findValue();
        String type = LdapSystemTags.LDAP_SERVER_TYPE.getTokenByResourceUuid(ldapServerUuid, LdapSystemTags.LDAP_SERVER_TYPE_TOKEN);

        if(LdapConstant.WindowsAD.TYPE.equals(type)){
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

    public String getMemberOfKey() {
        String ldapServerUuid = Q.New(LdapServerVO.class)
                .select(LdapServerVO_.uuid)
                .eq(LdapServerVO_.scope, scope)
                .findValue();
        String type = LdapSystemTags.LDAP_SERVER_TYPE.getTokenByResourceUuid(ldapServerUuid, LdapSystemTags.LDAP_SERVER_TYPE_TOKEN);

        if(LdapConstant.WindowsAD.TYPE.equals(type)){
            return LdapConstant.WindowsAD.MEMBER_OF_KEY;
        }

        if(LdapConstant.OpenLdap.TYPE.equals(type)){
            return LdapConstant.OpenLdap.MEMBER_OF_KEY;
        }

        // default WindowsAD
        return LdapConstant.WindowsAD.MEMBER_OF_KEY;
    }

    public String getGlobalUuidKey() {
        String ldapServerUuid = Q.New(LdapServerVO.class)
                .select(LdapServerVO_.uuid)
                .eq(LdapServerVO_.scope, scope)
                .findValue();
        String type = LdapSystemTags.LDAP_SERVER_TYPE.getTokenByResourceUuid(ldapServerUuid, LdapSystemTags.LDAP_SERVER_TYPE_TOKEN);

        if(LdapConstant.WindowsAD.TYPE.equals(type)){
            return LdapConstant.WindowsAD.GLOBAL_UUID_KEY;
        }

        if(LdapConstant.OpenLdap.TYPE.equals(type)){
            return LdapConstant.OpenLdap.GLOBAL_UUID_KEY;
        }

        // default WindowsAD
        return LdapConstant.WindowsAD.GLOBAL_UUID_KEY;
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

    private LdapSearchedResult searchWithoutProcessor(LdapTemplate ldapTemplate, String filter, SearchControls searchCtls, ResultFilter resultFilter, Integer count) {
        LdapSearchedResult ldapSearchedResult = new LdapSearchedResult();
        ldapSearchedResult.setResult(new ArrayList<>());

        List<Object> searchResult = new ArrayList<>();
        try {
            searchResult = ldapTemplate.search("", filter, searchCtls, new AbstractContextMapper<Object>() {
                @Override
                protected Object doMapFromContext(DirContextOperations ctx) {
                    if (resultFilter != null && !resultFilter.needSelect(ctx.getNameInNamespace())){
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

    private LdapSearchedResult pagedSearch(LdapTemplate ldapTemplate, String filter, SearchControls searchCtls, ResultFilter resultFilter, Integer count) {
        LdapSearchedResult ldapSearchedResult = new LdapSearchedResult();
        ldapSearchedResult.setResult(new ArrayList<>());

        PagedResultsDirContextProcessor processor = new PagedResultsDirContextProcessor(1000);
        try {
            do {
                List<Object> subResult = ldapTemplate.search("", filter, searchCtls, new AbstractContextMapper<Object>() {
                    @Override
                    protected Object doMapFromContext(DirContextOperations ctx) {
                        if (resultFilter != null && !resultFilter.needSelect(ctx.getNameInNamespace())){
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

    public List<Object> searchLdapEntry(String filter, Integer count, String[] returningAttributes, ResultFilter resultFilter, boolean searchAllAttributes) {
        LdapServerVO ldapServerVO = getLdapServer();
        return searchLdapEntry(ldapServerVO.getUuid(), filter, count, returningAttributes, resultFilter, searchAllAttributes);
    }

    public List<Object> searchLdapEntry(String ldapServerUuid, String filter, Integer count, String[] returningAttributes, ResultFilter resultFilter, boolean searchAllAttributes) {
        LdapTemplateContextSource ldapTemplateContextSource = readLdapServerConfiguration(ldapServerUuid);
        LdapTemplate ldapTemplate = ldapTemplateContextSource.getLdapTemplate();
        ldapTemplate.setContextSource(new SingleContextSource(ldapTemplateContextSource.getLdapContextSource().getReadOnlyContext()));

        SearchControls searchCtls = new SearchControls();
        searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        if (searchAllAttributes) {
            searchCtls.setReturningAttributes(null);
        } else {
            searchCtls.setReturningAttributes(returningAttributes == null ? getReturningAttributes() : returningAttributes);
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

    /**
     * @param filter
     * @param count
     *           count is null, return all
     * @param resultFilter
     *           resultFilter is null, do not check result
     *
     * @return
     */
    public List<Object> searchLdapEntry(String filter, Integer count, ResultFilter resultFilter){
        return searchLdapEntry(filter, count, null,resultFilter, false);
    }

    public List<Object> searchLdapEntry(String ldapServerUuid, String filter, Integer count, ResultFilter resultFilter){
        return searchLdapEntry(ldapServerUuid, filter, count, null,resultFilter, false);
    }

    private String[] getReturningAttributes() {
        Set<String> returnedAttSet = new HashSet<>();

        String queryLdapEntryReturnAttributes = LdapGlobalConfig.QUERY_LDAP_ENTRY_RETURN_ATTRIBUTES.value();
        if(StringUtils.isNotEmpty(queryLdapEntryReturnAttributes)){
            String separator = LdapGlobalConfig.QUERY_LDAP_ENTRY_RETURN_ATTRIBUTE_SEPARATOR.value();
            separator = separator != null ? separator : LdapConstant.QUERY_LDAP_ENTRY_RETURN_ATTRIBUTE_SEPARATOR;
            String[] attributes = queryLdapEntryReturnAttributes.split(separator);

            returnedAttSet.addAll(Arrays.asList(attributes));
        }

        returnedAttSet.addAll(Arrays.asList(LdapConstant.QUERY_LDAP_ENTRY_MUST_RETURN_ATTRIBUTES));

        returnedAttSet.add(getLdapUseAsLoginName());

        return returnedAttSet.toArray(new String[]{});
    }

    public LdapTemplateContextSource readLdapServerConfiguration() {
        LdapServerVO ldapServerVO = getLdapServer();
        LdapServerInventory ldapServerInventory = LdapServerInventory.valueOf(ldapServerVO);
        return loadLdap(ldapServerInventory);
    }

    public LdapTemplateContextSource readLdapServerConfiguration(String ldapServerUuid) {
        LdapServerVO ldapServerVO = Q.New(LdapServerVO.class)
                .eq(LdapServerVO_.uuid, ldapServerUuid)
                .find();
        LdapServerInventory ldapServerInventory = LdapServerInventory.valueOf(ldapServerVO);
        return loadLdap(ldapServerInventory);
    }

    // set LdapContextSource base as empty
    public LdapTemplateContextSource readRootLdapServerConfiguration(String ldapServerUuid) {
        LdapServerVO ldapServerVO = Q.New(LdapServerVO.class)
                .eq(LdapServerVO_.uuid, ldapServerUuid)
                .find();
        LdapServerInventory ldapServerInventory = LdapServerInventory.valueOf(ldapServerVO);
        return loadRootLdap(ldapServerInventory);
    }

    @Transactional(readOnly = true)
    public LdapServerVO getLdapServer() {
        final DatabaseFacade dbf = Platform.getComponentLoader().getComponent(DatabaseFacade.class);

        SimpleQuery<LdapServerVO> sq = dbf.createQuery(LdapServerVO.class);
        sq.add(LdapServerVO_.scope, SimpleQuery.Op.EQ, scope);
        List<LdapServerVO> ldapServers = sq.list();
        if (ldapServers.isEmpty()) {
            throw new CloudRuntimeException("No LDAP/AD server record in database.");
        }
        if (ldapServers.size() > 1) {
            throw new CloudRuntimeException("More than one LDAP/AD server record in database.");
        }
        return ldapServers.get(0);
    }

    public List<String> getUserDnGroups(List<String> ldapDnList, String ldapDn) {
        LdapTemplateContextSource ldapTemplateContextSource = readLdapServerConfiguration();
        LdapTemplate ldapTemplate = ldapTemplateContextSource.getLdapTemplate();

        List<String> groupDnList = new ArrayList();
        findLdapDnMemberOfList(ldapTemplate, ldapDn, groupDnList, new ArrayList<>());

        if(groupDnList.isEmpty()){
            return null;
        }

        ldapDnList.retainAll(groupDnList);

        if(ldapDnList.isEmpty()){
            return null;
        }

        return ldapDnList;
    }

    void findLdapDnMemberOfList(LdapTemplate ldapTemplate, String ldapDn, List<String> resultDnList, List<String> dnIgnoreList){
        if(dnIgnoreList.contains(ldapDn)){
            return;
        }

        AndFilter filter = new AndFilter();
        filter.and(new EqualsFilter(getMemberKey(), ldapDn));

        List<Object> groupList = ldapTemplate.search("", filter.toString(), new AbstractContextMapper<Object>() {
            @Override
            protected Object doMapFromContext(DirContextOperations ctx) {
                return ctx.getNameInNamespace();
            }
        });

        if(groupList.isEmpty()){
            dnIgnoreList.add(ldapDn);
            return;
        }

        for(Object groupObj : groupList){
            if(groupObj == null || !(groupObj instanceof String)){
                continue;
            }

            String groupDn = (String)groupObj;

            if(resultDnList.contains(groupDn)){
                continue;
            }

            resultDnList.add(groupDn);
            findLdapDnMemberOfList(ldapTemplate, groupDn, resultDnList, dnIgnoreList);
        }
    }

    public boolean validateDnExist(LdapTemplateContextSource ldapTemplateContextSource, String fullDn){
        try {
            String dn = fullDn.replace("," + ldapTemplateContextSource.getLdapContextSource().getBaseLdapPathAsString(), "");
            Object result = ldapTemplateContextSource.getLdapTemplate().lookup(dn, new AbstractContextMapper<Object>() {
                @Override
                protected Object doMapFromContext(DirContextOperations ctx) {
                    Attributes group = ctx.getAttributes();
                    return group;
                }
            });
            return result != null;
        }catch (Exception e){
            logger.warn(String.format("validateDnExist[%s] fail", fullDn), e);
            return false;
        }
    }

    public boolean validateDnExist(LdapTemplateContextSource ldapTemplateContextSource, String fullDn, Filter filter){
        try {
            String dn = fullDn.replace("," + ldapTemplateContextSource.getLdapContextSource().getBaseLdapPathAsString(), "");
            List<Object> result = ldapTemplateContextSource.getLdapTemplate().search(dn, filter.toString(), new AbstractContextMapper<Object>() {
                @Override
                protected Object doMapFromContext(DirContextOperations ctx) {
                    return ctx.getNameInNamespace();
                }
            });
            return result.contains(fullDn);
        }catch (Exception e){
            logger.warn(String.format("validateDnExist[dn=%s, filter=%s] fail", fullDn, filter.toString()), e);
            return false;
        }
    }

    String getPartialUserDn(LdapTemplateContextSource ldapTemplateContextSource,String key, String value) {
        return getFullUserDn(ldapTemplateContextSource.getLdapTemplate(), key, value).
                replace("," + ldapTemplateContextSource.getLdapContextSource().getBaseLdapPathAsString(), "");
    }

    public String getFullUserDn(String uid) {
        LdapTemplateContextSource ldapTemplateContextSource = readLdapServerConfiguration();
        String ldapUseAsLoginName = getLdapUseAsLoginName();
        String fullUserDn = null;
        try {
            fullUserDn = getFullUserDn(ldapTemplateContextSource.getLdapTemplate(), ldapUseAsLoginName, uid);

            if (fullUserDn.equals("")) {
                return null;
            }
        } catch (Exception e) {
            logger.debug(String.format("Get user dn of uid[%s] failed ", uid));
        }

        return fullUserDn;
    }

    public String getFullUserDn(LdapTemplate ldapTemplate, String key, String val) {
        EqualsFilter f = new EqualsFilter(key, val);
        return getFullUserDn(ldapTemplate, f.toString());
    }

    private String getFullUserDn(LdapTemplate ldapTemplate, String filter) {
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

    public boolean isValid(String uid, String password) {
        LdapTemplateContextSource ldapTemplateContextSource = readLdapServerConfiguration();
        String ldapUseAsLoginName = getLdapUseAsLoginName();
        try {
            boolean valid;
            String fullUserDn = getFullUserDn(ldapTemplateContextSource.getLdapTemplate(), ldapUseAsLoginName, uid);
            if (fullUserDn.equals("") || password.equals("")) {
                return false;
            }

            LdapServerVO ldapServerVO = getLdapServer();
            LdapServerInventory ldapServerInventory = LdapServerInventory.valueOf(ldapServerVO);
            LdapTemplateContextSource ldapTemplateContextSource2 = loadLdap(ldapServerInventory);

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

    public LdapAccountRefVO findLdapAccountRefVO(String ldapDn){
        LdapAccountRefVO ldapAccountRefVO = Q.New(LdapAccountRefVO.class)
                .eq(LdapAccountRefVO_.ldapUid, ldapDn).find();

        if(ldapAccountRefVO != null){
            return ldapAccountRefVO;
        }

        ldapAccountRefVO = findLdapAccountRefByAffiliatedGroup(ldapDn);
        return ldapAccountRefVO;
    }

    /**
     * step 1: Query the ldap group where the ldap user is located（all group）
     * step 2: Check if there is a ldap group bound to the ZStack account
     *              No ZStackAccount-LdapGroup binding，return null
     *              Only one ZStackAccount-LdapGroup binding，return it
     *              Multiple ZStackAccount-LdapGroup bindings，throw exception
     */
    private LdapAccountRefVO findLdapAccountRefByAffiliatedGroup(String ldapDn){

        List<String> ldapDnList = Q.New(LdapAccountRefVO.class)
                .select(LdapAccountRefVO_.ldapUid)
                .listValues();

        if(ldapDnList.isEmpty()){
            return null;
        }

        List<String> dnGroups = getUserDnGroups(ldapDnList, ldapDn);

        if(dnGroups == null){
            return null;
        }

        List<LdapAccountRefVO> vos = Q.New(LdapAccountRefVO.class)
                .in(LdapAccountRefVO_.ldapUid, dnGroups)
                .list();

        if(vos.size() == 1){
            return vos.get(0);
        }

        List<String> accountList = vos.stream().map(LdapAccountRefVO::getAccountUuid).distinct().collect(Collectors.toList());
        throw new CloudRuntimeException(String.format("The ldapUid[%s] is bound to multiple accounts: %s", ldapDn, accountList.toString()));

    }
}
