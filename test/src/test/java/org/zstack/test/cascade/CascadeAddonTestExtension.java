package org.zstack.test.cascade;

import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeAddOnExtensionPoint;
import org.zstack.core.cascade.CascadeException;
import org.zstack.core.cascade.CascadeExtensionPoint;
import org.zstack.header.core.Completion;
import org.zstack.header.host.HostVO;
import org.zstack.header.zone.ZoneVO;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2016/11/7.
 */
public class CascadeAddonTestExtension implements CascadeAddOnExtensionPoint {
    boolean success;

    @Override
    public CascadeExtensionPoint cascadeAddOn(String resourceName) {
        if (!HostVO.class.getSimpleName().equals(resourceName)) {
            return null;
        }

        return new CascadeExtensionPoint() {
            @Override
            public void syncCascade(CascadeAction action) throws CascadeException {

            }

            @Override
            public void asyncCascade(CascadeAction action, Completion completion) {
                success = true;
                completion.success();
            }

            @Override
            public List<String> getEdgeNames() {
                return asList(ZoneVO.class.getSimpleName());
            }

            @Override
            public String getCascadeResourceName() {
                return HostVO.class.getSimpleName();
            }

            @Override
            public CascadeAction createActionForChildResource(CascadeAction action) {
                return null;
            }
        };
    }
}
