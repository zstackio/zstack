package org.zstack.header.identity;

import org.zstack.header.rest.SDK;

@SDK(sdkClassName = "PolicyStatementEffect")
public enum StatementEffect {
    Allow,
    Deny,
}
