package de.foodplanner.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.lang.ArchRule;
import java.net.HttpURLConnection;

/**
 * Die ArchUnit-Regeln für die Abhängigkeitsrichtung aus §3.1. Jede Regel nimmt das Wurzelpaket als Parameter, damit
 * sie sowohl auf das Projekt ({@link #ROOT}) als auch auf die Fixtures in {@code ArchitectureRulesTest} anwendbar ist.
 */
final class ArchitectureRules {

    static final String ROOT = "de.foodplanner";

    private ArchitectureRules() {
    }

    static ArchRule dependenciesPointInward(String root) {
        return layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .withOptionalLayers(true)
                .layer("Domain").definedBy(root + ".domain..")
                .layer("Application").definedBy(root + ".application..")
                .layer("Infrastructure").definedBy(root + ".infrastructure..")
                .layer("Api").definedBy(root + ".api..")
                .layer("Ingestion").definedBy(root + ".ingestion..")
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Api", "Ingestion")
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure", "Api", "Ingestion")
                .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Api", "Ingestion")
                .whereLayer("Api").mayNotBeAccessedByAnyLayer()
                .whereLayer("Ingestion").mayNotBeAccessedByAnyLayer()
                .because("§3.1: Abhängigkeiten zeigen nur nach innen; infrastructure wird nur in den Composition Roots eingebunden");
    }

    static ArchRule domainOnlyUsesJdk(String root) {
        return classes().that().resideInAPackage(root + ".domain..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("java..", root + ".domain..")
                .because("§3.1: domain hat keine Abhängigkeiten");
    }

    static ArchRule domainUsesNoHttpOrSql(String root) {
        return noClasses().that().resideInAPackage(root + ".domain..")
                .should().dependOnClassesThat().resideInAnyPackage("java.net.http..", "java.sql..")
                .because("§3.1: domain enthält Entitäten und reine Arithmetik, kein HTTP und kein JDBC");
    }

    static ArchRule applicationOnlyUsesAllowedLibraries(String root) {
        return classes().that().resideInAPackage(root + ".application..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "java..",
                        root + ".domain..",
                        root + ".application..",
                        // spring-context, ohne spring-core und spring-beans
                        "org.springframework.context..",
                        "org.springframework.stereotype..",
                        "org.springframework.validation.annotation..",
                        // spring-tx
                        "org.springframework.transaction..",
                        // nur die Validierungs-API, nicht Hibernate Validator
                        "jakarta.validation..")
                .because("§3.1/§11: application nutzt von Spring nur spring-context, spring-tx und die Validierungs-API");
    }

    static ArchRule applicationUsesNoWebJdbcJpaOrHttpClient(String root) {
        return noClasses().that().resideInAPackage(root + ".application..")
                .should().dependOnClassesThat(resideInAnyPackage(
                                "java.net.http..",
                                "java.sql..",
                                "javax.sql..",
                                "org.springframework.web..",
                                "org.springframework.http..",
                                "org.springframework.jdbc..",
                                "jakarta.persistence..")
                        .or(assignableTo(HttpURLConnection.class)))
                .because("§3.1: application verwendet nichts aus Spring Web, JDBC, JPA oder einem HTTP-Client");
    }
}
