package org.zstack.test.search;

import junit.framework.Assert;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.header.query.Unqueryable;
import org.zstack.header.rest.APINoSee;
import org.zstack.test.Api;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class QueryTestValidator {
    private static final CLogger logger = Utils.getLogger(QueryTestValidator.class);

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

    public static <T> void validateEQ(APIQueryMessage msg, Api api, Class<T> replyClass, Object inventory, SessionInventory session) {
        try {
            List<Field> toTest = getFieldToTest(inventory);
            for (Field f : toTest) {
                f.setAccessible(true);
                Object value = f.get(inventory);
                QueryCondition c = new QueryCondition();
                c.setName(f.getName());
                if (value != null) {
                    c.setOp(QueryOp.EQ.toString());
                    c.setValue(value.toString());
                } else {
                    c.setOp(QueryOp.IS_NULL.toString());
                    c.setValues(null);
                }
                msg.getConditions().add(c);
                T reply = api.query(msg, replyClass, session);
                Field invField = replyClass.getDeclaredField("inventories");
                invField.setAccessible(true);
                List lst = (List) invField.get(reply);
                Assert.assertFalse(lst.isEmpty());
                validateHasInventory(lst, inventory, toTest);
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    public static <T> void validateEQ(APIQueryMessage msg, Api api, Class<T> replyClass, Object inventory) {
        validateEQ(msg, api, replyClass, inventory, api.getAdminSession());
    }

    public static <T> void validateRandomEQConjunction(APIQueryMessage msg, Api api, Class<T> replyClass, Object inventory, int conjunctedFieldNum) {
        validateRandomEQConjunction(msg, api, replyClass, inventory, api.getAdminSession(), conjunctedFieldNum);
    }

    public static <T> void validateRandomEQConjunction(APIQueryMessage msg, Api api, Class<T> replyClass, Object inventory, SessionInventory session, int conjunctedFieldNum) {
        List<Field> toTest = getFieldToTest(inventory);
        if (conjunctedFieldNum > toTest.size()) {
            throw new CloudRuntimeException(String.format("totally %s fields to test, but you requires %s", toTest.isEmpty(), conjunctedFieldNum));
        }

        List<Field> randomFields = new ArrayList<Field>();
        for (int i = 0; i < conjunctedFieldNum; i++) {
            Random r = new Random();
            int index = r.nextInt(toTest.size());
            Field f = toTest.get(index);
            randomFields.add(f);
        }

        try {
            for (Field f : randomFields) {
                f.setAccessible(true);
                Object value = f.get(inventory);
                QueryCondition c = new QueryCondition();
                c.setName(f.getName());
                if (value != null) {
                    c.setOp(QueryOp.EQ.toString());
                    c.setValue(value.toString());
                } else {
                    c.setOp(QueryOp.IS_NULL.toString());
                    c.setValues(null);
                }
                msg.getConditions().add(c);
            }

            T reply = api.query(msg, replyClass, session);
            Field invField = replyClass.getDeclaredField("inventories");
            invField.setAccessible(true);
            List lst = (List) invField.get(reply);
            Assert.assertFalse(lst.isEmpty());
            validateHasInventory(lst, inventory, toTest);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
}
