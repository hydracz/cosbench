# Azure Blob 分布式手动运行指南

本文档面向当前仓库这套 Azure Blob 自定义适配器和当前 Azure VMSS 环境，描述如何完成 controller/driver 的一次性环境准备，以及在环境正常后只做状态检查和手工提交新 workload。

适用前提：

- controller 与 driver 节点都能访问同一个 Git 仓库版本。
- driver 节点已经具备 Azure Blob Data Contributor 等权限，能够用托管身份访问目标存储账号。
- 当前环境对应的 Azure 资源名称仍是：
  - resource group: `rg-vivo-cosbench-eu`
  - controller VMSS: `vmss-vivo-cosbench-ctl-eu`
  - driver VMSS: `vmss-vivo-cosbench-drv-eu`
  - benchmark storage account: `vivocbblobbench001`

  当前推荐流程：

  1. 用 `azure-blob-cosbench-test/10-run-local.sh prepare <env>` 完成 controller + driver 的环境准备。
  2. 后续用 `azure-blob-cosbench-test/10-run-local.sh status <env>` 做健康检查。
  3. 状态正常后，用 `azure-blob-cosbench-test/10-run-local.sh submit --template <case> <env>` 把新的 workload 交给 controller 提交。

  只有在你需要排查底层服务时，才需要回到本文档后面的 controller/driver 手工命令。

## 1. 在所有节点准备源码和插件

controller 节点：

```bash
git clone https://github.com/hydracz/cosbench.git /opt/cosbench-src
cd /opt/cosbench-src
git checkout master
./dev/cosbench-azure/export-controller-plugin-set.sh
```

driver 节点：

```bash
git clone https://github.com/hydracz/cosbench.git /opt/cosbench-src
cd /opt/cosbench-src
git checkout master
./dev/cosbench-azure/export-driver-plugin-set.sh
```

如果只是更新到最新提交，可以直接：

```bash
cd /opt/cosbench-src
git fetch origin
git checkout master
git pull --ff-only origin master
```

说明：

- `export-controller-plugin-set.sh` 和 `export-driver-plugin-set.sh` 会为 config.ini 中引用的 OSGi bundle 生成无版本 alias，避免 `plugins/cosbench-foo not found`。
- 当前运行时建议显式安装 JDK 11 开发包，保证 `jar` 命令存在。

## 2. 配置文件

### 2.1 Driver 配置

driver 默认配置文件是 `release/conf/driver.conf`，当前手工运行保持最小配置即可：

```ini
[driver]
log_level = INFO
```

### 2.2 Controller 配置

controller 配置文件是 `release/conf/controller.conf`。当前 Azure 分布式环境建议使用绝对 archive 路径：

```ini
[controller]
name = controller
url = http://127.0.0.1:19088/controller
drivers = 16
concurrency = 1
log_level = INFO
log_file = log/system.log
archive_dir = /opt/cosbench-src/archive
```

当前环境的 driver 地址建议直接从 Azure CLI 生成，而不是手填。下面这段命令可以在 controller 节点直接生成完整 `controller.conf`：

```bash
resource_group=rg-vivo-cosbench-eu
driver_vmss=vmss-vivo-cosbench-drv-eu
controller_conf=/opt/cosbench-src/release/conf/controller.conf

mapfile -t driver_ips < <(
  az vmss nic list \
    -g "$resource_group" \
    --vmss-name "$driver_vmss" \
    --query '[].ipConfigurations[0].privateIPAddress' \
    -o tsv | awk 'NF'
)

{
  echo '[controller]'
  echo 'name = controller'
  echo 'url = http://127.0.0.1:19088/controller'
  echo "drivers = ${#driver_ips[@]}"
  echo 'concurrency = 1'
  echo 'log_level = INFO'
  echo 'log_file = log/system.log'
  echo 'archive_dir = /opt/cosbench-src/archive'
  echo
  idx=1
  for ip in "${driver_ips[@]}"; do
    echo "[driver${idx}]"
    echo "name = driver${idx}"
    echo "url = http://${ip}:18088/driver"
    echo
    idx=$((idx + 1))
  done
} > "$controller_conf"
```

### 2.3 Workload 配置

当前推荐的 6 个手工 case 位于 `azure-blob-cosbench-test/templates/`：

- `01-4k-read-100.xml`
- `02-4k-write-100.xml`
- `03-256k-read-100.xml`
- `04-256k-write-100.xml`
- `05-1m-read-100.xml`
- `06-1m-write-100.xml`

这些模板都默认使用：

```xml
<storage type="azure" config="auth_type=managed-identity" />
```

提交前请把 `__CPREFIX__` 替换成唯一前缀，避免多次运行命中同一批容器和对象。`10-run-local.sh submit` 会自动完成这个替换并把模板上传给 controller。

## 3. 启动 Driver 和 Controller

### 3.1 启动所有 driver

每台 driver 节点执行：

```bash
cd /opt/cosbench-src
./release/stop-driver.sh || true
./release/start-driver.sh
```

