package eu.csgroup.coprs.monitoring.common.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReloadableBeanFactoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReloadableBeanFactoryApplication.class, args);
    }
}
