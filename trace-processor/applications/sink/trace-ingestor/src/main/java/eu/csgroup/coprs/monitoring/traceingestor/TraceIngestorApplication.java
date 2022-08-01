package eu.csgroup.coprs.monitoring.traceingestor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(TraceIngestorConfiguration.class)
public class TraceIngestorApplication {

    public static void main(String[] args) {
        SpringApplication.run(TraceIngestorApplication.class, args);
    }

}
