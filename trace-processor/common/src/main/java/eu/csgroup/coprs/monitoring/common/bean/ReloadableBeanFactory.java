package eu.csgroup.coprs.monitoring.common.bean;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationPropertiesBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Factory that allow to reload configuration file managed by spring boot
 */
@Component
@Slf4j
public class ReloadableBeanFactory {
    @Autowired
    Environment env;

    @Autowired
    private ApplicationContext context;


    /**
     * Find configuration handled by the given class name
     *
     * @param className configuration class name
     * @return configuration instance of the given type if found otherwise false
     * @param <T> type of the configuration
     */
    public <T> T getBean(Class<T> className) {
        return getBeanConfiguration(className)
                .flatMap(beanConfig -> {
                            // Use property source name if annotation set otherwise bean name
                            final var psAnno = className.getAnnotation(org.springframework.context.annotation.PropertySource.class);
                            String psName;
                            if (psAnno != null) {
                                psName = psAnno.name();
                            } else {
                                psName = beanConfig.getName();
                            }

                            final var reloadableBean = findReloadablePropertySource(psName);
                            return reloadableBean.map(rb -> this.reload(beanConfig, rb, className));
                        }
                ).orElse(null);
    }

    /**
     * Reload configuration class when needed. Reload is done when associated {@link PropertySource} detected
     * a configuration file change. <br>
     * To reload the configuration class, destroy registered one before requesting to spring boot to create bean for
     * desired configutation class.
     *
     * @param beanConfig Instance wrapping configuration class
     * @param reloadableBean Property source that is in charge of loading configuration class
     * @param className configuration class type
     * @return configuration class
     * @param <T> configuration class type
     */
    private <T> T reload (ConfigurationPropertiesBean beanConfig, ReloadableBean reloadableBean, Class<T> className) {
        if (reloadableBean.isReloadNeeded()) {
            final var bean = beanConfig.getInstance();
            context.getAutowireCapableBeanFactory().destroyBean(bean);

            final var newBean = context.getAutowireCapableBeanFactory().createBean(className);
            reloadableBean.setReloaded();
            return newBean;
        } else {
            return castObjectToParameterizedType(beanConfig.getInstance());
        }
    }

    /**
     * Utility class to isolate unchecked cast
     *
     * @param object Object to cast
     * @return casted object
     * @param <T> type cast
     */
    @SuppressWarnings("unchecked")
    private <T> T castObjectToParameterizedType (Object object) {
        return (T) object;
    }

    /**
     * Find the {@link ConfigurationPropertiesBean} associated to the given configuration class
     *
     * @param className configuration class to find among those handled by spring boot
     * @return instance of {@link ConfigurationPropertiesBean} associated to the configuration class otherwise empty result
     * @param <T> configuration class type
     */
    private <T> Optional<ConfigurationPropertiesBean> getBeanConfiguration (Class<T> className) {
        final var beanConfig =  ConfigurationPropertiesBean.getAll(context).values().stream()
                .filter(cpb -> ClassUtils.getUserClass(Objects.requireNonNull(cpb.asBindTarget().getType().getRawClass())).equals(className))
                .findFirst();

        if (beanConfig.isEmpty()) {
            log.warn("No config associated to bean: %s".formatted(className.getName()));
        }

        return beanConfig;
    }

    /**
     * Find instance in charge of configuration class loading which is annotated with
     * {@link org.springframework.context.annotation.PropertySource} and configured with the given name
     *
     * @param propertySourceName name of the property source
     * @return Instance in charge of configuration class loading otherwise empty result if no property source has the
     * given name or allow reload of configuration class
     */
    private Optional<ReloadableBean> findReloadablePropertySource (String propertySourceName) {
        final var propertySource = findPropertySource(propertySourceName);
        if (propertySource instanceof ReloadableBean reloadableBean) {
            return Optional.of(reloadableBean);
        } else {
            log.warn("Bean with name '%s' is not reloadable".formatted(propertySourceName));
            return Optional.empty();
        }
    }

    /**
     * Find property source which has the given name
     *
     * @param propertySourceName property source name
     * @return {@link PropertySource} instance if found otherwise null
     */
    private PropertySource<?> findPropertySource (String propertySourceName) {
        return getPropertySources()
                .filter(ps -> propertySourceName.equals(ps.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     *
     * @return stream of property source handled by spring boot
     */
    private Stream<PropertySource<?>> getPropertySources () {
        return ((AbstractEnvironment)env).getPropertySources().stream();
    }
}