package org.zstack.core.cloudbus;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:35 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ResourceDestinationMaker {
    String makeDestination(String resourceUuid);

    boolean isManagedByUs(String resourceUuid);
}
