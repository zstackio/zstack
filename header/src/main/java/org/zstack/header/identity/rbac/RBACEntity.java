package org.zstack.header.identity.rbac;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RBACEntity {
    private static final CLogger logger = Utils.getLogger(RBACEntity.class);

    private String apiName;
    private APIMessage apiMessage;

    private List<String> additionalApisToCheck = new ArrayList<>();

    public RBACEntity(APIMessage apiMessage) {
        this.apiMessage = apiMessage;
        apiName = apiMessage.getClass().getName();

        List<RBAC.ExpendedFieldPermission> structs = RBAC.expendApiClassForPermissionCheck.get(apiMessage.getClass());

        if (structs == null) {
            return;
        }

        for (RBAC.ExpendedFieldPermission s : structs) {
            Field field = FieldUtils.getField(s.fieldName, apiMessage.getClass());

            if (field == null) {
                throw new CloudRuntimeException(String.format("Unknown field %s of class %s", s.fieldName, apiMessage.getClass()));
            }

            try {
                field.setAccessible(true);
                Object obj = field.get(apiMessage);

                if (obj == null) {
                    continue;
                }

                if (obj instanceof Collection && ((Collection) obj).isEmpty()) {
                    continue;
                }

                additionalApisToCheck.add(s.apiClass.getName());
            } catch (IllegalAccessException e) {
                throw new CloudRuntimeException(e);
            }
        }
    }

    public RBACEntity() {
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public APIMessage getApiMessage() {
        return apiMessage;
    }

    public void setApiMessage(APIMessage apiMessage) {
        this.apiMessage = apiMessage;
    }

    public List<String> getAdditionalApisToCheck() {
        return additionalApisToCheck;
    }

    public void setAdditionalApisToCheck(List<String> additionalApisToCheck) {
        this.additionalApisToCheck = additionalApisToCheck;
    }
}
