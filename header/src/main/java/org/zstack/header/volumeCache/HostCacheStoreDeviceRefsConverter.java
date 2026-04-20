package org.zstack.header.volumeCache;

import org.apache.commons.lang.StringUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.reflect.TypeToken;

/**
 * JPA AttributeConverter that serializes a List<HostCacheStoreDeviceRef>
 * to/from a JSON TEXT column.
 */
@Converter
public class HostCacheStoreDeviceRefsConverter
        implements AttributeConverter<List<HostCacheStoreDeviceRef>, String> {

    private static final CLogger logger = Utils.getLogger(HostCacheStoreDeviceRefsConverter.class);

    private static final Type LIST_TYPE =
            new TypeToken<List<HostCacheStoreDeviceRef>>() {}.getType();

    @Override
    public String convertToDatabaseColumn(List<HostCacheStoreDeviceRef> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return JSONObjectUtil.toJsonString(attribute);
    }

    @Override
    public List<HostCacheStoreDeviceRef> convertToEntityAttribute(String dbData) {
        if (StringUtils.isEmpty(dbData)) {
            return new ArrayList<>();
        }
        try {
            List<HostCacheStoreDeviceRef> result =
                    JSONObjectUtil.toList(dbData, LIST_TYPE);
            return result == null ? new ArrayList<>() : result;
        } catch (Exception e) {
            logger.warn(String.format("failed to deserialize HostCacheStoreVO.devices json[%s]; " +
                    "treat as empty", dbData), e);
            return new ArrayList<>();
        }
    }
}
