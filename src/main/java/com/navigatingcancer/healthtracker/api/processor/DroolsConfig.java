package com.navigatingcancer.healthtracker.api.processor;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DroolsConfig {


    public static final String TRIAGE_RULES = "rules/TRIAGE.drl";
    public static final String ACTION_NEEDED_RULES = "rules/ACTION_NEEDED.drl";
    public static final String WATCH_CAREFULLY_RULES = "rules/WATCH_CAREFULLY.drl";
    public static final String MISSED_CHECKINS_RULES = "rules/MISSED_CHECKINS.drl";
    public static final String OUT_OF_MEDICATION = "rules/OUT_OF_MEDICATION.drl";
    public static final String NO_ACTION_RULES = "rules/NO_ACTION_NEEDED.drl";
    public static final String PRO_CTCAE_TRIAGE = "rules/PRO_CTCAE_TRIAGE.drl";
    public static final String PRO_CTCAE_ACTION_NEEDED = "rules/PRO_CTCAE_ACTION_NEEDED.drl";
    public static final String PRO_CTCAE_NO_ACTION_NEEDED = "rules/PRO_CTCAE_NO_ACTION_NEEDED.drl";
    public static final String MN_RULES = "rules/MN_RULES.drl";

    @Bean
    public KieContainer kieContainer() {
        KieServices kieServices = KieServices.Factory.get();

        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        kieFileSystem.write(ResourceFactory.newClassPathResource(TRIAGE_RULES));
        kieFileSystem.write(ResourceFactory.newClassPathResource(ACTION_NEEDED_RULES));
        kieFileSystem.write(ResourceFactory.newClassPathResource(WATCH_CAREFULLY_RULES));
        kieFileSystem.write(ResourceFactory.newClassPathResource(MISSED_CHECKINS_RULES));
        kieFileSystem.write(ResourceFactory.newClassPathResource(OUT_OF_MEDICATION));
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        KieModule kieModule = kieBuilder.getKieModule();

        return kieServices.newKieContainer(kieModule.getReleaseId());
    }

    @Bean
    public KieContainer kiePROCTCAEContainer() {
        KieServices kieServices = KieServices.Factory.get();

        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        kieFileSystem.write(ResourceFactory.newClassPathResource(PRO_CTCAE_TRIAGE));
        kieFileSystem.write(ResourceFactory.newClassPathResource(ACTION_NEEDED_RULES));
        kieFileSystem.write(ResourceFactory.newClassPathResource(MISSED_CHECKINS_RULES));
        kieFileSystem.write(ResourceFactory.newClassPathResource(NO_ACTION_RULES));
        kieFileSystem.write(ResourceFactory.newClassPathResource(WATCH_CAREFULLY_RULES));
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        KieModule kieModule = kieBuilder.getKieModule();

        return kieServices.newKieContainer(kieModule.getReleaseId());
    }

    @Bean
    public KieContainer kieMNContainer() {
        KieServices kieServices = KieServices.Factory.get();

        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        kieFileSystem.write(ResourceFactory.newClassPathResource(PRO_CTCAE_TRIAGE));
        kieFileSystem.write(ResourceFactory.newClassPathResource(ACTION_NEEDED_RULES));
        kieFileSystem.write(ResourceFactory.newClassPathResource(MISSED_CHECKINS_RULES));
        kieFileSystem.write(ResourceFactory.newClassPathResource(NO_ACTION_RULES));
        kieFileSystem.write(ResourceFactory.newClassPathResource(WATCH_CAREFULLY_RULES));
        kieFileSystem.write(ResourceFactory.newClassPathResource(MN_RULES));
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        KieModule kieModule = kieBuilder.getKieModule();

        return kieServices.newKieContainer(kieModule.getReleaseId());
    }   

    @Bean
    public KieContainer kieStatusCheck() {
        KieServices kieServices = KieServices.Factory.get();

        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        kieFileSystem.write(ResourceFactory.newClassPathResource(MISSED_CHECKINS_RULES));
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        KieModule kieModule = kieBuilder.getKieModule();

        return kieServices.newKieContainer(kieModule.getReleaseId());
    }

    @Bean
    public KieContainer kieCTCAEStatusCheck() {
        KieServices kieServices = KieServices.Factory.get();

        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        kieFileSystem.write(ResourceFactory.newClassPathResource(NO_ACTION_RULES));
        kieFileSystem.write(ResourceFactory.newClassPathResource(MISSED_CHECKINS_RULES));
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        KieModule kieModule = kieBuilder.getKieModule();

        return kieServices.newKieContainer(kieModule.getReleaseId());
    }

}
