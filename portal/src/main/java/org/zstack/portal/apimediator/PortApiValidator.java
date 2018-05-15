package org.zstack.portal.apimediator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.ApiMessageValidator;
import org.zstack.header.rest.RestRequest;
import org.zstack.utils.DebugUtils;

import javax.persistence.TypedQuery;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PortApiValidator implements ApiMessageValidator {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public void validate(APIMessage msg, Field f, Object value, APIParam at) {
        if (value != null && at.resourceType() != Object.class) {
            if (value instanceof Collection) {
                final Collection col = (Collection) value;
                if (!col.isEmpty()) {
                    List<String> uuids = new SQLBatchWithReturn<List<String>>() {
                        @Override
                        protected List<String> scripts() {
                            String sql = String.format("select e.uuid from %s e where e.uuid in (:uuids)", at.resourceType().getSimpleName());
                            TypedQuery<String> q = databaseFacade.getEntityManager().createQuery(sql, String.class);
                            q.setParameter("uuids", col);
                            return q.getResultList();
                        }
                    }.execute();

                    if (uuids.size() != col.size()) {
                        List<String> invalids = new ArrayList<>();
                        for (Object o : col) {
                            String uuid = (String) o;
                            if (!uuids.contains(uuid)) {
                                invalids.add(uuid);
                            }
                        }

                        if (!invalids.isEmpty()) {
                            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.RESOURCE_NOT_FOUND,
                                    String.format("invalid field[%s], resource[uuids:%s, type:%s] not found", f.getName(), invalids, at.resourceType().getSimpleName())
                            ));
                        }
                    }
                }

            } else {
                DebugUtils.Assert(String.class.isAssignableFrom(f.getType()), String.format("field[%s] of message[%s] has APIParam.resourceType specified, then the field must be uuid which is a String, but actual is %s",
                        f.getName(), msg.getClass().getName(), f.getType()));

                if (!dbf.isExist(value, at.resourceType())) {
                    if (at.successIfResourceNotExisting()) {
                        RestRequest rat = msg.getClass().getAnnotation(RestRequest.class);
                        if (rat == null) {
                            throw new CloudRuntimeException(String.format("the API class[%s] does not have @RestRequest but it uses a successIfResourceNotExisting helper", msg.getClass()));
                        }

                        Pattern p = Pattern.compile("[0-9a-f]{8}[0-9a-f]{4}[1-5][0-9a-f]{3}[89ab][0-9a-f]{3}[0-9a-f]{12}");
                        Matcher mt = p.matcher(value.toString());
                        if (!mt.matches()){
                            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.RESOURCE_NOT_FOUND,
                                    String.format("invalid value[%s] of field [%s]", value, f.getName())));
                        }

                        APIEvent evt;
                        try {
                            evt = (APIEvent) rat.responseClass().getConstructor(String.class).newInstance(msg.getId());
                        } catch (Exception e) {
                            throw new CloudRuntimeException(e);
                        }

                        bus.publish(evt);
                        throw new StopRoutingException();
                    } else {
                        throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.RESOURCE_NOT_FOUND,
                                String.format("invalid field[%s], resource[uuid:%s, type:%s] not found", f.getName(), value, at.resourceType().getSimpleName())
                        ));
                    }
                }
            }
        }
    }
}
