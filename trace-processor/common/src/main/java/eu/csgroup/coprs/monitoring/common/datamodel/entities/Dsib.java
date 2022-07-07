package eu.csgroup.coprs.monitoring.common.datamodel.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Getter
@Setter
public class Dsib {
    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    private String filename;

    private int channelId;

    private String station;
}
