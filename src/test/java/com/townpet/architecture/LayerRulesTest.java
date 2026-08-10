package com.townpet.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.townpet")
class LayerRulesTest {

  @ArchTest
  static final ArchRule businessModulesDoNotDependOnSharedBusinessPackages =
      noClasses()
          .that()
          .resideOutsideOfPackage("com.townpet.common..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("com.townpet.common.domain..", "com.townpet.common.persistence..");

  @ArchTest
  static final ArchRule moduleInternalsAreNotExposedAsCrossModuleContracts =
      noClasses()
          .that()
          .resideOutsideOfPackage("com.townpet..infrastructure..")
          .and()
          .resideOutsideOfPackage("com.townpet.common..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "com.townpet..infrastructure..", "com.townpet..web..", "com.townpet..domain..");
}
