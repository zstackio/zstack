package org.zstack.core.convert;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.zstack.core.encrypt.EncryptFacade;
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

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptFacade.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptFacade.decrypt(dbData);
    }
}
