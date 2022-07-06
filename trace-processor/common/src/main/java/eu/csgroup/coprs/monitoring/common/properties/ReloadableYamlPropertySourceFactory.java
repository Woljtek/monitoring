package eu.csgroup.coprs.monitoring.common.properties;

import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;

import java.io.IOException;

public class ReloadableYamlPropertySourceFactory extends DefaultPropertySourceFactory {
    @Override
    public PropertySource<?> createPropertySource(String s, EncodedResource encodedResource)
            throws IOException {

        Resource internal = encodedResource.getResource();

        if (internal instanceof FileSystemResource)
            return new ReloadableYamlPropertySource(s, ((FileSystemResource) internal)
                    .getPath());
        if (internal instanceof FileUrlResource)
            return new ReloadableYamlPropertySource(s, ((FileUrlResource) internal)
                    .getURL()
                    .getPath());
        if (internal instanceof ClassPathResource)
            return new ReloadableYamlPropertySource(s, internal
                    .getFile()
                    .getAbsolutePath());

        return super.createPropertySource(s, encodedResource);
    }
}
