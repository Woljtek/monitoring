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

@Component
@Slf4j
public class ReloadableBeanFactory {
    @Autowired
    Environment env;

    @Autowired
    private ApplicationContext context;


    public <T> T getBean(Class<T> className) {
        return getBeanConfiguration(className)
                .flatMap(beanConfig -> {
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

    @SuppressWarnings("unchecked")
    private <T> T castObjectToParameterizedType (Object object) {
        return (T) object;
    }

    private <T> Optional<ConfigurationPropertiesBean> getBeanConfiguration (Class<T> className) {
        final var beanConfig =  ConfigurationPropertiesBean.getAll(context).values().stream()
            .filter(cpb -> ClassUtils.getUserClass(Objects.requireNonNull(cpb.asBindTarget().getType().getRawClass())).equals(className))
            .findFirst();

        if (beanConfig.isEmpty()) {
            log.warn("No config associated to bean: %s".formatted(className.getName()));
        }

        return beanConfig;
    }

    private Optional<ReloadableBean> findReloadablePropertySource (String propertySourceName) {
        final var propertySource = findPropertySource(propertySourceName);
        if (propertySource instanceof ReloadableBean reloadableBean) {
            return Optional.of(reloadableBean);
        } else {
            log.warn("Bean with name '%s' is not reloadable".formatted(propertySourceName));
            return Optional.empty();
        }
    }

    private PropertySource<?> findPropertySource (String propertySourceName) {
        return getPropertySources()
                .filter(ps -> propertySourceName.equals(ps.getName()))
                .findFirst()
                .orElse(null);
    }

    private Stream<PropertySource<?>> getPropertySources () {
        return ((AbstractEnvironment)env).getPropertySources().stream();
    }
}
