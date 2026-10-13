package org.ikasan.dynamic.cl;

import org.ikasan.spec.flow.Flow;
import org.ikasan.spec.module.Module;
import org.ikasan.testharness.flow.rule.IkasanFlowTestRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.Assert.assertEquals;

@SpringBootTest
@ContextConfiguration(classes = {TestModuleConfiguration.class, TestConfiguration.class})
@ActiveProfiles("test")
public class ModuleFactoryTest {

    @Autowired
    private Module<Flow> moduleUnderTest;

    public IkasanFlowTestRule flowTestRule = new IkasanFlowTestRule();

    @Test
    void testModuleFactoryIsAvailable() {
        Flow flow = moduleUnderTest.getFlow("test-flow1");
        flow.start();
        System.out.println("test-flow1 started!");
        assertEquals(Flow.RUNNING, flow.getState());

        System.out.println("test-flow1 stopped!");
        flow.stop();
        assertEquals(Flow.STOPPED, flow.getState());

        flow = moduleUnderTest.getFlow("test-flow2");
        flow.start();
        System.out.println("test-flow2 started!");
        assertEquals(Flow.RUNNING, flow.getState());

        System.out.println("test-flow2 stopped!");
        flow.stop();
        assertEquals(Flow.STOPPED, flow.getState());
    }
}
