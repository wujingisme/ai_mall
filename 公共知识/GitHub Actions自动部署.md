# GitHub Actions 自动部署后端

## 先看结论

本项目的自动部署只处理 Spring Boot 后端：

~~~text
本地修改 backend
→ 推送到 GitHub 的 main 分支
→ GitHub Actions 自动测试和打包
→ 通过 SSH 上传 JAR
→ 服务器自动重启后端
→ 接口检查成功后完成发布
~~~

不会上传到服务器的内容：

~~~text
公共知识/
PROJECT_MEMORY.md
admin/
frontend/
数据库密码、JWT_SECRET、微信 AppSecret
~~~

只有修改下面内容时才会触发后端部署：

~~~text
backend/
deploy/mall-backend-deploy.sh
.github/workflows/deploy-backend.yml
~~~

只修改公共知识并推送，不会触发部署，也不会把文档复制到服务器。

## 一、一次性准备服务器

### 1. 打开宝塔终端

1. 登录宝塔面板。
2. 点击左侧“终端”。
3. 看到类似下面的提示符，说明进入服务器终端：

   ~~~text
   [root@VM-0-2-opencloudos ~]#
   ~~~

4. 输入命令时，只输入代码框中的内容，不要输入前面的 [root@...]#。

### 2. 检查环境文件和换行符

输入：

~~~bash
file /www/wwwroot/mymall/backend/backend.env
~~~

正常示例：

~~~text
/www/wwwroot/mymall/backend/backend.env: ASCII text
~~~

如果末尾出现 CRLF line terminators，说明文件有 Windows 换行符，执行：

~~~bash
sed -i 's/\r$//' /www/wwwroot/mymall/backend/backend.env
~~~

再次执行 file 检查，确认不再出现 CRLF line terminators。

### 3. 设置环境文件权限

后端由宝塔的 www 用户运行，因此执行：

~~~bash
chown www:www /www/wwwroot/mymall/backend/backend.env
chmod 640 /www/wwwroot/mymall/backend/backend.env
~~~

检查 www 是否能读取：

~~~bash
sudo -u www test -r /www/wwwroot/mymall/backend/backend.env && echo "读取成功"
~~~

预期输出：

~~~text
读取成功
~~~

没有输出或出现 Permission denied 时，不要继续部署，先检查文件权限。

### 4. 检查 Java 路径

~~~bash
ls -l /www/server/java/jdk-17.0.8/bin/java
~~~

如果能看到类似下面的文件信息，说明路径正确：

~~~text
-rwxr-xr-x ... /www/server/java/jdk-17.0.8/bin/java
~~~

如果提示 No such file or directory，需要把实际 JDK 路径改到 deploy/mall-backend-deploy.sh 中的 JAVA_BIN。

### 5. 首次部署前停止宝塔项目

1. 回到宝塔面板。
2. 点击顶部“Java项目”。
3. 找到 mall-backend。
4. 点击右侧“设置”。
5. 进入“服务”页面。
6. 点击“停止”。

首次自动部署前必须停止旧项目，避免宝塔和部署脚本同时启动同一个 JAR。之后由部署脚本负责停止、启动和回滚。

## 二、生成 GitHub 用的 SSH 密钥

### 1. 打开本地 PowerShell

1. 在 Windows 开始菜单搜索“PowerShell”。
2. 打开 Windows PowerShell。
3. 正确的提示符类似：

   ~~~text
   PS C:\Users\你的用户名>
   ~~~

   不是服务器提示符 [root@VM-0-2-opencloudos ~]#。

### 2. 生成密钥

在本地 PowerShell 输入：

~~~powershell
ssh-keygen -t ed25519 -C "github-actions-ai-mall" -f "$env:USERPROFILE\.ssh\ai_mall_deploy"
~~~

看到提示 Enter passphrase (empty for no passphrase): 时直接按回车；再次要求确认时再按一次回车。

成功示例：

~~~text
Your identification has been saved in ...\ai_mall_deploy
Your public key has been saved in ...\ai_mall_deploy.pub
~~~

检查私钥文件：

~~~powershell
Test-Path "$env:USERPROFILE\.ssh\ai_mall_deploy"
~~~

预期输出：

~~~text
True
~~~

### 3. 把公钥添加到服务器

先在本地 PowerShell 显示公钥：

~~~powershell
Get-Content "$env:USERPROFILE\.ssh\ai_mall_deploy.pub"
~~~

复制显示的完整一行，通常以 ssh-ed25519 开头。

然后回到宝塔“终端”：

~~~bash
mkdir -p ~/.ssh
chmod 700 ~/.ssh
vi ~/.ssh/authorized_keys
~~~

