# COSBench 项目使用说明

本文档面向当前仓库源码使用场景，覆盖开发环境、编译方式、启动方式、压测提交、结果查看，以及 Azure Blob 自定义适配器的接入说明。

## 1. 项目定位

COSBench 是一个基于 Java 和 OSGi 的对象存储压测工具，典型运行形态分为两个角色：

- Controller：负责管理任务、调度 Driver、展示 Web 页面。
- Driver：负责真正执行存储读写请求。

当前仓库版本为 `0.4.2`。

## 2. 目录说明

仓库根目录中与日常使用最相关的目录如下：

- `dev/`：各个 COSBench OSGi 插件源码。
- `dist/main/`：OSGi 启动器依赖。
- `dist/osgi/libs/`：运行时依赖的第三方 OSGi bundle。
- `dist/osgi/plugins/`：COSBench 自身插件 jar 输出目录。
- `release/`：启动脚本、运行配置、样例 workload、日志与归档输出目录。
- `release/conf/`：Controller、Driver、Tomcat、OSGi 配置。
- `release/workloads/`：样例 workload XML。
- `dev/cosbench-azure/`：Azure Blob 自定义适配器源码与辅助脚本。

## 3. 环境要求

### 3.1 基础工具

- Git
- Java
- `curl`
- `nc`（netcat，启动脚本会用它检测 OSGi bundle 启动状态）

### 3.2 JDK 建议

- 日常源码编译可使用较新的 JDK。
- Azure 适配器目前编译为 Java 8 字节码。
- 当前仓库这套启动脚本已经补齐旧版 Equinox 在 JDK 11 和 JDK 17 下所需的兼容参数，Driver 和 Controller 都可以启动。
- 如果目标是长期稳定运行这套老运行时，Java 8 仍然是更保守的选择。

当前在较新的 JDK 上仍可能看到旧版 Equinox 的兼容性告警，但这不影响 Driver 和 Controller 启动成功。

## 4. 编译方式

### 4.1 原始项目的经典方式

原项目的历史构建方式主要是 Eclipse PDE 导出：

1. 将 `dev/` 下插件工程导入 Eclipse。
2. 把 `dist/main/`、`dist/osgi/`、`dist/osgi/libs/` 作为目标平台的一部分。
3. 修改某个插件后，通过 Eclipse 的 `Export -> Deployable plug-ins and fragments` 导出到 `dist/osgi/plugins/`。

这也是 [BUILD.md](BUILD.md) 中描述的主路径。

### 4.2 当前仓库可直接使用的辅助脚本

本次改造为 Azure 和 Driver 预检补充了一组脚本，适合在 macOS/Linux 上直接使用：

#### 只编译 Azure 适配器

```bash
./dev/cosbench-azure/build-azure-adaptor.sh
```

输出文件位于：

```bash
dist/osgi/plugins/cosbench-azure_0.4.2.jar
```

如果你不想手工改 XML，也可以直接在项目根目录执行辅助脚本，由脚本动态生成临时 workload 并提交。推荐使用新的通用脚本名 `submit-azure-workload.sh`，它支持四种认证方式：

- `azure-cli`
- `storage-key`
- `service-principal`
- `managed-identity`

默认认证方式是 `azure-cli`：

```bash
./submit-azure-workload.sh --account <your-storage-account>
```

如果你已经导出了环境变量，也可以直接复用：

```bash
export COSBENCH_AZURE_ACCOUNT=<your-storage-account>
./submit-azure-workload.sh
```

如需显式指定 endpoint 或 controller 地址：

```bash
./submit-azure-workload.sh \
	--account <your-storage-account> \
	--endpoint https://<your-storage-account>.blob.core.windows.net \
	--controller anonymous:cosbench@127.0.0.1:19088
```

如果要切换成其他认证方式，可以通过 `--auth-type` 指定。

Storage Key：

```bash
./submit-azure-workload.sh \
	--auth-type storage-key \
	--account <your-storage-account> \
	--storage-key '<your-storage-key>'
```

Service Principal：

```bash
./submit-azure-workload.sh \
	--auth-type service-principal \
	--account <your-storage-account> \
	--tenant-id <your-tenant-id> \
	--client-id <your-client-id> \
	--client-secret '<your-client-secret>'
```

Managed Identity：

```bash
./submit-azure-workload.sh \
	--auth-type managed-identity \
	--account <your-storage-account>
```

如果是 user-assigned managed identity，可以额外带上 client id：

```bash
./submit-azure-workload.sh \
	--auth-type managed-identity \
	--account <your-storage-account> \
	--client-id <managed-identity-client-id>
```

旧脚本名 `submit-azure-cli-workload.sh` 仍然保留，作为兼容入口，会自动转发到新脚本。

