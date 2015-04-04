package org.zstack.core.keyvalue;

import org.mvel2.MVEL;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 */
public class KeyValueDeserializer {
    public <T> T deserialize(Class<T> clz, List<KeyValueStruct> keyValues) {
        try {
            Object instance = clz.newInstance();
            /*
            for (KeyValueStruct s : keyValues) {
                String express = String.mediaType("CONTEXT_OBJECT.%s = value", s.getKey());
                Map vals = map(e("value", s.toValue()));
                Map context = map(e("CONTEXT_OBJECT", instance));
                System.out.println(String.mediaType("%s, value: %s", express, s.toValue()));
                MVEL.eval(express.toString(), context, vals);
            }
            */
            StringBuilder express = new StringBuilder();
            int i = 0;
            Map vals = new HashMap();
            for (KeyValueStruct struct : keyValues) {
                String valueName = "value" + i ++;
                express.append(String.format("\nCONTEXT_OBJECT.%s = %s;", struct.getKey(), valueName));
                vals.put(valueName, struct.toValue());
            }

            Map ctx = map();
            Map context = map(e("CONTEXT_OBJECT", ctx));
            System.out.println(express.toString());
            MVEL.eval(express.toString(), context, vals);
            instance = JSONObjectUtil.rehashObject(ctx, clz);
            return (T) instance;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
}
