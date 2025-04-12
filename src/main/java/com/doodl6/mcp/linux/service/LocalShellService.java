package com.doodl6.mcp.linux.service;

import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class LocalShellService {

    public String executeShell(String path, String shell) {
        StringBuilder output = new StringBuilder();
        Process process = null;
        BufferedReader reader = null;

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            // 分割 shell 命令，处理带参数的情况
            processBuilder.command("bash", "-c", shell);

            if (StringUtils.isBlank(path)) {
                path = "/";
            }
            // 设置工作目录
            processBuilder.directory(new File(path));

            // 启动进程
            process = processBuilder.start();

            // 读取命令输出
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // 读取错误输出
            reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = reader.readLine()) != null) {
                output.append("ERROR: ").append(line).append("\n");
            }

            // 等待命令执行完成
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                output.append("ERROR: Command exited with code ").append(exitCode);
            }
        } catch (IOException e) {
            output.append("IOException occurred: ").append(e.getMessage());
        } catch (InterruptedException e) {
            output.append("InterruptedException occurred: ").append(e.getMessage());
            Thread.currentThread().interrupt(); // 恢复中断状态
        } finally {
            // 清理资源
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    output.append("Failed to close reader: ").append(e.getMessage());
                }
            }
            if (process != null) {
                process.destroy();
            }
        }

        return output.toString();
    }
}
