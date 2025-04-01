package org.zstack.utils;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/4/8 14:17
 */
public class YamlUtils {

    public static String dump(Object obj) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        Yaml yaml = new Yaml(options);
        return yaml.dump(obj);
    }

    public static String dumpAs(Object obj, Tag tag) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        Yaml yaml = new Yaml(options);
        return yaml.dumpAs(obj, tag, null);
    }

    public static Object load(String str) {
        Yaml yaml = new Yaml();
        return yaml.load(str);
    }

    public static <T> T load(String str, Class<T> clazz) {
        Yaml yaml = new Yaml();
        return yaml.loadAs(str, clazz);
    }

}
