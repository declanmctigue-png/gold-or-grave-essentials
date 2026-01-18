// File: com/goldorgrave/essentials/GoGReflection.java
package com.goldorgrave.essentials;

import java.lang.reflect.Method;

public final class GoGReflection {
    private GoGReflection() {}

    public static Object invoke0(Object obj, String method) {
        try {
            Method m = obj.getClass().getMethod(method);
            m.setAccessible(true);
            return m.invoke(obj);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Object invoke(Object obj, String method, Object... args) {
        if (obj == null) return null;
        try {
            Method m = findMethod(obj.getClass(), method, args);
            if (m == null) return null;
            m.setAccessible(true);
            return m.invoke(obj, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method findMethod(Class<?> cls, String name, Object... args) {
        Method[] methods = cls.getMethods();
        outer:
        for (Method m : methods) {
            if (!m.getName().equals(name)) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length != args.length) continue;
            for (int i = 0; i < p.length; i++) {
                if (args[i] == null) continue;
                if (!box(p[i]).isAssignableFrom(box(args[i].getClass()))) continue outer;
            }
            return m;
        }
        return null;
    }

    private static Class<?> box(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == int.class) return Integer.class;
        if (c == long.class) return Long.class;
        if (c == double.class) return Double.class;
        if (c == float.class) return Float.class;
        if (c == boolean.class) return Boolean.class;
        if (c == short.class) return Short.class;
        if (c == byte.class) return Byte.class;
        if (c == char.class) return Character.class;
        return c;
    }
}
