package org.zstack.identity;

import org.zstack.core.db.SQLBatch;
import org.zstack.header.identity.*;
import org.zstack.header.identity.rbac.RBAC;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

public class IAMIdentityResourceGenerator implements IdentityResourceGenerateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(IAMIdentityResourceGenerator.class);

    public static String readAPIsForNormalAccountJSONStatement;

    @Override
    public String getIdentityType() {
        return AccountConstant.identityType.toString();
    }

    @Override
    public void prepareResources() {
        makeReadAPIsForNormalAccountJSONStatement();

        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(PolicyVO.class).eq(PolicyVO_.name, "DEFAULT-READ").set(PolicyVO_.data, readAPIsForNormalAccountJSONStatement).update();
            }
        }.execute();
    }

    private void makeReadAPIsForNormalAccountJSONStatement() {
        List<String> readAPIs = new ArrayList<>();
        APIMessage.apiMessageClasses.forEach(clz -> {
            if (APISyncCallMessage.class.isAssignableFrom(clz) && !RBAC.isAdminOnlyAPI(clz.getName())) {
                readAPIs.add(clz.getName());
            }
        });


        readAPIs.add(APIUpdateUserMsg.class.getName());

        PolicyStatement s = PolicyStatement.builder().name("read-apis-for-normal-account")
                .effect(StatementEffect.Allow)
                .actions(readAPIs)
                .build();

        readAPIsForNormalAccountJSONStatement = JSONObjectUtil.toJsonString(list(s)).replace("\\", "");
    }
}
