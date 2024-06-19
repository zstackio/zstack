package org.zstack.ldap.externalSearch;

import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.core.support.AggregateDirContextProcessor;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.ldap.LdapConstant;
import org.zstack.ldap.LdapExternalSearchExtensionPoint;
import org.zstack.ldap.LdapSearchedResult;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import java.util.*;
import java.util.function.Predicate;

import static org.zstack.core.Platform.operr;

public class AggregateSearch implements LdapExternalSearchExtensionPoint {
    private static final CLogger logger = Utils.getLogger(AggregateSearch.class);

    @Override
    public LdapSearchedResult trySearch(LdapTemplate ldapTemplate, String filter, SearchControls searchCtls, Predicate<String> resultFilter, Integer count) {
        LdapSearchedResult ldapSearchedResult = new LdapSearchedResult();

        AggregateDirContextProcessor aggregateDirContextProcessor = new AggregateDirContextProcessor();
        try {
            List<Object> aggregateResult = ldapTemplate.search("", filter, searchCtls, new AbstractContextMapper<Object>() {
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
            }, aggregateDirContextProcessor);

            aggregateResult.removeIf(Objects::isNull);

            if (count != null && aggregateResult.size() > count){
                ldapSearchedResult.setResult(aggregateResult.subList(0, count));
            } else {
                ldapSearchedResult.setResult(aggregateResult);
            }
        } catch (Exception e){
            logger.error("query ldap entry fail", e);
            ldapSearchedResult.setSuccess(false);
            ldapSearchedResult.setResult(null);
            ldapSearchedResult.setError(e.getMessage());
        }

        return ldapSearchedResult;
    }
}