#### 导出 Driver 所需插件集合

```bash
./dev/cosbench-azure/export-driver-plugin-set.sh
```

这个脚本会从当前源码树中按依赖顺序编译并输出 Driver 运行需要的 COSBench 插件 jar 到 `dist/osgi/plugins/`。

#### 导出 Controller 所需插件集合

```bash
./dev/cosbench-azure/export-controller-plugin-set.sh
```

这个脚本会从当前源码树中按依赖顺序编译并输出 Controller 运行需要的 COSBench 插件 jar 到 `dist/osgi/plugins/`。

#### 检查 Driver 插件是否齐全

```bash
./dev/cosbench-azure/check-driver-plugin-set.sh
```

它会把 `release/conf/.driver/config.ini` 中声明的插件，与 `dist/osgi/plugins/` 中实际存在的 jar 做对比。

#### 校验 Azure bundle 能否被 OSGi 解析并启动

```bash
./dev/cosbench-azure/validate-azure-bundle.sh
```

这个校验是“精简 OSGi 环境校验”，不是完整 Driver 启动，但足以验证 Azure 插件本身是否能被装载并达到 `ACTIVE` 状态。

## 5. 运行前准备

### 5.1 保留原仓库自带依赖 jar

当前仓库依旧保留原项目自带的依赖 jar，例如：

- `dist/main/`
- `dist/osgi/libs/`
- `ext/libs/`
- 某些历史 adaptor 目录下内嵌的第三方 jar

这些文件属于原仓库的一部分，不应删除。

### 5.2 不提交本地生成产物

本仓库已经配置忽略以下本地生成内容：

- `dist/osgi/plugins/cosbench-*.jar`
- `dev/**/bin/`
- `dev/cosbench-azure/.build/`
- `release/log/`
- `release/archive/`
- `release/workspace/.metadata/`

也就是说：原仓库自带 jar 保留，自己本地编译出来的插件 jar 不提交。

## 6. 启动方式

所有启动脚本都在 `release/` 目录下执行。

### 6.1 启动 Driver

```bash
./release/start-driver.sh
```

该脚本会启动 Driver 对应的 OSGi 运行时，并依次检查需要激活的 bundle。当前脚本已经把 Azure 插件加入 Driver 启动列表。

Driver OSGi console 端口：`18089`

Driver Web 地址通常为：

```text
http://127.0.0.1:18088/driver
```

如果你希望直接验证页面是否可打开，建议访问：

```text
http://127.0.0.1:18088/driver/index.html
```

### 6.2 启动 Controller

```bash
./release/start-controller.sh
```

Controller OSGi console 端口：`19089`

Controller Web 地址通常为：

```text
http://127.0.0.1:19088/controller
```

首次打开时更建议直接访问首页：

```text
http://127.0.0.1:19088/controller/index.html
```

### 6.3 同时启动

```bash
./release/start-all.sh
```

这个脚本会先启动 Driver，再启动 Controller。

在当前仓库中，`start-all.sh` 已经验证可以正常拉起两侧 Web 服务。启动完成后可直接访问：

- `http://127.0.0.1:18088/driver/index.html`
- `http://127.0.0.1:19088/controller/index.html`

### 6.4 停止服务

```bash
./release/stop-driver.sh
./release/stop-controller.sh
```

或者：

```bash
./release/stop-all.sh
```

### 6.5 默认登录信息

当前默认用户文件位于 `release/conf/cosbench-users.xml`。

默认登录账号为：

- 用户名：`anonymous`
- 密码：`cosbench`

如果页面端口已监听，但浏览器一直转圈没有响应，优先检查：

- 是否使用了仓库中的最新 `release/cosbench-start.sh`
- 是否通过 `./release/stop-all.sh` 后重新执行过 `./release/start-all.sh`
- `release/conf/cosbench-users.xml` 是否存在

## 7. 配置说明

### 7.1 Controller 配置

文件：`release/conf/controller.conf`

默认关键项：

- `drivers = 1`
- `archive_dir = archive`
- Driver 地址默认为 `http://127.0.0.1:18088/driver`

### 7.2 Driver 配置

文件：`release/conf/driver.conf`

当前默认只定义了日志级别：

- `log_level = INFO`

### 7.3 Driver OSGi 配置

文件：`release/conf/.driver/config.ini`

这里定义了 Driver 所需的第三方 bundle、COSBench 插件以及启动级别。当前已包含：

- `plugins/cosbench-azure@7:start`

## 8. 压测执行

### 8.1 使用样例 workload

仓库当前带有样例文件：

