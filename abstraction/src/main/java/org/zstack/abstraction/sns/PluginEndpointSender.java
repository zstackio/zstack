package org.zstack.abstraction.sns;

import org.zstack.abstraction.PluginRegister;

/**
 * PluginEndpointSender extends PluginRegister and contains sender
 * of sns endpoint.
 */
public interface PluginEndpointSender extends PluginRegister {
    boolean send(PluginEndpointData message);

    /**
     * endpoint type for API and UI usage
     * @return string of display type
     */
    String endpointType();
}