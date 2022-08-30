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
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.Component;
import org.zstack.header.core.encrypt.EncryptEntityMetadataVO;
import org.zstack.header.core.encrypt.EncryptEntityMetadataVO_;
import org.zstack.header.core.encrypt.EncryptEntityState;
import org.zstack.header.core.encrypt.PasswordEncryptType;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    public static Set<Field> getEncryptedFields() {
        return encryptedFields;
    }

    @Override
    public String encrypt(String decryptString) {
        return encryptDriver.encrypt(decryptString);
    }

    @Override
    public String decrypt(String encryptString) {
        return encryptDriver.decrypt(encryptString);
    }

    @Override
    public EncryptFacadeResult<String> encrypt(String data, String algType) {
        return encryptDriver.encrypt(data, algType);
    }

    @Override
    public EncryptFacadeResult<String> decrypt(String data, String algType) {
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
                        updateEncryptDataStateIfExists(className, field.getName(), EncryptEntityState.Encrypted);
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
                        updateEncryptDataStateIfExists(className, field.getName(), EncryptEntityState.NewAdded);
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
                                EncryptFacadeResult<String> encrypt = encrypt(decryptedString, key);
                                if (encrypt.error != null) {
                                    logger.error(String.format("Encryption error : %s", encrypt.error));
                                    throw new OperationFailureException(operr("Encryption error : %s", encrypt.error));
                                }

                                String sql = String.format("update %s set %s = :encrypted where uuid = :uuid", className, field.getName());

                                Query query = dbf.getEntityManager().createQuery(sql);
                                query.setParameter("encrypted", encrypt.getResult());
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

    private void collectAllEncryptPassword() {
        Set<Field> fields = Platform.getReflections().getFieldsAnnotatedWith(Convert.class);

        encryptedFields = fields.stream()
                .filter(field -> field.getAnnotation(Convert.class).converter().equals(PasswordConverter.class))
                .collect(Collectors.toSet());
    }

    private void initEncryptDriver() {
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
    }

    public void installGlobalConfigUpdateHooks() {
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
                //encryption cannot be changed
                if (PasswordEncryptType.SecurityResourceEncryption.toString().equals(oldConfig.value())) {
                    throw new OperationFailureException(operr("SecurityResourceEncryption cannot be change"));
                }

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
    }

    protected void handleNewAddedEncryptEntity() {
        if (PasswordEncryptType.None.toString().equals(EncryptGlobalConfig.ENABLE_PASSWORD_ENCRYPT.value())) {
            return;
        }

        List<EncryptEntityMetadataVO> metadataVOList = Q.New(EncryptEntityMetadataVO.class)
                .eq(EncryptEntityMetadataVO_.state, EncryptEntityState.NewAdded)
                .list();

        new SQLBatch() {
            @Override
            protected void scripts() {
                for (EncryptEntityMetadataVO metadata : metadataVOList) {
                    // do encrypt
                    long count = SQL.New(String.format("select count(1) from %s", metadata.getEntityName()), Long.class).find();
                    metadata.setState(EncryptEntityState.Encrypting);
                    metadata = dbf.updateAndRefresh(metadata);
                    String className = metadata.getEntityName();
                    String fieldName = metadata.getColumnName();
                    sql(String.format("select uuid from %s", metadata.getEntityName()), String.class)
                            .limit(1000)
                            .paginate(count, (List<String> uuids) -> {
                                for (String uuid : uuids) {
                                    String value = sql(String.format("select %s from %s where uuid = '%s'", fieldName, className, uuid)).find();

                                    try {
                                        // If part of the data has been encrypted, first decrypt all the data before encrypting
                                        String decryptedString = decrypt(value);
                                        String encryptedString = encrypt(decryptedString);

                                        String sql = String.format("update %s set %s = :encrypted where uuid = :uuid", className, fieldName);

                                        Query query = dbf.getEntityManager().createQuery(sql);
                                        query.setParameter("encrypted", encryptedString);
                                        query.setParameter("uuid", uuid);
                                        query.executeUpdate();
                                    } catch (Exception e) {
                                        logger.debug(String.format("encrypt error because : %s", e.getMessage()));
                                    }
                                }

                            });
                    metadata.setState(EncryptEntityState.Encrypted);
                    dbf.updateAndRefresh(metadata);
                }
            }
        }.execute();
    }

    private void collectEncryptEntityMetadata() {
        BeanUtils.reflections.getFieldsAnnotatedWith(Convert.class).forEach(ec -> {
            if (ec.getDeclaringClass().getAnnotation(Entity.class) == null || ec.getDeclaringClass().getAnnotation(Table.class) == null) {
                return;
            }

            if (!ec.getAnnotation(Convert.class).converter().equals(PasswordConverter.class)) {
                return;
            }

            String entityName = ec.getDeclaringClass().getCanonicalName();
            String columnName = ec.getName();
            createIfNotExists(entityName, columnName);
        });
    }

    private void createIfNotExists(String entity, String column) {
        if (Q.New(EncryptEntityMetadataVO.class)
                .eq(EncryptEntityMetadataVO_.entityName, entity)
                .eq(EncryptEntityMetadataVO_.columnName, column)
                .isExists()) {
            return;
        }

        EncryptEntityMetadataVO metadataVO = new EncryptEntityMetadataVO();
        metadataVO.setColumnName(column);
        metadataVO.setEntityName(entity);
        metadataVO.setState(EncryptEntityState.NewAdded);
        dbf.persist(metadataVO);
    }

    public void updateEncryptDataStateIfExists(String entity, String column, EncryptEntityState state) {
        String sql = String.format("update EncryptEntityMetadataVO set state = :state where columnName = :columnName and entityName like :entityName");
        Query query = dbf.getEntityManager().createQuery(sql);
        query.setParameter("state", state);
        query.setParameter("entityName", String.format("%%%s%%", entity));
        query.setParameter("columnName", column);
        query.executeUpdate();
    }

    @Override
    public boolean start() {
        initEncryptDriver();
        collectAllEncryptPassword();
        installGlobalConfigUpdateHooks();
        collectEncryptEntityMetadata();
        handleNewAddedEncryptEntity();

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