- `release/workloads/asus.xml`
- `release/conf/azure-config-storage-key-sample.xml`
- `release/conf/azure-config-service-principal-sample.xml`
- `release/conf/azure-config-managed-identity-sample.xml`
- `release/conf/azure-config-azure-cli-sample.xml`

Azure 样例已经拆成四个独立文件，分别对应四种认证方式，避免再手工改 `auth_type`。

例如：

```bash
export COSBENCH_AZURE_ACCOUNT=<account>
export AZURE_TENANT_ID=<tenant-id>
export AZURE_CLIENT_ID=<client-id>
export AZURE_CLIENT_SECRET=<client-secret>
```

然后 workload 中可只保留：

```xml
<storage type="azure" config="auth_type=service-principal" />
```

### 8.2 使用 CLI 提交任务

进入 `release/` 后执行：

```bash
./cli.sh submit workloads/asus.xml
```

查看任务状态：

```bash
./cli.sh info
```

取消任务：

```bash
./cli.sh cancel <workload_id>
```

如果 Controller 不在本机默认地址，可以追加第三个参数：

```bash
./cli.sh submit workloads/asus.xml anonymous:cosbench@127.0.0.1:19088
```

### 8.3 使用 Web 页面操作

启动成功后，可直接访问 Controller 页面查看和提交 workload：

```text
http://127.0.0.1:19088/controller
```

更稳妥的入口是直接访问首页：

```text
http://127.0.0.1:19088/controller/index.html
```

默认登录账号：`anonymous`

默认登录密码：`cosbench`

`j_security_check` 是 Tomcat FORM 登录使用的内部提交地址，属于正常登录流程的一部分，不需要手工访问。

## 9. 测试结果与日志

### 9.1 日志位置

启动日志和系统日志默认位于：

- `release/log/`

例如：

- `release/log/controller-boot.log`
- `release/log/driver-boot.log`
- `release/log/system.log`

### 9.2 结果归档位置

Controller 默认会把归档结果输出到：

```text
release/archive/
```

这个目录由 `release/conf/controller.conf` 的 `archive_dir=archive` 控制。

### 9.3 Web 页面查看结果

任务执行后，可在 Controller Web 页面查看：

- workload 状态
- 各阶段执行情况
- 吞吐与时延统计
- 归档后的历史结果

## 10. Azure Blob 适配器说明

Azure 适配器源码位于：

- `dev/cosbench-azure/`

支持的认证方式：

- `storage-key`
- `service-principal`
- `managed-identity`
- `azure-cli`

更完整的 Azure 适配器参数说明、样例配置和校验说明，请参考：

- `dev/cosbench-azure/README.md`

### 10.1 Azure workload 示例

如果你只是想尽快跑通一次 Azure 测试，建议按下面的顺序做。

### 10.2 Azure 快速测试步骤

#### 第一步：编译并做预检

```bash
./dev/cosbench-azure/build-azure-adaptor.sh
./dev/cosbench-azure/export-driver-plugin-set.sh
./dev/cosbench-azure/export-controller-plugin-set.sh
./dev/cosbench-azure/check-driver-plugin-set.sh
./dev/cosbench-azure/validate-azure-bundle.sh
```

#### 第二步：启动 Driver 和 Controller

注意：如果你准备通过环境变量提供 Azure 账号、密钥或 Azure CLI 相关参数，需要先在当前 shell 中 `export`，再启动 Driver 和 Controller。已经运行中的 Java 进程不会感知你后续新设置的环境变量。

```bash
./release/start-driver.sh
./release/start-controller.sh
```

Controller 页面：

```text
http://127.0.0.1:19088/controller
```

#### 第三步：按认证方式准备环境变量

Storage Key 模式：

```bash
export COSBENCH_AZURE_ACCOUNT=myaccount
export COSBENCH_AZURE_STORAGE_KEY='your-storage-key'
```

Service Principal 模式：

```bash
export COSBENCH_AZURE_ACCOUNT=myaccount
export AZURE_TENANT_ID='your-tenant-id'
export AZURE_CLIENT_ID='your-client-id'
export AZURE_CLIENT_SECRET='your-client-secret'
```

Managed Identity 模式：

```bash
export COSBENCH_AZURE_ACCOUNT=myaccount
```

Azure CLI 缓存登录模式：

```bash
az login
export COSBENCH_AZURE_ACCOUNT=myaccount
```

这种模式会复用当前用户已经缓存的 Azure CLI 登录态，内部通过 `az account get-access-token` 获取访问 Blob 的 bearer token。

#### 第四步：提交 workload

推荐直接在项目根目录使用辅助脚本，它会按认证方式选择对应模板，并把账号等参数注入临时 workload 后再提交。

Managed Identity：

```bash
./submit-azure-workload.sh \
	--auth-type managed-identity \
	--account myaccount
```

Storage Key：

