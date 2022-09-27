package eu.csgroup.coprs.monitoring.common.spring;

import eu.csgroup.coprs.monitoring.common.bean.ReloadableBeanFactory;
import eu.csgroup.coprs.monitoring.common.properties.ReloadablePropertySourceEnvironment;
import eu.csgroup.coprs.monitoring.common.properties.ReloadableYamlPropertySource;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = ReloadableBeanFactoryTest.TestInitializer.class)
@EnableConfigurationProperties({ PropertySource.class })
@SpringBootTest(classes = {ReloadableBeanFactory.class})
public class ReloadableBeanFactoryTest {
    public static final TemporaryFolder folder = new TemporaryFolder();

    private static final File yamlFile;

    static {
        try {
            folder.create();
            yamlFile = folder.newFile("test.yaml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    ReloadableBeanFactory beanFactory;

    @Test
    public void testLoadBean () {
        final var testProperty = beanFactory.getBean(PropertySource.class);

        assertThat(testProperty).extracting(PropertySource::getCollection)
                .matches(collection -> collection.size() == 3)
                .matches(collection -> collection.containsAll(List.of("col value 1", "col value 2", "col value 3")));
        assertThat(testProperty).extracting(PropertySource::getIntValue).isEqualTo(1989);
        assertThat(testProperty).extracting(PropertySource::getMap)
                .isEqualTo(Map.of(
                        "key1", "map value 10",
                        "key2", "map value 20",
                        "key3", "map value 30"
                ));
    }

    @Test
    public void testUpdateBean () {
        // Given
        PropertySource testProperty;
        final var updatedFile = new File("src/test/resources/spring/test-update.yaml");

        // When
        try {
            Files.copy(updatedFile.toPath(), yamlFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Check for update
        final var start = Instant.now();
        do {
            testProperty = beanFactory.getBean(PropertySource.class);
        } while (testProperty.getIntValue() == 1989 || Duration.between(Instant.now(), start).toMinutes() > 1);

        // Then
        assertThat(testProperty).extracting(PropertySource::getCollection)
                .matches(collection -> collection.size() == 3)
                .matches(collection -> collection.containsAll(List.of("col value 10", "col value 20", "col value 30")));
        assertThat(testProperty).extracting(PropertySource::getIntValue).isEqualTo(2022);
        assertThat(testProperty).extracting(PropertySource::getMap)
                .isEqualTo(Map.of(
                        "key10", "map value 10",
                        "key20", "map value 20",
                        "key30", "map value 30"
                ));
    }

    // ----

    public static class TestInitializer extends ExternalResource implements ApplicationContextInitializer<ConfigurableApplicationContext>, AutoCloseable {
        @Override
        public void close() throws Exception {

        }

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            final var originalFile = new File("src/test/resources/spring/test.yaml");

            try {
                Files.copy(originalFile.toPath(), yamlFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            final var env = ReloadablePropertySourceEnvironment.getInstance();
            env.setRefreshPeriod(1, TimeUnit.MILLISECONDS);

            try {
                TestPropertyValues.of(
                        "test.path=file:" + yamlFile.getAbsolutePath(),
                        "propertySource.yaml.reload=20"
                        ).applyTo(applicationContext.getEnvironment());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void testOther () {
        final var reloadableBean = new ReloadableYamlPropertySource("test", "src/test/resources/spring/test.yaml");
        final var reloadableBean2 = new ReloadableYamlPropertySource("test", "src/test/resources/spring/test-update.yaml");

        assertThat(reloadableBean).isNotEqualTo(reloadableBean2);
        assertThat(reloadableBean.toString()).isNotEqualTo(reloadableBean2.toString());

    }
}