检查 driver Web 端口与页面：

```bash
ss -ltnp | grep 18088
curl -fsS http://127.0.0.1:18088/driver/index.html | head
```

### 3.2 启动 controller

controller 节点执行：

```bash
cd /opt/cosbench-src
./release/stop-controller.sh || true
./release/start-controller.sh
```

检查 controller：

```bash
ss -ltnp | grep 19088
./release/cli.sh info anonymous:cosbench@127.0.0.1:19088
```

如果 `cli.sh info` 能列出所有 driver，就说明 controller 已经能够访问 driver。

## 4. 推荐提交流程

当前推荐直接在本地工作目录执行 submit，由脚本先做健康检查，再把模板上传给 controller 提交：

```bash
cd azure-blob-cosbench-test
bash 10-run-local.sh status cz-dev
bash 10-run-local.sh submit --template 01-4k-read-100 cz-dev
```

其余 5 个 case 依次替换为：

- `02-4k-write-100`
- `03-256k-read-100`
- `04-256k-write-100`
- `05-1m-read-100`
- `06-1m-write-100`

成功时本地脚本会返回：

```text
workload submitted: wN
view from windows client: http://<controller-private-ip>:19088/controller/workload.html?id=wN
```

## 5. 如需在 controller 本机直接提交

如果你明确需要在 controller 节点直接执行 COSBench CLI，可以沿用同样的 XML 内容，先把目标模板复制到 controller，再执行：

```bash
cd /opt/cosbench-src
export COSBENCH_AZURE_ACCOUNT=vivocbblobbench001

run_tag=azmanual001
sed "s/__CPREFIX__/$run_tag/g" /path/to/01-4k-read-100.xml > /tmp/${run_tag}.xml

./submit-azure-workload.sh \
  --auth-type managed-identity \
  --account "$COSBENCH_AZURE_ACCOUNT" \
  --template /tmp/${run_tag}.xml \
  --controller anonymous:cosbench@127.0.0.1:19088
```

成功时会返回：

```text
Accepted with ID: wN
```

## 6. 查看运行状态

查看全部 active workload：

```bash
cd /opt/cosbench-src
./release/cli.sh info anonymous:cosbench@127.0.0.1:19088
```

查看指定 workload 状态，例如 `w1`：

```bash
curl -fsS "http://127.0.0.1:19088/controller/cli/workload.action?id=w1&username=anonymous&password=cosbench"
```

返回形如：

```text
w1    Wed Apr 08 06:41:53 CST 2026    FINISHED
```

## 7. 查看日志与归档

### 7.1 Controller 侧

- 启动日志：`release/log/controller-boot.log`
- 运行时系统日志：`release/log/system.log`
- workload 汇总：`archive/run-history.csv`
- workload 指标总表：`archive/workloads.csv`
- 单次 workload 归档目录：`archive/wN-<workload-name>/`
- 单次 workload 详细日志：`archive/wN-<workload-name>/workload.log`
- 脚本输出汇总：`archive/wN-<workload-name>/scripts.log`

常用命令：

```bash
tail -n 200 /opt/cosbench-src/release/log/controller-boot.log
tail -n 200 /opt/cosbench-src/release/log/system.log
tail -n 50 /opt/cosbench-src/archive/run-history.csv
ls -la /opt/cosbench-src/archive
```

### 7.2 Driver 侧

- 启动日志：`release/log/driver-boot.log`
- 如需看 OSGi 元数据日志：`release/workspace/.metadata/.log`

常用命令：

```bash
tail -n 200 /opt/cosbench-src/release/log/driver-boot.log
tail -n 200 /opt/cosbench-src/release/workspace/.metadata/.log
```

### 7.3 结果文件怎么读

- 带宽测试关注 `workloads.csv` 或单 workload CSV 中 `Bandwidth` 列。
- IOPS 测试关注 `Throughput` 列，4KB 场景下它基本就是 IOPS。
- 延迟分布可以看 `*-rt-histogram.csv`。

## 8. 常见问题

### 8.1 controller 看不到 driver

先检查端口：

```bash
curl -fsS http://<driver-private-ip>:18088/driver/index.html | head
```

如果报 `No route to host`，通常是 firewalld 没放行。当前环境必须放行：

- driver: `18088/tcp`, `18089/tcp`
- controller: `19088/tcp`, `19089/tcp`

### 8.2 controller 启动时报 bundle not found

说明 `dist/osgi/plugins/` 下只有版本化 jar，没有 `plugins/cosbench-foo` 这种 alias。重新执行：

```bash
./dev/cosbench-azure/export-controller-plugin-set.sh
./dev/cosbench-azure/export-driver-plugin-set.sh
```

### 8.3 workload 已 finished，但脚本找不到 archive

当前 controller 是在 `/opt/cosbench-src` 目录启动的，因此归档目录应看：

```bash
/opt/cosbench-src/archive
```

不要误看成 `/opt/cosbench-src/release/archive`。