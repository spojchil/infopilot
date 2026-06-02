package io.github.spojchil.infopilot.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class InfoPilotApplication {

    /** 应用入口。无需额外配置，{@link SpringBootApplication} 自动完成组件扫描与自动配置。 */
    public static void main(String[] args) {
        SpringApplication.run(InfoPilotApplication.class, args);
    }
}
