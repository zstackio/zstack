package org.zstack.test.core.cascade;

import com.google.common.collect.Ordering;
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
import java.util.*;

/**
 */
public class TestAsyncCascade3 {
    CLogger logger = Utils.getLogger(TestAsyncCascade3.class);
    ComponentLoader loader;
    CascadeFacade casf;
    List<String> seq = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new WebBeanConstructor();
        con.addXml("PortalForUnitTest.xml");
        loader = con.build();
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
                logger.debug(String.format("asyncCascade: %s", getCascadeResourceName()));
                seq.add(this.getCascadeResourceName());
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
                if (Arrays.asList("zone").contains(action.getParentIssuer())) {
                    seq.add(this.getCascadeResourceName());
                }
                completion.success();
            }

            @Override
            public List<String> getEdgeNames() {
                return Arrays.asList("zone");
            }

            @Override
            public String getCascadeResourceName() {
                return "cluster";
            }

            @Override
            public CascadeAction createActionForChildResource(CascadeAction action) {
                return action.copy().setParentIssuer("cluster");
            }
        });

        map.put("primaryStorage", new AbstractAsyncCascadeExtension() {
            @Override
            public void asyncCascade(CascadeAction action, Completion completion) {
                if (action.getParentIssuer().equals("zone")) {
                    seq.add(this.getCascadeResourceName());
                }
                completion.success();
            }

            @Override
            public List<String> getEdgeNames() {
                return Arrays.asList("zone");
            }

            @Override
            public String getCascadeResourceName() {
                return "primaryStorage";
            }

            @Override
            public CascadeAction createActionForChildResource(CascadeAction action) {
                return action.copy().setParentIssuer("primaryStorage");
            }
        });

        map.put("host", new AbstractAsyncCascadeExtension() {
            @Override
            public void asyncCascade(CascadeAction action, Completion completion) {
                if (action.getParentIssuer().equals("zone")) {
                    seq.add(this.getCascadeResourceName());
                }
                completion.success();
            }

            @Override
            public List<String> getEdgeNames() {
                return Arrays.asList("zone");
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

        map.put("l2Network", new AbstractAsyncCascadeExtension() {
            @Override
            public void asyncCascade(CascadeAction action, Completion completion) {
                if (action.getParentIssuer().equals("zone")) {
                    seq.add(this.getCascadeResourceName());
                }
                completion.success();
            }

            @Override
            public List<String> getEdgeNames() {
                return Arrays.asList("zone");
            }

            @Override
            public String getCascadeResourceName() {
                return "l2Network";
            }

            @Override
            public CascadeAction createActionForChildResource(CascadeAction action) {
                return action.copy().setParentIssuer("l2Network");
            }
        });

        map.put("l3Network", new AbstractAsyncCascadeExtension() {
            @Override
            public void asyncCascade(CascadeAction action, Completion completion) {
                if (action.getParentIssuer().equals("zone")) {
                    seq.add(this.getCascadeResourceName());
                }
                completion.success();
            }

            @Override
            public List<String> getEdgeNames() {
                return Arrays.asList("zone");
            }

            @Override
            public String getCascadeResourceName() {
                return "l3Network";
            }

            @Override
            public CascadeAction createActionForChildResource(CascadeAction action) {
                return action.copy().setParentIssuer("l3Network");
            }
        });

        bootstrap(map);
        casf.asyncCascade("test", "zone", null, new Completion(null) {
            @Override
            public void success() {
            }

            @Override
            public void fail(ErrorCode errorCode) {
            }
        });

        // Note(WeiW): Since the last element must be issuer, shouldn't be ordered.
        // And the seq must be [cluster, host, l2Network, l3Network, primaryStorage, zone]
        Assert.assertTrue(Ordering.natural().isOrdered(seq.subList(0, seq.size()-1)));
    }

}
