package org.zstack.abstraction.sns;

import org.zstack.abstraction.PluginDriver;

/**
 * PluginEndpointSender extends PluginRegister and contains sender
 * of sns endpoint.
 */
public interface EndpointDriver extends PluginDriver {
    boolean send(PluginEndpointData message);
}