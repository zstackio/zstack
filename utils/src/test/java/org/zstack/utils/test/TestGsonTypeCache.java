package org.zstack.utils.test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Assert;
import org.junit.Test;
import org.zstack.utils.gson.GsonTypeCache;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;

public class TestGsonTypeCache {
    @SuppressWarnings("unchecked")
    private static Map<TypeToken<?>, ?> cache(Gson gson) throws Exception {
        Field f = Gson.class.getDeclaredField("typeTokenCache");
        f.setAccessible(true);
        return (Map<TypeToken<?>, ?>) f.get(gson);
    }

    @Test
    public void forgetsTypesOfTheLoaderOnly() throws Exception {
        URL classes = GsonCachePojo.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader plugin = new URLClassLoader(new URL[]{classes}, null)) {
            Class<?> pluginPojo = plugin.loadClass(GsonCachePojo.class.getName());
            Assert.assertNotSame(GsonCachePojo.class, pluginPojo);

            Gson gson = new Gson();
            gson.toJson(pluginPojo.newInstance());
            gson.toJson(new GsonCachePojo());
            gson.toJson(java.util.Collections.singletonList(pluginPojo.newInstance()),
                    TypeToken.getParameterized(List.class, pluginPojo).getType());

            Assert.assertEquals(2, GsonTypeCache.forget(gson, plugin));
            Assert.assertTrue(cache(gson).keySet().stream().anyMatch(t -> t.getRawType() == GsonCachePojo.class));
            Assert.assertTrue(cache(gson).keySet().stream().noneMatch(t -> GsonTypeCache.refersTo(t.getType(), plugin)));
        }
    }
}
