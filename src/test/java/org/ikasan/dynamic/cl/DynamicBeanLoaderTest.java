package org.ikasan.dynamic.cl;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for dynamically loading classes from Maven artifacts and registering them as Spring beans.
 */
class DynamicBeanLoaderTest {

    @Configuration
    static class TestConfig {
        // Empty configuration for testing
    }

    @Test
    void testLoadGsonAndRegisterAsSpringBean() throws Exception {
        // Create a Spring application context
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);

        try {
            // Create the dynamic bean loader
            DynamicBeanLoader loader = new DynamicBeanLoader(context);

            // Load Gson from Maven Central and register as a Spring bean
            Object gsonBean = loader.loadAndRegisterBean(
                    "com.google.code.gson:gson:2.10.1",
                    "com.google.gson.Gson",
                    "dynamicGson"
            );

            // Verify the bean was registered
            assertNotNull(gsonBean);
            assertTrue(context.containsBean("dynamicGson"));

            // Retrieve the bean from Spring context
            Object retrievedBean = context.getBean("dynamicGson");
            assertNotNull(retrievedBean);
            assertEquals(gsonBean, retrievedBean);
            assertEquals("com.google.gson.Gson", retrievedBean.getClass().getName());

            System.out.println("Successfully registered Gson as Spring bean!");
        } finally {
            context.close();
        }
    }

    @Test
    void testLoadBigQueueConsumerConfigAndRegisterAsSpringBean() throws Exception {
        // Create a Spring application context
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);

        try {
            // Create the dynamic bean loader
            DynamicBeanLoader loader = new DynamicBeanLoader(context);

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

            // Verify the bean was registered
            assertNotNull(configBean);
            assertTrue(context.containsBean("bigQueueConfig"));

            // Retrieve the bean from Spring context
            Object retrievedBean = context.getBean("bigQueueConfig");
            assertNotNull(retrievedBean);
            assertEquals(configBean, retrievedBean);
            assertEquals("org.ikasan.component.endpoint.bigqueue.consumer.configuration.BigQueueConsumerConfiguration",
                    retrievedBean.getClass().getName());

            // Set properties on the configuration bean using reflection
            Class<?> configClass = retrievedBean.getClass();

            // Set the available property
            var setPutErrorsToBackOfQueueMethod = configClass.getMethod("setPutErrorsToBackOfQueue", boolean.class);
            setPutErrorsToBackOfQueueMethod.invoke(retrievedBean, true);

            // Verify the property was set by reading it back
            var isPutErrorsToBackOfQueueMethod = configClass.getMethod("isPutErrorsToBackOfQueue");
            Object putErrorsToBackOfQueue = isPutErrorsToBackOfQueueMethod.invoke(retrievedBean);
            assertEquals(true, putErrorsToBackOfQueue);

            System.out.println("Successfully registered BigQueueConsumerConfiguration as Spring bean and set properties!");
        } finally {
            context.close();
        }
    }

    @Test
    void testLoadInstantiateAndRegisterBean() throws Exception {
        // Create a Spring application context
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);

        try {
            // Create the dynamic bean loader
            DynamicBeanLoader loader = new DynamicBeanLoader(context);

            // Load, instantiate, and register as a singleton
            Object configBean = loader.loadInstantiateAndRegisterBean(
                    "com.google.code.gson:gson:2.10.1",
                    "com.google.gson.GsonBuilder",
                    "gsonBuilder"
            );

            // Verify the bean was registered as a singleton
            assertNotNull(configBean);
            assertTrue(context.containsBean("gsonBuilder"));

            // Verify it's the same instance (singleton)
            Object retrievedBean1 = context.getBean("gsonBuilder");
            Object retrievedBean2 = context.getBean("gsonBuilder");
            assertSame(retrievedBean1, retrievedBean2);
            assertSame(configBean, retrievedBean1);

            System.out.println("Successfully registered instantiated bean as Spring singleton!");
        } finally {
            context.close();
        }
    }

    @Test
    void testSpringBeanRegistryDirectly() throws Exception {
        // Create a Spring application context
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);

        try {
            // Load a class dynamically
            MavenArtifactClassLoader classLoader = new MavenArtifactClassLoader();
            classLoader.loadArtifact("commons-lang:commons-lang:2.6");
            Class<?> stringUtilsClass = classLoader.loadClassFromArtifact("org.apache.commons.lang.StringUtils");

            // Register it directly using SpringBeanRegistry
            Object bean = SpringBeanRegistry.registerBean(context, "stringUtils", stringUtilsClass);

            // Verify
            assertNotNull(bean);
            assertTrue(SpringBeanRegistry.beanExists(context, "stringUtils"));

            // Remove the bean
            SpringBeanRegistry.removeBean(context, "stringUtils");
            assertFalse(SpringBeanRegistry.beanExists(context, "stringUtils"));

            System.out.println("Successfully tested SpringBeanRegistry!");
        } finally {
            context.close();
        }
    }

    @Test
    void testLoadBigQueueConsumerAsSpringBean() throws Exception {
        // Create a Spring application context
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);

        try {
            // Create the dynamic bean loader
            DynamicBeanLoader loader = new DynamicBeanLoader(context);

            // First, load and register the configuration bean
            String[] artifacts = {
                    "org.ikasan:bigqueue:1.0.0",
                    "org.ikasan:ikasan-big-queue:5.0.0"
            };

            Object configBean = loader.loadMultipleArtifactsAndRegisterBean(
                    artifacts,
                    "org.ikasan.component.endpoint.bigqueue.consumer.configuration.BigQueueConsumerConfiguration",
                    "bigQueueConfig"
            );

            assertNotNull(configBean);
            assertTrue(context.containsBean("bigQueueConfig"));

            // Now load the BigQueueConsumer class (note: we can't easily instantiate it without its dependencies)
            // But we can verify it's loadable and accessible via Spring if needed
            Class<?> consumerClass = loader.getClassLoader().loadClassFromArtifact(
                    "org.ikasan.component.endpoint.bigqueue.consumer.BigQueueConsumer"
            );

            assertNotNull(consumerClass);
            assertEquals("org.ikasan.component.endpoint.bigqueue.consumer.BigQueueConsumer", consumerClass.getName());

            // Demonstrate that we have both the configuration bean in Spring and access to the Consumer class
            Object springConfig = context.getBean("bigQueueConfig");
            assertNotNull(springConfig);

            System.out.println("Successfully loaded BigQueueConsumer class with Spring-managed configuration bean!");
        } finally {
            context.close();
        }
    }

    @Test
    void testSetPropertiesFromMap() throws Exception {
        // Create a Spring application context
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);

        try {
            // Create the dynamic bean loader
            DynamicBeanLoader loader = new DynamicBeanLoader(context);

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

            // Create a map of properties
            Map<String, Object> properties = new HashMap<>();
            properties.put("putErrorsToBackOfQueue", true);

            // Set properties using the helper method
            loader.setPropertiesOnBean(configBean, properties);

            // Verify properties were set
            Class<?> configClass = configBean.getClass();

            Object putErrorsToBackOfQueue = configClass.getMethod("isPutErrorsToBackOfQueue").invoke(configBean);
            assertEquals(true, putErrorsToBackOfQueue);

            System.out.println("Successfully set properties on Spring bean from Map!");
        } finally {
            context.close();
        }
    }

    @Test
    void testSetPropertiesFromPropertiesFile() throws Exception {
        // Create a Spring application context
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);

        try {
            // Create the dynamic bean loader
            DynamicBeanLoader loader = new DynamicBeanLoader(context);

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

            // Load properties from a .properties file
            Properties properties = new Properties();
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("bigqueue-config.properties")) {
                assertNotNull(input, "Properties file not found!");
                properties.load(input);
            }

            // Set properties using the helper method
            loader.setPropertiesOnBean(configBean, properties);

            // Verify properties were set
            Class<?> configClass = configBean.getClass();

            Object putErrorsToBackOfQueue = configClass.getMethod("isPutErrorsToBackOfQueue").invoke(configBean);
            assertEquals(true, putErrorsToBackOfQueue);

            System.out.println("Successfully set properties on Spring bean from Properties file!");
        } finally {
            context.close();
        }
    }
}
