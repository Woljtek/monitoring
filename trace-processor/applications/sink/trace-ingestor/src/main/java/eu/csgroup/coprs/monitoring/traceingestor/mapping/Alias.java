package eu.csgroup.coprs.monitoring.traceingestor.mapping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alias {
    private String entity;
    private String restrict;
}
