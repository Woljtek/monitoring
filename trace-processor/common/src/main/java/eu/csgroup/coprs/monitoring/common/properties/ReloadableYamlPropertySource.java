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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Alternative way to load YAML configuration file to keep information on bracket stored in key map
 * (not the case in spring boot lib).<br>
 * <br>
 * It also gives the possibility to detect configuration file change to reload the configuration
 * (see. {@link ReloadablePropertySourceEnvironment}).
 */
@Slf4j
public class ReloadableYamlPropertySource extends EnumerablePropertySource<String> implements ReloadableBean {
    ReloadingFileBasedConfigurationBuilder<FileBasedConfiguration> builder;

    /**
     * Indicator on configuration file change status (true when file was updated since last load)
     */
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    /**
     * Store directly all leaf of the tree instead of parsing it each time (used to reply to spring boot when requesting
     * for a property value)
     */
    private List<LeafProperty> leavesProperty;

    /**
     * Expression engine compatible with spring convention
     */
    private final ExpressionEngine expressionEngine;

    /**
     * Default constructor
     *
     * @param name Property source name (used to retrieve it by its name)
     * @param path Configuration file path
     */
    public ReloadableYamlPropertySource(String name, final String path) {
        super(StringUtils.hasText(name) ? name : path);

        Parameters params = new Parameters();
        // Read data from this file
        File propertiesFile = new File(path);

        // Create reader with reload notifier
        builder =
                new ReloadingFileBasedConfigurationBuilder<FileBasedConfiguration>(CustomYamlConfiguration.class)
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

        // Set expression engine to use by the builder
        ((YAMLConfiguration) getConfiguration()).setExpressionEngine(expressionEngine);

        // Create reload check
        final var env = ReloadablePropertySourceEnvironment.getInstance();
        PeriodicReloadingTrigger trigger = new PeriodicReloadingTrigger(builder.getReloadingController(),
                null, env.getRefreshPeriodValue(), env.getRefreshPeriodUnit());
        trigger.start();


    }

    /**
     *
     * @return Get loaded configuration file
     */
    public FileBasedConfiguration getConfiguration () {
        try {
            final var currentConfig =  builder.getConfiguration();
            // On configuration reload need to apply our expression engine.
            if (((YAMLConfiguration)currentConfig).getExpressionEngine() != expressionEngine) {
                ((YAMLConfiguration)currentConfig).setExpressionEngine(expressionEngine);
                // Reset
                leavesProperty = null;
            }
            return currentConfig;
        } catch (Exception e) {
            throw new PropertiesException(e);
        }
    }

    @Override
    public Object getProperty(String s) {
        log.trace("Required key: %s".formatted(s));

        // Find leaf associated to given property
        // And the return the value (otherwise null if not found)
        final var property = getLeaves().stream()
                .filter(leaf -> leaf.path.equals(s))
                .findFirst()
                .map(leaf -> leaf.delegate.getValue())
                .orElse(null);

        log.trace("Value: %s".formatted(property));
        return property;
    }

    @Override
    public String[] getPropertyNames() {
        // Return all founded property
        return getLeaves()
                .stream()
                .map(leaf -> leaf.path)
                .toList()
                .toArray(String[]::new);
    }

    /**
     * Get property name of the given node according to its value type (raw conf parameter). Property name is directly
     * retrieved from the child node ({@link ImmutableNode}) but in the case where its associated value is a collection,
     * the property name is suffixed with [%s] where %s is replaced by the position of the node (child node) in the parent
     * node (root node)
     *
     * @param rootNode root node containing child node parameter
     * @param childNode child node for which to get property name
     * @param rawConf value associated to child node
     * @return child node property name.
     */
    private String getPropertyName(ImmutableNode rootNode, ImmutableNode childNode, Object rawConf) {
        final var duplicate = rootNode != null ? rootNode.getChildren(childNode.getNodeName()) : List.of();
        // Surround property containing [] with [] to force spring bean to not remove initial []
        // (just the one added will be removed)
        final var propName = PropertyUtil.surroundPropertyName(childNode.getNodeName());
        if (rawConf instanceof Collection<?>) {
            return  "%s[%s]".formatted(propName, duplicate.indexOf(childNode));
        } else {
            return propName;
        }
    }

    /**
     * Get the index (position) of the node (current node parameter) in the parent node (root node parameter)<br>
     * <br>
     * Several node can share the same property name particularly when it's associated to a list structure
     *
     * @param rootNode parent node
     * @param currentNode node for which to find its position in the parent node
     * @return index of the node otherwise -1 if not found
     */
    public int getPropertyIndex(ImmutableNode rootNode, ImmutableNode currentNode) {
        return rootNode.getChildren(currentNode.getNodeName()).indexOf(currentNode);
    }

