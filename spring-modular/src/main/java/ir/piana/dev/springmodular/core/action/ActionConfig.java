package ir.piana.dev.springmodular.core.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import ir.piana.dev.springmodular.core.group.GroupFromYamlService;
import ir.piana.dev.springmodular.core.group.GroupProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

import java.util.List;

//@Configuration
public class ActionConfig {
    @Value("${app.mode.debug}")
    private boolean debug;

    @Value("#{systemProperties.debug != null}")
    Boolean isDebug = false;

    @Autowired
    private List<VueComponentProvider> vueComponentProviders;

    @Bean(name="objectMapper")
    public ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }

    @Bean(name="yamlObjectMapper")
    @DependsOn("objectMapper")
    public ObjectMapper yamlObjectMapper() {
        return new ObjectMapper(new YAMLFactory());
    }

    @Bean
    @DependsOn("yamlObjectMapper")
    @Scope("singleton")
    public GroupProvider getGroupProviderFromYaml(
            @Qualifier("objectMapper") ObjectMapper objectMapper,
            @Qualifier("yamlObjectMapper") ObjectMapper yamlObjectMapper,
            Environment env) {
        String property = env.getProperty("intellij.debug.agent");
        GroupFromYamlService groupProvider = new GroupFromYamlService(objectMapper, yamlObjectMapper, property != null && property.equalsIgnoreCase("true") ? true : false);
        groupProvider.init();
        return groupProvider;
    }

    @Bean("springVueResource")
    @Scope("singleton")
    @DependsOn("yamlObjectMapper")
    public SpringVueResource getSpringVueResource(GroupProvider groupProvider) {
        SpringVueResource springVueResource = ActionInstaller.getSpringVueResource(groupProvider);
        return springVueResource;
    }

    @Bean
    @DependsOn("springVueResource")
    public BeanDefinitionRegistryPostProcessor getBeanDefinitionRegistryPostProcessor(SpringVueResource springVueResource) {
        return new ActionRegistryPostProcessor(springVueResource);
    }
}
