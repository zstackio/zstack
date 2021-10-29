package org.zstack.core.encrypt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigVO;
import org.zstack.core.config.GlobalConfigVO_;
import org.zstack.core.db.*;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.core.workflow.ShareFlowChain;
import org.zstack.header.AbstractService;
import org.zstack.header.core.*;
import org.zstack.header.core.encrypt.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.storage.primary.AfterCreateImageCacheExtensionPoint;
import org.zstack.header.storage.primary.ImageCacheInventory;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.snapshot.*;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Entity;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Tuple;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 16/12/28.
 */
public class EncryptManagerImpl extends AbstractService {
    private static final CLogger logger = Utils.getLogger(EncryptManagerImpl.class);
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EncryptFacadeImpl encryptFacade;
    @Autowired
    private PluginRegistry pluginRgty;

    public static List<SignedColumn> signedList = new ArrayList<>();

    private static List<SignedColumn> getAllSignedIntegrity() {
        Set<Field> fields = Platform.getReflections().getFieldsAnnotatedWith(SignedText.class);
        List<SignedColumn> signedColumnList = new ArrayList<>();
        for (Field field : fields) {
            SignedColumn signedColumn = new SignedColumn();
            signedColumn.setSignedColumnNames(Arrays.stream(field.getAnnotation(SignedText.class).signedColumnName()).collect(Collectors.toList()));
            signedColumn.setPrimaryKey(field.getAnnotation(SignedText.class).primaryKey());
            signedColumn.setTableName(field.getAnnotation(SignedText.class).tableName());

            AppointColumn[] appoints = field.getAnnotation(SignedText.class).appointColumnName();
            for (AppointColumn appoint : appoints) {
                Map<String, String> appointColumn = new HashMap<>();
                appointColumn.put(appoint.column(), appoint.vaule());

                signedColumn.setAppointMap(appointColumn);
            }
            signedColumnList.add(signedColumn);
        }
        return signedColumnList;
    }


    @Override
    public boolean start() {
        signedList = Collections.unmodifiableList(getAllSignedIntegrity());
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
         if (msg instanceof StartDataProtectionMsg) {
             handle((StartDataProtectionMsg) msg);
         } else if (msg instanceof StartDataConfidentialityMsg) {
             handle((StartDataConfidentialityMsg) msg);
         } else {
             bus.dealWithUnknownMessage(msg);
         }

    }

    private void handle(StartDataConfidentialityMsg msg) {
        StartDataConfidentialityReply reply = new StartDataConfidentialityReply();
        encryptAllConfidentiality(msg.getEncryptType(), new Completion(msg) {
            @Override
            public void success() {
               bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
               reply.setError(errorCode);
               bus.reply(msg, reply);
            }
        });
    }

