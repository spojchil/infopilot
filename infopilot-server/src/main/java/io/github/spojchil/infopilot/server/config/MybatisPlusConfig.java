package io.github.spojchil.infopilot.server.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("io.github.spojchil.infopilot.server.mapper")
public class MybatisPlusConfig {}
