package com.ll.framework.ioc;

import com.ll.framework.ioc.annotations.Component;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ApplicationContext {
    private final String basePackage;
    private final Map<String, Object> beans = new HashMap<>();
    private final Map<String, Class<?>> beanClasses = new HashMap<>();

    public ApplicationContext(String basePackage) {
        this.basePackage = basePackage;
    }

    public void init() {
        Reflections reflections = new Reflections(basePackage);

        Set<Class<?>> componentAnnotations = reflections.getTypesAnnotatedWith(Component.class);

        Set<Class<?>> classes = new HashSet<>();
        for (Class<?> annotation : componentAnnotations) {
            if (annotation.isAnnotation()) {
                classes.addAll(reflections.getTypesAnnotatedWith(
                        (Class<? extends Annotation>) annotation
                ));
            }
        }

        classes.addAll(reflections.getTypesAnnotatedWith(Component.class));

        classes.stream()
                .filter(clazz -> !clazz.isInterface())
                .forEach(clazz -> {
                    String beanName = clazz.getSimpleName();
                    beanName = Character.toLowerCase(beanName.charAt(0)) + beanName.substring(1);
                    beanClasses.put(beanName, clazz);
                });
    }

    public <T> T genBean(String beanName) {
        if (beans.containsKey(beanName)) {
            return (T) beans.get(beanName);
        }

        Class<?> clazz = beanClasses.get(beanName);
        Constructor<?> constructor = clazz.getDeclaredConstructors()[0];

        try {
            Parameter[] params = constructor.getParameters();
            Object[] args = new Object[params.length];

            for (int i = 0; i < params.length; i++) {
                String paramBeanName = params[i].getType().getSimpleName();
                paramBeanName = Character.toLowerCase(paramBeanName.charAt(0)) + paramBeanName.substring(1);
                args[i] = genBean(paramBeanName);
            }

            Object instance = constructor.newInstance(args);
            beans.put(beanName, instance);

            return (T) instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
