package org.zstack.core.config;

import java.util.Map;

public interface GlobalConfigFacade {
	String updateConfig(String category, String name, String value);
	
	Map<String, GlobalConfig> getAllConfig();

    <T> T getConfigValue(String category, String name, Class<T> clz);

    GlobalConfig createGlobalConfig(GlobalConfigVO vo);
}
