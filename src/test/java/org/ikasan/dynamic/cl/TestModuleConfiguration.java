package org.ikasan.dynamic.cl;

import org.ikasan.builder.BuilderFactory;
import org.ikasan.module.ConfiguredModuleConfiguration;
import org.ikasan.spec.component.endpoint.Consumer;
import org.ikasan.spec.component.endpoint.Producer;
import org.ikasan.spec.flow.Flow;
import org.ikasan.spec.flow.FlowFactory;
import org.ikasan.spec.module.Module;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;

import java.util.List;

/**
 * Test configuration that enables Ikasan Spring Boot auto-configuration
 * to make ModuleFactory and other Ikasan beans available.
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    org.ikasan.backup.IkasanBackupAutoConfiguration.class
})
@ComponentScan(basePackages = {
    "org.ikasan.builder",
    "org.ikasan.spec.module",
    "org.ikasan.dynamic.cl"
})
@ImportResource( {
        "classpath:h2-datasource-conf.xml",
        "classpath:ikasan-transaction-pointcut-ikasanMessageListener.xml"
} )
public class TestModuleConfiguration implements FlowFactory {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private BuilderFactory builderFactory;

    @Autowired
    private ConfiguredModuleConfiguration configuredModuleConfiguration;

    @Override
    public List<Flow> create(String flowName, String profile) {
        return List.of(createFlow(flowName));
    }

    private Flow createFlow(String name) {
        Flow flow = builderFactory.getFlowBuilder("test-module", name)
                .consumer("bigQueueConsumer", (Consumer) applicationContext.getBean("bigQueueConsumer"))
                .producer("bigQueueProducer", (Producer) applicationContext.getBean("bigQueueProducer"))
                .build();

        return flow;
    }

    @Bean
    public Module<Flow> testModule() {
        return builderFactory.getModuleBuilder("test-module")
                .setConfiguration(this.configuredModuleConfiguration)
                .withFlowFactory(this)
                .build();
    }
}
