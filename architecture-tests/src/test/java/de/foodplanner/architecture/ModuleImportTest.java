package de.foodplanner.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

/**
 * Stellt sicher, dass der Import die Klassen der Module wirklich sieht. Die Regeln für domain und application
 * bestehen vorerst auch leer ({@code allowEmptyShould(true)} in {@link ArchitectureTest}); fiele ein Modul aus dem
 * Import, bemerkte das sonst keine Regel.
 */
@AnalyzeClasses(packages = ArchitectureRules.ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
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
