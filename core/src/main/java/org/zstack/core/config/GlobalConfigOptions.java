package org.zstack.core.config;

import java.util.List;

public class GlobalConfigOptions {
	private List<String> validValue;
	private Long numberGreaterThan;
	private Long numberLessThan;

	public List<String> getValidValue() {
		return validValue;
	}

	public void setValidValue(List<String> validValue) {
		this.validValue = validValue;
	}

	public Long getNumberGreaterThan() {
		return numberGreaterThan;
	}

	public void setNumberGreaterThan(Long numberGreaterThan) {
		this.numberGreaterThan = numberGreaterThan;
	}

	public Long getNumberLessThan() {
		return numberLessThan;
	}

	public void setNumberLessThan(Long numberLessThan) {
		this.numberLessThan = numberLessThan;
	}
}
