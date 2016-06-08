package scriptengine.kotlin.util;

import java.lang.reflect.Method;

public class ReflUtils {

  private ReflUtils() {
  }

  @Deprecated
  public static Object invoke(Class<?> clazz, Object obj, String methodName, Object... args) {
    try {
      Method method = findMethodByName(clazz, methodName);
      method.setAccessible(true);
      Object result = method.invoke(obj, args);
      return result;
    } catch (Throwable e) {
      throw new RuntimeException("invoke", e);
    }
  }

  @Deprecated
  public static Method findMethodByName(Class<?> clazz, String methodName) {
    for (Method method : clazz.getDeclaredMethods())
      if (method.getName().equalsIgnoreCase(methodName))
        return method;
    return null;
  }

  public static Method findMethod(Class<?> clazz, String method, Class<?>... prmTypes) {
    try {
      return clazz.getMethod(method, prmTypes);
    } catch (Exception e) {
      return null;
    }
  }

}