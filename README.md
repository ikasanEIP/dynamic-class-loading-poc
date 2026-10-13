# Dynamic Class Loading with Spring Bean Integration

A comprehensive Java library for dynamically loading classes from Maven artifacts at runtime and seamlessly integrating them as Spring beans with full property binding support. Includes a **production-ready integration with Ikasan EIP** demonstrating dynamic loading of BigQueue consumer/producer components into enterprise integration flows.

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture](#architecture)
  - [MavenArtifactClassLoader](#1-mavenartifactclassloader)
  - [DynamicBeanLoader](#2-dynamicbeanloader)
  - [SpringBootDynamicBeanLoader](#3-springbootdynamicbeanloader)
- [Quick Start](#quick-start)
  - [Basic Maven Artifact Loading](#basic-maven-artifact-loading)
  - [Spring Bean Registration](#spring-bean-registration)
  - [Property Binding from application.properties](#property-binding-from-applicationproperties)
- [Comprehensive Test Suite](#comprehensive-test-suite)
  - [MavenArtifactClassLoader Tests](#mavenartifactclassloader-tests-7-tests)
  - [DynamicBeanLoader Tests](#dynamicbeanloader-tests-7-tests)
  - [SpringBootDynamicBeanLoader Tests](#springbootdynamicbeanloader-tests-3-tests)
- [Real-World Example: Ikasan Integration Platform](#real-world-example-ikasan-integration-platform)
  - [Architecture Overview](#architecture-overview)
  - [Components Dynamically Loaded](#components-dynamically-loaded)
  - [FlowFactory Integration](#flowfactory-integration)
  - [Solving the ClassLoader Visibility Challenge](#solving-the-classloader-visibility-challenge)
  - [ModuleFactoryTest Demonstration](#modulefactorytest-demonstration)
  - [Key Benefits of This Approach](#key-benefits-of-this-approach)
  - [Running the Ikasan Integration Test](#running-the-ikasan-integration-test)
- [Use Cases](#use-cases)
- [Advanced Topics](#advanced-topics)
  - [Custom Repositories](#custom-repositories)
  - [ClassLoader Isolation](#classloader-isolation)
  - [Bean Lifecycle Hooks](#bean-lifecycle-hooks)
- [Dependencies](#dependencies)
- [Running Tests](#running-tests)
- [Best Practices](#best-practices)
- [Contributing](#contributing)
- [License](#license)

## Overview

This project provides a powerful solution for runtime plugin architectures, allowing you to:

- **Dynamically load JARs** from Maven coordinates without recompilation
- **Register classes as Spring beans** at runtime
- **Bind properties** from `application.properties` or other sources
- **Manage full bean lifecycle** through Spring's IoC container
- **Integrate with enterprise frameworks** like Ikasan EIP using the FlowFactory paradigm

## Key Features

### 🚀 Maven Artifact Resolution
- Download JARs from Maven Central or custom repositories
- Automatic dependency resolution
- ClassLoader isolation

### 🌱 Spring Integration
- Dynamic bean registration
- Full Spring lifecycle management
- Property binding from multiple sources
- Support for Spring Boot's `application.properties`

### ⚙️ Flexible Configuration
- Map-based property binding
- Properties file support
- Spring Environment integration
- Automatic type conversion (String, boolean, int, Integer, etc.)

### 🏭 Enterprise Integration (Ikasan EIP)
- Production-ready integration with Ikasan Enterprise Integration Platform
- Dynamic loading of BigQueue consumer and producer components
- FlowFactory paradigm for building integration flows
- Complete working example in `ModuleFactoryTest`
- Solves classloader visibility challenges for Spring AOP

## Architecture

The library consists of three main components:

### 1. MavenArtifactClassLoader
Core class loader that resolves and loads Maven artifacts dynamically.

```java
MavenArtifactClassLoader classLoader = new MavenArtifactClassLoader();
classLoader.loadArtifact("com.google.code.gson:gson:2.10.1");
Class<?> gsonClass = classLoader.loadClassFromArtifact("com.google.gson.Gson");
```

### 2. DynamicBeanLoader
Combines Maven artifact loading with Spring bean registration.

```java
DynamicBeanLoader loader = new DynamicBeanLoader(applicationContext);
Object bean = loader.loadAndRegisterBean(
    "com.google.code.gson:gson:2.10.1",
    "com.google.gson.Gson",
    "dynamicGson"
);
```

### 3. SpringBootDynamicBeanLoader
Spring Boot-aware loader with `application.properties` binding.

```java
SpringBootDynamicBeanLoader loader = new SpringBootDynamicBeanLoader(context);
Object bean = loader.loadRegisterAndBindProperties(
    artifacts,
    className,
    "beanName",
    "property.prefix"
);
```

## Quick Start

### Basic Maven Artifact Loading

```java
// Create class loader
MavenArtifactClassLoader classLoader = new MavenArtifactClassLoader();

// Load artifact and dependencies
classLoader.loadArtifact("com.google.code.gson:gson:2.10.1");

// Load and instantiate class
Class<?> gsonClass = classLoader.loadClassFromArtifact("com.google.gson.Gson");
Object gsonInstance = gsonClass.getDeclaredConstructor().newInstance();
```

### Spring Bean Registration

```java
// Create Spring context
AnnotationConfigApplicationContext context = 
    new AnnotationConfigApplicationContext(AppConfig.class);

// Create bean loader
DynamicBeanLoader loader = new DynamicBeanLoader(context);

// Load artifact and register as Spring bean
Object gsonBean = loader.loadAndRegisterBean(
    "com.google.code.gson:gson:2.10.1",
    "com.google.gson.Gson",
    "dynamicGson"
);

// Bean is now available in Spring context
Object bean = context.getBean("dynamicGson");
```

### Property Binding from application.properties

**application.properties:**
```properties
bigqueue.putErrorsToBackOfQueue=true
bigqueue.batchSize=250
```

**Code:**
```java
@Configuration
@PropertySource("classpath:application.properties")
class AppConfig {}

AnnotationConfigApplicationContext context = 
    new AnnotationConfigApplicationContext(AppConfig.class);

SpringBootDynamicBeanLoader loader = new SpringBootDynamicBeanLoader(context);

// Load, register, and bind properties in one call
Object configBean = loader.loadRegisterAndBindProperties(
    new String[]{"org.ikasan:ikasan-big-queue:5.0.0"},
    "org.ikasan.component.endpoint.bigqueue.consumer.configuration.BigQueueConsumerConfiguration",
    "bigQueueConfig",
    "bigqueue"  // property prefix
);

// Bean is configured from application.properties!
```

## Comprehensive Test Suite

The project includes 17 comprehensive tests demonstrating all functionality.

### MavenArtifactClassLoader Tests (7 tests)

#### 1. testLoadArtifactAndLoadClass
**Purpose:** Demonstrates basic artifact loading with dependencies

```java
classLoader.loadArtifact("com.google.guava:guava:32.1.3-jre");
Class<?> listsClass = classLoader.loadClassFromArtifact("com.google.common.collect.Lists");
Object arrayList = listsClass.getMethod("newArrayList").invoke(null);
```

**Key Points:**
- Loads Google Guava with all transitive dependencies
- Creates instances using static factory methods
- Verifies object creation from dynamically loaded classes

#### 2. testLoadArtifactOnly
**Purpose:** Load a single artifact without dependencies

```java
classLoader.loadArtifactOnly("commons-lang:commons-lang:2.6");
Class<?> stringUtils = classLoader.loadClassFromArtifact("org.apache.commons.lang.StringUtils");
Object result = stringUtils.getMethod("upperCase", String.class).invoke(null, "hello");
```

**Key Points:**
- Uses `loadArtifactOnly()` for dependency-free loading
- Invokes static utility methods via reflection
- Demonstrates working with utility classes

#### 3. testLoadClassNotFound
**Purpose:** Validates proper exception handling

```java
assertThrows(ClassNotFoundException.class, () -> {
    classLoader.loadClassFromArtifact("com.example.NonExistentClass");
});
```

**Key Points:**
- Ensures proper error handling
- Tests edge cases

#### 4. testAddCustomRepository
**Purpose:** Demonstrates custom Maven repository configuration

```java
classLoader.addRepository("custom", "https://repo1.maven.org/maven2/");
classLoader.loadArtifact("com.google.code.gson:gson:2.10.1");
```

**Key Points:**
- Adds custom Maven repositories
- Useful for private/corporate repositories
- Tests repository resolution

#### 5. testInstantiateLoadedClass
**Purpose:** Full serialization/deserialization workflow

```java
Class<?> gsonClass = classLoader.loadClassFromArtifact("com.google.gson.Gson");
Object gsonInstance = gsonClass.getDeclaredConstructor().newInstance();

Method toJsonMethod = gsonClass.getMethod("toJson", Object.class);
String json = (String) toJsonMethod.invoke(gsonInstance, 42);

Method fromJsonMethod = gsonClass.getMethod("fromJson", String.class, Class.class);
Object number = fromJsonMethod.invoke(gsonInstance, "42", Integer.class);
```

**Key Points:**
- Creates instances from loaded classes
- Invokes methods via reflection
- Demonstrates full object lifecycle

#### 6. testLoadArtifactWithDependencies
**Purpose:** Complex dependency resolution with Apache HttpClient

```java
classLoader.loadArtifact("org.apache.httpcomponents:httpclient:4.5.14");

// Main artifact
Class<?> httpClientBuilder = classLoader.loadClassFromArtifact(
    "org.apache.http.impl.client.HttpClientBuilder"
);

// Dependency: httpcore
Class<?> httpRequest = classLoader.loadClassFromArtifact(
    "org.apache.http.HttpRequest"
);

// Dependency: commons-logging
Class<?> logFactory = classLoader.loadClassFromArtifact(
    "org.apache.commons.logging.LogFactory"
);
```

**Key Points:**
- Loads artifact with multiple transitive dependencies
- Verifies all dependency classes are accessible
- Tests complex dependency graphs

#### 7. testLoadIkasanBigQueueAndInstantiate
**Purpose:** Load and inspect Ikasan BigQueue component

```java
classLoader.loadArtifact("org.ikasan:bigqueue:1.0.0");
classLoader.loadArtifact("org.ikasan:ikasan-big-queue:5.0.0");

Class<?> configClass = classLoader.loadClassFromArtifact(
    "org.ikasan.component.endpoint.bigqueue.consumer.configuration.BigQueueConsumerConfiguration"
);
Object configInstance = configClass.getDeclaredConstructor().newInstance();

Class<?> consumerClass = classLoader.loadClassFromArtifact(
    "org.ikasan.component.endpoint.bigqueue.consumer.BigQueueConsumer"
);
```

**Key Points:**
- Loads multiple related artifacts
- Demonstrates real-world enterprise component loading
- Inspects class structure (constructors, methods)

---

### DynamicBeanLoader Tests (7 tests)

#### 1. testLoadGsonAndRegisterAsSpringBean
**Purpose:** Basic Spring bean registration from Maven artifact

```java
DynamicBeanLoader loader = new DynamicBeanLoader(context);

Object gsonBean = loader.loadAndRegisterBean(
    "com.google.code.gson:gson:2.10.1",
    "com.google.gson.Gson",
    "dynamicGson"
);

assertTrue(context.containsBean("dynamicGson"));
Object retrievedBean = context.getBean("dynamicGson");
assertEquals(gsonBean, retrievedBean);
```

**Key Points:**
- Loads JAR from Maven
- Registers as Spring bean
- Bean available via Spring context
- **Spring Bean Creation:** Fully managed by Spring IoC

#### 2. testLoadBigQueueConsumerConfigAndRegisterAsSpringBean
**Purpose:** Spring bean with manual property setting

```java
Object configBean = loader.loadMultipleArtifactsAndRegisterBean(
    new String[]{"org.ikasan:bigqueue:1.0.0", "org.ikasan:ikasan-big-queue:5.0.0"},
    "org.ikasan.component.endpoint.bigqueue.consumer.configuration.BigQueueConsumerConfiguration",
    "bigQueueConfig"
);

// Set properties via reflection
Class<?> configClass = configBean.getClass();
configClass.getMethod("setPutErrorsToBackOfQueue", boolean.class)
    .invoke(configBean, true);

// Verify
Object value = configClass.getMethod("isPutErrorsToBackOfQueue")
    .invoke(configBean);
assertEquals(true, value);
```

**Key Points:**
- Loads multiple related artifacts
- **Spring Bean Creation:** Bean registered in Spring container
- **Properties Binding:** Manual property setting via reflection
- Demonstrates property configuration workflow

#### 3. testLoadInstantiateAndRegisterBean
**Purpose:** Pre-instantiated bean registration (singleton pattern)

```java
Object configBean = loader.loadInstantiateAndRegisterBean(
    "com.google.code.gson:gson:2.10.1",
    "com.google.gson.GsonBuilder",
    "gsonBuilder"
);

// Verify singleton behavior
Object retrievedBean1 = context.getBean("gsonBuilder");
Object retrievedBean2 = context.getBean("gsonBuilder");
assertSame(retrievedBean1, retrievedBean2);
assertSame(configBean, retrievedBean1);
```

**Key Points:**
- **Spring Bean Creation:** Registered as singleton
- Instance created before registration
- Guarantees singleton scope
- Same instance returned from Spring context

#### 4. testSpringBeanRegistryDirectly
**Purpose:** Low-level Spring bean registry operations

```java
MavenArtifactClassLoader classLoader = new MavenArtifactClassLoader();
classLoader.loadArtifact("commons-lang:commons-lang:2.6");
Class<?> stringUtilsClass = classLoader.loadClassFromArtifact(
    "org.apache.commons.lang.StringUtils"
);

// Register directly
SpringBeanRegistry.registerBean(context, "stringUtils", stringUtilsClass);
assertTrue(SpringBeanRegistry.beanExists(context, "stringUtils"));

// Remove bean
SpringBeanRegistry.removeBean(context, "stringUtils");
assertFalse(SpringBeanRegistry.beanExists(context, "stringUtils"));
```

**Key Points:**
- **Spring Bean Creation:** Direct bean definition manipulation
- Dynamic bean lifecycle management
- Add/remove beans at runtime
- Fine-grained control over bean registry

#### 5. testSetPropertiesFromMap
**Purpose:** Property binding from Map

```java
Object configBean = loader.loadMultipleArtifactsAndRegisterBean(
    artifacts,
    "org.ikasan.component.endpoint.bigqueue.consumer.configuration.BigQueueConsumerConfiguration",
    "bigQueueConfig"
);

Map<String, Object> properties = new HashMap<>();
properties.put("putErrorsToBackOfQueue", true);

// Bind properties
loader.setPropertiesOnBean(configBean, properties);

// Verify
Object value = configBean.getClass()
    .getMethod("isPutErrorsToBackOfQueue")
    .invoke(configBean);
assertEquals(true, value);
```

**Key Points:**
- **Properties Binding:** Map-based configuration
- Type-safe property setting
- Flexible property source
- Programmatic configuration

#### 6. testSetPropertiesFromPropertiesFile
**Purpose:** Property binding from .properties file

**bigqueue-config.properties:**
```properties
putErrorsToBackOfQueue=true
```

**Code:**
```java
Properties properties = new Properties();
try (InputStream input = getClass().getClassLoader()
        .getResourceAsStream("bigqueue-config.properties")) {
    properties.load(input);
}

loader.setPropertiesOnBean(configBean, properties);
```

**Key Points:**
- **Properties Binding:** File-based configuration
- Standard Java Properties format
- Automatic type conversion
- External configuration support

#### 7. testLoadBigQueueConsumerAsSpringBean
**Purpose:** Complex multi-bean scenario

```java
// Register configuration bean
Object configBean = loader.loadMultipleArtifactsAndRegisterBean(
    artifacts,
    "org.ikasan.component.endpoint.bigqueue.consumer.configuration.BigQueueConsumerConfiguration",
    "bigQueueConfig"
);

assertTrue(context.containsBean("bigQueueConfig"));

// Load consumer class
Class<?> consumerClass = loader.getClassLoader().loadClassFromArtifact(
    "org.ikasan.component.endpoint.bigqueue.consumer.BigQueueConsumer"
);

// Configuration bean available in Spring
Object springConfig = context.getBean("bigQueueConfig");
```

**Key Points:**
- **Spring Bean Creation:** Multiple beans from same artifact
- Configuration bean managed by Spring
- Component class accessible for later use
- Demonstrates dependency injection patterns

---

### SpringBootDynamicBeanLoader Tests (3 tests)

#### 1. testLoadBeanAndBindPropertiesFromApplicationProperties
**Purpose:** Spring Boot property binding with property prefixes

**application.properties:**
```properties
bigqueue.putErrorsToBackOfQueue=true
```

**Code:**
```java
@Configuration
@PropertySource("classpath:application.properties")
static class TestConfig {}

AnnotationConfigApplicationContext context = 
    new AnnotationConfigApplicationContext(TestConfig.class);

SpringBootDynamicBeanLoader loader = new SpringBootDynamicBeanLoader(context);

Object configBean = loader.loadMultipleArtifactsAndRegisterBean(
    artifacts,
    "org.ikasan.component.endpoint.bigqueue.consumer.configuration.BigQueueConsumerConfiguration",
    "bigQueueConfig"
);

// Bind from application.properties with prefix
loader.setPropertiesFromEnvironment(configBean, "bigqueue");

// Verify property was bound
Object value = configBean.getClass()
    .getMethod("isPutErrorsToBackOfQueue")
    .invoke(configBean);
assertEquals(true, value);
```

**Key Points:**
- **Spring Bean Creation:** Standard Spring bean registration
- **Properties Binding:** Automatic binding from `application.properties`
- Property prefix support (`bigqueue.*`)
- Uses Spring's Environment abstraction
- Type conversion handled automatically (String → boolean)

#### 2. testLoadRegisterAndBindPropertiesInOneCall
**Purpose:** Convenience method for complete workflow

```java
SpringBootDynamicBeanLoader loader = new SpringBootDynamicBeanLoader(context);

// Single call: load + register + bind
Object configBean = loader.loadRegisterAndBindProperties(
    new String[]{"org.ikasan:bigqueue:1.0.0", "org.ikasan:ikasan-big-queue:5.0.0"},
    "org.ikasan.component.endpoint.bigqueue.consumer.configuration.BigQueueConsumerConfiguration",
    "bigQueueConfig",
    "bigqueue"  // property prefix
);

assertTrue(context.containsBean("bigQueueConfig"));

// Properties automatically bound from application.properties
Object value = configBean.getClass()
    .getMethod("isPutErrorsToBackOfQueue")
    .invoke(configBean);
assertEquals(true, value);
```

**Key Points:**
- **Spring Bean Creation:** One-line bean creation
- **Properties Binding:** Automatic from application.properties
- Streamlined API
- Production-ready convenience method
- Reduces boilerplate

#### 3. testBindPropertiesFromEnvironment
**Purpose:** Spring Environment integration

```java
SpringBootDynamicBeanLoader loader = new SpringBootDynamicBeanLoader(context);

Object configBean = loader.loadMultipleArtifactsAndRegisterBean(
    artifacts,
    className,
    "bigQueueConfig"
);

// Bind using Spring Environment
loader.bindPropertiesFromEnvironment(configBean, "bigqueue");

// Verify it's a Spring-managed singleton
Object retrievedBean = context.getBean("bigQueueConfig");
assertSame(configBean, retrievedBean);
```

**Key Points:**
- **Spring Bean Creation:** Full Spring lifecycle
- **Properties Binding:** Spring Environment integration
- Singleton scope verified
- Works with all Spring property sources
- Supports property placeholders and profiles

---

## Property Binding Deep Dive

### Type Conversion

The library automatically handles type conversion from String to:

- `String` - Direct assignment
- `boolean` / `Boolean` - Uses `Boolean.parseBoolean()`
- `int` / `Integer` - Uses `Integer.parseInt()` / `Integer.valueOf()`
- Additional types can be added by extending `DynamicBeanLoader`

### Property Sources

Properties can be loaded from:

1. **Map** - Programmatic configuration
```java
Map<String, Object> props = new HashMap<>();
props.put("property", value);
loader.setPropertiesOnBean(bean, props);
```

2. **Properties File** - External configuration
```java
Properties props = new Properties();
props.load(new FileInputStream("config.properties"));
loader.setPropertiesOnBean(bean, props);
```

3. **Spring Environment** - Full Spring Boot integration
```java
loader.setPropertiesFromEnvironment(bean, "prefix");
```

### Property Prefix Pattern

Use property prefixes to organize related properties:

```properties
# application.properties
myapp.database.url=jdbc:mysql://localhost/db
myapp.database.username=user
myapp.cache.enabled=true
myapp.cache.ttl=300
```

```java
loader.setPropertiesFromEnvironment(databaseBean, "myapp.database");
loader.setPropertiesFromEnvironment(cacheBean, "myapp.cache");
```

## Use Cases

### 1. Plugin Architecture
Dynamically load and configure plugins at runtime without redeployment.

```java
String[] pluginCoordinates = config.getStringArray("plugins");
for (String coordinate : pluginCoordinates) {
    Object plugin = loader.loadRegisterAndBindProperties(
        new String[]{coordinate},
        "com.myapp.Plugin",
        "plugin-" + coordinate,
        "plugin." + pluginName
    );
}
```

### 2. Multi-Tenancy
Load tenant-specific components with isolated classloaders.

```java
for (Tenant tenant : tenants) {
    MavenArtifactClassLoader tenantClassLoader = new MavenArtifactClassLoader();
    // Load tenant-specific version
    tenantClassLoader.loadArtifact(tenant.getComponentVersion());
}
```

### 3. A/B Testing
Load different component versions for testing.

```java
Object versionA = loader.loadRegisterAndBindProperties(
    new String[]{"com.myapp:component:1.0.0"},
    "com.myapp.Component",
    "componentV1",
    "component.v1"
);

Object versionB = loader.loadRegisterAndBindProperties(
    new String[]{"com.myapp:component:2.0.0"},
    "com.myapp.Component",
    "componentV2",
    "component.v2"
);
```

### 4. Hot Reload / Hot Swap
Update components without restarting the application.

```java
// Remove old version
SpringBeanRegistry.removeBean(context, "component");

// Load new version
loader.loadRegisterAndBindProperties(
    new String[]{newVersion},
    className,
    "component",
    "component.config"
);
```

## Real-World Example: Ikasan Integration Platform

This project demonstrates a production-ready integration with **Ikasan EIP (Enterprise Integration Platform)**, dynamically loading BigQueue consumer and producer components and wiring them into an Ikasan flow using the FlowFactory paradigm.

### Architecture Overview

The integration demonstrates:
1. **Dynamic Component Loading** - BigQueue components loaded from Maven at runtime
2. **Spring Bean Registration** - Components registered as Spring beans without static configuration
3. **FlowFactory Integration** - Dynamically loaded components wired into Ikasan flows
4. **ClassLoader Isolation** - Custom classloader for runtime component visibility

### Components Dynamically Loaded

#### 1. BigQueueConsumer
**Maven Coordinates:** `org.ikasan:ikasan-big-queue:5.0.0`

```java
// Dynamically load BigQueue and ikasan-big-queue artifacts
classLoader.loadArtifact("org.ikasan:bigqueue:1.0.0");
classLoader.loadArtifact("org.ikasan:ikasan-big-queue:5.0.0");

// Load BigQueueConsumer class
Class<?> bigQueueConsumerClass = classLoader.loadClass(
    "org.ikasan.component.endpoint.bigqueue.consumer.BigQueueConsumer"
);

// Create supporting components
Class<?> bigQueueClass = classLoader.loadClass("org.ikasan.bigqueue.BigQueueImpl");
Class<?> inboundQueueMessageRunnerClass = classLoader.loadClass(
    "org.ikasan.component.endpoint.bigqueue.consumer.InboundQueueMessageRunner"
);
Class<?> serialiserClass = classLoader.loadClass(
    "org.ikasan.component.endpoint.bigqueue.serialiser.BigQueueMessageJsonSerialiser"
);

// Create IBigQueue instance
String queueDir = System.getProperty("java.io.tmpdir") + "/bigqueue-test";
Object bigQueue = bigQueueClass.getConstructor(String.class, String.class)
    .newInstance(queueDir, "test-queue");

// Create serialiser
Object serialiser = serialiserClass.getConstructor().newInstance();

// Create InboundQueueMessageRunner with bigQueue and serialiser
Object inboundQueueMessageRunner = inboundQueueMessageRunnerClass
    .getConstructor(
        classLoader.loadClass("org.ikasan.bigqueue.IBigQueue"),
        classLoader.loadClass("org.ikasan.spec.serialiser.Serialiser")
    )
    .newInstance(bigQueue, serialiser);

// Get TransactionManager from Spring context
jakarta.transaction.TransactionManager transactionManager =
    applicationContext.getBean(jakarta.transaction.TransactionManager.class);

// Create BigQueueConsumer with all dependencies
Object bigQueueConsumer = bigQueueConsumerClass
    .getConstructor(
        classLoader.loadClass("org.ikasan.bigqueue.IBigQueue"),
        classLoader.loadClass("org.ikasan.component.endpoint.bigqueue.consumer.InboundQueueMessageRunner"),
        jakarta.transaction.TransactionManager.class
    )
    .newInstance(bigQueue, inboundQueueMessageRunner, transactionManager);

// Register as Spring bean
SpringBeanRegistry.registerBeanInstance(applicationContext, "bigQueueConsumer", bigQueueConsumer);
```

**Key Points:**
- BigQueueConsumer reads from an inbound BigQueue
- Requires IBigQueue, InboundQueueMessageRunner, and TransactionManager
- InboundQueueMessageRunner handles message deserialization
- All dependencies constructed via reflection

#### 2. BigQueueProducer
**Maven Coordinates:** `org.ikasan:ikasan-big-queue:5.0.0`

```java
// Load BigQueueProducer class (from same artifact as consumer)
Class<?> bigQueueProducerClass = classLoader.loadClass(
    "org.ikasan.component.endpoint.bigqueue.producer.BigQueueProducer"
);

// Create outbound BigQueue instance
String outboundQueueDir = System.getProperty("java.io.tmpdir") + "/bigqueue-outbound";
Object outboundBigQueue = bigQueueClass.getConstructor(String.class, String.class)
    .newInstance(outboundQueueDir, "outbound-queue");

// Create BigQueueProducer with outbound queue
Object bigQueueProducer = bigQueueProducerClass
    .getConstructor(classLoader.loadClass("org.ikasan.bigqueue.IBigQueue"))
    .newInstance(outboundBigQueue);

// Register as Spring bean
SpringBeanRegistry.registerBeanInstance(applicationContext, "bigQueueProducer", bigQueueProducer);
```

**Key Points:**
- BigQueueProducer writes to an outbound BigQueue
- Simpler constructor - only requires IBigQueue
- Uses BigQueueMessageJsonSerialiser by default

### FlowFactory Integration

The dynamically loaded components are wired into Ikasan flows using the **FlowFactory paradigm**, which allows for dynamic flow creation at runtime:

```java
@Configuration
public class TestModuleConfiguration implements FlowFactory {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private BuilderFactory builderFactory;

    @Autowired
    private ConfiguredModuleConfiguration configuredModuleConfiguration;

    // FlowFactory interface implementation - creates flows dynamically
    @Override
    public List<Flow> create(String flowName, String profile) {
        return List.of(createFlow(flowName));
    }

    private Flow createFlow(String name) {
        // Build flow using dynamically loaded consumer and producer
        return builderFactory.getFlowBuilder("test-module", name)
                .consumer("bigQueueConsumer", (Consumer) applicationContext.getBean("bigQueueConsumer"))
                .producer("bigQueueProducer", (Producer) applicationContext.getBean("bigQueueProducer"))
                .build();
    }

    @Bean
    public Module<Flow> testModule() {
        // Module configured with FlowFactory for dynamic flow creation
        return builderFactory.getModuleBuilder("test-module")
                .setConfiguration(this.configuredModuleConfiguration)
                .withFlowFactory(this)  // FlowFactory enables runtime flow creation
                .build();
    }
}
```

**FlowFactory Benefits:**
- **Dynamic Flow Creation** - Flows created on-demand by calling `create(flowName, profile)`
- **Multiple Flow Support** - Single factory can create multiple flows (test-flow1, test-flow2, etc.)
- **Runtime Flexibility** - Flow structure determined at runtime based on flowName parameter
- **Configuration Integration** - Works with Ikasan's ConfiguredModuleConfiguration

**Flow Diagram:**
```
┌─────────────────────┐      ┌─────────────────────┐
│  BigQueueConsumer   │─────▶│  BigQueueProducer   │
│  (dynamically       │      │  (dynamically       │
│   loaded)           │      │   loaded)           │
└─────────────────────┘      └─────────────────────┘
        │                             │
        ▼                             ▼
   Inbound Queue              Outbound Queue
   (/tmp/bigqueue-test)      (/tmp/bigqueue-outbound)
```

### Solving the ClassLoader Visibility Challenge

**Problem:** When Spring AOP tries to create proxies for dynamically loaded components, it encounters `IllegalArgumentException` because interfaces like `NullPayloadCallback` are not visible from the app classloader.

**Solution:** Set the thread context classloader to the custom `MavenArtifactClassLoader` and keep it set throughout the application lifecycle:

```java
@PostConstruct
public void initializeDynamicBeans() throws Exception {
    MavenArtifactClassLoader classLoader = new MavenArtifactClassLoader();

    // Load artifacts
    classLoader.loadArtifact("org.ikasan:bigqueue:1.0.0");
    classLoader.loadArtifact("org.ikasan:ikasan-big-queue:5.0.0");

    // Set as thread context classloader - CRITICAL for AOP proxy creation
    Thread.currentThread().setContextClassLoader(classLoader);

    // Create and register beans...
    // DO NOT restore original classloader - keep it set permanently
}
```

**Why This Works:**
- Spring AOP uses the thread context classloader to load classes for proxy creation
- Setting it to our custom classloader makes all dynamically loaded classes visible
- Keeping it set ensures visibility throughout the bean lifecycle
- Enables Spring to create JDK dynamic proxies for interfaces defined in dynamically loaded JARs

### ModuleFactoryTest Demonstration

The `ModuleFactoryTest` demonstrates the complete integration by testing flow lifecycle management:

```java
@SpringBootTest
@ContextConfiguration(classes = {TestModuleConfiguration.class, TestConfiguration.class})
@ActiveProfiles("test")
public class ModuleFactoryTest {

    @Autowired
    private Module<Flow> moduleUnderTest;

    public IkasanFlowTestRule flowTestRule = new IkasanFlowTestRule();

    @Test
    void testModuleFactoryIsAvailable() {
        // Test flow1 - dynamically created via FlowFactory
        Flow flow = moduleUnderTest.getFlow("test-flow1");
        flow.start();
        assertEquals(Flow.RUNNING, flow.getState());

        flow.stop();
        assertEquals(Flow.STOPPED, flow.getState());

        // Test flow2 - also dynamically created via FlowFactory
        flow = moduleUnderTest.getFlow("test-flow2");
        flow.start();
        assertEquals(Flow.RUNNING, flow.getState());

        flow.stop();
        assertEquals(Flow.STOPPED, flow.getState());
    }
}
```

**Test Highlights:**
- **Multiple Dynamic Flows** - Creates test-flow1 and test-flow2 on-demand via FlowFactory
- **Flow Lifecycle Testing** - Tests start/stop transitions for each flow
- **State Verification** - Validates flows reach RUNNING and STOPPED states correctly
- **Runtime Flow Creation** - Flows don't exist until `getFlow()` is called, triggering FlowFactory

**Test Configuration:**
- `TestModuleConfiguration` - Implements FlowFactory for dynamic flow creation
- `TestConfiguration` - Dynamically loads and registers BigQueue components via `@PostConstruct`
- `@SpringBootTest` - Full Spring Boot context with Ikasan autoconfiguration
- `application-test.properties` - Datasource and Ikasan configuration
- `h2-datasource-conf.xml` - H2 database bean definitions for testing

### Key Benefits of This Approach

1. **Zero Static Configuration** - No XML or annotation scanning required for BigQueue components
2. **Version Flexibility** - Can load different component versions dynamically
3. **Runtime Plugin Architecture** - Add/remove components without recompilation
4. **Spring Integration** - Full Spring bean lifecycle and AOP support
5. **FlowFactory Paradigm** - Seamless integration with Ikasan's fluent flow builder API
6. **Production Ready** - Demonstrated with real Ikasan EIP components

### Running the Ikasan Integration Test

```bash
# Set Java home to JDK 26
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-26.0.1.jdk/Contents/Home

# Run the ModuleFactoryTest
mvn test -Dtest=ModuleFactoryTest
```

**Expected Output:**
- Ikasan Spring Boot context starts successfully
- BigQueueConsumer and BigQueueProducer beans registered dynamically
- Module "test-module" initialized with FlowFactory
- Flow "test-flow1" created dynamically, started, and stopped successfully
- Flow "test-flow2" created dynamically, started, and stopped successfully
- Console output shows:
  ```
  test-flow1 started!
  test-flow1 stopped!
  test-flow2 started!
  test-flow2 stopped!
  ```
- All state assertions pass
- Test passes ✓

## Advanced Topics

### Custom Repositories

Add private Maven repositories:

```java
MavenArtifactClassLoader classLoader = new MavenArtifactClassLoader();
classLoader.addRepository("company-nexus", "https://nexus.company.com/repository/maven-releases/");
classLoader.loadArtifact("com.company:private-component:1.0.0");
```

### ClassLoader Isolation

Each `MavenArtifactClassLoader` provides isolated classloading:

```java
MavenArtifactClassLoader cl1 = new MavenArtifactClassLoader();
cl1.loadArtifact("com.google.guava:guava:30.0-jre");

MavenArtifactClassLoader cl2 = new MavenArtifactClassLoader();
cl2.loadArtifact("com.google.guava:guava:31.0-jre");

// Different versions loaded in isolation
```

### Bean Lifecycle Hooks

Beans registered via this library participate in full Spring lifecycle:

```java
// Implement InitializingBean, DisposableBean
// @PostConstruct, @PreDestroy annotations work
// ApplicationContext events are propagated
```

## Dependencies

The library requires:

```xml
<dependencies>
    <!-- Maven Resolution -->
    <dependency>
        <groupId>org.apache.maven.resolver</groupId>
        <artifactId>maven-resolver-api</artifactId>
        <version>1.9.18</version>
    </dependency>
    <dependency>
        <groupId>org.apache.maven.resolver</groupId>
        <artifactId>maven-resolver-impl</artifactId>
        <version>1.9.18</version>
    </dependency>
    <dependency>
        <groupId>org.apache.maven.resolver</groupId>
        <artifactId>maven-resolver-connector-basic</artifactId>
        <version>1.9.18</version>
    </dependency>
    <dependency>
        <groupId>org.apache.maven.resolver</groupId>
        <artifactId>maven-resolver-transport-http</artifactId>
        <version>1.9.18</version>
    </dependency>
    <dependency>
        <groupId>org.apache.maven</groupId>
        <artifactId>maven-resolver-provider</artifactId>
        <version>3.9.6</version>
    </dependency>

    <!-- Spring Framework (provided by ikasan-eip-standalone or add separately) -->
    <!-- spring-context, spring-beans, spring-core -->
</dependencies>
```

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=SpringBootDynamicBeanLoaderTest

# Run specific test method
mvn test -Dtest=SpringBootDynamicBeanLoaderTest#testLoadBeanAndBindPropertiesFromApplicationProperties
```

## Best Practices

### 1. Use Property Prefixes
Organize properties with prefixes to avoid naming conflicts:

```properties
mycomponent.database.url=...
mycomponent.cache.enabled=...
othercomponent.api.key=...
```

### 2. Validate Beans After Loading
```java
Object bean = loader.loadRegisterAndBindProperties(...);
if (bean instanceof Validator) {
    ((Validator) bean).validate();
}
```

### 3. Handle Missing Properties Gracefully
```java
try {
    loader.setPropertiesFromEnvironment(bean, "prefix");
} catch (NoSuchMethodException e) {
    // Log warning and use defaults
    logger.warn("Property not found: {}", e.getMessage());
}
```

### 4. Clean Up Resources
```java
// When removing beans that hold resources
SpringBeanRegistry.removeBean(context, "beanName");
// Call cleanup methods if needed
```

## Limitations

- **Security**: Dynamically loaded code runs with full application permissions. Validate Maven coordinates carefully.
- **ClassLoader Isolation**: Classes loaded in one `MavenArtifactClassLoader` cannot be cast to classes in another, even if they're the same class.
- **Spring Scope**: Beans registered via this library default to singleton scope. Custom scopes require additional configuration.
- **Property Types**: Currently supports String, boolean, and int. Additional types require extending the type conversion logic.

## Future Enhancements

Potential areas for extension:

- Support for OSGi bundles
- Hot-reload with automatic bean refresh
- Property validation with JSR-303
- Support for additional property types (Date, LocalDateTime, etc.)
- Lazy loading of artifacts
- Caching of downloaded artifacts

## License

Ikasan Enterprise Integration Platform
BSD 3-Clause License

Copyright (c) 2018, Ikasan Enterprise Integration Platform
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of the copyright holder nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

## Contributing

To get started, [sign the Contributor License Agreement](https://www.clahub.com/agreements/ikasanEIP/ikasan).

For more information about contributing to the Ikasan project, please visit the [Ikasan GitHub repository](https://github.com/ikasanEIP/ikasan).

## Support

For issues and questions:
- GitHub Issues: [Add link]
- Documentation: [Add link]
- Contact: [Add contact]

---

**Built with ❤️ for dynamic, flexible Spring applications**
