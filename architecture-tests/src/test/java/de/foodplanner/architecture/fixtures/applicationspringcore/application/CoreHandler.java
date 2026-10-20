package de.foodplanner.architecture.fixtures.applicationspringcore.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class CoreHandler {

    @Autowired
    public CoreHandler(Environment environment) {
    }
}
