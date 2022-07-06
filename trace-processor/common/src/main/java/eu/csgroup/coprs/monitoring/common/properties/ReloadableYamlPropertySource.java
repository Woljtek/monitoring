package eu.csgroup.coprs.monitoring.common.properties;

import eu.csgroup.coprs.monitoring.common.bean.ReloadableBean;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.builder.ConfigurationBuilderEvent;
import org.apache.commons.configuration2.builder.ReloadingFileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.event.Event;
import org.apache.commons.configuration2.event.EventListener;
import org.apache.commons.configuration2.reloading.PeriodicReloadingTrigger;
import org.apache.commons.configuration2.tree.*;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
public class ReloadableYamlPropertySource extends EnumerablePropertySource<String> implements ReloadableBean {
    ReloadingFileBasedConfigurationBuilder<FileBasedConfiguration> builder;

    private final AtomicBoolean dirty = new AtomicBoolean(false);

    private List<LeafProperties> leafProperties;

    /**
     * Expression engine comptible with spring convention
     */
    private ExpressionEngine expressionEngine;


    public ReloadableYamlPropertySource(String name, final String path) {
        super(StringUtils.hasText(name) ? name : path);

        Parameters params = new Parameters();
        // Read data from this file
        File propertiesFile = new File(path);

        // Create reader with reload notifier
        builder =
                new ReloadingFileBasedConfigurationBuilder<FileBasedConfiguration>(YAMLConfiguration.class)
                        .configure(params.fileBased()
                                .setFile(propertiesFile));

        builder.addEventListener(
                ConfigurationBuilderEvent.RESET,
                (EventListener<Event>) event -> {
                    dirty.set(true);
                    log.info("Configuration file '%s' loaded".formatted(path));
                });

        // Create expression engine compatible with spring key property
        final var expEngConf = new DefaultExpressionEngineSymbols.Builder()
                .setPropertyDelimiter(PropertyUtil.PROPERTY_DELIMITER)
                .setEscapedDelimiter(PropertyUtil.ESCAPED_DELIMITER)
                .setIndexStart(PropertyUtil.INDEX_START)
                .setIndexEnd(PropertyUtil.INDEX_END)
                .setAttributeStart(PropertyUtil.ATTRIBUTE_START)
                .setAttributeEnd(PropertyUtil.ATTRIBUTE_END)
                .create();
        expressionEngine = new DefaultExpressionEngine(expEngConf);

        ((YAMLConfiguration) getConfiguration()).setExpressionEngine(expressionEngine);

        // Create reload check
        PeriodicReloadingTrigger trigger = new PeriodicReloadingTrigger(builder.getReloadingController(),
                null, 1, TimeUnit.MINUTES);
        trigger.start();


    }

    private String getPropertyName(ImmutableNode rootNode, ImmutableNode childNode) {
        final var duplicate = rootNode != null ? rootNode.getChildren(childNode.getNodeName()) : List.of();
        final var propName = PropertyUtil.surroundPropertyName(childNode.getNodeName());
        if (duplicate.size() > 1) {
            return  "%s[%s]".formatted(propName, duplicate.indexOf(childNode));
        } else {
            return propName;
        }
    }


    public FileBasedConfiguration getConfiguration () {
        try {
            final var currentConfig =  builder.getConfiguration();
            // On configuration reload need to apply our expression engine.
            if (((YAMLConfiguration)currentConfig).getExpressionEngine() != expressionEngine) {
                ((YAMLConfiguration)currentConfig).setExpressionEngine(expressionEngine);
                leafProperties = null;
            }
            return currentConfig;
        } catch (Exception e) {
            throw new PropertiesException(e);
        }
    }

    @Override
    public Object getProperty(String s) {
        log.trace("Required key: %s".formatted(s));

        final var property = getLeaf().stream()
                .filter(leaf -> leaf.path.equals(s))
                .findFirst()
                .map(leaf -> leaf.delegate.getValue())
                .orElse(null);

        log.trace("Value: %s".formatted(property));
        return property;
    }

    @Override
    public String[] getPropertyNames() {
        return getLeaf()
                .stream()
                .map(leaf -> leaf.path)
                .toList()
                .toArray(String[]::new);
    }

    private record LeafProperties (
            String path,
            ImmutableNode delegate
    ){
    }

    public List<LeafProperties> getLeaf() {
        if (leafProperties == null) {
            final var propertiesConfiguration = getConfiguration();
            final var nodeHandler = ((YAMLConfiguration) propertiesConfiguration).getNodeModel().getNodeHandler();

            final var rootNode = nodeHandler.getRootNode();
            leafProperties = getLeaf(null, rootNode, "");
            leafProperties.forEach(leaf -> log.trace("Found property: %s".formatted(leaf.path)));
        }

        return leafProperties;
    }

    public List<LeafProperties> getLeaf(ImmutableNode rootNode, ImmutableNode currentNode, String path) {
        if (currentNode.getChildren().isEmpty()) {
            return List.of(
                    new LeafProperties(
                            PropertyUtil.getPath(path, getPropertyName(rootNode, currentNode)),
                            currentNode));
        } else {
            return currentNode.getChildren()
                    .stream()
                    .map(childNode -> getLeaf(
                            currentNode,
                            childNode,
                            PropertyUtil.getPath(path, getPropertyName(rootNode, currentNode))))
                    .reduce(new Vector<>(), (l,n) -> {
                        l.addAll(n);
                        return l;
                    });
        }
    }

    @Override
    public boolean isReloadNeeded() {
        return dirty.get();
    }

    public void setReloaded () {
        dirty.set(false);
    }
}
