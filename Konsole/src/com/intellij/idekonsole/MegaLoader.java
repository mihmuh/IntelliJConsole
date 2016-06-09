package com.intellij.idekonsole;

import java.net.URL;
import java.net.URLClassLoader;

public class MegaLoader extends URLClassLoader {
    public MegaLoader(URL url) {
        // Make kotlin compiler happy
        super(new URL[]{url}, AllPluginsClassLoader.INSTANCE);
    }
}
