# Jenkins Git 插件核心功能演示手册

> 本仓库是 Jenkins Git 插件的测试仓库。本手册对应本地 Jenkins 服务器上已搭建的 4 个演示任务，
> 覆盖 Git 插件（git plugin 5.10.1）的核心功能。

## 1. 演示环境

| 项目 | 内容 |
|---|---|
| Jenkins | 2.585，http://localhost:8086 （Docker 容器 `jenkins`，账号 `root` / 密码 `root`） |
| Git 相关插件 | `git` 5.10.1、`git-client` 6.6.1、`git-parameter` 462.463.v496a_59f698e5、`workflow-multibranch`（多分支）、`github-branch-source`（GitHub 增强） |
| 演示仓库 | https://github.com/yinshi197/JenkinsFile_repo.git （分支：`main`、`test/test1`；公开仓库，匿名可克隆） |
| Jenkinsfile | 仓库根目录 `Jenkinsfile`（`checkout scm` → `chmod` → `build.sh` → `test.sh`） |

已创建的演示任务：

| 任务 | URL | 演示内容 |
|---|---|---|
| `demo-01-freestyle-basics` | http://localhost:8086/job/demo-01-freestyle-basics/ | FreeStyle：分支选择、浅克隆、轮询触发、GIT_* 环境变量、变更日志 |
| `demo-02-pipeline-checkout` | http://localhost:8086/job/demo-02-pipeline-checkout/ | Pipeline：`checkout` 步骤与高级选项、`git` 简写、changeSets |
| `demo-03-multibranch` | http://localhost:8086/job/demo-03-multibranch/ | 多分支流水线：自动发现 `main` 与 `test/test1`，Jenkinsfile 即流水线 |
| `demo-04-git-parameter` | http://localhost:8086/job/demo-04-git-parameter/ | 参数化构建：下拉选择分支/标签（Git Parameter） |

## 2. 核心功能地图

| # | 功能 | 说明 | 演示位置 |
|---|---|---|---|
| 1 | 源码管理（Git SCM）| 克隆/更新仓库，支持 HTTP(S)、SSH、本地路径 | demo-01 ~ 04 |
| 2 | 分支说明符 Branch Specifier | `*/main`、`**`、`*/test/*`、`refs/tags/*`，也可用构建参数 `${BRANCH}` | demo-01（`*/main`）、demo-04（`${BRANCH}`）|
| 3 | 凭证 Credentials | 私有仓库需 Username/Password、SSH 私钥或 Token 凭证 | 见 §6.1 |
| 4 | 克隆选项 | 浅克隆 depth、不拉取标签、克隆超时、参考仓库 | demo-01、demo-02 |
| 5 | 轮询触发器 Poll SCM | 定时 `git ls-remote` 检查新提交，有则构建（Webhook 的轮询替代方案） | demo-01（`H/15 * * * *`）|
| 6 | 构建环境变量 `GIT_*` | `GIT_COMMIT`/`GIT_BRANCH`/`GIT_URL`/`GIT_PREVIOUS_COMMIT` 等（**FreeStyle 注入，Pipeline 不注入**） | demo-01、demo-02 |
| 7 | 变更日志 changelog | 每次构建记录"谁在哪个提交改了什么"，第二次构建起生效 | demo-01、demo-02 §3 |
| 8 | `checkout` 步骤 | Pipeline 中检出代码的标准方式，返回 `GIT_COMMIT` 等 Map | demo-02 |
| 9 | 克隆扩展 | `CleanCheckout`、`LocalBranch`、`SparseCheckoutPaths`、`PruneStaleBranch`、子模块等 | demo-02 |
| 10 | `git` 简写步骤 | `git url:, branch:, changelog:false`（功能受限的简化版 checkout） | demo-02 |
| 11 | `currentBuild.changeSets` | Pipeline 中读取变更集（提交 ID/作者/说明） | demo-02 |
| 12 | 多分支流水线 Multibranch | 自动发现匹配的分支，每个分支自动生成子任务并执行各自 Jenkinsfile | demo-03 |
| 13 | 分支索引 Branch Indexing | 扫描 → 发现新分支 / 删除已删分支 / 触发构建，保留索引日志 | demo-03 |
| 14 | 参数化选分支 | Git Parameter 下拉选择远端分支/标签，供 Branch Specifier 使用 | demo-04 |
| 15 | 子模块 / refspec+PR / Webhook / 提交状态回写 | 进阶功能 | 见 §6 |

