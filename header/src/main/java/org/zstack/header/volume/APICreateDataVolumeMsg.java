package org.zstack.header.volume;

import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;

/**
 * @api
 * create a new data volume
 *
 * @category volume
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.header.volume.APICreateDataVolumeMsg": {
"name": "TestData",
"diskOfferingUuid": "9ffa46a4b7214537a582134ef31410f2",
"session": {
"uuid": "9a944b482a9540f39540f298afeffc2c"
}
}
}
 * @msg
 *
 * {
"org.zstack.header.volume.APICreateDataVolumeMsg": {
"name": "TestData",
"diskOfferingUuid": "9ffa46a4b7214537a582134ef31410f2",
"session": {
"uuid": "9a944b482a9540f39540f298afeffc2c"
},
"timeout": 1800000,
"id": "87df9707935f459181d15469b590b03f",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * see :ref:`APICreateDataVolumeEvent`
 */
@Action(category = VolumeConstant.ACTION_CATEGORY)
public class APICreateDataVolumeMsg extends APICreateMessage {
    /**
     * @desc max length of 255 characters
     */
	@APIParam(maxLength = 255)
	private String name;
    /**
     * @desc max length of 2048 characters
     * @optional
     */
    @APIParam(required = false, maxLength = 2048)
	private String description;
    /**
     * @desc uuid of disk offering the volume is created from
     */
	@APIParam(resourceType = DiskOfferingVO.class, checkAccount = true)
	private String diskOfferingUuid;
	
	public String getName() {
    	return name;
    }
	public void setName(String name) {
    	this.name = name;
    }
	public String getDescription() {
    	return description;
    }
	public void setDescription(String description) {
    	this.description = description;
    }
	public String getDiskOfferingUuid() {
    	return diskOfferingUuid;
    }
	public void setDiskOfferingUuid(String diskOfferingUuid) {
    	this.diskOfferingUuid = diskOfferingUuid;
    }
}
