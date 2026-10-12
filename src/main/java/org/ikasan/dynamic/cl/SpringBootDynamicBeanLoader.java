package org.ikasan.dynamic.cl;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.validation.DataBinder;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Boot-aware version of DynamicBeanLoader that can bind properties
 * from Spring's Environment (application.properties) to dynamically loaded beans.
 */
public class SpringBootDynamicBeanLoader extends DynamicBeanLoader {

    private final Environment environment;

    public SpringBootDynamicBeanLoader(ApplicationContext applicationContext) {
        super(applicationContext);
        this.environment = applicationContext.getEnvironment();
    }

    /**
     * Loads multiple Maven artifacts, registers a bean, and binds properties from application.properties
     * using a property prefix.
     *
     * @param mavenCoordinates array of Maven coordinates to load
     * @param className the fully qualified class name to load
     * @param beanName the name to register the bean under
     * @param propertyPrefix the prefix for properties in application.properties (e.g., "bigqueue")
     * @return the registered and configured Spring bean instance
     * @throws Exception if loading, registration, or binding fails
     */
    public Object loadRegisterAndBindProperties(String[] mavenCoordinates, String className, 
                                                String beanName, String propertyPrefix) throws Exception {
        // Load and register the bean
        Object bean = loadMultipleArtifactsAndRegisterBean(mavenCoordinates, className, beanName);

        // Bind properties from application.properties
        bindPropertiesFromEnvironment(bean, propertyPrefix);

        return bean;
    }

    /**
     * Binds properties from Spring's Environment to a bean using a property prefix.
     * Uses Spring Boot's Binder for type-safe property binding.
     *
     * @param bean the bean to bind properties to
     * @param propertyPrefix the prefix for properties (e.g., "bigqueue" will bind "bigqueue.name")
     * @throws Exception if binding fails
     */
    public void bindPropertiesFromEnvironment(Object bean, String propertyPrefix) throws Exception {
        try {
            // Try using Spring Boot's Binder (preferred method)
            Binder binder = Binder.get(environment);
            binder.bind(propertyPrefix, bean.getClass()).ifBound(boundBean -> {
                // Copy properties from bound bean to our bean
                copyProperties(boundBean, bean);
            });
        } catch (Exception e) {
            // Fallback to DataBinder approach
            bindUsingDataBinder(bean, propertyPrefix);
        }
    }

    /**
     * Binds properties using Spring's DataBinder (fallback method).
     *
     * @param bean the bean to bind properties to
     * @param propertyPrefix the property prefix
     */
    private void bindUsingDataBinder(Object bean, String propertyPrefix) {
        DataBinder binder = new DataBinder(bean);
        MutablePropertyValues pvs = new MutablePropertyValues();

        // Extract all properties with the given prefix
        String prefix = propertyPrefix + ".";
        ConfigurableEnvironment configurableEnv = (ConfigurableEnvironment) environment;
        for (String propertyName : configurableEnv.getPropertySources().stream()
                .flatMap(ps -> {
                    if (ps.getSource() instanceof Map) {
                        return ((Map<?, ?>) ps.getSource()).keySet().stream();
                    }
                    return java.util.stream.Stream.empty();
                })
                .map(Object::toString)
                .filter(name -> name.startsWith(prefix))
                .toList()) {

            String key = propertyName.substring(prefix.length());
            String value = environment.getProperty(propertyName);
            pvs.add(key, value);
        }

        binder.bind(pvs);
    }

    /**
     * Sets properties from Spring Environment using simple property names (without prefix).
     *
     * @param bean the bean to configure
     * @param propertyPrefix the prefix for properties in application.properties
     * @throws Exception if setting properties fails
     */
    public void setPropertiesFromEnvironment(Object bean, String propertyPrefix) throws Exception {
        java.util.Properties properties = new java.util.Properties();

        // Get all properties with the given prefix
        String prefix = propertyPrefix.isEmpty() ? "" : propertyPrefix + ".";

        // Iterate through all property sources
        ConfigurableEnvironment configurableEnv = (ConfigurableEnvironment) environment;
        configurableEnv.getPropertySources().forEach(propertySource -> {
            if (propertySource.getSource() instanceof Map) {
                Map<?, ?> source = (Map<?, ?>) propertySource.getSource();
                source.forEach((key, value) -> {
                    String keyStr = key.toString();
                    if (keyStr.startsWith(prefix)) {
                        String propertyName = keyStr.substring(prefix.length());
                        String propertyValue = environment.getProperty(keyStr);
                        if (propertyValue != null) {
                            properties.setProperty(propertyName, propertyValue);
                        }
                    }
                });
            }
        });

        // Set properties on the bean using Properties (which handles type conversion)
        if (!properties.isEmpty()) {
            setPropertiesOnBean(bean, properties);
        }
    }

    /**
     * Copies properties from one bean to another using reflection.
     *
     * @param source the source bean
     * @param target the target bean
     */
    private void copyProperties(Object source, Object target) {
        try {
            org.springframework.beans.BeanUtils.copyProperties(source, target);
        } catch (Exception e) {
            // If BeanUtils fails, try manual copy
            throw new RuntimeException("Failed to copy properties", e);
        }
    }
}