## 3. 演示任务详解

### 3.1 demo-01：FreeStyle 基础功能

**配置位置（UI 路径）**：任务 → 配置
- 源码管理 → Git → 仓库 URL：`https://github.com/yinshi197/JenkinsFile_repo.git`
- 分支说明符：`*/main`（只构建 main）
- Additional Behaviours：
  - *Advanced clone behaviours* → Shallow clone + Depth=1（浅克隆加速）
  - *Advanced clone behaviours* → Honor refspec ... 之上勾选 **不拉取标签**（noTags）
- 构建触发器 → Poll SCM：`H/15 * * * *`（每 15 分钟左右检查一次新提交）
- 构建步骤 → Execute shell：打印 `GIT_*` 变量、`git log`、检查 `.git/shallow`

**实测结果（构建 #1，SUCCESS）**：
```
GIT_URL                        = https://github.com/yinshi197/JenkinsFile_repo.git
GIT_BRANCH                     = origin/main
GIT_COMMIT                     = b33d07a028b23103c39c5885a43f661ffb6fff01
GIT_PREVIOUS_COMMIT            =      （首次构建为空）
GIT_PREVIOUS_SUCCESSFUL_COMMIT =      （首次构建为空）
[浅克隆] 命中 depth=1
```

**变更日志**：任务页 → 某次构建 → *变更记录*。首次构建显示 `First time build. Skipping changelog.`；
之后每次拉到新提交时，会显示该提交的作者、提交信息和触达文件列表。

**实测（推送新提交后）**：向 `main` 推送新提交 `6e14d85`（本手册）后，点击 *Poll Now*（对应接口
`POST /job/demo-01-freestyle-basics/polling`）→ 轮询发现新提交并自动排队构建 #2（SUCCESS），
变更日志即为该提交（作者 `1974126471`）。等定时轮询到点也会同样触发。

### 3.2 demo-02：Pipeline 中的 Git 用法

**演示阶段**：
1. **知识点 0**：内联 Pipeline 脚本中全局变量 `scm` 不可用，`checkout scm` 仅适用于"从 SCM 读取流水线脚本"的任务（见 demo-03）。
2. **显式 checkout + 高级选项**：`checkout([$class:'GitSCM', ...])` 一次性演示
   - `CloneOption`（`shallow: true, depth: 1`）浅克隆
   - `LocalBranch`（检出到匹配的本地分支，日志中可见 `git checkout -b test/test1`）
   - `CleanCheckout`（构建前清理工作区）
   - `SparseCheckoutPaths`（稀疏检出，只拉取 `scripts/`、`src/`，工作区中不再有 `pipeline/` 等目录）
   - **checkout 返回值**：`def scmVars = checkout(...)`，`scmVars.GIT_COMMIT` / `scmVars.GIT_BRANCH` 即本次检出的提交与分支
3. **`git` 简写步骤**：`git url:'...', branch:'main', changelog:false`（子目录 `dir('shorthand-checkout')` 内检出）
4. **变更日志 changeSets**：遍历 `currentBuild.changeSets` 打印 `commitId / author / msg`
5. **运行仓库脚本**：容器内无 `python3`，脚本自动降级为 `bash -n` 语法校验（演示跨环境兼容写法）

**实测要点（构建 #6，SUCCESS）**：浅克隆、稀疏检出、CleanCheckout 均在日志中生效；
`shell 里的 GIT_BRANCH = ''` —— 验证了下面的"常见坑 1"。

### 3.3 demo-03：多分支流水线

