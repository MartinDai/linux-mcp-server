package com.doodl6.mcp.linux.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class RemoteShellService {

    private final ResourceLoader resourceLoader;

    private final Map<String, HostConfig> hostConfigs;

    private static final String CONFIG_FILE = "classpath:host.config";

    /**
     * 缓存池，存储 machineIp -> SSHClient
     */
    private final Cache<String, SSHClient> sessionCache;

    /**
     * 过期时间，单位：分钟
     */
    private static final long CACHE_DURATION_MINUTES = 5;

    /**
     * 最大缓存大小
     */
    private static final int MAX_CACHE_SIZE = 100;

    public RemoteShellService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.sessionCache = Caffeine.newBuilder()
                .expireAfterAccess(CACHE_DURATION_MINUTES, TimeUnit.MINUTES)
                .maximumSize(MAX_CACHE_SIZE)
                .removalListener((key, value, cause) -> {
                    if (value != null) {
                        try {
                            ((SSHClient) value).disconnect();
                        } catch (IOException e) {
                            System.err.println("Failed to disconnect SSHClient for " + key + ": " + e.getMessage());
                        }
                    }
                })
                .build();
        this.hostConfigs = new HashMap<>();
    }

    @PostConstruct
    public void init() {
        loadHostConfigs();
    }

    private void loadHostConfigs() {
        try {
            Resource resource = resourceLoader.getResource(CONFIG_FILE);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue; // 跳过空行和注释
                    }
                    String[] parts = line.split("\\s+");
                    if (parts.length != 3) {
                        System.err.println("Invalid config line: " + line);
                        continue;
                    }
                    String ip = parts[0];
                    String username = parts[1];
                    String password = parts[2];
                    hostConfigs.put(ip, new HostConfig(username, password));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load host.config: " + e.getMessage(), e);
        }
    }

    public String executeShell(String machineIp, String path, String shell) {
        StringBuilder output = new StringBuilder();
        SSHClient sshClient = null;

        // 获取主机配置
        HostConfig config = hostConfigs.get(machineIp);
        if (config == null) {
            output.append("ERROR: No configuration found for IP: ").append(machineIp);
            return output.toString();
        }

        try {
            // 从缓存获取 SSHClient
            sshClient = sessionCache.get(machineIp, key -> null);
            if (sshClient == null) {
                sshClient = createNewSSHClient(machineIp, config);
                sessionCache.put(machineIp, sshClient);
            }

            // 检查会话是否有效
            if (!sshClient.isConnected() || !sshClient.isAuthenticated()) {
                sshClient.disconnect();
                sshClient = createNewSSHClient(machineIp, config);
                sessionCache.put(machineIp, sshClient); // 更新缓存
            }

            try (Session session = sshClient.startSession()) {
                // 构造命令
                String command;
                if (path != null && !path.trim().isEmpty()) {
                    command = String.format("cd %s && %s", path, shell);
                } else {
                    command = shell;
                }

                Session.Command cmd = session.exec(command);

                // 读取标准输出
                BufferedReader reader = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                // 读取错误输出
                reader = new BufferedReader(new InputStreamReader(cmd.getErrorStream()));
                while ((line = reader.readLine()) != null) {
                    output.append("ERROR: ").append(line).append("\n");
                }

                // 等待命令执行完成
                int exitCode = cmd.getExitStatus();
                if (exitCode != 0) {
                    output.append("ERROR: Command exited with code ").append(exitCode);
                }
            }
        } catch (IOException e) {
            output.append("IOException occurred: ").append(e.getMessage());
            // 如果发生错误，移除无效的缓存项
            sessionCache.invalidate(machineIp);
            if (sshClient != null) {
                try {
                    sshClient.disconnect();
                } catch (IOException ignored) {
                    // 忽略关闭错误
                }
            }
        }

        return output.toString();
    }

    private SSHClient createNewSSHClient(String machineIp, HostConfig hostConfig) throws IOException {
        SSHClient ssh = new SSHClient();
        try {
            // 禁用主机密钥验证（生产环境建议配置 known_hosts）
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.setConnectTimeout(5000); // 5秒连接超时
            ssh.connect(machineIp);
            ssh.authPassword(hostConfig.user, hostConfig.password);
            return ssh;
        } catch (IOException e) {
            try {
                ssh.disconnect();
            } catch (IOException ignored) {
                // 忽略关闭错误
            }
            throw e; // 重新抛出异常
        }
    }

    private record HostConfig(String user, String password) {
    }
}
