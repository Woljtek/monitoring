package eu.csgroup.coprs.monitoring.traceingestor.config;

import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.traceingestor.converter.Action;
import eu.csgroup.coprs.monitoring.traceingestor.converter.ActionFactory;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
public class Mapping {
        public static final String DEFAULT_FROM_ALIAS = "default_from_alias";
        public static final String DEFAULT_FROM_ALIAS_FORMAT = DEFAULT_FROM_ALIAS + "_%s";

        public static final String DEFAULT_CONVERT_ALIAS = "default_convert_alias";
        public static final String DEFAULT_CONVERT_ALIAS_FORMAT = DEFAULT_CONVERT_ALIAS + "_%s";

        public static final String ALIAS_SEPARATOR = "->";

        @Getter
        private final List<AliasWrapper<BeanProperty>> from = new ArrayList<>();

        private final List<AliasWrapper<Action>> action = new ArrayList<>();

        private BeanProperty to;

        private boolean removeEntityIfNull = false;

        private boolean setValueOnlyIfNull = false;


        public void setFrom (List<String> fromList) {
                this.from.clear();
                for (var fromEl : fromList) {
                        setFrom(fromEl);
                }
        }

        public void setFrom (String from) {
                var propPathAndAlias = from.split(ALIAS_SEPARATOR, 2);

                AliasWrapper<BeanProperty> wrapper;
                if (propPathAndAlias.length > 1) {
                        wrapper = new AliasWrapper<>(
                                propPathAndAlias[0].trim(),
                                new BeanProperty(propPathAndAlias[1].trim())
                        );
                } else {
                        wrapper = new AliasWrapper<>(
                                DEFAULT_FROM_ALIAS_FORMAT.formatted(this.from.size()),
                                new BeanProperty(propPathAndAlias[0].trim())
                        );
                }

                this.from.add(wrapper);
        }

        public void setAction (String action) {
                var actionAndAlias = action.split(ALIAS_SEPARATOR, 1);

                AliasWrapper<Action> wrapper;
                if (actionAndAlias.length > 1) {
                        wrapper = new AliasWrapper<>(
                                actionAndAlias[0].trim(),
                                new Action(actionAndAlias[1].trim())
                        );
                } else {
                        wrapper = new AliasWrapper<>(
                                DEFAULT_CONVERT_ALIAS_FORMAT.formatted(this.action.size()),
                                ActionFactory.getConverter(actionAndAlias[0].trim())
                        );
                }

                this.action.add(wrapper);
        }
}
