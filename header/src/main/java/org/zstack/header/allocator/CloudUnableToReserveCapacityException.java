package org.zstack.header.allocator;

import org.zstack.header.exception.CloudRuntimeException;

public class CloudUnableToReserveCapacityException extends CloudRuntimeException {
	public CloudUnableToReserveCapacityException(String msg) {
		super(msg);
	}
}
