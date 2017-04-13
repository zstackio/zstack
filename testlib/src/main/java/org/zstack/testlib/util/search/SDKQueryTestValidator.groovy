package org.zstack.testlib.util.search

import junit.framework.Assert
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.query.QueryOp
import org.zstack.header.query.Unqueryable
import org.zstack.header.rest.APINoSee
import org.zstack.testlib.Test
import org.zstack.utils.FieldUtils
import org.zstack.utils.Utils
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.logging.CLogger

import java.lang.reflect.Field

/**
 * Created by lining on 2017/3/28.
 */
class SDKQueryTestValidator {
    private static final CLogger logger = Utils.getLogger(SDKQueryTestValidator.class);

    private static List<Field> getFieldToTest(Object inventory) {
        List<Field> fs = FieldUtils.getAllFields(inventory.getClass());

        List<Field> ret = new ArrayList<Field>();
        for (Field f : fs) {
            if (Collection.class.isAssignableFrom(f.getType())) {
                continue;
            }

            if (f.isAnnotationPresent(Unqueryable.class)) {
                continue;
            }

            if (f.isAnnotationPresent(APINoSee.class)) {
                continue;
            }

            ret.add(f);
        }

        return ret;
    }

    private static boolean compareInventory(Object actual, Object expected, List<Field> toTest) throws IllegalArgumentException, IllegalAccessException {
        for (Field f : expected.getClass().getDeclaredFields()) {
            if (!toTest.contains(f)) {
                continue;
            }

            try {
                Field af = actual.getClass().getDeclaredField(f.getName());
                af.setAccessible(true);
                Object av = af.get(actual);

                f.setAccessible(true);
                Object ev = f.get(expected);

                if (ev == null && av == null) {
                    continue;
                }

                if (ev != null && !ev.equals(av)) {
                    logger.warn(String.format("Field[%s], expected value:%s, actual value: %s", f.getName(), ev, av));
                    return false;
                }
            } catch (NoSuchFieldException e) {
                return false;
            }
        }

        return true;
    }

    private static void validateHasInventory(List actual, Object expected, List<Field> toTest) throws IllegalArgumentException, IllegalAccessException {
        for (Object a : actual) {
            if (compareInventory(a, expected, toTest)) {
                return;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n============================================================");
        sb.append(String.format("\nexpected inventory: \n%s", JSONObjectUtil.toJsonString(expected)));
        sb.append(String.format("\nactual inventory: \n%s", JSONObjectUtil.toJsonString(actual)));
        sb.append("\n============================================================");
        logger.warn(sb.toString());
        Assert.fail();
    }

    static <T> void validateEQ(Object msg, Object inventory, Object session) {
        try {
            List<Field> toTest = getFieldToTest(inventory);
            for (Field f : toTest) {
                f.setAccessible(true);
                Object value = f.get(inventory);
                String queryName
                String queryOp
                String queryValue

                queryName = f.name
                if (value != null) {
                    queryOp = QueryOp.EQ.toString()
                    queryValue = value.toString()
                } else {
                    queryOp = " " + QueryOp.IS_NULL.toString()
                    queryValue = ""
                }
                msg.conditions.add("${queryName}${queryOp}${queryValue}".toString())
                msg.sessionId = session.uuid

                T result = msg.call()

                if(result.value == null || result.value.inventories == null || result.value.inventories.size == 0){
                    assert false
                }
                List lst = result.value.inventories
                Assert.assertFalse(lst.isEmpty())
                validateHasInventory(lst, inventory, toTest)
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    static <T> void validateEQ(Object msg, Object inventory) {
        validateEQ(msg, inventory,  Test.currentEnvSpec?.session)
    }

}
