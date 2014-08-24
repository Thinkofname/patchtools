package uk.co.thinkofdeath.patchtools;

import com.google.common.collect.Maps;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;

import java.util.Map;

public class ClassSetLoader extends ClassLoader {

    private final ClassSet classSet;
    private final Map<String, Class<?>> classes = Maps.newHashMap();

    public ClassSetLoader(ClassSet classSet) {
        this.classSet = classSet;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        String normalName = name.replace('.', '/');
        if (!classes.containsKey(normalName)) {
            byte[] clz = classSet.getClass(normalName);
            if (clz == null) {
                return super.loadClass(name);
            }
            Class<?> c = defineClass(name, clz, 0, clz.length);
            classes.put(normalName, c);
        }
        return classes.get(normalName);
    }
}
