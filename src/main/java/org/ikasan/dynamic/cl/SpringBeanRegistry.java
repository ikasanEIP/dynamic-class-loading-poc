package org.ikasan.dynamic.cl;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Utility class to dynamically register beans loaded from Maven artifacts into Spring's ApplicationContext.
 * This allows classes loaded at runtime via MavenArtifactClassLoader to be exposed as Spring beans.
 */
public class SpringBeanRegistry {

    /**
     * Registers a dynamically loaded class as a Spring bean.
     *
     * @param applicationContext the Spring application context
     * @param beanName the name to register the bean under
     * @param beanClass the class to register as a bean
     * @return the registered bean instance
     */
    public static Object registerBean(ApplicationContext applicationContext, String beanName, Class<?> beanClass) {
        ConfigurableApplicationContext configurableContext = (ConfigurableApplicationContext) applicationContext;
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) configurableContext.getBeanFactory();

        // Create bean definition
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(beanClass);
        BeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();

        // Register the bean
        registry.registerBeanDefinition(beanName, beanDefinition);

        // Return the bean instance
        return applicationContext.getBean(beanName);
    }

    /**
     * Registers a bean instance (already instantiated) into Spring's ApplicationContext.
     *
     * @param applicationContext the Spring application context
     * @param beanName the name to register the bean under
     * @param beanInstance the bean instance to register
     */
    public static void registerBeanInstance(ApplicationContext applicationContext, String beanName, Object beanInstance) {
        ConfigurableApplicationContext configurableContext = (ConfigurableApplicationContext) applicationContext;
        configurableContext.getBeanFactory().registerSingleton(beanName, beanInstance);
    }

    /**
     * Registers a dynamically loaded class as a Spring bean with constructor arguments.
     *
     * @param applicationContext the Spring application context
     * @param beanName the name to register the bean under
     * @param beanClass the class to register as a bean
     * @param constructorArgs the constructor arguments
     * @return the registered bean instance
     */
    public static Object registerBeanWithArgs(ApplicationContext applicationContext, String beanName, 
                                              Class<?> beanClass, Object... constructorArgs) {
        ConfigurableApplicationContext configurableContext = (ConfigurableApplicationContext) applicationContext;
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) configurableContext.getBeanFactory();

        // Create bean definition with constructor args
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(beanClass);
        for (Object arg : constructorArgs) {
            beanDefinitionBuilder.addConstructorArgValue(arg);
        }
        BeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();

        // Register the bean
        registry.registerBeanDefinition(beanName, beanDefinition);

        // Return the bean instance
        return applicationContext.getBean(beanName);
    }

    /**
     * Checks if a bean with the given name is already registered.
     *
     * @param applicationContext the Spring application context
     * @param beanName the bean name to check
     * @return true if the bean exists, false otherwise
     */
    public static boolean beanExists(ApplicationContext applicationContext, String beanName) {
        return applicationContext.containsBean(beanName);
    }

    /**
     * Removes a bean from the Spring context.
     *
     * @param applicationContext the Spring application context
     * @param beanName the name of the bean to remove
     */
    public static void removeBean(ApplicationContext applicationContext, String beanName) {
        ConfigurableApplicationContext configurableContext = (ConfigurableApplicationContext) applicationContext;
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) configurableContext.getBeanFactory();
        
        if (registry.containsBeanDefinition(beanName)) {
            registry.removeBeanDefinition(beanName);
        }
    }
}
