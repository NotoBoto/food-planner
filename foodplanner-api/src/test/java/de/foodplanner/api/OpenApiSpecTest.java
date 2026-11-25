package de.foodplanner.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import de.foodplanner.infrastructure.PostgresTestConfiguration;

/**
 * springdoc liefert die Spezifikation und Swagger UI, und {@code web/openapi.json} entspricht ihr (§7).
 *
 * <p>Neu schreiben nach einer Änderung an Endpunkten oder DTOs: in IntelliJ die Run Configuration
 * „OpenAPI-Spezifikation neu schreiben“ oder {@value #REWRITE_COMMAND}
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class OpenApiSpecTest {

    /** Surefire startet die Tests im Modulordner, web/ liegt daneben. */
    private static final Path SPEC_FILE = Path.of("..", "web", "openapi.json");

    /** Läuft auch in einem frischen Klon, weil -am die benötigten Module mitbaut. */
    static final String REWRITE_COMMAND = "./mvnw -pl foodplanner-api -am test -Dtest=OpenApiSpecTest "
            + "-Dsurefire.failIfNoSpecifiedTests=false -Dopenapi.write=true";

    @Autowired
    MockMvc mockMvc;

    @Test
    void committedSpecMatchesTheApi() throws Exception {
        String actual = normalize(mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8));

        if (Boolean.getBoolean("openapi.write")) {
            Files.writeString(SPEC_FILE, actual, StandardCharsets.UTF_8);
            return;
        }

        assertThat(SPEC_FILE).as("web/openapi.json fehlt").exists();
        assertThat(normalize(Files.readString(SPEC_FILE, StandardCharsets.UTF_8)))
                .as("web/openapi.json ist veraltet. Neu schreiben mit der IntelliJ-Run-Configuration "
                        + "\"OpenAPI-Spezifikation neu schreiben\" oder " + REWRITE_COMMAND)
                .isEqualTo(actual);
    }

    @Test
    void swaggerUiIsServed() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void swaggerUiIsReachableUnderTheDocumentedAddress() throws Exception {
        // Die Adresse aus der Spezifikation (§9.1) leitet auf die eigentliche Seite weiter.
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
    }

    @Test
    void sortIsDescribedInSnakeCase() throws Exception {
        String spec = mockMvc.perform(get("/v3/api-docs"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(spec).contains("\"price_asc\"").doesNotContain("\"PRICE_ASC\"");
    }

    /** Gleiche Zeilenenden auf jedem System und genau ein Zeilenumbruch am Ende. */
    private static String normalize(String json) {
        return json.replace("\r\n", "\n").strip() + "\n";
    }
}
