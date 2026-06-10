package org.zstack.core.scim;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class ScimGlobalProperty {
    @GlobalProperty(name = "scim.receiver.token", defaultValue = "")
    public static String SCIM_RECEIVER_TOKEN;

    @GlobalProperty(name = "scim.receiver.signatureSecret", defaultValue = "")
    public static String SCIM_RECEIVER_SIGNATURE_SECRET;
}
