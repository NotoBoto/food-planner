package de.foodplanner.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Wendet die Regeln aus {@link ArchitectureRules} auf die Produktionsklassen aller Module an.
 *
 * <p>Die Regeln für domain und application prüfen echte Klassen (application seit #23, domain seit #19); keine Regel
 * darf leer bestehen.
 */
@AnalyzeClasses(packages = ArchitectureRules.ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule classesResideInModulePackages =
            ArchitectureRules.classesResideInModulePackages(ArchitectureRules.ROOT);

    @ArchTest
    static final ArchRule dependenciesPointInward = ArchitectureRules.dependenciesPointInward(ArchitectureRules.ROOT);

    @ArchTest
    static final ArchRule domainOnlyUsesJdk =
            ArchitectureRules.domainOnlyUsesJdk(ArchitectureRules.ROOT);

    @ArchTest
    static final ArchRule domainUsesNoNetworkOrJdbc =
            ArchitectureRules.domainUsesNoNetworkOrJdbc(ArchitectureRules.ROOT);

    @ArchTest
    static final ArchRule applicationOnlyUsesAllowedLibraries =
            ArchitectureRules.applicationOnlyUsesAllowedLibraries(ArchitectureRules.ROOT);

    @ArchTest
    static final ArchRule applicationUsesNoNetworkOrJdbc =
            ArchitectureRules.applicationUsesNoNetworkOrJdbc(ArchitectureRules.ROOT);
}
