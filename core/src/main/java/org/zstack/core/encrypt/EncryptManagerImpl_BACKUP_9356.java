package org.zstack.core.encrypt;

<<<<<<< HEAD
import org.apache.commons.codec.binary.Base64;
import org.dom4j.io.STAXEventReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
=======
import org.reflections.Reflections;
import org.reflections.scanners.*;
import org.zstack.core.db.SQL;
import org.reflections.util.ClasspathHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
>>>>>>> 179716a... encrypt feature fix sql platform exception
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.encrypt.APIUpdateEncryptKeyEvent;
import org.zstack.header.core.encrypt.APIUpdateEncryptKeyMsg;
import org.zstack.header.core.encrypt.ENCRYPT;
<<<<<<< HEAD
import org.zstack.header.core.encrypt.ENCRYPTParam;
=======
>>>>>>> 179716a... encrypt feature fix sql platform exception
import org.zstack.header.message.Message;
import org.zstack.header.volume.VolumeType;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
<<<<<<< HEAD
import java.security.Key;
=======
>>>>>>> 179716a... encrypt feature fix sql platform exception
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;


/**
 * Created by mingjian.deng on 16/12/28.
 */
public class EncryptManagerImpl extends AbstractService {
    private static final CLogger logger = Utils.getLogger(EncryptManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    private static Reflections reflections;

    static {
        try {
            reflections = new Reflections(ClasspathHelper.forPackage("org.zstack"),
                    new SubTypesScanner(), new MethodAnnotationsScanner(), new FieldAnnotationsScanner(),
                    new MemberUsageScanner(), new MethodParameterNamesScanner(), new ResourcesScanner(),
                    new TypeAnnotationsScanner(), new TypeElementsScanner());
        }catch (Throwable e) {
            logger.warn(String.format("unhandled exception when in EncryptManagerImpl's static block, %s", e.getMessage()), e);
        }
    }


    @Autowired
    private DatabaseFacade dbf;

    @Override
    public boolean start() {

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIUpdateEncryptKeyMsg) {
            handle((APIUpdateEncryptKeyMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Transactional
    private void handle(APIUpdateEncryptKeyMsg msg){
<<<<<<< HEAD
        Set<Method> map = Platform.encryptedMethodsMap;
        logger.debug("decrypt passwords with old key and encrypt with new key");

=======
        Set<Method> map = getAllEncryptPassword();
        logger.debug("decrypt passwords with old key and encrypt with new key");
>>>>>>> 179716a... encrypt feature fix sql platform exception
        EncryptRSA rsa = new EncryptRSA();

        for (Method method: map) {
            String old_key = EncryptGlobalConfig.ENCRYPT_ALGORITHM.value();
            String new_key = msg.getEncryptKey();

            Class tempClass = method.getDeclaringClass();
            String className = tempClass.getSimpleName();
            String paramName = "password";

            logger.debug(String.format("className is : %s",className));
            logger.debug(String.format("paramName is: %s ",paramName));

<<<<<<< HEAD
            String sql1 = "select uuid from "+className;
            logger.debug(String.format("sql1 is: %s ",sql1));
            Query q1 = dbf.getEntityManager().createNativeQuery(sql1);
            List uuidList = q1.getResultList();

            for (int i=0; i<uuidList.size(); i++){
                String sql2 = "select "+paramName+" from "+className+" where uuid = \""+uuidList.get(i)+"\"";
                logger.debug(String.format("sql2 is: %s ",sql2));
                Query q2 = dbf.getEntityManager().createNativeQuery(sql2);
                String preEncrypttxt = q2.getResultList().get(0).toString();
=======
            List uuidList = SQL.New(String.format("select uuid from %s ",className)).list();

            for (int i=0; i<uuidList.size(); i++){

                String preEncrypttxt = SQL.New(String.format("select %s from %s where uuid = :uuid ",paramName,className)).param("uuid",uuidList.get(i)).find();
>>>>>>> 179716a... encrypt feature fix sql platform exception
                logger.debug(String.format("preEncrypttxt is: %s ",preEncrypttxt));
                try {

                    String password = (String) rsa.decrypt1(preEncrypttxt);
<<<<<<< HEAD
                   //rsa.updateKey(msg.getEncryptKey());
                    String newencrypttxt = (String) rsa.encrypt(password,msg.getEncryptKey());

                    logger.debug(String.format("new encrypt text is: %s",newencrypttxt));
                    String sql3 = "update "+className+" set "+paramName+" = :newencrypttxt where uuid = :uuid";

                    Query query = dbf.getEntityManager().createQuery(sql3);
                    query.setParameter("newencrypttxt",newencrypttxt);
                    query.setParameter("uuid",uuidList.get(i));

                    query.executeUpdate();

                    /*String sql4 = "select "+paramName+" from "+className+" where uuid = \""+uuidList.get(i)+"\"";
                    logger.debug(String.format("sql2 is: %s ",sql4));
                    Query q4 = dbf.getEntityManager().createNativeQuery(sql4);
                    String updateEncrypt = q4.getResultList().get(0).toString();
=======
                    String newencrypttxt = (String) rsa.encrypt(password,msg.getEncryptKey());
                    logger.debug(String.format("new encrypt text is: %s",newencrypttxt));

                    SQL.New(String.format("update %s set %s = :newencrypttxt where uuid = :uuid", className, paramName)).param("newencrypttxt", newencrypttxt).param("uuid", uuidList.get(i)).execute();

                    /*String updateEncrypt = SQL.New(String.format("select %s from %s where uuid = :uuid",paramName,className)).param("uuid",uuidList.get(i)).find();
>>>>>>> 179716a... encrypt feature fix sql platform exception
                    String updateEncryptdecrypt = (String)rsa.decrypt(updateEncrypt,msg.getEncryptKey());
                    logger.debug(String.format("updateEncrypt is: %s ",updateEncrypt));
                    logger.debug(String.format("updateEncryptdecrypt is: %s ",updateEncryptdecrypt));*/

                }catch (Exception e){
<<<<<<< HEAD
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
=======
                    logger.debug("sql exec error", e);
                }

            }
        }
        try {
            rsa.updateKey(msg.getEncryptKey());
        }catch (Exception e){
            logger.debug("update key in encryptrsa error", e);
        }

        APIUpdateEncryptKeyEvent evt = new APIUpdateEncryptKeyEvent(msg.getId());
        bus.publish(evt);
    }

    private static Set<Method> getAllEncryptPassword() {
        Set<Method> encrypteds = reflections.getMethodsAnnotatedWith(ENCRYPT.class);
        for (Method encrypted: encrypteds) {
            logger.debug(String.format("found encrypted method[%s:%s]", encrypted.getDeclaringClass(), encrypted.getName()));
        }
        return encrypteds;
>>>>>>> 179716a... encrypt feature fix sql platform exception
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(EncryptGlobalConfig.SERVICE_ID);
    }
}
