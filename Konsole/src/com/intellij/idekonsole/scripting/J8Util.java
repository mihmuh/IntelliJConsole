package com.intellij.idekonsole.scripting;

import java.util.Collection;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

//bridge for kotlin-J8 interop until kotlin 1.1 public release
public class J8Util {
    public static <T> Stream<T> stream(Collection<T> c) {
        return c.stream();
    }
}
