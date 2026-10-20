package de.foodplanner.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

/**
 * Stellt sicher, dass der Import die Klassen der Module wirklich sieht. Ohne diesen Test bestünden alle Regeln
 * leer, sobald der Import nichts findet ({@code archRule.failOnEmptyShould=false}).
 */
@AnalyzeClasses(packages = "de.foodplanner", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleImportTest {

    @ArchTest
    void importsClassesOfCompositionRoots(JavaClasses classes) {
        assertThat(classes.contain("de.foodplanner.api.ApiApplication")).isTrue();
        assertThat(classes.contain("de.foodplanner.ingestion.IngestionApplication")).isTrue();
    }

    @ArchTest
    void excludesTestClasses(JavaClasses classes) {
        // Fixtures liegen in de.foodplanner.architecture.fixtures und dürfen nie im Hauptscan landen
        assertThat(classes).noneMatch(c -> c.getPackageName().startsWith("de.foodplanner.architecture"));
    }
}
