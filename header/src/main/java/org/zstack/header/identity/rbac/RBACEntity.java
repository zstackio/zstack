package org.zstack.header.identity.rbac;

import org.zstack.header.message.APIMessage;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RBACEntity {
    private static final CLogger logger = Utils.getLogger(RBACEntity.class);

    private String apiName;
    private APIMessage apiMessage;

    private final List<APIMessage> additionalApisToCheck = new ArrayList<>();

    public RBACEntity(APIMessage apiMessage) {
        this.apiMessage = apiMessage;
        apiName = apiMessage.getClass().getName();

        final List<Function<APIMessage, List<APIMessage>>> functions = RBAC.expendPermissionCheckList(apiMessage.getClass());

        if (functions == null) {
            return;
        }

        additionalApisToCheck.addAll(functions.stream()
                .map(function -> function.apply(apiMessage))
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toList()));
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

    public List<APIMessage> getAdditionalApisToCheck() {
        return additionalApisToCheck;
    }
}
