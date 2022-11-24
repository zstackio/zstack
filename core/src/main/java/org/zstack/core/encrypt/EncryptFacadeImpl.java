package org.zstack.core.encrypt;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigBeforeUpdateExtensionPoint;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.convert.PasswordConverter;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.Component;
import org.zstack.header.core.encrypt.PasswordEncryptType;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Query;
import javax.persistence.Table;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Created by kayo on 2018/9/7.
 */
public class EncryptFacadeImpl implements EncryptFacade, Component {
    private static final CLogger logger = Utils.getLogger(EncryptFacadeImpl.class);

    public static EncryptRSA rsa = new EncryptRSA();

    private EncryptDriver encryptDriver;

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRegistry;

    public static Set<Field> encryptedFields = new HashSet<>();

    @Override
    public String encrypt(String decryptString) {
        return encryptDriver.encrypt(decryptString);
    }

    @Override
    public String decrypt(String encryptString) {
        return encryptDriver.decrypt(encryptString);
    }

    @Override
    public ErrorableValue<String> encrypt(String data, String algType) {
        return encryptDriver.encrypt(data, algType);
    }

    @Override
    public ErrorableValue<String> decrypt(String data, String algType) {
        return encryptDriver.decrypt(data, algType);
    }

    private void encryptAllPassword() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                for (Field field : encryptedFields) {
                    List<String> classNames = new ArrayList<>();

                    if (field.getDeclaringClass().getAnnotation(Entity.class) != null && field.getDeclaringClass().getAnnotation(Table.class) != null) {
                        classNames.add(field.getDeclaringClass().getSimpleName());
                    } else {
                        classNames.addAll(BeanUtils.reflections.getSubTypesOf(field.getDeclaringClass()).stream()
                                .filter(aClass -> aClass.getAnnotation(Entity.class) != null && aClass.getAnnotation(Table.class) != null)
                                .map(Class::getSimpleName)
                                .collect(Collectors.toList()));
                    }

                    for (String className : classNames) {
                        List<String> uuids = sql(String.format("select uuid from %s", className)).list();

                        for (String uuid : uuids) {
                            String value = sql(String.format("select %s from %s where uuid = '%s'", field.getName(), className, uuid)).find();

                            try {
                                String encryptedString = encrypt(value);

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
            }
        }.execute();
    }

    private void decryptAllPassword() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                for (Field field : encryptedFields) {
                    List<String> classNames = new ArrayList<>();

                    if (field.getDeclaringClass().getAnnotation(Entity.class) != null && field.getDeclaringClass().getAnnotation(Table.class) != null) {
                        classNames.add(field.getDeclaringClass().getSimpleName());
                    } else {
                        classNames.addAll(BeanUtils.reflections.getSubTypesOf(field.getDeclaringClass()).stream()
                                .filter(aClass -> aClass.getAnnotation(Entity.class) != null && aClass.getAnnotation(Table.class) != null)
                                .map(Class::getSimpleName)
                                .collect(Collectors.toList()));
                    }

                    for (String className : classNames) {
                        List<String> uuids = sql(String.format("select uuid from %s", className)).list();

                        for (String uuid : uuids) {
                            String encryptedString = sql(String.format("select %s from %s where uuid = '%s'", field.getName(), className, uuid)).find();

                            try {
                                String decryptString = decrypt(encryptedString);

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
            }
        }.execute();
    }

    private void encryptAllPasswordWithNewKey(String key) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                for (Field field : encryptedFields) {
                    List<String> classNames = new ArrayList<>();

                    if (field.getDeclaringClass().getAnnotation(Entity.class) != null && field.getDeclaringClass().getAnnotation(Table.class) != null) {
                        classNames.add(field.getDeclaringClass().getSimpleName());
                    } else {
                        classNames.addAll(BeanUtils.reflections.getSubTypesOf(field.getDeclaringClass()).stream()
                                .filter(aClass -> aClass.getAnnotation(Entity.class) != null && aClass.getAnnotation(Table.class) != null)
                                .map(Class::getSimpleName)
                                .collect(Collectors.toList()));
                    }

                    for (String className : classNames) {
                        List<String> uuids = sql(String.format("select uuid from %s", className)).list();

                        for (String uuid : uuids) {
                            String encryptedString = sql(String.format("select %s from %s where uuid = '%s'", field.getName(), className, uuid)).find();

                            try {
                                String decryptedString = decrypt(encryptedString);
                                ErrorableValue<String> encrypt = encrypt(decryptedString, key);
                                if (encrypt.error != null) {
                                    logger.error(String.format("Encryption error : %s", encrypt.error));
                                    throw new OperationFailureException(operr("Encryption error : %s", encrypt.error));
                                }

                                String sql = String.format("update %s set %s = :encrypted where uuid = :uuid", className, field.getName());

                                Query query = dbf.getEntityManager().createQuery(sql);
                                query.setParameter("encrypted", encrypt.result);
                                query.setParameter("uuid", uuid);
                                query.executeUpdate();
                            } catch (Exception e) {
                                logger.debug(String.format("decrypt origin password error because : %s",e.getMessage()));
                            }
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
        String driverType = EncryptGlobalConfig.ENCRYPT_DRIVER.value();
        for (EncryptDriver driver : pluginRegistry.getExtensionList(EncryptDriver.class)) {
            if (!driverType.equals(driver.getDriverType().toString())) {
                continue;
            }

            encryptDriver = driver;
            break;
        }

        if (encryptDriver == null) {
            throw new CloudRuntimeException(String.format("no matched encrypt driver[type:%s] can be found", driverType));
        }

        encryptedFields = getAllEncryptPassword();

        EncryptGlobalConfig.ENCRYPT_DRIVER.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                for (EncryptDriver driver : pluginRegistry.getExtensionList(EncryptDriver.class)) {
                    if (!newConfig.value().equals(driver.getDriverType().toString())) {
                        continue;
                    }

                    encryptDriver = driver;
                    break;
                }
            }
        });

        EncryptGlobalConfig.ENABLE_PASSWORD_ENCRYPT.installLocalBeforeUpdateExtension(new GlobalConfigBeforeUpdateExtensionPoint() {
            @Override
            public void beforeUpdateExtensionPoint(GlobalConfig oldConfig, String newValue) {
//                //encryption cannot be changed
//                if (PasswordEncryptType.SecurityResourceEncryption.toString().equals(oldConfig.value())) {
//                    throw new OperationFailureException(operr("SecurityResourceEncryption cannot be change"));
//                }

                // avoid encrypt twice need to do synchronized encrypted(decrypted)
                // e.g. use javax.persistence.Query set password to update encrypt password
                // when PasswordConverter check this value is true the password will be encrypt
                // again before persist, so this beforeUpdateExtension is necessary
                if (PasswordEncryptType.LocalEncryption.toString().equals(newValue)) {
                    encryptAllPassword();
                }
            }
        });

        EncryptGlobalConfig.ENABLE_PASSWORD_ENCRYPT.installLocalUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                if (PasswordEncryptType.None.toString().equals(newConfig.value())) {
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
