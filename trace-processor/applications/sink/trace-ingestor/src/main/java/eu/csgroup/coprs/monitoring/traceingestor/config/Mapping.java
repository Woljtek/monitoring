package eu.csgroup.coprs.monitoring.traceingestor.config;

import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;


@Data
@NoArgsConstructor
public class Mapping {

        private BeanProperty from;

        private Pattern match;

        private BeanProperty to;

        private String convert;

        private boolean removeEntityIfNull = false;
}