**配置**：*Multibranch Pipeline* → Branch Sources → Git
- 项目仓库 URL：同上；**Include branches**：`main test/*`（只发现匹配的分支）
- Build Configuration：by Jenkinsfile，脚本路径 `Jenkinsfile`
- Scan Triggers：`PeriodicFolderTrigger`（每天自动扫描一次；也可以点 *Scan Multibranch Pipeline Now*，或由 webhook 触发）

**实测结果**：扫描后自动生成 2 个子任务：
- `main`（构建 #1、#2 均 SUCCESS）
- `test/test1`（构建 #1、#2 均 SUCCESS）

**实测（推送新提交后）**：向 `main` 和 `test/test1` 各推送一个新提交，执行
*Scan Multibranch Pipeline Now* 后，两个分支均自动排队构建（#3，均 SUCCESS），
各自变更日志都记录了新提交 `6e14d85`。

每个分支任务使用**该分支自己的 Jenkinsfile** 执行流水线；分支删除后会按 *Orphaned Item Strategy* 自动移除子任务。分支索引日志记录每次扫描的发现/删除情况（任务页 → *Branch Indexing Log*）。

### 3.4 demo-04：参数化选择分支/标签（git-parameter 插件）

**配置**：
- 参数：Git Parameter，名称 `BRANCH`，类型 *Branch*，默认值 `origin/main`（下拉值来自远端分支列表）
- 源码管理：Git → 分支说明符 **`${BRANCH}`**（构建时将参数替换为实际引用）
- 使用方式：*Build with Parameters* 选择后构建；直接 *立即构建* 则使用默认值

**实测结果（构建 #1，SUCCESS）**：
```
BRANCH 参数      = origin/test/test1       ← 构建时传入
Checking out Revision b33d07a... (refs/remotes/origin/test/test1)
GIT_BRANCH(实际) = origin/test/test1
```

## 4. Pipeline 可复用片段

```groovy
// 1) 从 SCM 读取的 Jenkinsfile 中最常用：checkout scm
def scmVars = checkout scm
echo "本次构建: ${scmVars.GIT_BRANCH} @ ${scmVars.GIT_COMMIT}"

// 2) 显式 GitSCM（本地建的内联任务、或需要自定义选项时）
def scmVars = checkout([
    $class: 'GitSCM',
    branches: [[name: '*/main']],
    extensions: [
        [$class: 'CloneOption', shallow: true, depth: 1, timeout: 10],
        [$class: 'CleanCheckout'],
        [$class: 'LocalBranch', localBranch: '**']
    ],
    userRemoteConfigs: [[
        url: 'https://github.com/yinshi197/JenkinsFile_repo.git',
        credentialsId: ''          // 私有仓库填凭证 ID；公开仓库留空
    ]]
])

// 3) git 简写步骤（简化版，仅支持少量参数）
git url: 'https://github.com/yinshi197/JenkinsFile_repo.git', branch: 'main', changelog: false

// 4) 读取变更日志（变更集）
for (set in currentBuild.changeSets) {
    for (item in set.items) {
        echo "提交 ${item.commitId.take(8)} | 作者 ${item.author} | ${item.msg.trim()}"
    }
}

// 5) SSH 私钥访问私有仓库
checkout([$class: 'GitSCM',
    userRemoteConfigs: [[url: 'git@github.com:ORG/REPO.git', credentialsId: 'github-ssh-key']],
    branches: [[name: '*/main']]])
```

## 5. 常见坑（本环境实测）

1. **Pipeline 不会自动注入 `GIT_*` 环境变量**（FreeStyle 会）。
   Pipeline 中请使用 `checkout` 的返回值（`scmVars.GIT_COMMIT`）或直接 `git rev-parse HEAD`；
   直接引用 `$GIT_COMMIT` 会得到空值，引用未定义变量还会抛 `MissingPropertyException`。
2. **首次构建没有变更日志**：日志显示 `First time build. Skipping changelog.`，第二次构建起才有对比。
3. **浅克隆会影响变更日志的完整性**：`depth=1` 时本地没有历史，新旧提交之间无法完整对比；
   需要完整 changelog 时尽量不要浅克隆（demo-01 为演示克隆加速才开启）。
