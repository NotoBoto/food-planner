package de.foodplanner.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import java.util.List;

/**
 * Stellt sicher, dass der Import die Klassen der Module wirklich sieht. Die Regeln in {@link ArchitectureTest} bestehen
 * nicht leer; fiele ein Modul aus dem Import, schlüge dort die Regel für dessen Paket an, diese Prüfung benennt die
 * Ursache.
 */
@AnalyzeClasses(packages = ArchitectureRules.ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleImportTest {

    @ArchTest
    void importsClassesOfEveryModule(JavaClasses classes) {
        // maven-compiler-plugin legt package-info.class auch ohne Annotationen an (createMissingPackageInfoClass)
        assertThat(List.of(
                "de.foodplanner.domain.search.SearchText",
                "de.foodplanner.application.package-info",
                "de.foodplanner.infrastructure.package-info",
                "de.foodplanner.api.ApiApplication",
                "de.foodplanner.ingestion.IngestionApplication"))
                .allMatch(classes::contain);
    }

    @ArchTest
    void excludesTestClasses(JavaClasses classes) {
        // Fixtures liegen in de.foodplanner.architecture.fixtures und dürfen nie im Hauptscan landen
        assertThat(classes).noneMatch(c -> c.getPackageName().startsWith("de.foodplanner.architecture"));
    }
}
