package org.zstack.test.unittest.ldap;

import com.unboundid.ldap.sdk.*;
import org.junit.ClassRule;
import org.junit.Test;
import org.zapodot.junit.ldap.EmbeddedLdapRule;
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder;

import java.util.Arrays;

public class TestLdapSearchCase {
    public static String DOMAIN_DSN = "dc=example,dc=com";

    static {
        System.setProperty("com.unboundid.ldap.sdk.debug.enabled", "true");
        System.setProperty("com.unboundid.ldap.sdk.debug.level", "FINEST");
        System.setProperty("com.unboundid.ldap.sdk.LDAPConnectionOptions.followReferrals", "true");
    }

    @ClassRule
    public static EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder
            .newInstance()
            .usingDomainDsn("dc=example,dc=com")
            .importingLdifs("users-import.ldif")
            .bindingToPort(10389)
            .build();

    @Test
    public void testLdapWithIgnore() {
        try (LDAPConnection ldapConnection = embeddedLdapRule.unsharedLdapConnection()) {
            ldapConnection.getConnectionOptions().setFollowReferrals(false);
            SearchResult searchResult = ldapConnection.search(
                    DOMAIN_DSN,
                    SearchScope.SUB,
                    "(objectclass=*)");

            System.out.printf("Found %d entries%n", searchResult.getEntryCount());
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                System.out.println(entry.getDN());
                for (Attribute attribute : entry.getAttributes()) {
                    System.out.printf("  %s: %s%n", attribute.getName(), attribute.getValue());
                }
            }

            System.out.println("Found referral URLs:" + Arrays.toString(searchResult.getReferralURLs()));
            assert searchResult.getEntryCount() == 6;
        } catch (Exception e) {
            assert false : "Unexpected error during testLdapWithIgnore";
        }
    }

    @Test
    public void testLdapWithReferral() throws LDAPException {
        try (LDAPConnection ldapConnection = embeddedLdapRule.unsharedLdapConnection()) {
            ldapConnection.getConnectionOptions().setFollowReferrals(true);
            SearchResult searchResult = ldapConnection.search(
                    DOMAIN_DSN,
                    SearchScope.SUB,
                    "(objectclass=*)");

            System.out.printf("Found %d entries%n", searchResult.getEntryCount());
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                System.out.println(entry.getDN());
                for (Attribute attribute : entry.getAttributes()) {
                    System.out.printf("  %s: %s%n", attribute.getName(), attribute.getValue());
                }
            }

            System.out.println("Found referral URLs:" + Arrays.toString(searchResult.getReferralURLs()));
            assert searchResult.getReferenceCount() == 1024;
        } catch (StackOverflowError e) {
            System.out.print("stack over flow expected");
            return;
        }

        assert false : "Unexpected here";
    }
}
