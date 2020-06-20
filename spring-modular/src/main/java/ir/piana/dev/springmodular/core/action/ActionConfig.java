package ir.piana.dev.springmodular.core.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import ir.piana.dev.springmodular.core.group.GroupFromYamlService;
import ir.piana.dev.springmodular.core.group.GroupProvider;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//@Configuration
public class ActionConfig {
    @Value("${app.mode.debug}")
    private boolean debug;

    @Value("#{systemProperties.debug != null}")
    Boolean isDebug = false;

    @Bean(name="objectMapper")
    public ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }

    @Bean(name="yamlObjectMapper")
    @DependsOn("objectMapper")
    public ObjectMapper yamlObjectMapper() {
        return new ObjectMapper(new YAMLFactory());
    }

    @Bean("groupProviderFromYaml")
    @DependsOn("yamlObjectMapper")
    @Scope("singleton")
    public GroupProvider getGroupProviderFromYaml(
            @Qualifier("objectMapper") ObjectMapper objectMapper,
            @Qualifier("yamlObjectMapper") ObjectMapper yamlObjectMapper,
            Environment env) {
        String property = env.getProperty("intellij.debug.agent");
        GroupFromYamlService groupProvider = new GroupFromYamlService(objectMapper, yamlObjectMapper,
                property != null && property.equalsIgnoreCase("true") ? true : false);
        groupProvider.init();
        return groupProvider;
    }

    @Bean("componentInstaller")
    @Scope("singleton")
    @DependsOn({"groupProviderFromYaml"})
    public ComponentInstaller getComponentInstaller(
            @Qualifier("yamlObjectMapper") ObjectMapper mapper,
            GroupProvider groupProvider) {
        List<VueComponentProvider> componentProviders = new ArrayList<>();

        try {
            Reflections reflections = new Reflections("ir.piana");
            Set<Class<? extends VueComponentProvider>> componentProviderClasses = reflections.getSubTypesOf(VueComponentProvider.class);
            for(Class c : componentProviderClasses) {
                if(!c.getSimpleName().equalsIgnoreCase("action"))
                    componentProviders.add((VueComponentProvider) c.newInstance());
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return new ComponentInstaller(mapper, groupProvider, componentProviders);
    }

    @Bean("springVueResource")
    @Scope("singleton")
    @DependsOn("componentInstaller")
    public SpringVueResource getSpringVueResource(ComponentInstaller componentInstaller) {
        SpringVueResource springVueResource = componentInstaller.getSpringVueResource();
        return springVueResource;
    }

    @Bean
    @DependsOn("springVueResource")
    public BeanDefinitionRegistryPostProcessor getBeanDefinitionRegistryPostProcessor(SpringVueResource springVueResource) {
        return new ActionRegistryPostProcessor(springVueResource);
    }
}
