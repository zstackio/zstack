package org.zstack.header.message;

import org.zstack.utils.StringDSL;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by lining on 2017/11/14.
 */
public class DocUtils {
    private static Map<String, Integer> apiUuidMap = new HashMap<>();

    public static final long date = 1510669257141L;

    public static String uuidForAPIDoc(){
        String apiName = new Throwable().getStackTrace()[2].getClassName();

        if(!apiUuidMap.containsKey(apiName)){
            apiUuidMap.put(apiName, 0);
        }

        Integer index = apiUuidMap.get(apiName);
        String uuidKey = apiName + index;
        String uuid = UUID.nameUUIDFromBytes(uuidKey.getBytes()).toString().replaceAll("-", "");

        apiUuidMap.put(apiName, ++index);
        return uuid;
    }

    public static void removeApiUuidMap(String apiName){
        apiUuidMap.remove(apiName);
    }

    public static String createFixedUuid(Class<?> voClass) {
        return StringDSL.createFixedUuid(voClass);
    }

    public static Timestamp timestamp() {
        return new Timestamp(date);
    }
}
