# Linux MCP Server

基于 Spring Boot 和 Spring AI 框架实现的 MCP Server，支持在本地或远程 Linux 主机上执行 shell 命令。该服务器实现了 Model Context Protocol (MCP)，可以与支持 MCP 的 AI 助手集成，提供安全的 shell 命令执行能力。

## 功能特性

- **本地命令执行**：支持在本地 Linux 系统上执行 shell 命令
- **远程 SSH 执行**：通过 SSH 连接到远程 Linux 主机执行命令
- **连接池管理**：自动管理 SSH 连接，支持连接复用和自动清理
- **JSON 配置**：简单的 JSON 格式主机配置文件
- **Spring Boot 集成**：基于 Spring Boot 框架，易于部署和扩展
- **MCP 协议支持**：完全兼容 Model Context Protocol 标准

## 项目结构

```
src/
├── main/
│   ├── java/com/doodl6/mcp/linux/
│   │   ├── LinuxMcpServerApplication.java    # 主启动类
│   │   ├── config/
│   │   │   └── ToolConfig.java               # 工具配置
│   │   └── service/
│   │       ├── ShellService.java             # Shell 服务（本地执行）
│   │       └── RemoteShellService.java       # 远程 Shell 服务（SSH 执行）
│   └── resources/
│       └── hosts.json                        # 主机配置文件
```

## 配置说明

### 主机配置（hosts.json）

在 `src/main/resources/hosts.json` 文件中配置远程主机信息：

```json
[
  {
    "ip": "192.168.1.100",
    "username": "root",
    "password": "your_password"
  },
  {
    "ip": "10.0.0.50",
    "username": "admin",
    "password": "admin_password"
  }
]
```

配置字段说明：
- `ip`：远程主机 IP 地址
- `username`：SSH 登录用户名
- `password`：SSH 登录密码

## 使用说明

### 环境要求

- Java 17 或更高版本
- Maven 3.6+ 或 Gradle 7.0+
- Linux/macOS/Windows（开发环境）
- 目标 Linux 主机需要开启 SSH 服务

### 启动服务

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd linux-mcp-server
   ```

2. **配置主机信息**
   编辑 `src/main/resources/hosts.json` 文件，添加你的主机配置

3. **编译运行**
   ```bash
   mvn spring-boot:run
   ```
   或
   ```bash
   mvn clean package
   java -jar target/linux-mcp-server-0.0.1-SNAPSHOT.jar
   ```

### MCP 工具

服务通过 MCP 协议暴露一个统一的 shell 执行工具，AI 助手可以通过 MCP 客户端调用：

#### Shell 执行工具
- **工具名称**：`executeShell`
- **描述**：在本地或远程 Linux 主机上执行 shell 命令
- **参数**：
  - `machineIp`（可选）：目标主机 IP 地址
    - 如果为空、`localhost` 或 `127.0.0.1`，则在本地执行
    - 如果为其他 IP 地址，则通过 SSH 远程执行（需在 hosts.json 中配置）
  - `path`（可选）：执行命令的工作目录
  - `shell`：要执行的 shell 命令

#### 工具调用方式

该工具通过 MCP 协议供 AI 助手调用，不是传统的 REST API。AI 助手会根据用户需求自动选择合适的参数调用此工具。例如：

- **本地执行**：当用户询问本地系统信息时，AI 助手会调用工具并传入 `localhost` 或空的 `machineIp`
- **远程执行**：当用户指定某个 IP 地址的主机时，AI 助手会传入对应的 `machineIp` 参数

### 与 AI 助手集成

该 MCP Server 可以与支持 MCP 协议的 AI 助手集成，如 Claude Desktop 等。配置方式请参考对应 AI 助手的 MCP 集成文档。

### 安全注意事项

- 请妥善保管 hosts.json 配置文件，避免密码泄露
- 建议在生产环境中使用 SSH 密钥认证替代密码认证
- 限制 SSH 用户权限，避免使用 root 用户
- 在防火墙中限制 SSH 访问来源

## 开发

### 技术栈

- Spring Boot 3.4+
- Spring AI
- SSHJ（SSH 客户端库）
- Caffeine（缓存库）
- Jackson（JSON 处理）

### 贡献

欢迎提交 Issue 和 Pull Request 来完善项目功能。
