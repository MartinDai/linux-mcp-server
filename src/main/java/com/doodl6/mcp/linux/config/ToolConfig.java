package com.doodl6.mcp.linux.config;

import com.doodl6.mcp.linux.service.ShellService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolConfig {

    @Bean
    public ToolCallbackProvider shellTools(ShellService shellService) {
        return MethodToolCallbackProvider.builder().toolObjects(shellService).build();
    }
}
