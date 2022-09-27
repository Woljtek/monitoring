package eu.csgroup.coprs.monitoring.common.jpa;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

public class EntitySpecification {

    private EntitySpecification () {

    }

    public static <T extends DefaultEntity> Specification<T> getEntityBy (String attributeName, Object attributeValue) {
        return (root, query, criteriaBuilder) -> {
            if (attributeValue instanceof Collection<?>) {
                return root.get(attributeName).in((Collection<?>) attributeValue);
            } else {
                return criteriaBuilder.equal(root.get(attributeName), attributeValue);
            }
        };
    }
}
