package eu.csgroup.coprs.monitoring.tracefilter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
//@ComponentScan("eu.csgroup.coprs.monitoring")
@Import(TraceFilterConfiguration.class)
public class TraceFilterApplication {

    public static void main(String[] args) {
        SpringApplication.run(TraceFilterApplication.class, args);
    }

}
