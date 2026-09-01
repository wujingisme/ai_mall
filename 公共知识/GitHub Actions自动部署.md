# GitHub Actions 自动部署

## 适用范围

当前方案只自动部署 Spring Boot 后端：推送 `main` 分支且修改 `backend/` 后，GitHub Actions 自动测试、打包、上传 JAR、重启服务器并检查接口。数据库、`backend.env`、JWT 密钥和微信密钥仍保留在服务器，不进入 GitHub。

## 一次性服务器准备

服务器目录：`/www/wwwroot/mymall/backend`。

1. 确认环境文件存在：

   ```bash
   ls -l /www/wwwroot/mymall/backend/backend.env
   file /www/wwwroot/mymall/backend/backend.env
   ```

2. 如果显示 `CRLF line terminators`，执行：

   ```bash
   sed -i 's/\r$//' /www/wwwroot/mymall/backend/backend.env
   ```

3. 应用由宝塔的 `www` 用户运行，因此 `www` 必须能读取环境文件：

   ```bash
   chown www:www /www/wwwroot/mymall/backend/backend.env
   chmod 640 /www/wwwroot/mymall/backend/backend.env
   ```

4. 首次自动部署前，先在宝塔停止正在运行的 `mall-backend`。之后由部署脚本负责停止、启动和回滚，不要让宝塔和脚本同时启动同一个 JAR。

5. 部署脚本默认使用：

   ```text
   Java：/www/server/java/jdk-17.0.8/bin/java
   端口：SERVER_PORT，未设置时为 8080
   日志：/www/wwwroot/mymall/backend/logs/mall-backend.log
   ```

   如果服务器 JDK 路径不同，需要修改 `deploy/mall-backend-deploy.sh` 中的 `JAVA_BIN`。

## GitHub 密钥配置

### 1. 生成专用 SSH 密钥

在本地 PowerShell 执行，路径可按实际情况调整：

```powershell
ssh-keygen -t ed25519 -C "github-actions-ai-mall" -f "$env:USERPROFILE\.ssh\ai_mall_deploy"
```
出现提示时连续按两次回车，不设置密码。会生成两个文件：
C:\Users\你的用户名\.ssh\ai_mall_deploy
C:\Users\你的用户名\.ssh\ai_mall_deploy.pub
ai_mall_deploy：私钥，只放 GitHub
ai_mall_deploy.pub：公钥，放服务器
把生成的 `ai_mall_deploy.pub` 公钥追加到服务器部署账号的：

```text
~/.ssh/authorized_keys
```

私钥 `ai_mall_deploy` 只放到 GitHub Secret，不要提交到仓库。

### 2. 获取服务器指纹

在本地执行：

```powershell
ssh-keyscan -H 124.221.241.24
```

把完整输出保存为 GitHub Secret `DEPLOY_KNOWN_HOSTS`。工作流会校验指纹，不使用不安全的自动接受主机密钥方式。

### 3. 添加 GitHub Actions Secrets

打开 GitHub 仓库：`Settings` → `Secrets and variables` → `Actions` → `New repository secret`，添加：

| Secret 名称 | 值 |
| --- | --- |
| `DEPLOY_HOST` | `124.221.241.24` |
| `DEPLOY_PORT` | `22` |
| `DEPLOY_USER` | 有权限部署的 SSH 用户；首次可用 `root`，稳定后建议换成专用用户 |
| `DEPLOY_PATH` | `/www/wwwroot/mymall/backend` |
| `DEPLOY_SSH_KEY` | `ai_mall_deploy` 私钥的完整内容 |
| `DEPLOY_KNOWN_HOSTS` | `ssh-keyscan` 的完整输出 |

如果不用 `root`，该用户必须能无密码执行：

```bash
sudo bash /www/wwwroot/mymall/backend/deploy/mall-backend-deploy.sh <jar路径>
```

## 如何触发

提交并推送后端代码：

```bash
git add backend deploy .github/workflows/deploy-backend.yml
git commit -m "deploy backend"
git push origin main
```

也可以在 GitHub 仓库的 `Actions` → `Deploy backend` → `Run workflow` 手动触发。

工作流成功后，服务器会：

1. 将旧 JAR 备份到 `backend/backups/`。
2. 上传新 JAR 到 `backend/releases/`。
3. 停止旧进程并以 `www` 用户启动新进程。
4. 请求 `/api/v1/shop/products?page=1&pageSize=1` 做启动检查。
5. 检查失败时自动恢复最近的旧 JAR。

## 发布后检查

服务器执行：

```bash
ps -ef | grep '[m]all-backend-1.0.0.jar'
tail -n 100 /www/wwwroot/mymall/backend/logs/mall-backend.log
curl -i "http://127.0.0.1:8080/api/v1/shop/products?page=1&pageSize=1"
```

## 回滚

查看备份：

```bash
ls -lt /www/wwwroot/mymall/backend/backups/
```

将需要的备份 JAR 作为参数重新执行部署脚本即可；脚本会再次进行启动检查：

```bash
bash /www/wwwroot/mymall/backend/deploy/mall-backend-deploy.sh \
  /www/wwwroot/mymall/backend/backups/mall-backend-YYYYMMDDHHMMSS.jar
```

## 边界

- 只有修改 `backend/`、部署脚本或工作流文件才会自动触发后端发布。
- 管理后台网页和微信小程序暂未接入此工作流。
- 微信小程序即使自动构建，真机预览、上传和审核仍需要微信开发者工具或微信 CI。
- 不要把 `backend.env`、数据库密码、JWT 密钥、微信 AppSecret 或 SSH 私钥提交到仓库。
