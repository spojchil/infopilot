package io.github.spojchil.infopilot.server.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/** MyBatis-Plus 配置。扫描 {@code mapper} 包以注册 Mapper 接口，无需 XML。 */
@Configuration
@MapperScan("io.github.spojchil.infopilot.server.mapper")
public class MybatisPlusConfig {}