    /**
     * Get value (in root raw conf parameter) associated to the given node (current node parameter).<br>
     * <br>
     * Depending on the type of the root raw conf value is retrieved differently. In case of a map, the property name of
     * the node is used as a key. For collection, the index of the node is used (see. {@link #getPropertyIndex(ImmutableNode, ImmutableNode)}).
     * In other cases, the root raw conf parameter is returned.
     *
     * @param rootNode parent node
     * @param currentNode node for which to get associated configuration
     * @param rootRawConf Configuration in which to find the node configuration
     * @return node configuration otherwise root raw conf parameter if not found.
     */
    public Object getRawConf(ImmutableNode rootNode, ImmutableNode currentNode, Object rootRawConf) {
        var rawConf = rootRawConf;
        if (rootRawConf instanceof Map<?,?>) {
            rawConf = this.<Map<String, String>>castObject(rootRawConf).get(currentNode.getNodeName());
        } else if (rootRawConf instanceof Collection<?>) {
            int index = getPropertyIndex(rootNode, currentNode);
            rawConf = this.<List<String>>castObject(rawConf).get(index);
        }

        return rawConf;
    }

    /**
     * Utility class to isolate unchecked cast
     *
     * @param object Object to cast
     * @return casted object according to given parameter type
     * @param <T> desired type
     */
    @SuppressWarnings("unchecked")
    private <T> T castObject (Object object) {
        return (T) object;
    }

    private record LeafProperty(
            String path,
            ImmutableNode delegate
    ){
    }

    /**
     * Get all leaves of the configuration file. If the operation was not already processed do it otherwise use the cached
     * result.<br>
     * <br>
     * This function take into account the case of configuration file reload after an update, to execute the operation
     * another time and retrieve updated leaves.
     *
     * @return all leaves of the configuration
     */
    public List<LeafProperty> getLeaves() {
        // Reset leaves property variable if reload was processed.
        final var propertiesConfiguration = getConfiguration();
        if (leavesProperty == null) {
            final var nodeHandler = ((CustomYamlConfiguration) propertiesConfiguration).getNodeModel().getNodeHandler();
            final var rawConf = ((CustomYamlConfiguration) propertiesConfiguration).getCache();

            final var rootNode = nodeHandler.getRootNode();
            leavesProperty = getLeaves(null, rootNode, "", rawConf);
            leavesProperty.forEach(leaf -> log.trace("Found property: %s".formatted(leaf.path)));
        }

        return leavesProperty;
    }

    /**
     * Get leaves of the given node and child node.
     *
     * @param rootNode parent node (used to construct property name of the node. See.
     * {@link #getPropertyName(ImmutableNode, ImmutableNode, Object)})
     * @param currentNode current node
     * @param path path of the current node
     * @param rootRawConf configuration associated to parent node (used to construct property name of the node. See.
     * {@link #getPropertyName(ImmutableNode, ImmutableNode, Object)})
     * @return all founded leaves
     */
    public List<LeafProperty> getLeaves(ImmutableNode rootNode, ImmutableNode currentNode, String path, Object rootRawConf) {
        if (currentNode.getChildren().isEmpty()) {
            // If node does not have child node stop search and create leaf associated to the node
            return List.of(
                    new LeafProperty(
                            PropertyUtil.getPath(path, getPropertyName(rootNode, currentNode, rootRawConf)),
                            currentNode));
        } else {
            // Get configuration of the current node
            var rawConf = rootRawConf;
            if (rootNode != null) {
                rawConf = getRawConf(rootNode, currentNode, rootRawConf);
            }
            final var currentRawConf = rawConf;

            return currentNode.getChildren()
                    .stream()
                    // For each child node find leaves
                    .map(childNode -> getLeaves(
                            currentNode,
                            childNode,
                            PropertyUtil.getPath(
                                    path,
                                    getPropertyName(rootNode, currentNode, rootRawConf)),
                            getRawConf(currentNode, childNode, currentRawConf))
                    )
                    // Then collect all result into one list
                    .reduce(new Vector<>(), (l,n) -> {
                        l.addAll(n);
                        return l;
                    });
        }
    }

    /**
     * Indicate to the factory {@link eu.csgroup.coprs.monitoring.common.bean.ReloadableBeanFactory} that the configuration object is obsolete and
     * must be recreated
     *
     * @return true if configuration file changed since last creation
     */
    @Override
    public boolean isReloadNeeded() {
        return dirty.get();
    }

    /**
     * Use by {@link eu.csgroup.coprs.monitoring.common.bean.ReloadableBeanFactory} to indicate that configuration file changes was taken into
     * account and configuration object was recreated
     */
    public void setReloaded () {
        dirty.set(false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReloadableYamlPropertySource that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(builder, that.builder)
                && Objects.equals(dirty, that.dirty)
                && Objects.equals(leavesProperty, that.leavesProperty)
                && Objects.equals(expressionEngine, that.expressionEngine);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), builder, dirty, leavesProperty, expressionEngine);
    }
}