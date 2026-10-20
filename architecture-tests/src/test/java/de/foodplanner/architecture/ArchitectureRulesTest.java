package de.foodplanner.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Prüft die Regeln gegen Fixture-Pakete unter {@code fixtures.<fall>}, die wie die echten Module gegliedert sind.
 * Der Hauptscan in {@link ArchitectureTest} schließt Testklassen aus und sieht diese Fixtures nie.
 */
class ArchitectureRulesTest {

    private static final String FIXTURES = "de.foodplanner.architecture.fixtures";

    private static final JavaClasses OUTWARD = fixture("outward");

    private static final String[] NETWORK_OR_JDBC = {
            "java.net.http.HttpClient", "java.net.HttpURLConnection", "java.net.Socket", "java.net.ServerSocket",
            "java.net.DatagramSocket", "java.nio.channels.SocketChannel", "java.sql.Connection",
            "java.net.URL.openStream()", "java.net.URL.openConnection()", "java.net.URL.getContent()"};

    @Test
    void allRulesAcceptAllowedFixture() {
        JavaClasses classes = fixture("allowed");
        for (ArchRule rule : List.of(
                ArchitectureRules.classesResideInModulePackages(root("allowed")),
                ArchitectureRules.dependenciesPointInward(root("allowed")),
                ArchitectureRules.domainOnlyUsesJdk(root("allowed")),
                ArchitectureRules.domainUsesNoNetworkOrJdbc(root("allowed")),
                ArchitectureRules.applicationOnlyUsesAllowedLibraries(root("allowed")),
                ArchitectureRules.applicationUsesNoNetworkOrJdbc(root("allowed")))) {
            EvaluationResult result = rule.evaluate(classes);
            assertThat(result.hasViolation())
                    .as(rule.getDescription() + "\n" + result.getFailureReport())
                    .isFalse();
        }
    }

    @Test
    void classesMustResideInModulePackages() {
        // sonst entginge z. B. ein HTTP-Client in de.foodplanner.support allen Schichtregeln
        assertViolation(ArchitectureRules.classesResideInModulePackages(root("outsidelayer")), fixture("outsidelayer"),
                "outsidelayer.support.Leak");
    }

    @Test
    void domainMustNotUseSpring() {
        assertViolation(ArchitectureRules.domainOnlyUsesJdk(root("domainspring")), fixture("domainspring"),
                "org.springframework.stereotype.Component");
    }

    @Test
    void domainMustNotUseNullAnnotations() {
        assertViolation(ArchitectureRules.domainOnlyUsesJdk(root("domainnullmarked")), fixture("domainnullmarked"),
                "org.jspecify.annotations.NullMarked");
    }

    @Test
    void domainMustNotUseNetworkOrJdbc() {
        // java.. ist für domain erlaubt; erst die zweite Regel schließt Netzwerk und JDBC aus
        JavaClasses classes = fixture("domainnetwork");
        assertThat(ArchitectureRules.domainOnlyUsesJdk(root("domainnetwork")).evaluate(classes).hasViolation())
                .isFalse();
        assertViolation(ArchitectureRules.domainUsesNoNetworkOrJdbc(root("domainnetwork")), classes, NETWORK_OR_JDBC);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "OutwardDomain.toApplication",
            "OutwardDomain.toInfrastructure",
            "OutwardDomain.toApi",
            "OutwardDomain.toIngestion",
            "OutwardHandler.toInfrastructure",
            "OutwardHandler.toApi",
            "OutwardHandler.toIngestion",
            "OutwardAdapter.toApi",
            "OutwardAdapter.toIngestion",
            "OutwardEndpoint.toIngestion",
            "OutwardCommand.toApi"})
    void dependenciesMustPointInward(String forbiddenField) {
        // ein Feld je verbotener Kante, damit jede Schichtgrenze einzeln geprüft ist
        assertViolation(ArchitectureRules.dependenciesPointInward(root("outward")), OUTWARD, forbiddenField + ">");
    }

    @Test
    void applicationMustNotUseNetworkOrJdbc() {
        assertViolation(ArchitectureRules.applicationUsesNoNetworkOrJdbc(root("applicationnetwork")),
                fixture("applicationnetwork"), NETWORK_OR_JDBC);
    }

    @Test
    void applicationMustNotUseWebJdbcOrJpa() {
        assertViolation(ArchitectureRules.applicationOnlyUsesAllowedLibraries(root("applicationio")),
                fixture("applicationio"),
                "javax.net.ssl.HttpsURLConnection", "javax.sql.DataSource",
                "org.springframework.web.client.RestClient", "org.springframework.http.HttpHeaders",
                "org.springframework.jdbc.core.simple.JdbcClient", "jakarta.persistence.EntityManager");
    }

    @Test
    void applicationMustNotUseSpringCoreOrBeans() {
        assertViolation(ArchitectureRules.applicationOnlyUsesAllowedLibraries(root("applicationspringcore")),
                fixture("applicationspringcore"),
                "org.springframework.beans.factory.annotation.Autowired", "org.springframework.core.env.Environment");
    }

    @Test
    void applicationMustNotUseHibernateValidator() {
        assertViolation(ArchitectureRules.applicationOnlyUsesAllowedLibraries(root("applicationhibernate")),
                fixture("applicationhibernate"),
                "org.hibernate.validator.constraints.Length");
    }

    private static void assertViolation(ArchRule rule, JavaClasses classes, String... expectedInReport) {
        EvaluationResult result = rule.evaluate(classes);
        assertThat(result.hasViolation()).isTrue();
        assertThat(String.join("\n", result.getFailureReport().getDetails())).contains(expectedInReport);
    }

    private static JavaClasses fixture(String name) {
        return new ClassFileImporter().importPackages(root(name));
    }

    private static String root(String name) {
        return FIXTURES + "." + name;
    }
}
