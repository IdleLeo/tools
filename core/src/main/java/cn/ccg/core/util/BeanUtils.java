package cn.ccg.core.util;

import lombok.extern.slf4j.Slf4j;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Slf4j
public class BeanUtils {


    public static Map<String, Object> bean2Map(Object obj) {
        if (obj == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor property : propertyDescriptors) {
                String key = property.getName();

                // 过滤class属性
                if (!"class".equals(key)) {
                    // 得到property对应的getter方法
                    Method getter = property.getReadMethod();
                    Object value = getter.invoke(obj);

                    map.put(key, value);
                }

            }
        } catch (Exception e) {
            System.out.println("transBean2Map Error " + e);
        }

        return map;
    }

    public static void map2Bean(Map<String, Object> map, Object obj) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();

            for (PropertyDescriptor property : propertyDescriptors) {
                String key = property.getName();

                if (map.containsKey(key)) {
                    Object value = map.get(key);
                    // 得到property对应的setter方法
                    Method setter = property.getWriteMethod();
                    setter.invoke(obj, value);
                }

            }

        } catch (Exception e) {
            System.out.println("transMap2Bean Error " + e);
        }
    }

    public static synchronized void beanFieldCopy(Object src, Object des) {
        beanFieldCopy(src, des, "", true);
    }

    public static synchronized void beanFieldCopy(Object src, Object des, String prefix, boolean desPrefix) {
        Class<?> srcClass = src.getClass();
        Class<?> desClass = des.getClass();
        HashSet<String> setFields = new HashSet();

        Method[] srcMethods = srcClass.getMethods();
        Method[] desMethods = desClass.getMethods();

        for (Method desMethod : desMethods) {
            String desMethodName = desMethod.getName();

            if (desMethodName.startsWith("set")) {
                if (desPrefix) {
                    setFields.add(desMethodName.substring(3 + prefix.length()
                    ));
                } else {
                    setFields.add(desMethodName.substring(3
                    ));
                }
            }
        }

        for (Method method : srcMethods) {
            String srcMethodName = method.getName();
            if (srcMethodName.startsWith("get")) {
                String fieldName;
                if (desPrefix) {
                    fieldName = srcMethodName.substring(3);
                } else {
                    fieldName = srcMethodName.substring(3 + prefix.length());
                }
                if (setFields.contains(fieldName)) {
                    String invokeMethodName;
                    if (desPrefix) {
                        invokeMethodName = "set" + prefix + fieldName;
                    } else {
                        invokeMethodName = "set" + fieldName;
                    }
                    try {
                        Method invokeMethod = desClass.getMethod(
                                invokeMethodName,
                                method.getReturnType());
                        Object result = method.invoke(src);
                        if (result == null) {
                            continue;
                        }
                        invokeMethod.invoke(des, result);
                    } catch (NoSuchMethodException ignored){

                    }
                    catch (Exception e) {
                        //log.info("bean copy exception: destination no such method " + invokeMethodName);
                        e.printStackTrace();
                    }
                }
            } else if (srcMethodName.startsWith("is")) {
                String fieldName;
                if (desPrefix) {
                    fieldName = srcMethodName.substring(2);
                } else {
                    fieldName = srcMethodName.substring(2 + prefix.length());
                }
                if (setFields.contains(fieldName)) {
                    String invokeMethodName;
                    if (desPrefix) {
                        invokeMethodName = "set" + prefix + fieldName;
                    } else {
                        invokeMethodName = "set" + fieldName;
                    }
                    try {
                        Method invokeMethod = desClass.getMethod(
                                invokeMethodName,
                                method.getReturnType());
                        Object result = method.invoke(src);
                        if (result == null) {
                            continue;
                        }
                        invokeMethod.invoke(des, result);
                    }catch (NoSuchMethodException ignored){

                    }
                    catch (Exception e) {
                        //log.info("bean copy exception: destination no such method " + invokeMethodName);
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
