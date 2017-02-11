package org.zstack.test.core.cascade;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cascade.*;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.test.BeanConstructor;
import org.zstack.test.WebBeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * test stop cascading by return null from createActionForChildResource
 */
public class TestAsyncCascade2 {
    CLogger logger = Utils.getLogger(TestAsyncCascade2.class);
    ComponentLoader loader;
    CascadeFacade casf;
    boolean success1;
    boolean success2;
    boolean success3;
    boolean success4;
    boolean success5;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new WebBeanConstructor();
        loader = con.build();
        con.addXml("PortalForUnitTest.xml");
        casf = loader.getComponent(CascadeFacade.class);
    }


    private void bootstrap(Map<String, CascadeExtensionPoint> exts) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = CascadeFacadeImpl.class.getDeclaredMethod("populateCascadeNodes", Map.class);
        method.setAccessible(true);
        method.invoke(casf, exts);

        method = CascadeFacadeImpl.class.getDeclaredMethod("populateTree");
        method.setAccessible(true);
        method.invoke(casf);
    }

    @Test
    public void test() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Map<String, CascadeExtensionPoint> map = new HashMap<String, CascadeExtensionPoint>();
        map.put("zone", new AbstractAsyncCascadeExtension() {

            @Override
            public void asyncCascade(CascadeAction action, Completion completion) {
                success1 = true;
                completion.success();
            }

            @Override
            public List<String> getEdgeNames() {
                return Arrays.asList();
            }

            @Override
            public String getCascadeResourceName() {
                return "zone";
            }

            @Override
            public CascadeAction createActionForChildResource(CascadeAction action) {
                return action.copy().setParentIssuer("zone");
            }
        });

        map.put("cluster", new AbstractAsyncCascadeExtension() {

            @Override
            public void asyncCascade(CascadeAction action, Completion completion) {
                if (Arrays.asList("zone", "primaryStorage").contains(action.getParentIssuer())) {
                    success2 = true;
                }
                completion.success();
            }

            @Override
            public List<String> getEdgeNames() {
                return Arrays.asList("zone", "primaryStorage");
            }

            @Override
            public String getCascadeResourceName() {
                return "cluster";
            }

            @Override
            public CascadeAction createActionForChildResource(CascadeAction action) {
                if (action.getParentIssuer().equals("zone")) {
                    return action.copy().setParentIssuer("cluster");
                }

                return null;
            }
        });

        map.put("primaryStorage", new AbstractAsyncCascadeExtension() {
            @Override
            public void asyncCascade(CascadeAction action, Completion completion) {
                if (Arrays.asList("zone", "cluster").contains(action.getParentIssuer())) {
                    success3 = true;
                }
                completion.success();
            }

            @Override
            public List<String> getEdgeNames() {
                return Arrays.asList("zone", "cluster");
            }

            @Override
            public String getCascadeResourceName() {
                return "primaryStorage";
            }

            @Override
            public CascadeAction createActionForChildResource(CascadeAction action) {
                if (action.getParentIssuer().equals("zone")) {
                    return action.copy().setParentIssuer("primaryStorage");
                }

                return null;
            }
        });

        map.put("host", new AbstractAsyncCascadeExtension() {
            @Override
            public void asyncCascade(CascadeAction action, Completion completion) {
                if (action.getParentIssuer().equals("cluster")) {
                    success4 = (!success4 ? true : false);
                }
                completion.success();
            }

            @Override
            public List<String> getEdgeNames() {
                return Arrays.asList("cluster");
            }

            @Override
            public String getCascadeResourceName() {
                return "host";
            }

            @Override
            public CascadeAction createActionForChildResource(CascadeAction action) {
                return action.copy().setParentIssuer("host");
            }
        });

        bootstrap(map);
        casf.asyncCascade("test", "zone", null, new Completion(null) {
            @Override
            public void success() {
                success5 = true;
            }

            @Override
            public void fail(ErrorCode errorCode) {
            }
        });

        Assert.assertTrue(success1);
        Assert.assertTrue(success2);
        Assert.assertTrue(success3);
        Assert.assertTrue(success4);
        Assert.assertTrue(success5);
    }

}