4. **内联的非沙箱 Pipeline 脚本需要管理员审批**：修改脚本后哈希变化，需在
   *Manage Jenkins → In-process Script Approval* 重新批准（否则报 `UnapprovedUsageException`）。
   本目录附 `approve-and-wait.groovy` 可在脚本控制台自动等待并批准。
5. **POST XML 必须声明字符集**：用 REST API 创建/更新中文配置时，
   `Content-Type` 要写 `application/xml; charset=UTF-8`，否则中文会变 `?`，
   更新 config.xml 时甚至会报 `An invalid XML character (Unicode: 0x80)`。

## 6. 进阶功能做法说明

### 6.1 凭证（私有仓库）
*Manage Jenkins → Credentials → System → Global credentials → Add Credentials*：
- Username/Password（GitHub 建议用 Personal Access Token 当密码）
- SSH Username with private key（URL 用 `git@github.com:...`）
- 然后在任务的"源码管理"或 `checkout(userRemoteConfigs:[[credentialsId:'...']])` 中引用。

### 6.2 Webhook 触发（替代轮询）
- GitHub 仓库 → Settings → Webhooks → `http://<可从公网访问的Jenkins>/github-webhook/`。
- 本地 Jenkins 需要内网穿透（ngrok、smee.io、Cloudflare Tunnel 等）。
- 本地拿不到公网地址时，用 demo-01 的 Poll SCM 或手动 *Scan/Build* 即可。

### 6.3 构建 GitHub PR / 任意 ref（refspec）
```
Advanced（源码管理）→ Refspec:
+refs/pull/*/head:refs/remotes/origin/pr/*
分支说明符:
PR-*          （或 */pr/* 视配置）
```

### 6.4 子模块
源码管理 → Additional Behaviours → *Advanced sub-modules behaviours* →
勾选 *Recursively update submodules* / *Use credentials from default remote of parent repository*。

### 6.5 标签（tag）构建
- 分支说明符写 `refs/tags/*`（构建所有标签）或 `refs/tags/v1.*`；
- 或沿用 demo-04 的 Git Parameter，把类型换成 *Tag*。

### 6.6 GitHub 提交状态回写（commit status）
使用 `github-branch-source` + *GitHub Server* 配置（需 Token），
构建成功/失败状态会显示在 GitHub 提交旁（本环境暂未配置，避免需要真实 Token）。

## 7. 如何重建这些任务

本目录 `job-configs/` 保存了 4 个任务的完整 `config.xml`，可直接通过 REST API 重建：

```bash
# 1) 取 CSRF crumb（Jenkins 2.585 需要）
curl -c cookies.txt -u root:root http://localhost:8086/crumbIssuer/api/json
# 记下返回的 crumb，设为 $CRUMB

# 2) 创建任务（注意 charset=UTF-8，否则中文乱码）
curl -X POST -u root:root -b cookies.txt \
  -H "Jenkins-Crumb: $CRUMB" \
  -H "Content-Type: application/xml; charset=UTF-8" \
  --data-binary @demo-01-freestyle-basics.xml \
  "http://localhost:8086/createItem?name=demo-01-freestyle-basics"
```

或直接在 Jenkins UI 中照 §3 的"配置位置"手动配置。各文件对应关系：

| 文件 | 说明 |
|---|---|
| `demo-01-freestyle-basics.xml` | FreeStyle 基础演示任务 |
| `demo-02-pipeline-checkout.xml` | Pipeline 演示任务（内联脚本） |
| `demo-03-multibranch.xml` | 多分支流水线任务 |
| `demo-04-git-parameter.xml` | 参数化构建任务 |
| `approve-and-wait.groovy` | 脚本控制台工具：等待并批准待审批的内联 Pipeline 脚本 |
| `serialize-gitparameter.groovy` | 脚本控制台工具：用当前插件版本生成 Git Parameter 的准确 XML |
