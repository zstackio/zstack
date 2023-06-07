package org.zstack.core.encrypt;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.*;
import org.zstack.core.convert.PasswordConverter;
import org.zstack.core.convert.SpecialDataConverter;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.Component;
import org.zstack.header.core.encrypt.*;
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
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRegistry;

    public static Set<Field> encryptedFields = new HashSet<>();

    public static List<CovertSubClass> covertSubClasses = new ArrayList<>();

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

    private String getQuerySql(List<CovertSubClass> covertSubClasses, String className, String fieldName, String uuid) {
        List<String> whereSqlList = covertSubClasses.stream()
                .filter(subClass -> subClass.classSimpleName().equals(className) && !subClass.columnName().isEmpty())
                .map(subClass -> String.format(" %s = '%s'", subClass.columnName(), subClass.columnValue()))
                .collect(Collectors.toList());

        String querySql = String.format("select %s from %s where uuid = '%s'", fieldName, className, uuid);
        if (!whereSqlList.isEmpty()) {
            querySql = querySql + String.format(" and (%s)", whereSqlList.stream().collect(Collectors.joining(" or ")));
        }
        return querySql;
    }

    private void encryptAllPassword() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                for (Field field : encryptedFields) {
                    List<String> classNames = getClassName(field, covertSubClasses);

                    for (String className : classNames) {
                        List<String> uuids = sql(String.format("select uuid from %s", className)).list();

                        for (String uuid : uuids) {
                            String querySql = getQuerySql(covertSubClasses, className, field.getName(), uuid);
                            String value = sql(querySql).find();

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

    private void beforeRecoverDataDecryptAllPassword() {
        Map<Field, List<String>> classnameFilter = new HashMap<>();
        for (Field field : encryptedFields) {
            List<String> classNames = new ArrayList<>();

            if (field.getDeclaringClass().getAnnotation(Entity.class) != null && field.getDeclaringClass().getAnnotation(Table.class) != null) {
                classNames.add(field.getDeclaringClass().getSimpleName());
            } else {
                List<String> subClassNames = BeanUtils.reflections.getSubTypesOf(field.getDeclaringClass()).stream()
                        .filter(aClass -> aClass.getAnnotation(Entity.class) != null && aClass.getAnnotation(Table.class) != null)
                        .map(Class::getSimpleName)
                        .collect(Collectors.toList());

                classNames.addAll(subClassNames);
            }

            classnameFilter.put(field, classNames);
        }

        decryptAllPassword(classnameFilter);
    }

    private void decryptAllPassword(Map<Field, List<String>> classnameFilter) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                for (Field field : encryptedFields) {
                    List<CovertSubClass> covertSubClasses = new ArrayList<>();
                    List<String> classNames = classnameFilter.get(field);

                    if (classNames == null || classNames.isEmpty()) {
                        classNames = getClassName(field, covertSubClasses);
                    }

                    for (String className : classNames) {
                        List<String> uuids = sql(String.format("select uuid from %s", className)).list();

                        for (String uuid : uuids) {
                            String querySql = getQuerySql(covertSubClasses, className, field.getName(), uuid);
                            String encryptedString = sql(querySql).find();

                            try {
                                String decryptString = decrypt(encryptedString);

                                String sql = String.format("update %s set %s = :decrypted where uuid = :uuid", className, field.getName());

                                Query query = dbf.getEntityManager().createNativeQuery(sql);
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
                    List<String> classNames = getClassName(field, covertSubClasses);

                    for (String className : classNames) {
                        List<String> uuids = sql(String.format("select uuid from %s", className)).list();

                        for (String uuid : uuids) {
                            String querySql = getQuerySql(covertSubClasses, className, field.getName(), uuid);
                            String encryptedString = sql(querySql).find();

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
                .filter(field -> field.getAnnotation(Convert.class).converter().equals(PasswordConverter.class) ||
                        field.getAnnotation(Convert.class).converter().equals(SpecialDataConverter.class))
                .collect(Collectors.toSet());
        for (Field field : encryptedFields) {
            getCovertSubClassList(field);
        }
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
                // avoid encrypt twice need to do synchronized encrypted(decrypted)
                // e.g. use javax.persistence.Query set password to update encrypt password
                // when PasswordConverter check this value is true the password will be encrypt
                // again before persist, so this beforeUpdateExtension is necessary
                if (PasswordEncryptType.LocalEncryption.toString().equals(newValue)
                        || PasswordEncryptType.SecurityResourceEncryption.toString().equals(newValue)) {
                    encryptAllPassword();
                }
            }
        });

        EncryptGlobalConfig.ENABLE_PASSWORD_ENCRYPT.installLocalUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                if (PasswordEncryptType.None.toString().equals(newConfig.value())) {
                    decryptAllPassword(new HashMap<>());
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
                                    String querySql = getQuerySql(covertSubClasses, className, fieldName, uuid);
                                    String value = sql(querySql).find();

                                    if (StringUtils.isEmpty(value)){
                                        continue;
                                    }

                                    try {
                                        // If part of the data has been encrypted, first decrypt all the data before encrypting
                                        String decryptedString = decrypt(value);
                                        String encryptedString = encrypt(decryptedString);

                                        String sql = String.format("update %s set %s = :encrypted where uuid = :uuid", className, fieldName);

                                        // need to use createNativeQuery. if use createQuery, it will be encrypted again after inserting into db
                                        Query query = dbf.getEntityManager().createNativeQuery(sql);
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

    private void getCovertSubClassList(Field field) {
        if (field.getDeclaringClass().getAnnotation(CovertSubClasses.class) != null) {
            covertSubClasses.addAll(Arrays.asList(field.getDeclaringClass().getAnnotation(CovertSubClasses.class).value()));
        }

	    List<CovertSubClass> subClassNames = BeanUtils.reflections.getSubTypesOf(field.getDeclaringClass()).stream()
			    .filter(aClass -> aClass.getAnnotation(CovertSubClasses.class) != null)
			    .map(subClass -> subClass.getAnnotation(CovertSubClasses.class).value())
			    .flatMap(Arrays::stream).collect(Collectors.toList());
        covertSubClasses.addAll(subClassNames);
    }

    private List<String> getClassName(Field field, List<CovertSubClass> covertSubClasses) {
        List<String> classNames = new ArrayList<>();
        if (covertSubClasses == null || covertSubClasses.isEmpty()) {
            getCovertSubClassList(field);
        }

        if (field.getDeclaringClass().getAnnotation(Entity.class) != null && field.getDeclaringClass().getAnnotation(Table.class) != null) {
            classNames.add(field.getDeclaringClass().getSimpleName());
        } else {
            List<String> subClassNames = BeanUtils.reflections.getSubTypesOf(field.getDeclaringClass()).stream()
                    .filter(aClass -> aClass.getAnnotation(Entity.class) != null && aClass.getAnnotation(Table.class) != null)
                    .map(Class::getSimpleName)
                    .collect(Collectors.toList());

            List<String> filterClassName = covertSubClasses.stream().map(CovertSubClass::classSimpleName).collect(Collectors.toList());
            List<String> covertSubClassNames = subClassNames.stream().filter(filterClassName::contains).collect(Collectors.toList());
            classNames.addAll(covertSubClassNames.isEmpty() ? subClassNames : covertSubClassNames);
        }

        return classNames;
    }

    private void collectEncryptEntityMetadata() {
        for (Field field : encryptedFields) {
            List<String> classNames = getClassName(field, covertSubClasses);

            for (String className : classNames) {
                createIfNotExists(className, field.getName());
            }
        }
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

    @Override
    public void updateEncryptDataStateIfExists(String entity, String column, EncryptEntityState state) {
        String sql = String.format("update EncryptEntityMetadataVO set state = :state where columnName = :columnName and entityName = :entityName");
        Query query = dbf.getEntityManager().createQuery(sql);
        query.setParameter("state", state);
        query.setParameter("entityName", entity);
        query.setParameter("columnName", column);
        query.executeUpdate();
    }

    private void removeConvertRecoverData() {
        if (Q.New(EncryptEntityMetadataVO.class)
                .isExists()) {
            return;
        }

        if (PasswordEncryptType.None.toString().equals(EncryptGlobalConfig.ENABLE_PASSWORD_ENCRYPT.value())) {
            return;
        }

        beforeRecoverDataDecryptAllPassword();
    }

    protected void handleNeedDecryptEntity() {
        if (PasswordEncryptType.None.toString().equals(EncryptGlobalConfig.ENABLE_PASSWORD_ENCRYPT.value())) {
            return;
        }

        List<EncryptEntityMetadataVO> metadataVOList = Q.New(EncryptEntityMetadataVO.class)
                .eq(EncryptEntityMetadataVO_.state, EncryptEntityState.NeedDecrypt)
                .list();

        new SQLBatch() {
            @Override
            protected void scripts() {
                for (EncryptEntityMetadataVO metadata : metadataVOList) {
                    long count = SQL.New(String.format("select count(1) from %s", metadata.getEntityName()), Long.class).find();
                    String className = metadata.getEntityName();
                    String fieldName = metadata.getColumnName();
                    sql(String.format("select uuid from %s", metadata.getEntityName()), String.class)
                            .limit(1000)
                            .paginate(count, (List<String> uuids) -> {
                                for (String uuid : uuids) {
                                    String querySql = String.format("select %s from %s where uuid = '%s'", fieldName, className, uuid);
                                    String value = sql(querySql).find();

                                    if (StringUtils.isEmpty(value)) {
                                        continue;
                                    }

                                    try {
                                        String decryptedString = decrypt(value);
                                        String sql = String.format("update %s set %s = :decrypted where uuid = :uuid", className, fieldName);
                                        Query query = dbf.getEntityManager().createQuery(sql);
                                        query.setParameter("decrypted", decryptedString);
                                        query.setParameter("uuid", uuid);
                                        query.executeUpdate();
                                    } catch (Exception e) {
                                        logger.debug(String.format("handleNeedDecryptEntity error because : %s", e.getMessage()));

                                    }
                                }

                            });
                    dbf.remove(metadata);
                }
            }
        }.execute();
    }

    @Override
    public boolean start() {
        initEncryptDriver();
        collectAllEncryptPassword();
        installGlobalConfigUpdateHooks();
        removeConvertRecoverData();
        collectEncryptEntityMetadata();
        handleNewAddedEncryptEntity();
        handleNeedDecryptEntity();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
