package org.zstack.header.core.scheduler;


/**
 * Created by root on 8/3/16.
 */
public interface APICreateSchedulerJobMessage {
//    @APIParam(maxLength = 255)
//    private String name;
//    @APIParam(maxLength = 2048, required = false)
//    private String description;
//
//    @APINoSee
//    private String jobName;
//
//    private String targetResourceUuid;

    String getTargetResourceUuid();
}
