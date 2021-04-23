package org.zstack.core.encrypt;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigBeforeUpdateExtensionPoint;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.convert.PasswordConverter;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.SwxaUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Convert;
import javax.persistence.Query;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by kayo on 2018/9/7.
 */
public class EncryptFacadeImpl implements EncryptFacade, Component {
    private static final CLogger logger = Utils.getLogger(EncryptFacadeImpl.class);

    public static EncryptRSA rsa = new EncryptRSA();

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    public static Set<Field> encryptedFields = new HashSet<>();

    @Override
    public String encrypt(String decryptString) {
        if (!EncryptGlobalConfig.ENABLE_PASSWORD_ENCRYPT.value(Boolean.class)) {
            return decryptString;
        }

        try {
            //return rsa.encrypt1(decryptString);
            return SwxaUtils.genCipherData1(decryptString);
        } catch (Exception e) {
            throw new CloudRuntimeException(e.getMessage());
        }

    }

    @Override
    public String decrypt(String encryptString) {
        if (!EncryptGlobalConfig.ENABLE_PASSWORD_ENCRYPT.value(Boolean.class)) {
            return encryptString;
        }

        try {
            //return (String) rsa.decrypt1(encryptString);
            return SwxaUtils.verifyAndDecryptCipherData1(encryptString);
        } catch (Exception e) {
            throw new CloudRuntimeException(e.getMessage());
        }

    }

    private void encryptAllPassword() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                for (Field field : encryptedFields) {
                    String className = field.getDeclaringClass().getSimpleName();

                    List<String> uuids = sql(String.format("select uuid from %s", className)).list();

                    for (String uuid : uuids) {
                        String value = sql(String.format("select %s from %s where uuid = '%s'", field.getName(), className, uuid)).find();

                        try {
                            //String encryptedString = rsa.encrypt1(value);
                            String encryptedString = SwxaUtils.genCipherData1(value);

                            String sql = String.format("update %s set %s = :encrypted where uuid = :uuid", className, field.getName());

                            Query query = dbf.getEntityManager().createQuery(sql);
                            query.setParameter("encrypted", encryptedString);
                            query.setParameter("uuid", uuid);
                            query.executeUpdate();
                        } catch (Exception e) {
                            logger.debug(String.format("encrypt error because : %s",e.getMessage()));
                        }
                    }
                }
            }
        }.execute();
    }

    private void decryptAllPassword() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                for (Field field : encryptedFields) {
                    String className = field.getDeclaringClass().getSimpleName();

                    List<String> uuids = sql(String.format("select uuid from %s", className)).list();

                    for (String uuid : uuids) {
                        String encryptedString = sql(String.format("select %s from %s where uuid = '%s'", field.getName(), className, uuid)).find();

                        try {
                            //String decryptString = (String) rsa.decrypt1(encryptedString);
                            String decryptString = SwxaUtils.verifyAndDecryptCipherData1(encryptedString);

                            String sql = String.format("update %s set %s = :decrypted where uuid = :uuid", className, field.getName());

                            Query query = dbf.getEntityManager().createQuery(sql);
                            query.setParameter("decrypted", decryptString);
                            query.setParameter("uuid", uuid);
                            query.executeUpdate();
                        } catch (Exception e) {
                            logger.debug(String.format("decrypt password error because : %s",e.getMessage()));
                        }
                    }
                }
            }
        }.execute();

    }

    private void encryptAllPasswordWithNewKey(String key) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                for (Field field : encryptedFields) {
                    String className = field.getDeclaringClass().getSimpleName();

                    List<String> uuids = sql(String.format("select uuid from %s", className)).list();

                    for (String uuid : uuids) {
                        String encryptedString = sql(String.format("select %s from %s where uuid = '%s'", field.getName(), className, uuid)).find();

                        try {
                            String decryptString = (String) rsa.decrypt1(encryptedString);
                            encryptedString = rsa.encrypt(decryptString, key);

                            String sql = String.format("update %s set %s = :encrypted where uuid = :uuid", className, field.getName());

                            Query query = dbf.getEntityManager().createQuery(sql);
                            query.setParameter("encrypted", encryptedString);
                            query.setParameter("uuid", uuid);
                            query.executeUpdate();
                        } catch (Exception e) {
                            logger.debug(String.format("decrypt origin password error because : %s",e.getMessage()));
                        }
                    }
                }
            }
        }.execute();
    }

    private static Set<Field> getAllEncryptPassword() {
        Set<Field> fields = Platform.getReflections().getFieldsAnnotatedWith(Convert.class);

        return fields.stream().filter(field -> field.getAnnotation(Convert.class).converter().equals(PasswordConverter.class)).collect(Collectors.toSet());
    }

    @Override
    public boolean start() {
        encryptedFields = getAllEncryptPassword();

        EncryptGlobalConfig.ENABLE_PASSWORD_ENCRYPT.installLocalBeforeUpdateExtension(new GlobalConfigBeforeUpdateExtensionPoint() {
            @Override
            public void beforeUpdateExtensionPoint(GlobalConfig oldConfig, String newValue) {
                // avoid encrypt twice need to do synchronized encrypted(decrypted)
                // e.g. use javax.persistence.Query set password to update encrypt password
                // when PasswordConverter check this value is true the password will be encrypt
                // again before persist, so this beforeUpdateExtension is necessary
                if (Boolean.valueOf(newValue)) {
                    encryptAllPassword();
                }
            }
        });

        EncryptGlobalConfig.ENABLE_PASSWORD_ENCRYPT.installLocalUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                if (!newConfig.value(Boolean.class)) {
                    decryptAllPassword();
                }
            }
        });

        EncryptGlobalConfig.ENCRYPT_ALGORITHM.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                String key = newConfig.value(String.class);
                encryptAllPasswordWithNewKey(key);
                try {
                    rsa.updateKey(key);
                } catch (Exception e) {
                    logger.debug("update key in encryptrsa error");
                    logger.debug(String.format("error is : %s",e.getMessage()));
                }
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
