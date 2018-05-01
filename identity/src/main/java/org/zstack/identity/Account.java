package org.zstack.identity;

import org.zstack.core.Platform;
import org.zstack.core.config.GlobalConfigVO;
import org.zstack.core.config.GlobalConfigVO_;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.identity.*;
import org.zstack.header.message.Message;
import org.zstack.utils.DebugUtils;

import javax.persistence.Tuple;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface Account {
    void handleMessage(Message msg);

    class AccountBuilder {
        String uuid;
        String name;
        String description;
        String password;
        AccountType type = AccountType.Normal;
        Map<String, Long> quota = new HashMap<>();

        public static AccountBuilder New() {
            return new AccountBuilder();
        }

        public AccountBuilder uuid(String v) {
            uuid = v;
            return this;
        }

        public AccountBuilder quota(Map<String, Long> v) {
            quota = v;
            return this;
        }

        public AccountBuilder name(String v) {
            name = v;
            return this;
        }

        public AccountBuilder description(String v) {
            description = v;
            return this;
        }

        public AccountBuilder password(String v) {
            password = v;
            return this;
        }

        public AccountBuilder type(AccountType v) {
            type = v;
            return this;
        }

        public AccountBuilder build() {
            // nothing, just for builder style
            return this;
        }
    }

    static String getAccountUuidOfResource(String resUuid) {
        return Q.New(AccountResourceRefVO.class).select(AccountResourceRefVO_.ownerAccountUuid)
                .eq(AccountResourceRefVO_.resourceUuid, resUuid).findValue();
    }

    static AccountInventory create(AccountBuilder builder) {
        DebugUtils.Assert(builder.name != null, "name cannot be null");
        DebugUtils.Assert(builder.type != null, "type cannot be null");

        return new SQLBatchWithReturn<AccountInventory>() {
            @Override
            protected AccountInventory scripts() {
                AccountVO avo = new AccountVO();
                avo.setUuid(builder.uuid == null ? Platform.getUuid() : builder.uuid);
                avo.setName(builder.name);
                avo.setDescription(builder.description);
                avo.setPassword(builder.password);
                avo.setType(builder.type);
                persist(avo);

                List<Tuple> ts = q(GlobalConfigVO.class).select(GlobalConfigVO_.name, GlobalConfigVO_.value)
                        .eq(GlobalConfigVO_.category, AccountConstant.QUOTA_GLOBAL_CONFIG_CATETORY).listTuple();

                for (Tuple t : ts) {
                    String rtype = t.get(0, String.class);
                    long quota = Long.valueOf(t.get(1, String.class));

                    if (builder.quota != null && builder.quota.containsKey(rtype)) {
                        quota = builder.quota.get(rtype);
                    }

                    QuotaVO qvo = new QuotaVO();
                    qvo.setUuid(Platform.getUuid());
                    qvo.setIdentityType(AccountVO.class.getSimpleName());
                    qvo.setIdentityUuid(avo.getUuid());
                    qvo.setName(rtype);
                    qvo.setValue(quota);
                    qvo.setAccountUuid(avo.getUuid());
                    persist(qvo);
                    reload(qvo);
                }

                reload(avo);
                return AccountInventory.valueOf(avo);
            }
        }.execute();
    }
}
