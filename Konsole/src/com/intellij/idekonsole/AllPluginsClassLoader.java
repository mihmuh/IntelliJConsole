package com.intellij.idekonsole;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.util.text.StringHash;
import com.intellij.util.containers.ContainerUtil;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;

public class AllPluginsClassLoader extends ClassLoader {
    static final AllPluginsClassLoader INSTANCE = new AllPluginsClassLoader();

    final Map<Long, ClassLoader> myLuckyGuess = ContainerUtil.newConcurrentMap();

    public AllPluginsClassLoader() {
        // Groovy performance: do not specify parent loader to enable our luckyGuesser
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        //long ts = System.currentTimeMillis();

        int p0 = name.indexOf("$");
        int p1 = p0 > 0 ? name.indexOf("$", p0 + 1) : -1;
        String base = p0 > 0 ? name.substring(0, Math.max(p0, p1)) : name;
        long hash = StringHash.calc(base);

        ClassLoader loader = myLuckyGuess.get(hash);
        if (loader == this) throw new ClassNotFoundException(name);

        Class<?> c = null;
        if (loader != null) {
            try {
                c = loader.loadClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        if (c == null) {
            boolean first = true;
            for (IdeaPluginDescriptor descriptor : PluginManagerCore.getPlugins()) {
                ClassLoader l = descriptor.getPluginClassLoader();
                if (l == null || l == loader) continue;
                try {
                    l.loadClass(base);

                    if (first) {
                        myLuckyGuess.put(hash, l);
                    }
                    first = false;
                    try {
                        c = l.loadClass(name);
                        break;
                    } catch (ClassNotFoundException e) {
                        if (p0 > 0) break;
                        if (name.startsWith("java.") || name.startsWith("groovy.")) break;
                    }
                } catch (ClassNotFoundException ignored) {
                }
            }
            if (first && loader == null) {
                myLuckyGuess.put(hash, this);
            }
        }

        //LOG.info("AllPluginsLoader [" + StringUtil.formatDuration(System.currentTimeMillis() - ts) + "]: " + (c != null ? "+" : "-") + name);
        if (c != null) return c;
        myLuckyGuess.put(StringHash.calc(name), this);

        throw new ClassNotFoundException(name);
    }

    @Override
    protected URL findResource(String name) {
        return getClass().getClassLoader().getResource(name);
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        return getClass().getClassLoader().getResources(name);
    }
}
