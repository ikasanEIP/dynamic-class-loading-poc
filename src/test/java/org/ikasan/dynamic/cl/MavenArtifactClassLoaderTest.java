package org.ikasan.dynamic.cl;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MavenArtifactClassLoader.
 */
class MavenArtifactClassLoaderTest {

    @Test
    void testLoadArtifactAndLoadClass() throws Exception {
        // Create the class loader
        MavenArtifactClassLoader classLoader = new MavenArtifactClassLoader();

        // Load a well-known artifact (Google Guava)
        classLoader.loadArtifact("com.google.guava:guava:32.1.3-jre");

        // Load a class from the artifact
        Class<?> clazz = classLoader.loadClassFromArtifact("com.google.common.collect.Lists");

        // Verify the class was loaded
        assertNotNull(clazz);
        assertEquals("com.google.common.collect.Lists", clazz.getName());

        // Create an instance and invoke a method
        Method newArrayListMethod = clazz.getMethod("newArrayList");
        Object arrayListInstance = newArrayListMethod.invoke(null);

        // Verify the instance was created
        assertNotNull(arrayListInstance);
        assertTrue(arrayListInstance instanceof List);
    }

    @Test
    void testLoadArtifactOnly() throws Exception {
        // Create the class loader
        MavenArtifactClassLoader classLoader = new MavenArtifactClassLoader();

        // Load only the artifact without dependencies
        classLoader.loadArtifactOnly("commons-lang:commons-lang:2.6");

        // Load a class from the artifact
        Class<?> clazz = classLoader.loadClassFromArtifact("org.apache.commons.lang.StringUtils");

        // Verify the class was loaded
        assertNotNull(clazz);
        assertEquals("org.apache.commons.lang.StringUtils", clazz.getName());

        // Invoke a static method on the loaded class
        Method upperCaseMethod = clazz.getMethod("upperCase", String.class);
        Object result = upperCaseMethod.invoke(null, "hello");

        // Verify the method worked
        assertEquals("HELLO", result);
    }

    @Test
    void testLoadClassNotFound() {
        MavenArtifactClassLoader classLoader = new MavenArtifactClassLoader();

        // Try to load a class that doesn't exist
        assertThrows(ClassNotFoundException.class, () -> {
            classLoader.loadClassFromArtifact("com.example.NonExistentClass");
        });
    }

    @Test
    void testAddCustomRepository() throws Exception {
        MavenArtifactClassLoader classLoader = new MavenArtifactClassLoader();

        // Add a custom repository (Maven Central mirror example)
        classLoader.addRepository("custom", "https://repo1.maven.org/maven2/");

        // Load an artifact
        classLoader.loadArtifact("com.google.code.gson:gson:2.10.1");

        // Load a class from the artifact
        Class<?> gsonClass = classLoader.loadClassFromArtifact("com.google.gson.Gson");

        // Verify the class was loaded
        assertNotNull(gsonClass);
        assertEquals("com.google.gson.Gson", gsonClass.getName());

        // Create an instance and use it
        Object gsonInstance = gsonClass.getDeclaredConstructor().newInstance();

        // Use Gson to serialize a simple object
        Method toJsonMethod = gsonClass.getMethod("toJson", Object.class);
        String json = (String) toJsonMethod.invoke(gsonInstance, "test");

        // Verify the serialization worked
        assertEquals("\"test\"", json);
    }

    @Test
    void testInstantiateLoadedClass() throws Exception {
        MavenArtifactClassLoader classLoader = new MavenArtifactClassLoader();

        // Load Gson library
        classLoader.loadArtifact("com.google.code.gson:gson:2.10.1");

        // Load the Gson class
        Class<?> gsonClass = classLoader.loadClassFromArtifact("com.google.gson.Gson");

        // Create an instance
        Object gsonInstance = gsonClass.getDeclaredConstructor().newInstance();

        // Verify the instance was created
        assertNotNull(gsonInstance);
        assertEquals("com.google.gson.Gson", gsonInstance.getClass().getName());

        // Test serialization and deserialization
        Method toJsonMethod = gsonClass.getMethod("toJson", Object.class);
        Method fromJsonMethod = gsonClass.getMethod("fromJson", String.class, Class.class);

        // Serialize a number
        String json = (String) toJsonMethod.invoke(gsonInstance, 42);
        assertEquals("42", json);

        // Deserialize back to an integer
        Object number = fromJsonMethod.invoke(gsonInstance, "42", Integer.class);
        assertEquals(42, number);
    }

