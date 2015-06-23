package org.zstack.core.componentloader;

import org.zstack.core.Platform;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import java.util.*;


public class PluginRegistryImpl implements PluginRegistryIN {
    private static final CLogger logger = CLoggerImpl.getLogger(PluginRegistryImpl.class);
    private Map<String, List<PluginExtension>> extensions = new HashMap<String, List<PluginExtension>>();
    private Map<String, List<PluginExtension>> extensionsByInterfaceName = new HashMap<String, List<PluginExtension>>();
    private Map<Class, List> extensionsByInterfaceClass = new HashMap<Class, List>();

    private void sortPlugins() {
        for (List<PluginExtension> exts : extensionsByInterfaceName.values()) {
            Collections.sort(exts, new Comparator<PluginExtension>() {
                @Override
                public int compare(PluginExtension o1, PluginExtension o2) {
                    // greater order means the position is more proceeding in plugin list
                    return o2.getOrder() - o1.getOrder();
                }
            });
        }
    }
    
    private void buildPluginTree() {
        ComponentLoader loader = Platform.getComponentLoader();
        for (Map.Entry<String, List<PluginExtension>> entry : extensions.entrySet()) {
            for (PluginExtension ext : entry.getValue()) {
                /*
                 * If instance-id is specified in extension declaration, find
                 * that bean used as extension implementation. Otherwise, use
                 * parent plugin as implementation
                 */
                try {
                	Class<?> interfaceClass = Class.forName(ext.getReferenceInterface());
                	Object instance = null;
                    if (!"".equals(ext.getInstanceId())) {
                        instance = loader.getComponentByBeanName(ext.getInstanceId());
                    } else {
                    	instance = loader.getComponentByBeanName(ext.getBeanName());
                    }
                    ext.setInstance(instance);
                    
                    if (!interfaceClass.isInstance(ext.getInstance())) {
                    	throw new IllegalArgumentException(String.format("%s is not instance of interface %s", ext.getInstance().getClass().getCanonicalName(), interfaceClass.getCanonicalName()));
                    }
                    
                    List<PluginExtension> exts = extensionsByInterfaceName.get(ext.getReferenceInterface());
                    if (exts == null) {
                    	exts = new ArrayList<PluginExtension>(1);
                    }
                    exts.add(ext);
                    extensionsByInterfaceName.put(ext.getReferenceInterface(), exts);
                } catch (Exception e) {
                    logger.warn(String.format("%s, mark extension referred to interface [%s] in bean[name=%s, class=%s] as invalid. Checking the bean XML file to fix it", e.getMessage(), ext.getReferenceInterface(), ext.getBeanName(), ext.getBeanClassName()), e);
                }
            }
        }
    }

    @Override
    public void initialize() {
        buildPluginTree();
        sortPlugins();
        createClassPluginInstanceMap();
        logger.info("Plugin system has been initialized successfully");
    }

    private void createClassPluginInstanceMap() {
        for (Map.Entry<String, List<PluginExtension>> e : extensionsByInterfaceName.entrySet()) {
            String className = e.getKey();
            List<PluginExtension> exts = e.getValue();

            try {
                Class clazz = Class.forName(className);
                List instances = new ArrayList();
                for (PluginExtension ext : exts) {
                    if (!instances.contains(ext.getInstance())) {
                        instances.add(ext.getInstance());
                    }
                }
                extensionsByInterfaceClass.put(clazz, instances);
            } catch (Exception ex) {
                throw new CloudRuntimeException(ex);
            }
        }
    }


    @Override
    public void processExtensions(List<PluginExtension> exts, ExtensionProcessor processor, Object[] args) {
        for (PluginExtension ext : exts) {
            try {
                processor.process(ext, args);
            } catch (Exception e) {
                logger.warn("Unhandled exception happened while " + processor.getClass().getCanonicalName() + " process extesnion " + ext.getReferenceInterface() + " implemented by:" + ext.getInstance().getClass().getCanonicalName(), e);
            }
        }
    }

    @Override
    public <T> List<T> getExtensionList(Class<T> clazz) {
        List<T> exts = extensionsByInterfaceClass.get(clazz);
        return exts == null ? new ArrayList<T>() : exts;
    }

    @Override
    public List<PluginExtension> getExtensionByInterfaceName(String interfaceName) {
		List<PluginExtension> exts = extensionsByInterfaceName.get(interfaceName);
		if (exts == null) {
			exts = new ArrayList<PluginExtension>(0);
		}
		
	    return exts;
    }

	@Override
    public void processExtensionByInterfaceName(String interfaceName, ExtensionProcessor processor, Object[] args) {
		List<PluginExtension>  exts = getExtensionByInterfaceName(interfaceName);
		processExtensions(exts, processor, args);
    }

	public void setExtensions(Map<String, List<PluginExtension>> extensions) {
    	this.extensions = extensions;
    }
}
