package org.zstack.core.config;

import org.zstack.utils.data.Pair;

import java.util.List;

public class GlobalConfigOptions {
	private List<String> validValue;
	private Pair<Long, Long> numberRange;
	private Long numberGreaterThan;
	private Long numberLessThan;

	public List<String> getValidValue() {
		return validValue;
	}

	public void setValidValue(List<String> validValue) {
		this.validValue = validValue;
	}

	public Pair<Long, Long> getNumberRange() {
		return numberRange;
	}

	public void setNumberRange(Pair<Long, Long> numberRange) {
		this.numberRange = numberRange;
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
