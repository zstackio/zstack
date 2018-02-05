package org.zstack.header.message;

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
}
