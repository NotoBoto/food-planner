package de.foodplanner.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldConjunction;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.NetworkChannel;

/**
 * Die ArchUnit-Regeln für die Abhängigkeitsrichtung aus §3.1. Jede Regel nimmt das Wurzelpaket als Parameter, damit
 * sie sowohl auf das Projekt ({@link #ROOT}) als auch auf die Fixtures in {@code ArchitectureRulesTest} anwendbar ist.
 */
final class ArchitectureRules {

    static final String ROOT = "de.foodplanner";

    private ArchitectureRules() {
    }

    static ArchRule classesResideInModulePackages(String root) {
        return classes().that().resideInAPackage(root + "..")
                .should().resideInAnyPackage(
                        root + ".domain..",
                        root + ".application..",
                        root + ".infrastructure..",
                        root + ".api..",
                        root + ".ingestion..")
                .because("die übrigen Regeln greifen nur in den Modulpaketen aus §3.1");
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
                .because("§3.1: domain hat keine Abhängigkeiten, auch keine Null-Annotationen");
    }

    static ArchRule domainUsesNoNetworkOrJdbc(String root) {
        return noNetworkOrJdbc(root + ".domain..")
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
                        "org.springframework.scheduling..",
                        "org.springframework.cache..",
                        // spring-tx
                        "org.springframework.transaction..",
                        // nur die Validierungs-API, nicht Hibernate Validator
                        "jakarta.validation..",
                        // Null-Annotationen (@NullMarked), wie Spring 7 sie selbst verwendet
                        "org.jspecify.annotations..",
                        // Logging-API, die Spring Boot an Logback bindet
                        "org.slf4j..")
                .because("§3.1/§11: application nutzt von Spring nur spring-context, spring-tx und die Validierungs-API");
    }

    static ArchRule applicationUsesNoNetworkOrJdbc(String root) {
        // Spring Web, JDBC, JPA und javax.* schließt schon applicationOnlyUsesAllowedLibraries aus
        return noNetworkOrJdbc(root + ".application..")
                .because("§3.1: application verwendet nichts aus Spring Web, JDBC, JPA oder einem HTTP-Client");
    }

    /**
     * Netzwerk und JDBC aus dem JDK, also alles, was die Allowlist {@code java..} durchlässt: HTTP-Client, RMI, JDBC,
     * Sockets, Kanäle, Namensauflösung ({@link InetAddress}, {@link SocketAddress}) und Netzwerkschnittstellen.
     * {@link URL} und {@link java.net.URI} selbst bleiben erlaubt, nur die Aufrufe, die eine Verbindung öffnen, nicht.
     */
    private static ClassesShouldConjunction noNetworkOrJdbc(String layerPackage) {
        return noClasses().that().resideInAPackage(layerPackage)
                .should().dependOnClassesThat(
                        resideInAnyPackage("java.net.http..", "java.net.spi..", "java.rmi..", "java.sql..")
                        .or(assignableTo(InetAddress.class))
                        .or(assignableTo(SocketAddress.class))
                        .or(assignableTo(NetworkInterface.class))
                        .or(assignableTo(URLConnection.class))
                        .or(assignableTo(Socket.class))
                        .or(assignableTo(ServerSocket.class))
                        .or(assignableTo(DatagramSocket.class))
                        .or(assignableTo(NetworkChannel.class)))
                .orShould().callMethod(URL.class, "openConnection")
                .orShould().callMethod(URL.class, "openConnection", Proxy.class)
                .orShould().callMethod(URL.class, "openStream")
                .orShould().callMethod(URL.class, "getContent")
                .orShould().callMethod(URL.class, "getContent", Class[].class);
    }
}
