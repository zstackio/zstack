package org.zstack.core;

import org.zstack.header.exception.CloudRuntimeException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2016/6/23.
 */
public class MessageCommandRecorder {
    private static Class starter;

    private static List<Class> followers = new ArrayList<>();

    public static void start(Class s) {
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        if (starter != null) {
            throw new RuntimeException(String.format("already a starter[%s] here", starter));
        }

        starter = s;
    }

    public static void record(String c) {
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        try {
            followers.add(Class.forName(c));
        } catch (ClassNotFoundException e) {
            throw new CloudRuntimeException(e);
        }
    }

    public static void record(Class c) {
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        followers.add(c);
    }

    public static List<Class> end() {
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            return null;
        }

        List<Class> ret = followers.stream().collect(Collectors.toList());
        reset();
        return ret;
    }

    public static void reset() {
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        starter = null;
        followers = new ArrayList<>();
    }

    public static String endAndToString() {
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            return null;
        }

        StringBuilder sb = new StringBuilder(String.format("\n\nCALLING CHAIN FOR: %s", starter));
        for (Class c : followers) {
            sb.append(String.format("\n ---> %s", c));
        }

        reset();
        return sb.toString();
    }
}
