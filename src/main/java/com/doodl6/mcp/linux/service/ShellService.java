package com.doodl6.mcp.linux.service;

import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

@Service
public class ShellService {

    @Resource
    private LocalShellService localShellService;

    @Resource
    private RemoteShellService remoteShellService;

    public record Output(String result) {
    }

    @McpTool(description = "在目标机器上执行shell命令", generateOutputSchema = true)
    public Output executeShell(@McpToolParam(description = "目标机器ip，默认localhost", required = false) String machineIp,
                               @McpToolParam(description = "执行路径，默认/", required = false) String path,
                               @McpToolParam(description = "shell命令") String shell) {
        String result;
        if (StringUtils.isBlank(machineIp) || "127.0.0.1".equals(machineIp) || "localhost".equals(machineIp)) {
            result = localShellService.executeShell(path, shell);
        } else {
            result = remoteShellService.executeShell(machineIp, path, shell);
        }

        return new Output(result);
    }

}
