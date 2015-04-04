package org.zstack.header.search;

import org.zstack.header.message.APIMessage;

public class APISearchGenerateSqlTriggerMsg extends APIMessage {
	private String resultPath;

	public String getResultPath() {
		return resultPath;
	}

	public void setResultPath(String resultPath) {
		this.resultPath = resultPath;
	}
}
