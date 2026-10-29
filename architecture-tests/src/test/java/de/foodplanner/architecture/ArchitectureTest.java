package de.foodplanner.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Wendet die Regeln aus {@link ArchitectureRules} auf die Produktionsklassen aller Module an.
 *
 * <p>domain und application haben noch keine Klassen; ihre Regeln dürfen deshalb vorerst leer bestehen. Sobald ein
 * Modul seine erste Klasse hat, {@code allowEmptyShould(true)} bei seinen Regeln entfernen und die Klasse in
 * {@link ModuleImportTest} aufnehmen (#8, #9).
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
            ArchitectureRules.domainOnlyUsesJdk(ArchitectureRules.ROOT).allowEmptyShould(true);

    @ArchTest
    static final ArchRule domainUsesNoNetworkOrJdbc =
            ArchitectureRules.domainUsesNoNetworkOrJdbc(ArchitectureRules.ROOT).allowEmptyShould(true);

    @ArchTest
    static final ArchRule applicationOnlyUsesAllowedLibraries =
            ArchitectureRules.applicationOnlyUsesAllowedLibraries(ArchitectureRules.ROOT).allowEmptyShould(true);

    @ArchTest
    static final ArchRule applicationUsesNoNetworkOrJdbc =
            ArchitectureRules.applicationUsesNoNetworkOrJdbc(ArchitectureRules.ROOT).allowEmptyShould(true);
}
