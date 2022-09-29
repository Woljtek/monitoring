package eu.csgroup.coprs.monitoring.traceingestor.mapping;

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


        public Mapping (BeanProperty from, BeanProperty to) {
                this.from = from;
                this.to = to;
        }

        public void setMatch(String match) {
                this.match = Pattern.compile(match);
        }

        /*public Mapping(BeanProperty from) {
                this.from = from;
        }

        public Mapping(BeanProperty from, BeanProperty to) {
                this.from = from;
                this.to = to;
        }*/

    /*public static Mapping from(String tracePath, String entityPath) {
        return new Mapping(
                new BeanProperty(tracePath),
                new BeanProperty(entityPath)
        );
    }*/
}
