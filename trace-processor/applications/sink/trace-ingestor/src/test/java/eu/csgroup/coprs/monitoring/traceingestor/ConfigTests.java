package eu.csgroup.coprs.monitoring.traceingestor;

import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.traceingestor.config.*;
import eu.csgroup.coprs.monitoring.traceingestor.converter.Action;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigTests {
    @Test
    public void testAliasWrapperWithBeanProperty () {
        // Given
        final var config = new AliasWrapper<BeanProperty>("test", new BeanProperty("test.field"));
        final var duplicateConfig = new AliasWrapper<BeanProperty>("test", new BeanProperty("test.field"));

        final var otherConfig1 = new AliasWrapper<BeanProperty>("other test", new BeanProperty("test.field"));
        final var otherConfig2 = new AliasWrapper<BeanProperty>("test", new BeanProperty("other_test.field"));

        // When


        // Then
        assertThat(config).isEqualTo(duplicateConfig);
        assertThat(otherConfig1).isNotEqualTo(config);
        assertThat(otherConfig2).isNotEqualTo(config);
        assertThat(otherConfig1).isNotEqualTo(otherConfig2);
    }

    @Test
    public void testAliasWrapperWithAction () {
        // Given
        final var config = new AliasWrapper<Action>("test", new Action("FORMAT al1"));
        final var duplicateConfig = new AliasWrapper<Action>("test", new Action("FORMAT al1"));

        final var otherConfig1 = new AliasWrapper<Action>("other test", new Action("FORMAT al1"));
        final var otherConfig2 = new AliasWrapper<Action>("test", new Action("FORMAT al1 al2"));

        // When


        // Then
        assertThat(config).isEqualTo(duplicateConfig);
        assertThat(otherConfig1).isNotEqualTo(config);
        assertThat(otherConfig2).isNotEqualTo(config);
        assertThat(otherConfig1).isNotEqualTo(otherConfig2);
    }

    @Test
    public void testMapping () {
        // Given
        // From
        final var from1 = "test.field";
        final var from2 = "al2 -> test.field2";
        // Action
        final var action1 = "FORMAT .* %1$s al2";
        final var action2 = "FORMAT .* %1$s al1";
        // To
        final var to = "dest.field";
        // remove entity if null
        // set value only if null

        // When
        final var mapping = createMapping(List.of(from1, from2), List.of(action1, action2), to, false, false);
        final var duplicateMapping = createMapping(List.of(from1, from2), List.of(action1, action2), to, false, false);

        final var otherMapping1 = createMapping(List.of(from2, from1), List.of(action1, action2), to, false, false);
        final var otherMapping2 = createMapping(List.of(from1, from2), List.of(action2, action1), to, false, false);

        final var otherMapping3 = createMapping(List.of("test.other_field", from2), List.of(action1, action2), to, false, false);
        final var otherMapping4 = createMapping(List.of(from1, from2), List.of("FORMAT .* %1$s otherAlias", action2), to, false, false);
        final var otherMapping5 = createMapping(List.of(from1, from2), List.of(action1, action2), "dest.other_field", false, false);
        final var otherMapping6 = createMapping(List.of(from1, from2), List.of(action1, action2), to, true, false);
        final var otherMapping7 = createMapping(List.of(from1, from2), List.of(action1, action2), to, false, true);

        // Then
        assertThat(mapping).isEqualTo(duplicateMapping)
        // -- Check order equality
            .isNotEqualTo(otherMapping1)
            .isNotEqualTo(otherMapping2)
        // -- Value changes
            .isNotEqualTo(otherMapping3)
            .isNotEqualTo(otherMapping4)
            .isNotEqualTo(otherMapping5)
            .isNotEqualTo(otherMapping6)
            .isNotEqualTo(otherMapping7);
    }

    @Test
    public void testAlias () {
        // Given
        String entity1 = "Chunk";
        String entity2 = "Dsib";

        String restrict1 = "ExternalInput";
        String restrict2 = "AuxData";

        // When
        final var alias = new Alias();
        alias.setEntity(entity1);
        alias.setRestrict(restrict1);

        final var duplicateAlias = new Alias();
        duplicateAlias.setEntity(entity1);
        duplicateAlias.setRestrict(restrict1);

        final var otherAlias1 = new Alias();
        otherAlias1.setEntity(entity2);
        otherAlias1.setRestrict(restrict1);

        final var otherAlias2 = new Alias();
        otherAlias2.setEntity(entity1);
        otherAlias2.setRestrict(restrict2);

        // Then
        assertThat(alias).isEqualTo(duplicateAlias)
                .isNotEqualTo(otherAlias1)
                .isNotEqualTo(otherAlias2);
    }

    @Test
    public void testDuplicateProcessing () {
        // Given
        final var query1 = "SQL query 1";
        final var query2 = "SQL query 2";

        final var rules1 = Map.of("bean.field1", "test 1");
        final var rules2 = Map.of("bean.field2", "test 2");

        // When
        final var duplicateProcessing = new DuplicateProcessing();
        duplicateProcessing.setQuery(query1);
        duplicateProcessing.setRules(rules1);

        final var duplicateDuplicateProcessing = new DuplicateProcessing();
        duplicateDuplicateProcessing.setQuery(query1);
        duplicateDuplicateProcessing.setRules(rules1);

        final var otherDuplicateProcessing1 = new DuplicateProcessing();
        otherDuplicateProcessing1.setQuery(query2);
        otherDuplicateProcessing1.setRules(rules1);

        final var otherDuplicateProcessing2 = new DuplicateProcessing();
        otherDuplicateProcessing2.setQuery(query1);
        otherDuplicateProcessing2.setRules(rules2);

        // Then
        assertThat(duplicateProcessing)
                .isEqualTo(duplicateDuplicateProcessing)
                .isNotEqualTo(otherDuplicateProcessing1)
                .isNotEqualTo(otherDuplicateProcessing2);
    }

    @Test
    public void testIngestion () {
        // Given
        final var name1 = "config1";
        final var name2 = "config2";

        final var from1 = "test.field";
        final var action1 = "FORMAT .* %1$s al2";
        final var to = "dest.field";
        final var mapping1 = createMapping(List.of(from1), List.of(action1), to, false, false);
        final var mapping2 = createMapping(List.of(from1), List.of(action1), to, false, true);

        final var entity1 = "Chunk";
        final var entity2 = "Dsib";
        final var restrict1 = "ExternalInput";
        final var restrict2 = "AuxData";

        final var alias1 = new Alias();
        alias1.setEntity(entity1);
        alias1.setRestrict(restrict1);
        final var alias2 = new Alias();
        alias2.setEntity(entity2);
        alias2.setRestrict(restrict2);

        // duplicateProcessing
        final var query1 = "SQL query 1";
        final var query2 = "SQL query 2";
        final var rules1 = Map.of("bean.field", "test 1");
        final var rules2 = Map.of("bean.field", "test 2");
        final var duplicateProcessing1 = new DuplicateProcessing();
        duplicateProcessing1.setQuery(query1);
        duplicateProcessing1.setRules(rules1);
        final var duplicateProcessing2 = new DuplicateProcessing();
        duplicateProcessing2.setQuery(query2);
        duplicateProcessing2.setRules(rules2);

        // When
        final var ingestion = new Ingestion();
        ingestion.setName(name1);
        ingestion.setMappings(List.of(mapping1));
        ingestion.setAlias(Map.of("alias1", alias1));
        ingestion.setDuplicateProcessings(List.of(duplicateProcessing1));

        final var duplicateIngestion = new Ingestion();
        duplicateIngestion.setName(name1);
        duplicateIngestion.setMappings(List.of(mapping1));
        duplicateIngestion.setAlias(Map.of("alias1", alias1));
        duplicateIngestion.setDuplicateProcessings(List.of(duplicateProcessing1));

        final var otherIngestion1 = new Ingestion();
        otherIngestion1.setName(name2);
        otherIngestion1.setMappings(List.of(mapping1));
        otherIngestion1.setAlias(Map.of("alias1", alias1));
        otherIngestion1.setDuplicateProcessings(List.of(duplicateProcessing1));

        final var otherIngestion2 = new Ingestion();
        otherIngestion2.setName(name1);
        otherIngestion2.setMappings(List.of(mapping2));
        otherIngestion2.setAlias(Map.of("alias1", alias1));
        otherIngestion2.setDuplicateProcessings(List.of(duplicateProcessing1));

        final var otherIngestion3 = new Ingestion();
        otherIngestion3.setName(name1);
        otherIngestion3.setMappings(List.of(mapping1));
        otherIngestion3.setAlias(Map.of("alias1", alias2));
        otherIngestion3.setDuplicateProcessings(List.of(duplicateProcessing1));

        final var otherIngestion4 = new Ingestion();
        otherIngestion4.setName(name1);
        otherIngestion4.setMappings(List.of(mapping1));
        otherIngestion4.setAlias(Map.of("alias1", alias1));
        otherIngestion4.setDuplicateProcessings(List.of(duplicateProcessing2));

        // Then
        assertThat(ingestion)
                .isEqualTo(duplicateIngestion)
                .isNotEqualTo(otherIngestion1)
                .isNotEqualTo(otherIngestion2)
                .isNotEqualTo(otherIngestion3)
                .isNotEqualTo(otherIngestion4);
    }

    private Mapping createMapping(List<String> from, List<String> action, String to, boolean removeEntityIfNull, boolean setValueOnlyIfNull) {
        final var mapping = new Mapping();
        mapping.setFrom(from);
        action.forEach(mapping::setAction);
        mapping.setTo(new BeanProperty(to));
        mapping.setRemoveEntityIfNull(removeEntityIfNull);
        mapping.setSetValueOnlyIfNull(setValueOnlyIfNull);

        return mapping;
    }
}
