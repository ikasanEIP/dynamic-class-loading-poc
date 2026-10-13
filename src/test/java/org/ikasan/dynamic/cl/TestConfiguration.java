package org.ikasan.dynamic.cl;

import jakarta.annotation.PostConstruct;
import org.ikasan.builder.BuilderFactory;
import org.ikasan.builder.ModuleFactory;
import org.ikasan.module.ConfiguredModuleConfiguration;
import org.ikasan.spec.flow.Flow;
import org.ikasan.spec.module.Module;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;

/**
 * Configuration for dynamically loading BigQueueConsumer bean.
 */
@Configuration
public class TestConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    @Primary
    public ConfiguredModuleConfiguration configuredModuleConfiguration() {
        ConfiguredModuleConfiguration configuration = new ConfiguredModuleConfiguration();
        configuration.getFlowDefinitions().put("test-flow1", "MANUAL");
        configuration.getFlowDefinitionProfiles().put("test-flow1", "demo");
        configuration.getFlowDefinitions().put("test-flow2", "MANUAL");
        configuration.getFlowDefinitionProfiles().put("test-flow2", "demo");
        return configuration;
    }

    @PostConstruct
    public void initializeDynamicBeans() throws Exception {
        // Use dynamic class loading to load BigQueueConsumer from Maven
        MavenArtifactClassLoader classLoader = new MavenArtifactClassLoader();

        // Load the ikasan-big-queue component which contains BigQueueConsumer
        classLoader.loadArtifact("org.ikasan:bigqueue:1.0.0");
        classLoader.loadArtifact("org.ikasan:ikasan-big-queue:5.0.0");

        // Set the thread context classloader to allow Spring to access dynamically loaded classes
        // Keep it set throughout the application lifecycle so dynamically loaded classes remain visible
        Thread.currentThread().setContextClassLoader(classLoader);

        // Load the BigQueueConsumer class
        Class<?> bigQueueConsumerClass = classLoader.loadClass("org.ikasan.component.endpoint.bigqueue" +
                ".consumer.BigQueueConsumer");

        // Load IBigQueue (BigQueueImpl) class
        Class<?> bigQueueClass = classLoader.loadClass("org.ikasan.bigqueue.BigQueueImpl");

        // Load InboundQueueMessageRunner class
        Class<?> inboundQueueMessageRunnerClass = classLoader.loadClass("org.ikasan.component.endpoint" +
                ".bigqueue.consumer.InboundQueueMessageRunner");

        // Load BigQueueMessageJsonSerialiser class
        Class<?> serialiserClass = classLoader.loadClass("org.ikasan.component.endpoint.bigqueue" +
                ".serialiser.BigQueueMessageJsonSerialiser");

        // Get TransactionManager from Spring context
        jakarta.transaction.TransactionManager transactionManager = applicationContext
                .getBean(jakarta.transaction.TransactionManager.class);

        // Create IBigQueue instance with temp directory and queue name
        String queueDir = System.getProperty("java.io.tmpdir") + "/bigqueue-test";
        String queueName = "test-queue";
        Object bigQueue = bigQueueClass.getConstructor(String.class, String.class)
            .newInstance(queueDir, queueName);

        // Create BigQueueMessageJsonSerialiser instance
        Object serialiser = serialiserClass.getConstructor()
            .newInstance();

        // Create InboundQueueMessageRunner instance with bigQueue and serialiser
        Object inboundQueueMessageRunner = inboundQueueMessageRunnerClass
            .getConstructor(classLoader.loadClass("org.ikasan.bigqueue.IBigQueue"),
                           classLoader.loadClass("org.ikasan.spec.serialiser.Serialiser"))
            .newInstance(bigQueue, serialiser);

        // Create the BigQueueConsumer instance manually using reflection
        Object bigQueueConsumer = bigQueueConsumerClass
            .getConstructor(
                classLoader.loadClass("org.ikasan.bigqueue.IBigQueue"),
                classLoader.loadClass("org.ikasan.component.endpoint.bigqueue.consumer.InboundQueueMessageRunner"),
                jakarta.transaction.TransactionManager.class
            )
            .newInstance(bigQueue, inboundQueueMessageRunner, transactionManager);

        // Register the BigQueueConsumer instance
        SpringBeanRegistry.registerBeanInstance(applicationContext, "bigQueueConsumer", bigQueueConsumer);

        // Load BigQueueProducer class
        Class<?> bigQueueProducerClass = classLoader.loadClass("org.ikasan.component.endpoint.bigqueue.producer.BigQueueProducer");

        // Create outbound BigQueue instance
        String outboundQueueDir = System.getProperty("java.io.tmpdir") + "/bigqueue-outbound";
        String outboundQueueName = "outbound-queue";
        Object outboundBigQueue = bigQueueClass.getConstructor(String.class, String.class)
            .newInstance(outboundQueueDir, outboundQueueName);

        // Create BigQueueProducer instance with outbound queue
        Object bigQueueProducer = bigQueueProducerClass
            .getConstructor(classLoader.loadClass("org.ikasan.bigqueue.IBigQueue"))
            .newInstance(outboundBigQueue);

        // Register the BigQueueProducer instance
        SpringBeanRegistry.registerBeanInstance(applicationContext, "bigQueueProducer", bigQueueProducer);
    }
}
