package de.foodplanner.ingestion;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import picocli.CommandLine;

@Component
public class SpringPicocliFactory implements CommandLine.IFactory {

    private final ApplicationContext context;

    SpringPicocliFactory(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public <K> K create(Class<K> cls) throws Exception {
        try {
            return context.getBean(cls);
        } catch (NoSuchBeanDefinitionException e) {
            return CommandLine.defaultFactory().create(cls);
        }
    }
}
