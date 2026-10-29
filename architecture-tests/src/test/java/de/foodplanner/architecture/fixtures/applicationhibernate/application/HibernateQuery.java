package de.foodplanner.architecture.fixtures.applicationhibernate.application;

import org.hibernate.validator.constraints.Length;

public record HibernateQuery(@Length(max = 10) String name) {
}
