package org.ikasan.dynamic.cl;

import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

/**
 * Service that combines MavenArtifactClassLoader with Spring bean registration
 * to dynamically load classes from Maven artifacts and register them as Spring beans.
 */
public class DynamicBeanLoader {

    private final MavenArtifactClassLoader classLoader;
    private final ApplicationContext applicationContext;

    public DynamicBeanLoader(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.classLoader = new MavenArtifactClassLoader();
    }

    /**
     * Loads a Maven artifact, loads a class from it, and registers it as a Spring bean.
     *
     * @param mavenCoordinate the Maven coordinate (groupId:artifactId:version)
     * @param className the fully qualified class name to load
     * @param beanName the name to register the bean under
     * @return the registered Spring bean instance
     * @throws Exception if loading or registration fails
     */
    public Object loadAndRegisterBean(String mavenCoordinate, String className, String beanName) throws Exception {
        // Load the artifact
        classLoader.loadArtifact(mavenCoordinate);

        // Load the class
        Class<?> clazz = classLoader.loadClassFromArtifact(className);

        // Register as Spring bean
        return SpringBeanRegistry.registerBean(applicationContext, beanName, clazz);
    }

    /**
     * Loads a Maven artifact, instantiates a class from it, and registers the instance as a Spring bean.
     *
     * @param mavenCoordinate the Maven coordinate (groupId:artifactId:version)
     * @param className the fully qualified class name to load
     * @param beanName the name to register the bean under
     * @return the registered Spring bean instance
     * @throws Exception if loading or registration fails
     */
    public Object loadInstantiateAndRegisterBean(String mavenCoordinate, String className, String beanName) throws Exception {
        // Load the artifact
        classLoader.loadArtifact(mavenCoordinate);

        // Load the class
        Class<?> clazz = classLoader.loadClassFromArtifact(className);

        // Instantiate
        Object instance = clazz.getDeclaredConstructor().newInstance();

        // Register the instance as a singleton Spring bean
        SpringBeanRegistry.registerBeanInstance(applicationContext, beanName, instance);

        return instance;
    }

    /**
     * Loads multiple Maven artifacts before loading a class and registering as a bean.
     * Useful when the class depends on multiple artifacts.
     *
     * @param mavenCoordinates array of Maven coordinates to load
     * @param className the fully qualified class name to load
     * @param beanName the name to register the bean under
     * @return the registered Spring bean instance
     * @throws Exception if loading or registration fails
     */
    public Object loadMultipleArtifactsAndRegisterBean(String[] mavenCoordinates, String className, String beanName) throws Exception {
        // Load all artifacts
        for (String coordinate : mavenCoordinates) {
            classLoader.loadArtifact(coordinate);
        }

        // Load the class
        Class<?> clazz = classLoader.loadClassFromArtifact(className);

        // Register as Spring bean
        return SpringBeanRegistry.registerBean(applicationContext, beanName, clazz);
    }

    /**
     * Loads a Maven artifact, instantiates a class with constructor arguments,
     * and registers it as a Spring bean.
     *
     * @param mavenCoordinate the Maven coordinate (groupId:artifactId:version)
     * @param className the fully qualified class name to load
     * @param beanName the name to register the bean under
     * @param constructorArgs the constructor arguments
     * @return the registered Spring bean instance
     * @throws Exception if loading or registration fails
     */
    public Object loadAndRegisterBeanWithArgs(String mavenCoordinate, String className, String beanName, 
                                              Object... constructorArgs) throws Exception {
        // Load the artifact
        classLoader.loadArtifact(mavenCoordinate);

        // Load the class
        Class<?> clazz = classLoader.loadClassFromArtifact(className);

        // Register as Spring bean with constructor args
        return SpringBeanRegistry.registerBeanWithArgs(applicationContext, beanName, clazz, constructorArgs);
    }

    /**
     * Sets properties on a bean using reflection based on a Map of property names to values.
     * Follows JavaBean naming conventions (setPropertyName).
     *
     * @param bean the bean to set properties on
     * @param properties map of property names to values
     * @throws Exception if setting properties fails
     */
    public void setPropertiesOnBean(Object bean, Map<String, Object> properties) throws Exception {
        Class<?> beanClass = bean.getClass();

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String propertyName = entry.getKey();
            Object value = entry.getValue();

            // Convert property name to setter method name (e.g., "queueDir" -> "setQueueDir")
            String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);

            // Find and invoke the setter method
            Method setter = findSetterMethod(beanClass, setterName, value);
            if (setter != null) {
                setter.invoke(bean, value);
            } else {
                throw new NoSuchMethodException("No setter found for property: " + propertyName + " (tried " + setterName + ")");
            }
        }
    }

    /**
     * Sets properties on a bean using a Properties object.
     *
     * @param bean the bean to set properties on
     * @param properties Properties object containing property names and values
     * @throws Exception if setting properties fails
     */
    public void setPropertiesOnBean(Object bean, Properties properties) throws Exception {
        Class<?> beanClass = bean.getClass();

        for (String propertyName : properties.stringPropertyNames()) {
            String value = properties.getProperty(propertyName);

            // Convert property name to setter method name
            String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);

            // Try to find setter with String parameter first
            try {
                Method setter = beanClass.getMethod(setterName, String.class);
                setter.invoke(bean, value);
            } catch (NoSuchMethodException e) {
                // Try boolean
                try {
                    Method setter = beanClass.getMethod(setterName, boolean.class);
                    setter.invoke(bean, Boolean.parseBoolean(value));
                } catch (NoSuchMethodException e2) {
                    // Try Integer
                    try {
                        Method setter = beanClass.getMethod(setterName, Integer.class);
                        setter.invoke(bean, Integer.valueOf(value));
                    } catch (NoSuchMethodException e3) {
                        // Try int
                        try {
                            Method setter = beanClass.getMethod(setterName, int.class);
                            setter.invoke(bean, Integer.parseInt(value));
                        } catch (NoSuchMethodException e4) {
                            throw new NoSuchMethodException("No setter found for property: " + propertyName);
                        }
                    }
                }
            }
        }
    }

    /**
     * Finds a setter method for a given name and value type.
     *
     * @param beanClass the bean class
     * @param setterName the setter method name
     * @param value the value to set
     * @return the setter method, or null if not found
     */
    private Method findSetterMethod(Class<?> beanClass, String setterName, Object value) {
        Method[] methods = beanClass.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                Class<?> paramType = method.getParameterTypes()[0];
                // Check if the parameter type is compatible with the value
                if (paramType.isAssignableFrom(value.getClass()) ||
                    (paramType.isPrimitive() && isCompatiblePrimitive(paramType, value))) {
                    return method;
                }
            }
        }
        return null;
    }

    /**
     * Checks if a primitive type is compatible with a value.
     *
     * @param primitiveType the primitive type
     * @param value the value
     * @return true if compatible
     */
    private boolean isCompatiblePrimitive(Class<?> primitiveType, Object value) {
        if (primitiveType == int.class && value instanceof Integer) return true;
        if (primitiveType == long.class && value instanceof Long) return true;
        if (primitiveType == boolean.class && value instanceof Boolean) return true;
        if (primitiveType == double.class && value instanceof Double) return true;
        if (primitiveType == float.class && value instanceof Float) return true;
        return false;
    }

    /**
     * Gets the underlying MavenArtifactClassLoader.
     *
     * @return the class loader
     */
    public MavenArtifactClassLoader getClassLoader() {
        return classLoader;
    }
}
