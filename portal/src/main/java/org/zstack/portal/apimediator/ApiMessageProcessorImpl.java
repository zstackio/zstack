package org.zstack.portal.apimediator;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor.InterceptorPosition;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.*;
import org.zstack.portal.apimediator.schema.Service;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:46 PM
 * To change this template use File | Settings | File Templates.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ApiMessageProcessorImpl implements ApiMessageProcessor {
    private static CLogger logger = Utils.getLogger(ApiMessageProcessorImpl.class);
    private Map<Class, ApiMessageDescriptor> descriptors = new HashMap<Class, ApiMessageDescriptor>();
    private Map<Class, Set<GlobalApiMessageInterceptor>> globalInterceptors = new HashMap<Class, Set<GlobalApiMessageInterceptor>>();
    private Set<GlobalApiMessageInterceptor> globalInterceptorsForAllMsg = new HashSet<GlobalApiMessageInterceptor>();
    private Comparator<ApiMessageInterceptor> msgInterceptorComparator = Comparator
            .comparingInt(ApiMessageProcessorImpl::interceptorPositionToOrder)
            .thenComparing(ApiMessageInterceptor::getPriority);

    private static int interceptorPositionToOrder(ApiMessageInterceptor interceptor) {
        if (interceptor instanceof GlobalApiMessageInterceptor) {
            return ((GlobalApiMessageInterceptor) interceptor).getPosition().ordinal();
        }

        return InterceptorPosition.DEFAULT.ordinal();
    }

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private boolean unitTestOn;
    //    add switch to skip api check for minimal
    private boolean minimalOn;
    private List<String> configFolders;
    List<String> supportApis;

    private void dump() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Class, ApiMessageDescriptor> e : descriptors.entrySet()) {
            ApiMessageDescriptor desc = e.getValue();
            sb.append("\n-------------------------------------------");
            sb.append(String.format("\nname: %s", desc.getName()));
            sb.append(String.format("\nconfigured service id: %s", desc.getServiceId()));
            sb.append(String.format("\nconfig path: %s", desc.getConfigPath()));
            List<String> inc = new ArrayList<String>();
            for (ApiMessageInterceptor ic : desc.getInterceptors()) {
                inc.add(ic.getClass().getName());
            }
            sb.append(String.format("\ninterceptors: %s", inc));
            sb.append("\n-------------------------------------------");
        }

        logger.debug(String.format("ApiMessageDescriptor dump:\n%s", sb.toString()));
    }

    public ApiMessageProcessorImpl(Map<String, Object> config) {
        this.unitTestOn = CoreGlobalProperty.UNIT_TEST_ON;
        this.minimalOn = Platform.isMinimalOn();
        this.configFolders = (List <String>)config.get("serviceConfigFolders");
        this.supportApis = new ArrayList<>();

        populateGlobalInterceptors();

        try {
            JAXBContext context = JAXBContext.newInstance("org.zstack.portal.apimediator.schema");
            List<String> paths = new ArrayList<String>();
            for (String configFolder : this.configFolders) {
                paths.addAll(PathUtil.scanFolderOnClassPath(configFolder));
            }

            for (String p : paths) {
                if (!p.endsWith(".xml")) {
                    logger.warn(String.format("ignore %s which is not ending with .xml", p));
                    continue;
                }

                File cfg = new File(p);
                Unmarshaller unmarshaller = context.createUnmarshaller();
                Service schema = (Service) unmarshaller.unmarshal(cfg);
                createDescriptor(schema, cfg.getAbsolutePath());
            }

            if (!this.unitTestOn) {
                dump();
            }
        } catch (JAXBException e) {
            throw new CloudRuntimeException(e);
        }
    }

    private void prepareInterceptors(ApiMessageDescriptor desc, Service.Message mschema, Service schema) {
        ComponentLoader loader = Platform.getComponentLoader();
        List<ApiMessageInterceptor> interceptors = new ArrayList<ApiMessageInterceptor>();
        List<String> icNames = new ArrayList<String>();
        icNames.addAll(mschema.getInterceptor());
        icNames.addAll(schema.getInterceptor());
        for (String name : icNames) {
            try {
                ApiMessageInterceptor ic = loader.getComponentByBeanName(name);
                interceptors.add(ic);
            } catch (NoSuchBeanDefinitionException ne) {
                if (this.minimalOn) {
                    continue;
                }
                if (!this.unitTestOn) {
                    throw new CloudRuntimeException(String.format("Cannot find ApiMessageInterceptor[%s] for message[%s] described in %s. Make sure the ApiMessageInterceptor is configured in spring bean xml file", name, desc.getName(), desc.getConfigPath()), ne);
                }
            }
        }

        Set<GlobalApiMessageInterceptor> gis = new HashSet<GlobalApiMessageInterceptor>();
        for (Map.Entry<Class, Set<GlobalApiMessageInterceptor>> e : globalInterceptors.entrySet()) {
            Class baseMsgClz = e.getKey();
            if (baseMsgClz.isAssignableFrom(desc.getClazz())) {
                gis.addAll(e.getValue());
            }
        }

        List<ApiMessageInterceptor> globalInterceptors = new ArrayList<>();

        for (GlobalApiMessageInterceptor gi : gis) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("install GlobalApiMessageInterceptor[%s] to message[%s]", gi.getClass().getName(), desc.getClazz().getName()));
            }
            globalInterceptors.add(gi);
        }

        for (GlobalApiMessageInterceptor gi : globalInterceptorsForAllMsg) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("install GlobalApiMessageInterceptor[%s] to message[%s]", gi.getClass().getName(), desc.getClazz().getName()));
            }
            globalInterceptors.add(gi);
        }

        globalInterceptors.addAll(interceptors);
        globalInterceptors.sort(this.msgInterceptorComparator);

        desc.setInterceptors(globalInterceptors);
    }


    private void createDescriptor(Service schema, String cfgPath) {
        for (Service.Message mschema : schema.getMessage()) {
            Class msgClz = null;
            try {
                msgClz = Class.forName(mschema.getName());
            } catch (ClassNotFoundException e) {
                String err = String.format("unable to create ApiMessageDescriptor for message[name:%s, path:%s]", mschema.getName(), cfgPath);
                throw new CloudRuntimeException(err, e);
            }

            ApiMessageDescriptor old = descriptors.get(msgClz);
            if (old != null) {
                throw new CloudRuntimeException(String.format("Duplicate message description. Message[%s] is described in %s and %s", mschema.getName(), old.getConfigPath(), cfgPath));
            }

            ApiMessageDescriptor desc = new ApiMessageDescriptor();
            desc.setName(mschema.getName());
            String serviceId = mschema.getServiceId() != null ? mschema.getServiceId() : schema.getId();
            desc.setServiceId(serviceId);
            desc.setConfigPath(cfgPath);
            desc.setClazz(msgClz);

            prepareInterceptors(desc, mschema, schema);
            List<org.zstack.header.Service> services = pluginRgty.getExtensionList(org.zstack.header.Service.class);
            if (services.stream().anyMatch(it -> bus.makeLocalServiceId(desc.getServiceId()).equals(it.getId()))) {
                supportApis.add(desc.getClazz().getSimpleName());
            }
            buildApiParams(desc);

            descriptors.put(msgClz, desc);
        }
    }

    private void buildApiParams(ApiMessageDescriptor desc) {
        Class msgClz = desc.getClazz();
        List<Field> fields = FieldUtils.getAllFields(msgClz);

        class FP {
            Field field;
            APIParam param;
        }

        Map<String, FP> fmap = new HashMap<String, FP>();
        for (Field f : fields) {
            APIParam at = f.getAnnotation(APIParam.class);
            if (at == null) {
                continue;
            }

            FP fp = new FP();
            fp.field = f;
            fp.param = f.getAnnotation(APIParam.class);
            fmap.put(f.getName(), fp);
        }

        OverriddenApiParams at = desc.getClazz().getAnnotation(OverriddenApiParams.class);
        if (at != null) {
            for (OverriddenApiParam atp : at.value()) {
                Field f = FieldUtils.getField(atp.field(), msgClz);
                if (f == null) {
                    throw new CloudRuntimeException(String.format("cannot find the field[%s] specified in @OverriddenApiParam of class[%s]",
                            atp.field(), msgClz));
                }

                FP fp = new FP();
                fp.field = f;
                fp.param = atp.param();
                fmap.put(atp.field(), fp);
            }
        }

        for (FP fp : fmap.values()) {
            desc.getFieldApiParams().put(fp.field, fp.param);
        }
    }

    @Override
    public APIMessage process(APIMessage msg) throws ApiMessageInterceptionException {
        ApiMessageDescriptor desc = descriptors.get(msg.getClass());
        if (desc == null) {
            throw new CloudRuntimeException(String.format("Message[%s] has no ApiMessageDescriptor", msg.getClass().getName()));
        }

        for (ApiMessageInterceptor ic : desc.getInterceptors()) {
            msg = ic.intercept(msg);
        }

        return msg;
    }

    @Override
    public ApiMessageDescriptor getApiMessageDescriptor(APIMessage msg) {
        return descriptors.get(msg.getClass());
    }

    @Override
    public List<String> getSupportApis() {
        return supportApis;
    }

    private void populateGlobalInterceptors() {
        for (GlobalApiMessageInterceptor gi : pluginRgty.getExtensionList(GlobalApiMessageInterceptor.class)) {
            if (gi.getMessageClassToIntercept() == null) {
                globalInterceptorsForAllMsg.add(gi);
            } else {
                for (Class msgClz : gi.getMessageClassToIntercept()) {
                    Set<GlobalApiMessageInterceptor> gis = globalInterceptors.get(msgClz);
                    if (gis == null) {
                        gis = new HashSet<GlobalApiMessageInterceptor>();
                        globalInterceptors.put(msgClz, gis);
                    }
                    gis.add(gi);
                }
            }
        }
    }
}
