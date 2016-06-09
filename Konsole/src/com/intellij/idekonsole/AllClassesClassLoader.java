package com.intellij.idekonsole;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.util.text.StringHash;
import com.intellij.util.containers.ContainerUtil;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;

public class AllClassesClassLoader extends ClassLoader {
    public AllClassesClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> fromParent = null;
        try {
            fromParent = getParent().loadClass(name);
        } catch (ClassNotFoundException e) {
            //do nothing
        }
        if (fromParent != null) return fromParent;

        for (IdeaPluginDescriptor descriptor : PluginManagerCore.getPlugins()) {
            ClassLoader l = descriptor.getPluginClassLoader();
            if (l == null) continue;
            try {
                return l.loadClass(name);
            } catch (ClassNotFoundException e) {
                if (name.startsWith("java.") || name.startsWith("groovy.")) break;
            }
        }
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
