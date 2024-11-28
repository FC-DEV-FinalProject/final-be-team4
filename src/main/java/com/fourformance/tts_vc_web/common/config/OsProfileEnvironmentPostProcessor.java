package com.fourformance.tts_vc_web.common.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;

public class OsProfileEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "osProfile";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String osName = System.getProperty("os.name").toLowerCase();
        String activeProfile = "linux"; // 기본 프로파일

        if (osName.contains("win")) {
            activeProfile = "windows";
        } else if (osName.contains("mac")) {
            activeProfile = "mac";
        }

        // 프로파일을 환경에 추가
        environment.addActiveProfile(activeProfile);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
