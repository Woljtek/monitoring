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

        if (internal instanceof FileSystemResource fileSystemResource)
            return new ReloadableYamlPropertySource(s, fileSystemResource.getPath());
        if (internal instanceof FileUrlResource fileUrlResource)
            return new ReloadableYamlPropertySource(s, fileUrlResource.getURL().getPath());
        if (internal instanceof ClassPathResource)
            return new ReloadableYamlPropertySource(s, internal
                    .getFile()
                    .getAbsolutePath());

        return super.createPropertySource(s, encodedResource);
    }
}
