package de.foodplanner.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Wendet die Regeln aus {@link ArchitectureRules} auf die Produktionsklassen aller Module an.
 */
@AnalyzeClasses(packages = ArchitectureRules.ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule classesResideInModulePackages =
            ArchitectureRules.classesResideInModulePackages(ArchitectureRules.ROOT);

    @ArchTest
    static final ArchRule dependenciesPointInward = ArchitectureRules.dependenciesPointInward(ArchitectureRules.ROOT);

    @ArchTest
    static final ArchRule domainOnlyUsesJdk = ArchitectureRules.domainOnlyUsesJdk(ArchitectureRules.ROOT);

    @ArchTest
    static final ArchRule domainUsesNoHttpOrSql = ArchitectureRules.domainUsesNoHttpOrSql(ArchitectureRules.ROOT);

    @ArchTest
    static final ArchRule applicationOnlyUsesAllowedLibraries =
            ArchitectureRules.applicationOnlyUsesAllowedLibraries(ArchitectureRules.ROOT);

    @ArchTest
    static final ArchRule applicationUsesNoWebJdbcJpaOrHttpClient =
            ArchitectureRules.applicationUsesNoWebJdbcJpaOrHttpClient(ArchitectureRules.ROOT);
}