在 vi 中操作：

1. 按 i 进入编辑模式。
2. 粘贴公钥整行。
3. 按 Esc。
4. 输入 :wq。
5. 按回车保存退出。

最后执行：

~~~bash
chmod 600 ~/.ssh/authorized_keys
~~~

注意：服务器中放的是 .pub 公钥；没有 .pub 后缀的文件是私钥，只能放到 GitHub Secret。

### 4. 测试密钥登录

回到本地 PowerShell 执行：

~~~powershell
ssh -i "$env:USERPROFILE\.ssh\ai_mall_deploy" -o IdentitiesOnly=yes root@124.221.241.24
~~~

第一次出现主机指纹询问时输入 yes。

如果直接进入：

~~~text
[root@VM-0-2-opencloudos ~]#
~~~

且没有要求服务器密码，说明密钥登录成功。输入 exit 返回本地 PowerShell。

## 三、获取服务器指纹

必须在本地 PowerShell 执行，不要在服务器终端执行：

~~~powershell
ssh-keyscan.exe -H 124.221.241.24
~~~

示例输出：

~~~text
# 124.221.241.24:22 SSH-2.0-OpenSSH
|1|xxxxx ssh-ed25519 AAAAxxxxx
|1|xxxxx ecdsa-sha2-nistp256 AAAAxxxxx
~~~

复制 ssh-keyscan.exe 输出的全部内容，包括以 # 开头的行也可以复制。

不要复制下面这些：

~~~text
PS E:\ai_mall\backend>
ssh-keyscan.exe -H 124.221.241.24
[root@VM-0-2-opencloudos ~]#
~~~

## 四、在 GitHub 页面添加 6 个 Secret

### 1. 打开页面

1. 浏览器打开：https://github.com/wujingisme/ai_mall
2. 点击仓库顶部的 Settings。
3. 点击左侧 Secrets and variables。
4. 点击展开后的 Actions。
5. 点击绿色按钮 New repository secret。

如果看不到 Settings，说明当前 GitHub 账号没有仓库管理权限。

### 2. 添加普通值

每添加一个都要点击 Add secret 保存，不能把 6 个值一次性粘贴到一个 Secret 中。

添加以下 4 个：

| Name（名称） | Secret（值） |
| --- | --- |
| DEPLOY_HOST | 124.221.241.24 |
| DEPLOY_PORT | 22 |
| DEPLOY_USER | root |
| DEPLOY_PATH | /www/wwwroot/mymall/backend |

页面操作示例：

~~~text
Name：DEPLOY_HOST
Secret：124.221.241.24
点击 Add secret
~~~

### 3. 添加私钥

在本地 PowerShell 执行：

~~~powershell
Get-Content "$env:USERPROFILE\.ssh\ai_mall_deploy" -Raw
~~~

复制完整内容，包括下面两行：

~~~text
-----BEGIN OPENSSH PRIVATE KEY-----
...
-----END OPENSSH PRIVATE KEY-----
~~~

回到 GitHub 页面：

~~~text
点击 New repository secret
Name：DEPLOY_SSH_KEY
Secret：粘贴完整私钥
点击 Add secret
~~~

不要把私钥发给别人，也不要放入代码文件。

### 4. 添加服务器指纹

回到 GitHub 页面：

~~~text
点击 New repository secret
Name：DEPLOY_KNOWN_HOSTS
Secret：粘贴 ssh-keyscan.exe 输出的全部内容
点击 Add secret
~~~

### 5. 检查结果

页面上应看到以下 6 个名称：

~~~text
DEPLOY_HOST
DEPLOY_PORT
DEPLOY_USER
DEPLOY_PATH
DEPLOY_SSH_KEY
DEPLOY_KNOWN_HOSTS
~~~

只能看到名称、看不到具体值是正常的。

## 五、把自动部署文件推送到 GitHub

### 1. 打开本地项目目录

在本地 PowerShell 执行：

~~~powershell
cd E:\ai_mall
~~~

### 2. 添加本次部署文件

只执行下面命令：

~~~powershell
git add .github deploy PROJECT_MEMORY.md
git add "公共知识\GitHub Actions自动部署.md"
~~~

检查暂存内容：

~~~powershell
git diff --cached --name-only
~~~

预期至少包含：

~~~text
.github/workflows/deploy-backend.yml
PROJECT_MEMORY.md
deploy/mall-backend-deploy.sh
公共知识/GitHub Actions自动部署.md
~~~

如果出现 LF will be replaced by CRLF，这是 Windows 换行符提示，不是错误，可以继续。

### 3. 提交并推送

