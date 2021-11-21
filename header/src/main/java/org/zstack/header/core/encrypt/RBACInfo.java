package org.zstack.header.core.encrypt;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name(EncryptConstant.CERTIFICATE_ACTION_CATEGORY)
                .adminOnlyAPIs(APIStartDataProtectionMsg.class,
                        APICheckBatchDataIntegrityMsg.class)
                .build();
    }

    @Override
    public void contributeToRoles() {

    }

    @Override
    public void roles() {

    }

    @Override
    public void globalReadableResources() {

    }
}
