package eu.csgroup.coprs.monitoring.traceingestor.config;

import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.traceingestor.converter.Action;
import eu.csgroup.coprs.monitoring.traceingestor.converter.ActionFactory;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * Configuration class to define a mapping between a trace field ('from' clause) and an entity field ('to' clause)
 * Each from clause can be associated to an alias (by using {@link Mapping#ALIAS_SEPARATOR}) for an easy use in 'action' clause.<br>
 * <br>
 * An 'action' clause intends to do an operation on the 'from' clause to set a different value for the 'to' clause.<br>
 * If the argument of an action contains whitespace you must set argument between double quote.<br>
 * <br>
 * Conditions can be set for the value on the 'to' clause which are:
 * <ul>
 *     <li>'removeEntityIfNull': Do not keep entity if value defined by the 'to' clause is null</li>
 *     <li>'setValueOnlyIfNull': Set value only if the original one defined by the 'to' clause is null</li>
 * </ul>
 */
@Data
@NoArgsConstructor
public class Mapping {
        public static final String DEFAULT_FROM_ALIAS = "default_from_alias";
        public static final String DEFAULT_FROM_ALIAS_FORMAT = DEFAULT_FROM_ALIAS + "_%s";

        public static final String DEFAULT_CONVERT_ALIAS = "default_convert_alias";
        public static final String DEFAULT_CONVERT_ALIAS_FORMAT = DEFAULT_CONVERT_ALIAS + "_%s";

        /**
         * From clause must be in the form: {@literal <}aliasName{@literal >} -> {@literal <}bean.property.path{@literal >}
         */
        public static final String ALIAS_SEPARATOR = "->";

        /**
         * List of 'from' clause associated to a trace field and linked to an alias
         */
        @Getter
        private final List<AliasWrapper<BeanProperty>> from = new ArrayList<>();

        /**
         * List of action to execute on value coming from 'from' clause or on intermediate value and linked to an alias
         */
        private final List<AliasWrapper<Action>> action = new ArrayList<>();

        /**
         * 'to' clause associated to an entity field
         */
        private BeanProperty to;

        /**
         * Flag to avoid null value on an entity field associated to a 'to' clause
         */
        private boolean removeEntityIfNull = false;
        /**
         * Flag to avoid value erasure (only null value is supported) on an entity field associated to a 'to' clause
         */
        private boolean setValueOnlyIfNull = false;


        public void setFrom (List<String> fromList) {
                this.from.clear();
                for (var fromEl : fromList) {
                        setFrom(fromEl);
                }
        }

        public void setFrom (String from) {
                // Check if an alias is set
                var propPathAndAlias = from.split(ALIAS_SEPARATOR, 2);

                AliasWrapper<BeanProperty> wrapper;
                if (propPathAndAlias.length > 1) {
                        // Use the one set
                        wrapper = new AliasWrapper<>(
                                propPathAndAlias[0].trim(),
                                new BeanProperty(propPathAndAlias[1].trim())
                        );
                } else {
                        // or create default one (unique) by using action size list
                        wrapper = new AliasWrapper<>(
                                DEFAULT_FROM_ALIAS_FORMAT.formatted(this.from.size()),
                                new BeanProperty(propPathAndAlias[0].trim())
                        );
                }

                this.from.add(wrapper);
        }

        public void setAction (String action) {
                // Check if an alias is set
                var actionAndAlias = action.split(ALIAS_SEPARATOR, 1);

                AliasWrapper<Action> wrapper;
                if (actionAndAlias.length > 1) {
                        // Use the one set
                        wrapper = new AliasWrapper<>(
                                actionAndAlias[0].trim(),
                                new Action(actionAndAlias[1].trim())
                        );
                } else {
                        // or create default one (unique) by using action size list
                        wrapper = new AliasWrapper<>(
                                DEFAULT_CONVERT_ALIAS_FORMAT.formatted(this.action.size()),
                                ActionFactory.getConverter(actionAndAlias[0].trim())
                        );
                }

                this.action.add(wrapper);
        }
}
