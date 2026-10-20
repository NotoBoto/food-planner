package de.foodplanner.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Prüft die Regeln gegen Fixture-Pakete unter {@code fixtures.<fall>}, die wie die echten Module gegliedert sind.
 * Der Hauptscan in {@link ArchitectureTest} schließt Testklassen aus und sieht diese Fixtures nie.
 */
class ArchitectureRulesTest {

    private static final String FIXTURES = "de.foodplanner.architecture.fixtures";

    @Test
    void allRulesAcceptAllowedFixture() {
        JavaClasses classes = fixture("allowed");
        for (ArchRule rule : List.of(
                ArchitectureRules.dependenciesPointInward(root("allowed")),
                ArchitectureRules.domainOnlyUsesJdk(root("allowed")),
                ArchitectureRules.domainUsesNoHttpOrSql(root("allowed")),
                ArchitectureRules.applicationOnlyUsesAllowedLibraries(root("allowed")),
                ArchitectureRules.applicationUsesNoWebJdbcJpaOrHttpClient(root("allowed")))) {
            EvaluationResult result = rule.evaluate(classes);
            assertThat(result.hasViolation())
                    .as(rule.getDescription() + "\n" + result.getFailureReport())
                    .isFalse();
        }
    }

    @Test
    void domainMustNotUseSpring() {
        assertViolation(ArchitectureRules.domainOnlyUsesJdk(root("domainspring")), fixture("domainspring"),
                "org.springframework.stereotype.Component");
    }

    @Test
    void domainMustNotUseHttpClientOrJdbc() {
        // java.. ist für domain erlaubt; erst die zweite Regel schließt HTTP-Client und JDBC aus
        assertThat(ArchitectureRules.domainOnlyUsesJdk(root("domainhttpsql"))
                .evaluate(fixture("domainhttpsql")).hasViolation()).isFalse();
        assertViolation(ArchitectureRules.domainUsesNoHttpOrSql(root("domainhttpsql")), fixture("domainhttpsql"),
                "java.net.http.HttpClient", "java.sql.Connection");
    }

    @ParameterizedTest
    @CsvSource({
            "domainoutward, OutwardDomain",
            "applicationoutward, OutwardHandler",
            "infrastructureoutward, OutwardAdapter",
            "rootsisolated, CrossEndpoint"})
    void dependenciesMustPointInward(String fixtureName, String offender) {
        assertViolation(ArchitectureRules.dependenciesPointInward(root(fixtureName)), fixture(fixtureName), offender);
    }

    @Test
    void applicationMustNotUseWebJdbcJpaOrHttpClient() {
        assertViolation(ArchitectureRules.applicationUsesNoWebJdbcJpaOrHttpClient(root("applicationio")),
                fixture("applicationio"),
                "java.net.http.HttpClient", "java.net.HttpURLConnection", "javax.net.ssl.HttpsURLConnection",
                "java.sql.Connection", "javax.sql.DataSource", "org.springframework.web.client.RestClient",
                "org.springframework.http.HttpHeaders", "org.springframework.jdbc.core.simple.JdbcClient",
                "jakarta.persistence.EntityManager");
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
