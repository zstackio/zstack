package org.zstack.appliancevm;

import java.util.List;

/**
 * Created by weiwang on 22/10/2017
 */
public interface ApvmCascadeFilterExtensionPoint {
    List<ApplianceVmVO> filterApplianceVmCascade(List<ApplianceVmVO> applianceVmVOS);
}
