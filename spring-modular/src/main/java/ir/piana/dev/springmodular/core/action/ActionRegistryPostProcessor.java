package ir.piana.dev.springmodular.core.action;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;

import java.util.Map;

//@Component
public class ActionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {
    private SpringVueResource springVueResource;

    public ActionRegistryPostProcessor(SpringVueResource springVueResource) {
        this.springVueResource = springVueResource;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
            throws BeansException {
        if(springVueResource.getBeanMap() != null) {
            Map<String, Map.Entry<String, String>> beanMap = springVueResource.getBeanMap();
            for (String key : beanMap.keySet()) {
                RootBeanDefinition beanDefinition =
                        new RootBeanDefinition(key);
                registry.registerBeanDefinition(beanMap.get(key).getKey(), beanDefinition);
            }
        }
//        serviceDefinition.setTargetType(MyService.class); //The service interface
//        serviceDefinition.setRole(BeanDefinition.ROLE_APPLICATION);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory)
            throws BeansException {
    }
}
