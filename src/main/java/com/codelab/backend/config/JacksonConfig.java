//package com.codelab.backend.config;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.databind.json.JsonMapper;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.jackson2.SecurityJackson2Modules;
//
//@Configuration
//public class JacksonConfig {
//
//    // This bean is ONLY for OAuth2 cookie serialization
//    // We give it a specific name so Spring does NOT use it
//    // as the default ObjectMapper for REST requests
//    @Bean(name = "oauth2ObjectMapper")
//    public ObjectMapper oauth2ObjectMapper() {
//        ObjectMapper mapper = JsonMapper.builder()
//                .addModule(new JavaTimeModule())
//                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
//                .build();
//
//        mapper.registerModules(
//                SecurityJackson2Modules.getModules(
//                        JacksonConfig.class.getClassLoader()
//                )
//        );
//
//        return mapper;
//    }
//}