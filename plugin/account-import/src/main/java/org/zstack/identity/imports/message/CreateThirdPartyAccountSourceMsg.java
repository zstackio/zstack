package org.zstack.identity.imports.message;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.identity.imports.header.AbstractAccountSourceSpec;

public class CreateThirdPartyAccountSourceMsg extends NeedReplyMessage {
    private AbstractAccountSourceSpec spec;

    public String getType() {
        return spec == null ? null : spec.getType();
    }

    public AbstractAccountSourceSpec getSpec() {
        return spec;
    }

    public void setSpec(AbstractAccountSourceSpec spec) {
        this.spec = spec;
    }
}
