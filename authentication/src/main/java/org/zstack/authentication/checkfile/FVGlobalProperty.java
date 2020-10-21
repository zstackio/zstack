package org.zstack.authentication.checkfile;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by Wenhao.Zhang on 20/10/21
 */
@GlobalPropertyDefinition
public class FVGlobalProperty {
    
    @GlobalProperty(name="initMNFileVerification", defaultValue = "true")
    public static boolean initMNFileVerification;
    
}
