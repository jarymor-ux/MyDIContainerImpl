package ru.ostap;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class DIContainer {
    private final Map<Class<?>, Object> objectMap = new HashMap<>();

    //Сканирует классы и регает их в контейнер
    public void scan(String basePackage) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = basePackage.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (resource.getProtocol().equals("file")) {
                File dir = new File(resource.toURI());
                for (File file : dir.listFiles()) {
                    if (file.getName().endsWith(".class")) {
                        String className = basePackage + '.' + file.getName().substring(0, file.getName().length() - 6);
                        Class<?> clazz = Class.forName(className);
                        if (clazz.isAnnotationPresent(Component.class)) {
                            register(clazz);
                        }
                    }
                }
            }
        }
    }
    //Добовляет класс в контейнер
    public void register(Class<?> clazz) throws IllegalAccessException, InstantiationException {

        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() > 0) {
                objectMap.put(clazz, constructor);
                return;
            }
        }

        Object instance = clazz.newInstance();
        objectMap.put(clazz, instance);


        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> interfaceClass : interfaces) {
            objectMap.put(interfaceClass, instance);
        }
    }
    //Создаёт экземпляр объекта заданного класса
    public <T> T getInstance(Class<T> clazz) throws Exception {
        if (!objectMap.containsKey(clazz)) {
            throw new IllegalArgumentException("Class " + clazz.getName() + " is not registered.");
        }

        Object obj = objectMap.get(clazz);
        if (obj instanceof Constructor) {
            return createInstanceWithConstructor((Constructor<?>) obj);
        } else {
            return clazz.cast(obj);
        }
    }


    //Создаёт объект используя заданный конструктор
    private <T> T createInstanceWithConstructor(Constructor<?> constructor) throws Exception {
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] parameters = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            parameters[i] = getInstance(parameterTypes[i]);
        }
        constructor.setAccessible(true);
        return (T) constructor.newInstance(parameters);
    }

    public void wireDependencies() throws Exception {
        for (Object obj : objectMap.values()) {
            if (obj instanceof Constructor<?> constructor) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                for (Class<?> parameterType : parameterTypes) {
                    if (!objectMap.containsKey(parameterType)) {
                        throw new IllegalArgumentException("Dependency for class " + parameterType.getName() + " is not registered.");
                    }
                }
            } else {
                injectDependencies(obj);
            }
        }
    }

    //Тут понятно)
    private void injectDependencies(Object obj) throws Exception {
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Autowired.class)) {
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                Object dependency = objectMap.get(fieldType);
                if (dependency instanceof Constructor) {
                    field.set(obj, createInstanceWithConstructor((Constructor<?>) dependency));
                } else {
                    field.set(obj, dependency);
                }
            }
        }
    }
}
