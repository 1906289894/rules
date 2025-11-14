//package com.wb.rules.config;
//
//import org.kie.api.KieServices;
//import org.kie.api.builder.KieBuilder;
//import org.kie.api.builder.KieFileSystem;
//import org.kie.api.builder.KieModule;
//import org.kie.api.runtime.KieContainer;
//import org.kie.api.runtime.KieSession;
//import org.kie.internal.io.ResourceFactory;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.Resource;
//import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
//import org.springframework.core.io.support.ResourcePatternResolver;
//
//import java.io.IOException;
//
//@Configuration
//public class DroolsConfig {
//    private static final KieServices kieServices = KieServices.Factory.get();
//    //指定规则文件路径
//    private static final String RULES_PATH = "rules/";
//
//    @Bean
//    public KieFileSystem kieFileSystem() throws IOException {
//        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
//        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
//        Resource[] resources = resourcePatternResolver.getResources("classpath*:" + RULES_PATH + "**/*.drl");
//        for(Resource resource : resources) {
//            kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_PATH + resource.getFilename()));
//        }
//        return kieFileSystem;
//    }
//
//    @Bean
//    public KieContainer kieContainer() throws IOException {
//        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem());
//        kieBuilder.buildAll();
//        KieModule kieModule = kieBuilder.getKieModule();
//        return kieServices.newKieContainer(kieModule.getReleaseId());
//    }
//
//    @Bean
//    public KieSession kieSession() throws IOException {
//        return kieContainer().newKieSession();
//    }
//}
