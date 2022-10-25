package org.zstack.sugonSdnController.controller;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class SugonSdnControllerGlobalProperty {
    @GlobalProperty(name="Tf.Scheme", defaultValue = "http")
    public static String TF_CONTROLLER_SCHEME;

    @GlobalProperty(name="Tf.Port", defaultValue = "8082")
    public static int TF_CONTROLLER_PORT;
}