```bash
./submit-azure-workload.sh \
	--auth-type storage-key \
	--account myaccount \
	--storage-key 'your-storage-key'
```

Service Principal：

```bash
./submit-azure-workload.sh \
	--auth-type service-principal \
	--account myaccount \
	--tenant-id 'your-tenant-id' \
	--client-id 'your-client-id' \
	--client-secret 'your-client-secret'
```

Azure CLI 缓存登录：

```bash
./submit-azure-workload.sh \
	--auth-type azure-cli \
	--account myaccount
```

如果你确实想直接提交固定样例 XML，也可以继续使用底层 CLI：

```bash
cd release
./cli.sh submit conf/azure-config-azure-cli-sample.xml
```

如果 Driver/Controller 已经启动，而你是在启动之后才设置 `COSBENCH_AZURE_ACCOUNT`，那么这个样例会在 `init` 阶段立即终止，并在日志中报出 `azure adaptor requires either endpoint or account`。遇到这种情况，需要先：

```bash
export COSBENCH_AZURE_ACCOUNT=<your-storage-account>
./release/stop-all.sh
./release/start-all.sh
```

或者直接把账号写进 workload 的 storage 配置，例如：

```xml
<storage type="azure" config="auth_type=azure-cli;account=<your-storage-account>" />
```

#### 第五步：查看状态和结果

```bash
cd release
./cli.sh info
```

结果与日志位置：

- 启动日志：`release/log/`
- 运行期系统日志：`log/system.log`
- workload 归档结果：`archive/`
- Web 页面：`http://127.0.0.1:19088/controller`

#### 第六步：JDK 选择建议

- 如果你只是编译 Azure 插件、做最小化 bundle 校验，可以继续用 JDK 11。
- 当前这套仓库脚本已经可以在 JDK 11 和 JDK 17 下启动完整的 Driver/Controller 链路。
- 如果你要尽量贴近老版本的原始运行环境，或者准备做长期联调，JDK 8 仍然更稳妥。

当前主要剩余差异是旧版 Equinox 在较新的 JDK 上仍可能打印 reflective access 一类兼容性告警，而不是启动失败。

Storage key：

```xml
<storage type="azure" config="auth_type=storage-key;account=<account>;storagekey=<storage-key>" />
```

Service principal：

```xml
<storage type="azure" config="auth_type=service-principal;account=<account>;tenantid=<tenant-id>;clientid=<client-id>;clientsecret=<client-secret>" />
```

Managed identity：

```xml
<storage type="azure" config="auth_type=managed-identity;account=<account>" />
```

Azure CLI 缓存登录：

```xml
<storage type="azure" config="auth_type=azure-cli;account=<account>" />
```

## 11. 当前已验证范围

当前仓库已经验证过以下内容：

- Azure 适配器可以单独编译。
- Azure bundle 可以在精简 OSGi 运行时中达到 `ACTIVE`。
- Driver 所需插件集合可以从当前源码树导出并做齐全性检查。
- Controller 所需插件集合也可以从当前源码树导出。
- `./release/start-driver.sh` 可以在当前 JDK 11 和 JDK 17 环境下成功启动。
- `./release/start-controller.sh` 可以在当前 JDK 11 和 JDK 17 环境下成功启动。
- `./release/start-all.sh` 可以按顺序成功拉起 Driver 和 Controller，并在启动完成后正常返回。

当前仍需注意：

- 在 JDK 11 或 JDK 17 上仍可能出现旧版 Equinox 的 reflective access 兼容性告警。
- 如果要做长期稳定联调，Java 8 仍然是更保守的运行环境。

如果你的目标只是开发 Azure 适配器、编译插件、运行当前仓库里的 Driver/Controller 启动脚本，那么 JDK 11 或 JDK 17 都可以直接继续使用；如果你的目标是尽量贴近历史生产环境并减少兼容性告警，建议准备 Java 8 作为最终联调环境。

## 12. 推荐使用流程

```bash
# 1) 编译 Azure 插件
./dev/cosbench-azure/build-azure-adaptor.sh

# 2) 导出 Driver 所需插件
./dev/cosbench-azure/export-driver-plugin-set.sh

# 3) 导出 Controller 所需插件
./dev/cosbench-azure/export-controller-plugin-set.sh

# 4) 校验 Driver 插件集合
./dev/cosbench-azure/check-driver-plugin-set.sh

# 5) 启动 Driver / Controller
./release/start-driver.sh
./release/start-controller.sh

# 6) 提交 workload
./cli.sh submit workloads/asus.xml
```

如果要先验证 Azure 插件本身，而不是完整启动 Driver，可先执行：

```bash
./dev/cosbench-azure/validate-azure-bundle.sh
```