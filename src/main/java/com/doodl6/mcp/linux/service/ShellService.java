package com.doodl6.mcp.linux.service;

import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class ShellService {

    @Resource
    private LocalShellService localShellService;

    @Resource
    private RemoteShellService remoteShellService;

    @Tool(description = "Execute given shell on a target linux or a local machine")
    public String executeShell(String machineIp, String path, String shell) {
        String result;
        if (StringUtils.isBlank(machineIp) || "127.0.0.1".equals(machineIp) || "localhost".equals(machineIp)) {
            result = localShellService.executeShell(path, shell);
        } else {
            result = remoteShellService.executeShell(machineIp, path, shell);
        }

        return result;
    }

}
