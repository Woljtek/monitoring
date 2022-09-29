package eu.csgroup.coprs.monitoring.tracefilter;

import org.apache.commons.io.IOUtils;
import org.junit.rules.ExternalResource;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class TestInitializer extends ExternalResource implements ApplicationContextInitializer<ConfigurableApplicationContext>, AutoCloseable {
    @Override
    public void close() throws Exception {

    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            TestPropertyValues.of(
                    "filter.path=classpath:filter.yaml",
                    "spring.profiles.active=dev"
            ).applyTo(applicationContext.getEnvironment());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