    private void handle(StartDataProtectionMsg msg) {
        StartDataProtectionReply reply = new StartDataProtectionReply();
        FlowChain chain = new ShareFlowChain();
        chain.setName("start-data-protection");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                String __name__ = "start-encrypted-confidentiality";
                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        StartDataConfidentialityMsg confidentialityMsg = new StartDataConfidentialityMsg();
                        confidentialityMsg.setEncryptType(msg.getEncryptType());
                        bus.makeLocalServiceId(confidentialityMsg, EncryptGlobalConfig.SERVICE_ID);
                        bus.send(confidentialityMsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "start-encrypted-audits-and-globalconfig-integrity";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        encryptedAllDateIntegrity(trigger);
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "start-image-integrity";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> imageUuids = Q.New(ImageVO.class).select(ImageVO_.uuid).listValues();

                        new While<>(imageUuids).each((uuid, com) -> {
                            GetImageEncryptedMsg encryptedMsg = new GetImageEncryptedMsg();
                            encryptedMsg.setImageUuid(uuid);
                            bus.makeTargetServiceIdByResourceUuid(encryptedMsg, ImageConstant.SERVICE_ID, uuid);
                            bus.send(encryptedMsg, new CloudBusCallBack(com) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        com.addError(reply.getError());
                                        com.allDone();
                                        return;
                                    }
                                    GetImageEncryptedReply encryptedReply = reply.castReply();
                                    EncryptFacadeResult<String> encrypt = encryptFacade.encrypt(encryptedReply.getEncrypt(), EncryptType.HMAC.toString());

                                    if (encrypt.getError() != null) {
                                        logger.error(String.format("encryption error : %s", encrypt.getError()));
                                        com.addError(encrypt.getError());
                                        com.allDone();
                                        return;
                                    }

                                    EncryptionIntegrityVO vo = new EncryptionIntegrityVO();
                                    vo.setResourceType(ImageVO.class.getSimpleName());
                                    vo.setResourceUuid(encryptedReply.getImageUuid());
                                    vo.setSignedText(encrypt.getResult());
                                    dbf.persist(vo);
                                    com.done();
                                }
                            });
                        } ).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errorCodeList.getCauses().isEmpty()) {
                                    trigger.fail(errorCodeList.getCause());
                                    return;
                                }

                                trigger.next();
                            }
                        });

                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "start-snapshot-integrity";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<VolumeSnapshotVO> snapshotVOS = Q.New(VolumeSnapshotVO.class).list();

                        new While<>(snapshotVOS).each((snapshotVO, com)-> {
                            GetVolumeSnapshotEncryptedMsg encryptMsg = new GetVolumeSnapshotEncryptedMsg();

                            encryptMsg.setSnapshotUuid(snapshotVO.getUuid());
                            encryptMsg.setPrimaryStorageUuid(snapshotVO.getPrimaryStorageUuid());
                            encryptMsg.setPrimaryStorageInstallPath(snapshotVO.getPrimaryStorageInstallPath());
                            bus.makeTargetServiceIdByResourceUuid(encryptMsg, VolumeSnapshotConstant.SERVICE_ID, snapshotVO.getUuid());
                            bus.send(encryptMsg, new CloudBusCallBack(com) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        com.addError(reply.getError());
                                        com.allDone();
                                        return;
                                    }

                                    GetVolumeSnapshotEncryptedReply encryptedReply = reply.castReply();
                                    EncryptFacadeResult<String> encrypt = encryptFacade.encrypt(encryptedReply.getEncrypt(), EncryptType.HMAC.toString());

                                    if (encrypt.getError() != null) {
                                        logger.error(String.format("encryption error : %s", encrypt.getError()));
                                        com.addError(encrypt.getError());
                                        com.allDone();
                                        return;
                                    }
                                    EncryptionIntegrityVO vo = new EncryptionIntegrityVO();
                                    vo.setResourceType(VolumeSnapshotVO.class.getSimpleName());
                                    vo.setResourceUuid(encryptedReply.getSnapshotUuid());
                                    vo.setSignedText(encrypt.getResult());
                                    dbf.persist(vo);
                                    com.done();
                                }
                            });

                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errorCodeList.getCauses().isEmpty()) {
                                    trigger.fail(errorCodeList.getCause());
                                    return;
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "start-imageCache-integrity";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<ImageCacheVO> cacheVOS = SQL.New("select cache from ImageCacheVO cache, " +
                                "PrimaryStorageVO storage where cache.primaryStorageUuid = storage.uuid " +
                                "and storage.type= 'LocalStorage'", ImageCacheVO.class).list();

                        new While<>(cacheVOS).each((vo, com) -> {
                            ImageCacheInventory inventory = ImageCacheInventory.valueOf(vo);
                            String[] pair = inventory.getInstallUrl().split(";");
                            String hostUuid = pair[1].replaceFirst("hostUuid://", "");

                            pluginRgty.getExtensionList(AfterCreateImageCacheExtensionPoint.class)
                                    .forEach(exp -> exp.saveEncryptAfterCreateImageCache(hostUuid, inventory));
                            com.done();

                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errorCodeList.getCauses().isEmpty()) {
                                    trigger.fail(errorCodeList.getCause());
                                    return;
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "start-zstack.properties-integrity";

                    @Override
                    public boolean skip(Map data) {
                        return CoreGlobalProperty.UNIT_TEST_ON;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        String cmd = "zstack-ctl set_properties_encrypt";

                        ShellResult result = ShellUtils.runAndReturn(cmd);
                        if (!result.isReturnCode(0)) {
                            trigger.fail(operr("fail to backup database: %s", result.getExecutionLog()));
                        }
                        trigger.next();
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        bus.reply(msg, reply);
                    }
                });

            }
        }).start();
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateEncryptKeyMsg) {
            handle((APIUpdateEncryptKeyMsg) msg);
        } else if (msg instanceof APIStartDataProtectionMsg) {
            handle((APIStartDataProtectionMsg) msg);
        } else if (msg instanceof APICheckBatchDataIntegrityMsg) {
            handle((APICheckBatchDataIntegrityMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APICheckBatchDataIntegrityMsg msg) {
        APICheckBatchDataIntegrityReply reply = new APICheckBatchDataIntegrityReply();

        Map<String, Boolean> map = new HashMap<>();

        SignedColumn signedColumn = signedList.stream().filter(signedColumn1 -> signedColumn1.getTableName().equals(msg.getResourceType())).findFirst().orElse(null);
        if (signedColumn == null) {
            throw new OperationFailureException(operr("This resourceType[%s] of data encryption does not exist", msg.getResourceType()));
        }

        for (String uuid : msg.getResourceUuids()) {
            String encryptDB = Q.New(EncryptionIntegrityVO.class)
                    .select(EncryptionIntegrityVO_.signedText)
                    .eq(EncryptionIntegrityVO_.resourceType, msg.getResourceType())
                    .eq(EncryptionIntegrityVO_.resourceUuid, uuid)
                    .findValue();
            if (encryptDB == null) {
                map.put(uuid, false);
                continue;
            }

            String valueSql = String.format("select %s from %s where %s = '%s'",
                    String.join(",", signedColumn.getSignedColumnNames()), msg.getResourceType(), signedColumn.getPrimaryKey(), uuid);
            Tuple tuple = SQL.New(valueSql, Tuple.class).find();
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < signedColumn.getSignedColumnNames().size(); i++) {
                sb.append(tuple.get(i));
            }
            
            EncryptFacadeResult<String> encrypt = encryptFacade.encrypt(sb.toString(), EncryptType.HMAC.toString());

            if (encrypt.getError() != null) {
                logger.error(String.format("encryption error : %s", encrypt.getError()));
                reply.setError(encrypt.getError());
                bus.reply(msg, reply);
                return;
            }

            map.put(uuid, encryptDB.equals(encrypt.getResult()));
        }
        reply.setResourceMap(map);
        bus.reply(msg, reply);
    }

    private void handle(APIStartDataProtectionMsg msg) {
        APIStartDataProtectionEvent event = new APIStartDataProtectionEvent(msg.getId());
        StartDataProtectionMsg dataProtectionMsg = new StartDataProtectionMsg();
        dataProtectionMsg.setEncryptType(msg.getEncryptType());
        bus.makeLocalServiceId(dataProtectionMsg, EncryptGlobalConfig.SERVICE_ID);
        bus.send(dataProtectionMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void encryptedAllDateIntegrity(FlowTrigger trigger) {
        List<ErrorCode> errorCodes = new ArrayList<>();

        for (SignedColumn signedColumn : signedList) {
            String selectColumn = String.join(", ", signedColumn.getSignedColumnNames());
            StringBuilder countSb = new StringBuilder(String.format("select count(1) from %s", signedColumn.getTableName()));
            StringBuilder selectSb = new StringBuilder(String.format("select %s, %s from %s",
                    signedColumn.getPrimaryKey(), selectColumn, signedColumn.getTableName()));
            StringBuilder whereSb = new StringBuilder();

            if (signedColumn.getAppointMap() != null && !signedColumn.getAppointMap().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                whereSb.append(" where ");
                signedColumn.getAppointMap().forEach((appointName, appointValue) -> {
                    if (sb.length() > 0) {
                        sb.append(" and ");
                    }
                    sb.append(String.format("%s = '%s'", appointName, appointValue));
                });
                whereSb.append(sb);
            }

            if (whereSb.length() > 0) {
                countSb.append(whereSb);
                selectSb.append(whereSb);
            }

            long count = SQL.New(countSb.toString(), Long.class).find();
            SQL.New(selectSb.toString(), Tuple.class).limit(1000).paginate(count, (List<Tuple> tuples, PaginateCompletion paginateCompletion) -> {
                new While<>(tuples).each((tuple, innerWhileCompletion) -> {
                    String resourceUuid = tuple.get(0) instanceof Long ? Long.toString((Long) tuple.get(0)) : (String) tuple.get(0);
                    String encryotValue = "";
                    for (int i = 1; i < signedColumn.getSignedColumnNames().size() + 1; i++) {
                        encryotValue += (String) tuple.get(i);
                    }

                    EncryptFacadeResult result = encryptFacade.encrypt(encryotValue, EncryptType.HMAC.toString());
                    if (result.getError() != null) {
                        logger.error(String.format("encryption error: %s", result.getError()));
                        innerWhileCompletion.addError(result.getError());
                        innerWhileCompletion.allDone();
                        return;
                    }

                    EncryptionIntegrityVO integrityVO = new EncryptionIntegrityVO();
                    integrityVO.setResourceType(signedColumn.getTableName());
                    integrityVO.setSignedText(result.getResult().toString());
                    integrityVO.setResourceUuid(resourceUuid);
                    dbf.persist(integrityVO);
                    innerWhileCompletion.done();

                }).run(new WhileDoneCompletion(paginateCompletion) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            errorCodes.addAll(errorCodeList.getCauses());
                            logger.error(String.format("encryption error: %s", errorCodeList.getCause()));
                            paginateCompletion.allDone();
                            return;
                        }
                        paginateCompletion.done();
                    }
                });
            }, new NopeNoErrorCompletion());
        }

        if (!errorCodes.isEmpty()) {
            trigger.fail(errorCodes.get(0));
            return;
        }

        trigger.next();
    }

    @Transactional
    private void encryptAllConfidentiality(String encryptType, Completion completion) {
        Set<Field> encryptedFields = encryptFacade.encryptedFields;

        if (PasswordEncryptType.LocalEncryption.toString().equals(EncryptGlobalConfig.ENABLE_PASSWORD_ENCRYPT.value(String.class))) {
            decryptAllPassword(encryptedFields);
        }

        EncryptGlobalConfig.ENCRYPT_DRIVER.updateValue(encryptType);
        encryptAllPassword(encryptedFields, completion);
    }

    private void encryptAllPassword(Set<Field> encryptedFields, Completion completion) {
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

                            if (value == null) {
                                continue;
                            }

                            try {
                                EncryptFacadeResult<String> encrypt = encryptFacade.encrypt(value, EncryptType.SM4.toString());
                                if (encrypt.getError() != null) {
                                    logger.error(String.format("encryption error : %s", encrypt.getError()));
                                    completion.fail(encrypt.getError());
                                    return;
                                }

                                String sql = String.format("update %s set %s = :encrypted where uuid = :uuid", className, field.getName());

                                Query query = dbf.getEntityManager().createQuery(sql);
                                query.setParameter("encrypted", encrypt.getResult());
                                query.setParameter("uuid", uuid);
                                query.executeUpdate();
                            } catch (Exception e) {
                                logger.debug(String.format("encrypt error because : %s", e.getMessage()));
                                completion.fail(operr("operation error, because:%s", e.getMessage()));
                                return;
                            }
                        }
                    }
                }

                UpdateQuery.New(GlobalConfigVO.class)
                        .eq(GlobalConfigVO_.category, EncryptGlobalConfig.CATEGORY)
                        .eq(GlobalConfigVO_.name, EncryptGlobalConfig.ENABLE_PASSWORD_ENCRYPT.getName())
                        .set(GlobalConfigVO_.value, PasswordEncryptType.ScurityResoueceEncryption.toString())
                        .update();

                completion.success();
            }
        }.execute();
    }
    private void decryptAllPassword(Set<Field> encryptedFields) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                for (Field field : encryptedFields) {
                    String className = field.getDeclaringClass().getSimpleName();

                    List<String> uuids = sql(String.format("select uuid from %s", className)).list();

                    for (String uuid : uuids) {
                        String encryptedString = sql(String.format("select %s from %s where uuid = '%s'", field.getName(), className, uuid)).find();

                        try {
                            String decryptString = encryptFacade.decrypt(encryptedString);

                            String sql = String.format("update %s set %s = :decrypted where uuid = :uuid", className, field.getName());

                            Query query = dbf.getEntityManager().createQuery(sql);
                            query.setParameter("decrypted", decryptString);
                            query.setParameter("uuid", uuid);
                            query.executeUpdate();
                        } catch (Exception e) {
                            logger.debug(String.format("decrypt password error because : %s",e.getMessage()));
                            return;
                        }
                    }
                }
            }
        }.execute();
    }


    @Transactional
    private void handle(APIUpdateEncryptKeyMsg msg){
        Set<Method> map = Platform.encryptedMethodsMap;
        logger.debug("decrypt passwords with old key and encrypt with new key");

        EncryptRSA rsa = new EncryptRSA();

        for (Method method: map) {
            String old_key = EncryptGlobalConfig.ENCRYPT_ALGORITHM.value();
            String new_key = msg.getEncryptKey();

            Class tempClass = method.getDeclaringClass();
            String className = tempClass.getSimpleName();
            String paramName = "password";

            String sql1 = "select uuid from "+className;
            Query q1 = dbf.getEntityManager().createNativeQuery(sql1);
            List uuidList = q1.getResultList();

            for (int i=0; i<uuidList.size(); i++){
                String sql2 = "select "+paramName+" from "+className+" where uuid = \""+uuidList.get(i)+"\"";
                Query q2 = dbf.getEntityManager().createNativeQuery(sql2);
                String preEncrypttxt = q2.getResultList().get(0).toString();
                try {

                    String password = (String) rsa.decrypt1(preEncrypttxt);
                    String newencrypttxt = (String) rsa.encrypt(password,msg.getEncryptKey());
                    String sql3 = "update "+className+" set "+paramName+" = :newencrypttxt where uuid = :uuid";

                    Query query = dbf.getEntityManager().createQuery(sql3);
                    query.setParameter("newencrypttxt",newencrypttxt);
                    query.setParameter("uuid",uuidList.get(i));

                    query.executeUpdate();

                }catch (Exception e){
                    logger.debug("sql exec error");
                    logger.debug(String.format("error is : %s",e.getMessage()));
                    e.printStackTrace();
                }

            }
        }
        try {
            rsa.updateKey(msg.getEncryptKey());
        }catch (Exception e){
            logger.debug("update key in encryptrsa error");
            logger.debug(String.format("error is : %s",e.getMessage()));
            e.printStackTrace();
        }

        APIUpdateEncryptKeyEvent evt = new APIUpdateEncryptKeyEvent(msg.getId());
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(EncryptGlobalConfig.SERVICE_ID);
    }
}
