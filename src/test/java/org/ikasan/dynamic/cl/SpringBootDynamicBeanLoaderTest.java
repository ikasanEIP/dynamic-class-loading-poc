package org.ikasan.dynamic.cl;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Spring Boot-aware dynamic bean loading with application.properties.
 */
class SpringBootDynamicBeanLoaderTest {

    @Configuration
    @PropertySource("classpath:application.properties")
    static class TestConfig {
        // Configuration that loads application.properties
    }

    @Test
    void testLoadBeanAndBindPropertiesFromApplicationProperties() throws Exception {
        // Create a Spring application context with application.properties loaded
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);

        try {
            // Verify properties are loaded
            Environment env = context.getEnvironment();
            assertEquals("true", env.getProperty("bigqueue.putErrorsToBackOfQueue"));

            // Create the Spring Boot-aware dynamic bean loader
            SpringBootDynamicBeanLoader loader = new SpringBootDynamicBeanLoader(context);

            // Load BigQueue artifacts
            String[] artifacts = {
                    "org.ikasan:bigqueue:1.0.0",
                    "org.ikasan:ikasan-big-queue:5.0.0"
            };

            // Load and register BigQueueConsumerConfiguration as a Spring bean
            Object configBean = loader.loadMultipleArtifactsAndRegisterBean(
                    artifacts,
                    "org.ikasan.component.endpoint.bigqueue.consumer.configuration.BigQueueConsumerConfiguration",
                    "bigQueueConfig"
            );

            // Bind properties from application.properties using the "bigqueue" prefix
            loader.setPropertiesFromEnvironment(configBean, "bigqueue");

            // Verify properties were set from application.properties
            Class<?> configClass = configBean.getClass();

            Object putErrorsToBackOfQueue = configClass.getMethod("isPutErrorsToBackOfQueue").invoke(configBean);
            assertEquals(true, putErrorsToBackOfQueue);

            System.out.println("Successfully loaded bean and bound properties from application.properties!");
        } finally {
            context.close();
        }
    }

    @Test
    void testLoadRegisterAndBindPropertiesInOneCall() throws Exception {
        // Create a Spring application context with application.properties loaded
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);

        try {
            // Create the Spring Boot-aware dynamic bean loader
            SpringBootDynamicBeanLoader loader = new SpringBootDynamicBeanLoader(context);

            // Load BigQueue artifacts
            String[] artifacts = {
                    "org.ikasan:bigqueue:1.0.0",
                    "org.ikasan:ikasan-big-queue:5.0.0"
            };

            // Load, register, and bind properties in one call
            Object configBean = loader.loadRegisterAndBindProperties(
                    artifacts,
                    "org.ikasan.component.endpoint.bigqueue.consumer.configuration.BigQueueConsumerConfiguration",
                    "bigQueueConfig",
                    "bigqueue"
            );

            // Verify the bean was registered
            assertNotNull(configBean);
            assertTrue(context.containsBean("bigQueueConfig"));

            // Verify properties were bound from application.properties
            Class<?> configClass = configBean.getClass();
            Object putErrorsToBackOfQueue = configClass.getMethod("isPutErrorsToBackOfQueue").invoke(configBean);
            assertEquals(true, putErrorsToBackOfQueue);

            System.out.println("Successfully loaded, registered, and bound properties from application.properties in one call!");
        } finally {
            context.close();
        }
    }

    @Test
    void testBindPropertiesFromEnvironment() throws Exception {
        // Create a Spring application context with application.properties loaded
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);

        try {
            // Verify application.properties is loaded
            Environment env = context.getEnvironment();
            assertNotNull(env.getProperty("bigqueue.putErrorsToBackOfQueue"));

            // Create the Spring Boot-aware dynamic bean loader
            SpringBootDynamicBeanLoader loader = new SpringBootDynamicBeanLoader(context);

            // Load and register bean
            String[] artifacts = {
                    "org.ikasan:bigqueue:1.0.0",
                    "org.ikasan:ikasan-big-queue:5.0.0"
            };

            Object configBean = loader.loadMultipleArtifactsAndRegisterBean(
                    artifacts,
                    "org.ikasan.component.endpoint.bigqueue.consumer.configuration.BigQueueConsumerConfiguration",
                    "bigQueueConfig"
            );

            // Bind properties from Spring Environment
            loader.bindPropertiesFromEnvironment(configBean, "bigqueue");

            // Verify the bean is a Spring-managed singleton
            Object retrievedBean = context.getBean("bigQueueConfig");
            assertSame(configBean, retrievedBean);

            System.out.println("Successfully bound properties from Spring Environment to dynamically loaded bean!");
        } finally {
            context.close();
        }
    }
}
