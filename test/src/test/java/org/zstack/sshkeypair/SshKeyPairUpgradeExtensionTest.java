package org.zstack.sshkeypair;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.sshkeypair.*;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.PatternedSystemTag;

import javax.persistence.NonUniqueResultException;
import javax.persistence.Tuple;
import javax.persistence.metamodel.SingularAttribute;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class SshKeyPairUpgradeExtensionTest {
    private final Map<String, String> vmKeys = new LinkedHashMap<>();
    private final Map<String, String> vmAccounts = new HashMap<>();
    private final List<SshKeyPairVO> keys = new ArrayList<>();
    private final List<SshKeyPairRefVO> refs = new ArrayList<>();
    private final Map<Field, Object> originalAttributes = new HashMap<>();
    private MockedStatic<Q> queries;
    private MockedStatic<SQL> sql;
    private PatternedSystemTag originalTag;
    private boolean originalUpgradeFlag;
    private SshKeyPairUpgradeExtension upgrade;

    @Before
    public void setUp() throws Exception {
        // Initialize global properties before overriding the migration flag.
        Platform.getUuid();
        originalTag = VmSystemTags.SSHKEY;
        originalUpgradeFlag = SshKeyPairGlobalProperty.UPGRADE_SSH_KEY_PAIR_FROM_SYSTEM_TAG;
        SshKeyPairGlobalProperty.UPGRADE_SSH_KEY_PAIR_FROM_SYSTEM_TAG = true;
        VmSystemTags.SSHKEY = mock(PatternedSystemTag.class);
        when(VmSystemTags.SSHKEY.getTokenByResourceUuid(anyString(), anyString()))
                .thenAnswer(call -> vmKeys.get(call.getArgument(0)));
        attributes(AccountResourceRefVO_.class, "accountUuid", "resourceUuid", "resourceType");
        attributes(SshKeyPairVO_.class, "uuid", "publicKey");
        attributes(SshKeyPairRefVO_.class, "sshKeyPairUuid", "resourceUuid", "resourceType");

        queries = mockStatic(Q.class);
        queries.when(() -> Q.New(AccountResourceRefVO.class)).thenAnswer(call -> accountQuery());
        queries.when(() -> Q.New(SshKeyPairVO.class)).thenAnswer(call -> keyQuery());
        queries.when(() -> Q.New(SshKeyPairRefVO.class)).thenAnswer(call -> refQuery());
        sql = mockStatic(SQL.class);
        SQL vmQuery = mock(SQL.class, RETURNS_SELF);
        sql.when(() -> SQL.New(anyString(), eq(Tuple.class))).thenReturn(vmQuery);
        doAnswer(call -> {
            Consumer<List<Tuple>> page = call.getArgument(1);
            page.accept(vmKeys.keySet().stream()
                    .map(vm -> tuple(vmAccounts.get(vm), vm)).collect(Collectors.toList()));
            return null;
        }).when(vmQuery).paginate(anyLong(), org.mockito.ArgumentMatchers.<Consumer<List<Tuple>>>any());

        upgrade = new SshKeyPairUpgradeExtension();
        upgrade.dbf = mock(DatabaseFacade.class);
        doAnswer(call -> {
            for (Object row : (Collection<?>) call.getArgument(0)) {
                if (row instanceof SshKeyPairVO) {
                    keys.add((SshKeyPairVO) row);
                } else {
                    refs.add((SshKeyPairRefVO) row);
                }
            }
            return null;
        }).when(upgrade.dbf).persistCollection(anyCollection());
    }

    @After
    public void tearDown() throws Exception {
        if (sql != null) sql.close();
        if (queries != null) queries.close();
        VmSystemTags.SSHKEY = originalTag;
        SshKeyPairGlobalProperty.UPGRADE_SSH_KEY_PAIR_FROM_SYSTEM_TAG = originalUpgradeFlag;
        for (Map.Entry<Field, Object> entry : originalAttributes.entrySet()) {
            entry.getKey().set(null, entry.getValue());
        }
    }

    @Test
    public void reusesEachCaseVariantAndRemainsIdempotent() {
        addVm("vm-upper", "account", "Abc$");
        addVm("vm-lower", "account", "abc$");
        addKey("key-lower", "account", "abc$");
        addKey("key-upper", "account", "Abc$");

        assertTrue(upgrade.start());
        assertTrue(upgrade.start());

        assertEquals(2, keys.size());
        assertEquals(2, refs.size());
        assertAssociation("vm-upper", "key-upper");
        assertAssociation("vm-lower", "key-lower");
    }

    @Test
    public void createsExactKeyInsteadOfReusingWrongCaseCandidate() {
        addVm("vm", "account", "abc$");
        addKey("wrong-case", "account", "Abc$");

        assertTrue(upgrade.start());
        assertEquals(2, keys.size());
        String exactUuid = keys.stream().filter(key -> "abc$".equals(key.getPublicKey()))
                .findFirst().get().getUuid();
        assertAssociation("vm", exactUuid);
        assertTrue(upgrade.start());
        assertEquals(2, keys.size());
        assertEquals(1, refs.size());
    }

    @Test
    public void initialMigrationPreservesCaseAndSharesIdenticalTags() {
        addVm("upper-1", "account", "Abc$");
        addVm("upper-2", "account", "Abc$");
        addVm("lower", "account", "abc$");

        assertTrue(upgrade.start());
        assertTrue(upgrade.start());

        assertEquals(2, keys.size());
        assertEquals(3, refs.size());
        for (SshKeyPairRefVO ref : refs) {
            SshKeyPairVO key = keys.stream().filter(k -> k.getUuid().equals(ref.getSshKeyPairUuid()))
                    .findFirst().get();
            assertEquals(vmKeys.get(ref.getResourceUuid()), key.getPublicKey());
        }
    }

    @Test
    public void doesNotReuseAnotherAccountsExactKey() {
        addVm("vm", "account", "Abc$");
        addKey("other-key", "other-account", "Abc$");

        assertTrue(upgrade.start());
        assertTrue(upgrade.start());

        assertEquals(2, keys.size());
        assertEquals(1, refs.size());
        assertNotEquals("other-key", refs.get(0).getSshKeyPairUuid());
        assertEquals("account", keys.get(1).getAccountUuid());
    }

    @Test(expected = NonUniqueResultException.class)
    public void rejectsMultipleExactMatches() {
        addVm("vm", "account", "Abc$");
        addKey("key-1", "account", "Abc$");
        addKey("key-2", "account", "Abc$");
        try {
            upgrade.start();
        } finally {
            assertTrue(refs.isEmpty());
            verify(upgrade.dbf, never()).persistCollection(anyCollection());
        }
    }

    @Test
    public void disabledMigrationDoesNotQueryOrPersist() {
        SshKeyPairGlobalProperty.UPGRADE_SSH_KEY_PAIR_FROM_SYSTEM_TAG = false;
        assertTrue(upgrade.start());
        queries.verifyNoInteractions();
        sql.verifyNoInteractions();
        verifyNoInteractions(upgrade.dbf);
    }

    private Q accountQuery() {
        Map<Object, Object> filters = new HashMap<>();
        Q query = query(filters);
        when(query.count()).thenAnswer(call -> (long) vmKeys.size());
        when(query.listValues()).thenAnswer(call -> keys.stream()
                .filter(key -> key.getAccountUuid().equals(filters.get(AccountResourceRefVO_.accountUuid)))
                .map(SshKeyPairVO::getUuid).collect(Collectors.toList()));
        return query;
    }

    private Q keyQuery() {
        Map<Object, Object> filters = new HashMap<>();
        Q query = query(filters);
        // Model the case-insensitive SQL predicate while preserving stored strings.
        java.util.function.Supplier<List<SshKeyPairVO>> candidates = () -> keys.stream()
                .filter(key -> ((Collection<?>) filters.get(SshKeyPairVO_.uuid)).contains(key.getUuid()))
                .filter(key -> key.getPublicKey().equalsIgnoreCase((String) filters.get(SshKeyPairVO_.publicKey)))
                .collect(Collectors.toList());
        when(query.listTuple()).thenAnswer(call -> candidates.get().stream()
                .map(key -> tuple(key.getUuid(), key.getPublicKey())).collect(Collectors.toList()));
        when(query.findValue()).thenAnswer(call -> {
            List<SshKeyPairVO> matches = candidates.get();
            if (matches.size() > 1) throw new NonUniqueResultException();
            return matches.isEmpty() ? null : matches.get(0).getUuid();
        });
        return query;
    }

    private Q refQuery() {
        Map<Object, Object> filters = new HashMap<>();
        Q query = query(filters);
        when(query.isExists()).thenAnswer(call -> refs.stream().anyMatch(ref ->
                ref.getSshKeyPairUuid().equals(filters.get(SshKeyPairRefVO_.sshKeyPairUuid))
                && ref.getResourceUuid().equals(filters.get(SshKeyPairRefVO_.resourceUuid))
                && ref.getResourceType().equals(filters.get(SshKeyPairRefVO_.resourceType))));
        return query;
    }

    private Q query(Map<Object, Object> filters) {
        Q query = mock(Q.class, RETURNS_SELF);
        when(query.eq(any(), any())).thenAnswer(call -> {
            filters.put(call.getArgument(0), call.getArgument(1));
            return query;
        });
        when(query.in(any(), anyCollection())).thenAnswer(call -> {
            filters.put(call.getArgument(0), call.getArgument(1));
            return query;
        });
        return query;
    }

    private void attributes(Class<?> metamodel, String... names) throws Exception {
        for (String name : names) {
            Field field = metamodel.getField(name);
            originalAttributes.put(field, field.get(null));
            field.set(null, mock(SingularAttribute.class));
        }
    }

    private Tuple tuple(String first, String second) {
        Tuple tuple = mock(Tuple.class);
        when(tuple.get(0)).thenReturn(first);
        when(tuple.get(1)).thenReturn(second);
        when(tuple.get(0, String.class)).thenReturn(first);
        when(tuple.get(1, String.class)).thenReturn(second);
        return tuple;
    }

    private void addVm(String uuid, String accountUuid, String key) {
        vmKeys.put(uuid, key);
        vmAccounts.put(uuid, accountUuid);
    }

    private void addKey(String uuid, String accountUuid, String publicKey) {
        SshKeyPairVO key = new SshKeyPairVO();
        key.setUuid(uuid);
        key.setAccountUuid(accountUuid);
        key.setPublicKey(publicKey);
        keys.add(key);
    }

    private void assertAssociation(String vmUuid, String keyUuid) {
        assertTrue(refs.stream().anyMatch(ref -> vmUuid.equals(ref.getResourceUuid())
                && keyUuid.equals(ref.getSshKeyPairUuid())
                && VmInstanceVO.class.getSimpleName().equals(ref.getResourceType())));
    }
}
