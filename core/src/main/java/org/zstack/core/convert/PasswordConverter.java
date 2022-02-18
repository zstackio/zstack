package org.zstack.core.convert;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.encrypt.EncryptFacade;
import org.zstack.core.encrypt.EncryptGlobalConfig;
import org.zstack.header.core.encrypt.PasswordEncryptType;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Created by kayo on 2018/9/7.
 */
@Component
@Converter
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PasswordConverter implements AttributeConverter<String, String> {
    private static final CLogger logger = Utils.getLogger(PasswordConverter.class);

    private static EncryptFacade encryptFacade;

    @Autowired
    public void initEncryptFacade(EncryptFacade encryptFacade){
        PasswordConverter.encryptFacade = encryptFacade;
    }

    /*
     * Why use Transactional?
     * - entity query inside PasswordConvert will make PasswordConvert execute again, finally the thread ends up with infinite loop. so use new transcational to aoivd this issue.
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (PasswordEncryptType.None.toString().equals(EncryptGlobalConfig.ENABLE_PASSWORD_ENCRYPT.value(String.class))) {
            return attribute;
        }
        if (StringUtils.isEmpty(attribute)) {
            return attribute;
        }
        return encryptFacade.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (PasswordEncryptType.None.toString().equals(EncryptGlobalConfig.ENABLE_PASSWORD_ENCRYPT.value(String.class))) {
            return dbData;
        }

        if (StringUtils.isEmpty(dbData)) {
            return dbData ;
        }

        return encryptFacade.decrypt(dbData);
    }
}
