package org.zstack.core.cloudbus;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Controller;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MessageIntegrityChecker implements Component {
    private Set<String> basePkgs;
    
    private void check() {
        List<String> badMessages = new ArrayList<String>();
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
        scanner.addIncludeFilter(new AssignableTypeFilter(APIEvent.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(APIMessage.class));
        scanner.addExcludeFilter(new AnnotationTypeFilter(Controller.class));
        scanner.addExcludeFilter(new AnnotationTypeFilter(org.springframework.stereotype.Component.class));
        for (String pkg : getBasePkgs()) {
            for (BeanDefinition bd : scanner.findCandidateComponents(pkg)) {
                Class<?> clazz;
                try {
                    clazz = Class.forName(bd.getBeanClassName());
                } catch (ClassNotFoundException e1) {
                    throw new CloudRuntimeException(e1);
                }
                
                try {
                    clazz.getConstructor();
                } catch (NoSuchMethodException e) {
                    badMessages.add(clazz.getName());
                } catch (SecurityException e) {
                    throw new CloudRuntimeException(e);
                }
            }
        }
        
        if (!badMessages.isEmpty()) {
            throw new CloudRuntimeException(String.format("Message %s must have a public zero-parameter constructor", badMessages));
        }
    }
    
    @Override
    public boolean start() {
        check();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    public Set<String> getBasePkgs() {
        if (basePkgs == null) {
            basePkgs = new HashSet<String>(1);
            basePkgs.add("org.zstack");
        }
        return basePkgs;
    }

    public void setBasePkgs(Set<String> basePkgs) {
        this.basePkgs = basePkgs;
    }
}
