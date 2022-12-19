package eu.csgroup.coprs.monitoring.traceingestor.entity.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

public class JsonValidator {
    @Getter
    @Autowired
    ObjectMapper objectMapper;


}