    @Test
    void testLoadArtifactWithDependencies() throws Exception {
        // Create the class loader
        MavenArtifactClassLoader classLoader = new MavenArtifactClassLoader();

        // Load Apache HttpClient which has multiple dependencies (httpcore, commons-logging, etc.)
        classLoader.loadArtifact("org.apache.httpcomponents:httpclient:4.5.14");

        // Load the main HttpClient class
        Class<?> httpClientBuilderClass = classLoader.loadClassFromArtifact("org.apache.http.impl.client.HttpClientBuilder");
        assertNotNull(httpClientBuilderClass);

        // Create an HttpClient instance using the builder
        Method createMethod = httpClientBuilderClass.getMethod("create");
        Object builderInstance = createMethod.invoke(null);
        assertNotNull(builderInstance);

        Method buildMethod = httpClientBuilderClass.getMethod("build");
        Object httpClientInstance = buildMethod.invoke(builderInstance);
        assertNotNull(httpClientInstance);

        // Verify we can load a class from a dependency (httpcore)
        Class<?> httpRequestClass = classLoader.loadClassFromArtifact("org.apache.http.HttpRequest");
        assertNotNull(httpRequestClass);
        assertEquals("org.apache.http.HttpRequest", httpRequestClass.getName());

        // Verify we can load a class from another dependency (commons-logging)
        Class<?> logFactoryClass = classLoader.loadClassFromArtifact("org.apache.commons.logging.LogFactory");
        assertNotNull(logFactoryClass);

        // Use the LogFactory to get a logger
        Method getLogMethod = logFactoryClass.getMethod("getLog", Class.class);
        Object logger = getLogMethod.invoke(null, httpClientBuilderClass);
        assertNotNull(logger);
    }

    @Test
    void testLoadIkasanBigQueueAndInstantiate() throws Exception {
        // Create the class loader
        MavenArtifactClassLoader classLoader = new MavenArtifactClassLoader();

        // Load the Ikasan BigQueue artifact with all its dependencies
        classLoader.loadArtifact("org.ikasan:bigqueue:1.0.0");
        classLoader.loadArtifact("org.ikasan:ikasan-big-queue:5.0.0");

        // Load and instantiate the configuration class
        Class<?> configClass = classLoader.loadClassFromArtifact("org.ikasan.component.endpoint.bigqueue.consumer.configuration.BigQueueConsumerConfiguration");
        assertNotNull(configClass);
        assertEquals("org.ikasan.component.endpoint.bigqueue.consumer.configuration.BigQueueConsumerConfiguration", configClass.getName());

        // Create an instance of the configuration
        Object configInstance = configClass.getDeclaredConstructor().newInstance();
        assertNotNull(configInstance);
        assertEquals("org.ikasan.component.endpoint.bigqueue.consumer.configuration.BigQueueConsumerConfiguration", configInstance.getClass().getName());

        // Now load BigQueueConsumer class
        Class<?> bigQueueConsumerClass = classLoader.loadClassFromArtifact("org.ikasan.component.endpoint.bigqueue.consumer.BigQueueConsumer");
        assertNotNull(bigQueueConsumerClass);
        assertEquals("org.ikasan.component.endpoint.bigqueue.consumer.BigQueueConsumer", bigQueueConsumerClass.getName());

        // BigQueueConsumer doesn't have a no-arg constructor, it requires dependencies
        // Let's verify the class is loaded and inspect its constructors
        var constructors = bigQueueConsumerClass.getDeclaredConstructors();
        assertTrue(constructors.length > 0, "BigQueueConsumer should have constructors");

        // Verify the class has the expected methods
        Method setConfigurationMethod = bigQueueConsumerClass.getMethod("setConfiguration", configClass);
        assertNotNull(setConfigurationMethod);

        Method getConfigurationMethod = bigQueueConsumerClass.getMethod("getConfiguration");
        assertNotNull(getConfigurationMethod);

        // Successfully loaded BigQueueConsumer class and its configuration from dynamically loaded Maven artifacts!
        System.out.println("Successfully loaded BigQueueConsumer class from ikasan-big-queue with " + constructors.length + " constructor(s)");
    }
}