执行：

~~~powershell
git commit -m "ci: add backend automatic deployment"
~~~

看到类似下面内容表示提交成功：

~~~text
[main 422daec] ci: add backend automatic deployment
~~~

继续执行：

~~~powershell
git push origin main
~~~

看到类似下面内容表示推送成功：

~~~text
main -> main
~~~

如果出现 Could not resolve host: github.com，这是本地网络或 DNS 问题。网络恢复后重新执行 git push origin main 即可。

## 六、在 GitHub 查看自动部署

### 1. 打开工作流

1. 打开仓库主页。
2. 点击顶部 Actions。
3. 点击左侧 Deploy backend。

推送成功后会看到一条新的运行记录，例如：

~~~text
ci: add backend automatic deployment
main
In progress
~~~

点击这条记录，再点击 build-and-deploy 查看详情。

### 2. 查看每个步骤

工作流包含：

~~~text
Checkout
Set up Java 17
Build and test backend
Validate deployment secrets
Configure SSH
Prepare remote directories
Upload artifact and deploy script
Restart backend and run health check
~~~

判断方式：

~~~text
绿色对勾：成功
黄色转圈：正在执行
红色叉号：失败
~~~

第一次运行在 Build and test backend 停留几分钟是正常的，因为需要下载 Maven 依赖。

### 3. 没有自动运行时

1. 进入 Actions。
2. 点击 Deploy backend。
3. 点击右侧 Run workflow。
4. 分支选择 main。
5. 点击绿色 Run workflow。

不要在上一次运行仍是 In progress 时重复点击。

## 七、部署成功后的服务器检查

在宝塔“终端”执行。

### 1. 检查进程

~~~bash
ps -ef | grep '[m]all-backend-1.0.0.jar'
~~~

正常会看到一行 Java 进程，例如：

~~~text
www ... /www/wwwroot/mymall/backend/mall-backend-1.0.0.jar
~~~

### 2. 查看日志

~~~bash
tail -n 100 /www/wwwroot/mymall/backend/logs/mall-backend.log
~~~

重点看是否有：

~~~text
Started MallApplication
Tomcat started on port 8080
~~~

### 3. 测试接口

~~~bash
curl -i "http://127.0.0.1:8080/api/v1/shop/products?page=1&pageSize=1"
~~~

返回 HTTP/1.1 200 或正常 JSON，说明后端已启动。返回 500 时，查看上一步日志。

## 八、常见失败信息

| 失败信息 | 处理方式 |
| --- | --- |
| Missing repository secret | GitHub Secret 名称必须完全一致，重新检查 6 个名称 |
| Host key verification failed | 重新执行 ssh-keyscan.exe -H 124.221.241.24，更新 DEPLOY_KNOWN_HOSTS |
| Permission denied (publickey) | 检查服务器 ~/.ssh/authorized_keys 是否有完整公钥，以及权限是否为 600 |
| backend.env contains Windows CRLF | 在服务器执行 sed -i 's/\r$//' /www/wwwroot/mymall/backend/backend.env |
| www user cannot read backend.env | 检查 chown www:www 和 chmod 640 |
| Java executable not found | 检查服务器 JDK 路径并修改脚本中的 JAVA_BIN |
| New backend failed the health check | 查看日志；脚本会自动恢复上一份 JAR |
| Could not resolve host: github.com | 本地网络或 DNS 故障，网络恢复后重新推送 |

失败时只复制失败步骤的错误文字，不要复制 SSH 私钥、数据库密码或 JWT 密钥。

## 九、手动回滚

查看旧 JAR：

~~~bash
ls -lt /www/wwwroot/mymall/backend/backups/
~~~

选择一个备份文件，执行：

~~~bash
bash /www/wwwroot/mymall/backend/deploy/mall-backend-deploy.sh \
  /www/wwwroot/mymall/backend/backups/mall-backend-YYYYMMDDHHMMSS.jar
~~~

脚本会停止当前版本、恢复备份并重新执行接口检查。

## 十、以后公共知识文档的统一规则

以后所有供学习或操作使用的文档都放在：

~~~text
E:\ai_mall\公共知识\
~~~

每份文档建议按照以下顺序编写：

~~~text
1. 目的和适用范围
2. 操作前准备
3. 点击路径
4. 需要填写的内容
5. 需要执行的命令
6. 预期输出示例
7. 成功判断
8. 失败排查
9. 回滚或撤销方法
10. 安全注意事项
~~~

文档中不写真实密码、Token、私钥、微信 AppSecret 或个人敏感数据。只修改 公共知识/ 时不会触发当前后端部署工作流，也不会上传到服务器。
