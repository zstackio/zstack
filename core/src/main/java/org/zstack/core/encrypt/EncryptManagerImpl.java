package org.zstack.core.encrypt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.core.workflow.ShareFlowChain;
import org.zstack.header.AbstractService;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.encrypt.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.snapshot.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.Tuple;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

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
    private EncryptDriver encryptDriver;

    public static Map<String, SignedColumn> signedMap = new HashMap<>();

    private static Map<String, SignedColumn> getAllSignedIntegrity() {
        Set<Field> fields = Platform.getReflections().getFieldsAnnotatedWith(SignedText.class);
        Map<String, SignedColumn> signedColumnMap = new HashMap<>();
        for (Field field : fields) {
            SignedColumn signedColumn = new SignedColumn();
            signedColumn.setSignedColumnNames(Arrays.stream(field.getAnnotation(SignedText.class).signedColumnName()).collect(Collectors.toList()));

            AppointColumn[] appoints = field.getAnnotationsByType(AppointColumn.class);
            for (AppointColumn appoint : appoints) {
                Map<String, String> appointColumn = new HashMap<>();
                appointColumn.put(appoint.column(), appoint.vaule());

                signedColumn.setAppointMap(appointColumn);
            }
            signedColumnMap.put(field.getAnnotation(SignedText.class).tableName(), signedColumn);
        }
        return signedColumnMap;
    }


    @Override
    public boolean start() {
        signedMap = getAllSignedIntegrity();
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
         } else {
             bus.dealWithUnknownMessage(msg);
         }

    }

    private void handle(StartDataProtectionMsg msg) {
        StartDataProtectionReply reply = new StartDataProtectionReply();
        FlowChain chain = new ShareFlowChain();
        chain.setName("start-data-protection");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                String __name__ = "start-encrypted-Confidentiality";
                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        encryptAllConfidentiality(msg.getEncryptType());
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "start-encrypted-audits-and-globalconfig-integrity";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        encryptedAllDateIntegrity();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "start-image-integrity";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> imageUuids = Q.New(ImageVO.class).select(ImageVO_.uuid).listValues();

                        new While<>(imageUuids).each((uuid, completion) -> {
                            GetImageEncryptedMsg encryptedMsg = new GetImageEncryptedMsg();
                            encryptedMsg.setImageUuid(uuid);
                            bus.makeTargetServiceIdByResourceUuid(encryptedMsg, ImageConstant.SERVICE_ID, uuid);
                            bus.send(encryptedMsg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        completion.addError(reply.getError());
                                        completion.done();
                                        return;
                                    }
                                    GetImageEncryptedReply encryptedReply = reply.castReply();
                                    EncryptionIntegrityVO vo = new EncryptionIntegrityVO();
                                    vo.setResourceType(ImageVO.class.getSimpleName());
                                    vo.setResourceUuid(encryptedReply.getImageUuid());
                                    //todo integrity
                                    vo.setSignedText(encryptDriver.encrypt(encryptedReply.getEncrypted()));
                                    dbf.persist(vo);
                                    completion.done();
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
                            bus.send(msg, new CloudBusCallBack(com) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        com.addError(reply.getError());
                                        com.done();
                                        return;
                                    }

                                    GetVolumeSnapshotEncryptedReply encryptedReply = reply.castReply();
                                    EncryptionIntegrityVO vo = new EncryptionIntegrityVO();
                                    vo.setResourceType(ImageVO.class.getSimpleName());
                                    vo.setResourceUuid(encryptedReply.getSnapshotUuid());
                                    //todo integrity
                                    vo.setSignedText(encryptDriver.encrypt(encryptedReply.getEncrypt()));
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
        SignedColumn signedColumn = signedMap.get(msg.getResourceType());
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

            String valueSql = String.format("select %s from %s where uuid = '%s'",
                    String.join(",", signedColumn.getSignedColumnNames()), msg.getResourceType(), uuid);
            Tuple tuple = SQL.New(valueSql + String.format(" and uuid = '%s'", uuid), Tuple.class).find();
            String encryotValue = String.join("_", (CharSequence) Arrays.asList(tuple.toArray()));
            String encryptedString = encryptDriver.encrypt(encryotValue);

            map.put(uuid, encryptDB.equals(encryptedString));
        }
        reply.setResourceMap(map);
        bus.reply(msg, reply);

    }

    private void handle(APIStartDataProtectionMsg msg) {
        APIStartDataProtectionEvent event = new APIStartDataProtectionEvent(msg.getId());
        StartDataProtectionMsg dataProtectionMsg = new StartDataProtectionMsg();
        dataProtectionMsg.setEncryptType(msg.getEncryptType());
        bus.makeLocalServiceId(msg, EncryptGlobalConfig.SERVICE_ID);
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

    private void encryptedAllDateIntegrity() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                signedMap.forEach((tableName, signedColumn) -> {
                    String sql = String.format("select uuid from %s ", tableName);

                    String splicingSql = null;
                    if (!signedColumn.getAppointMap().isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        splicingSql = " where";
                        signedColumn.getAppointMap().forEach((appointName, appointValue) -> {
                            if (sb.length() > 0) {
                                sb.append(" and ");
                            }
                           sb.append(String.format("%s = '%s'", appointName, appointValue));
                        });
                        splicingSql += sb.toString();
                    }

                    if (splicingSql != null) {
                        sql += splicingSql;
                    }
                    List<String> uuids = sql(sql).list();
                    for (String uuid : uuids) {
                        String valueSql = String.format("select %s from %s ",
                                String.join(",", signedColumn.getSignedColumnNames()), tableName);
                        if (splicingSql != null) {
                            valueSql += splicingSql;
                        }
                        Tuple tuple = sql(valueSql + String.format(" and uuid = '%s'", uuid), Tuple.class).find();
                        String encryotValue = String.join("_", (CharSequence) Arrays.asList(tuple.toArray()));

                        try {
                            //todo integrity
                            String encryptedString = encryptDriver.encrypt(encryotValue);
                            String updateSql = String.format("insert into %s (resourceUuid, resourceType, signedText) values('%s', '%s', '%s')",
                                    EncryptionIntegrityVO.class.getSimpleName() , uuid, tableName, encryptedString);
                            Query query = dbf.getEntityManager().createQuery(updateSql);
                            query.executeUpdate();
                        } catch (Exception e) {
                            logger.debug(String.format("encrypt error because : %s",e.getMessage()));
                        }
                    }

                });
            }
        }.execute();
    }

    @Transactional
    private void encryptAllConfidentiality(String encryptType) {
        Set<Field> encryptedFields = encryptFacade.encryptedFields;

        if (EncryptGlobalConfig.ENABLE_PASSWORD_ENCRYPT.value(Integer.class) == 1) {
            decryptAllPassword(encryptedFields);
        }

        EncryptGlobalConfig.ENCRYPT_DRIVER.updateValue(encryptType);
        encryptAllPassword(encryptedFields);
    }

    private void encryptAllPassword(Set<Field> encryptedFields) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                for (Field field : encryptedFields) {
                    String className = field.getDeclaringClass().getSimpleName();

                    List<String> uuids = sql(String.format("select uuid from %s", className)).list();

                    for (String uuid : uuids) {
                        String value = sql(String.format("select %s from %s where uuid = '%s'", field.getName(), className, uuid)).find();

                        try {
                            String encryptedString = encryptDriver.encrypt(value);

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
                            String decryptString = encryptDriver.decrypt(encryptedString);

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
