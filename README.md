# Linux MCP Server

基于 Spring Boot 与 Spring AI 构建的 Model Context Protocol (MCP) 服务端，实现对本地与远程 Linux 主机的安全 Shell 命令执行。通过 MCP 标准接口，可被 Claude Desktop 等支持 MCP 的 AI 助手直接接入，扩展 AI 的运维能力。

## 目录
- [简介](#简介)
- [功能亮点](#功能亮点)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [与 AI 助手集成](#与-ai-助手集成)
- [安全建议](#安全建议)
- [开发与调试](#开发与调试)
- [许可证](#许可证)

## 简介
Linux MCP Server 将 Shell 执行封装成一个 MCP 工具 `executeShell`，由 Spring AI 自动注册到 MCP Server。客户端通过标准 MCP 调用，指定目标主机和命令，即可在本地或远程 Linux 机器上执行脚本，返回标准输出与错误日志。

## 功能亮点
- **双模式执行**：自动判断命令执行位置，支持本地进程调度与远程 SSH 执行。
- **连接池管理**：基于 Caffeine 缓存复用 SSH 连接，默认 5 分钟自动过期清理。
- **轻量配置**：使用简单的 `hosts.json` 维护远程主机清单，可热重启生效。
- **Spring Boot 原生集成**：开箱即用的 `mvn spring-boot:run`/可执行 JAR 部署方式，默认监听 `3001` 端口。
- **MCP 协议兼容**：依赖 `spring-ai-starter-mcp-server-webmvc`，支持 STREAMABLE HTTP 通道，适配主流 MCP 客户端。

## 快速开始
### 环境准备
- Java 17+
- Maven 3.6+（推荐使用仓库自带的 `./mvnw`/`mvnw.cmd`）
- 具备 SSH 服务的目标 Linux 主机

### 获取代码
```bash
git clone <repository-url>
cd linux-mcp-server
```

### 配置远程主机
编辑 `src/main/resources/hosts.json`：
```json
[
  {
    "ip": "192.168.1.100",
    "username": "devops",
    "password": "your_password"
  }
]
```
- `ip`：目标主机地址，需与 `executeShell` 中的 `machineIp` 对应。
- `username`/`password`：SSH 登录凭据，目前采用密码认证，可在代码中扩展密钥登录。

> ⚠️ 建议不要将生产凭据提交到版本库，使用环境变量或配置中心管理敏感信息。

### 启动服务
开发环境直接运行：
```bash
mvn spring-boot:run
```
或打包独立运行：
```bash
mvn clean package
java -jar target/linux-mcp-server-1.0.0.jar
```
启动完成后，默认在 `http://localhost:3001/mcp` 暴露 MCP 接入点。

## 配置说明
关键配置集中在 `src/main/resources/application.yml`：
```yaml
server:
  port: 3001            # MCP Server 监听端口
spring.ai.mcp.server:
  name: linux-mcp-server
  version: 1.0.0
  protocol: STREAMABLE  # 使用流式 HTTP 传输
  streamable-http:
    mcp-endpoint: /mcp  # MCP 入口路径
```
如需自定义：
- 更改端口或 `mcp-endpoint` 时，记得同步更新 MCP 客户端配置。
- 若需要连接更多主机，直接在 `hosts.json` 中追加条目并重启服务。

## 与 AI 助手集成
以 Claude Desktop 为例，可在其 `claude_desktop_config.json` 中添加：
```json
{
  "mcpServers": [
    {
      "name": "linux-mcp-server",
      "type": "http",
      "command": "http",
      "args": ["http://localhost:3001/mcp"]
    }
  ]
}
```
连接成功后，Claude 将自动发现工具 `executeShell`：
- `machineIp`（可选）：为空或本地地址执行本机命令。
- `path`（可选）：命令执行目录，缺省为 `/`。
- `shell`（必填）：需要执行的完整命令。

## 安全建议
- **凭据管理**：`hosts.json` 以明文保存密码，仅用于开发或受控环境；生产环境请改用密钥认证或外部安全存储。
- **主机校验**：当前使用 `PromiscuousVerifier` 跳过主机指纹校验，部署到生产前应替换为已知主机列表以防中间人攻击。
- **权限最小化**：为远程机器创建受限用户，避免使用 `root` 账户并限制可执行命令范围。
- **网络限制**：通过防火墙/安全组限制 MCP Server 与目标主机的访问来源。

## 开发与调试
- 代码结构主要位于 `src/main/java/com/doodl6/mcp/linux`，可按需扩展新的工具方法。
- 如需调整 SSH 缓存策略，可修改 `RemoteShellService` 中的 `CACHE_DURATION_MINUTES`、`MAX_CACHE_SIZE`。
- 修改完成后建议手动触发一次 `executeShell`（本地与远程各一次）验证输出与错误处理逻辑。

## 许可证
项目基于 [MIT 许可证](LICENSE) 开源，可自由使用与二次开发。
