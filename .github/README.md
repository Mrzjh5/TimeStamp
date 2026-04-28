# 时光印记 - GitHub Actions 自动构建说明

## 🚀 功能说明

每次你 push 代码到 `main` 分支，或创建 Pull Request，GitHub Actions 会自动：

1. **编译 Debug APK** - 用于测试
2. **编译 Release APK** - 用于发布
3. **自动上传构建产物** - 可直接下载 APK

## 📥 下载 APK

### 方法一：Actions 页面下载

1. 进入你的 GitHub 仓库
2. 点击 **Actions** 标签页
3. 选择最新的 workflow 运行
4. 在 **Artifacts** 部分下载 APKs

### 方法二：直接下载（配置后）

Workflow 运行完成后，APKs 会以 Artifact 形式保存：
- `debug-apk` - 测试版
- `release-apk` - 发布版

## 🔧 使用步骤

### 1. 创建 GitHub 仓库

```bash
# 在 GitHub 上创建新仓库，然后：
git init
git add .
git commit -m "Initial commit: 时光印记打卡组件"
git branch -M main
git remote add origin https://github.com/你的用户名/TimeStamp.git
git push -u origin main
```

### 2. 上传代码

```bash
git push origin main
```

### 3. 查看构建结果

1. 打开 GitHub 仓库
2. 点击 **Actions** 标签
3. 等待构建完成（通常 3-5 分钟）
4. 点击构建任务 → Artifacts 下载 APK

### 4. 安装到手机

1. 下载 APK 到手机
2. 允许"安装未知来源应用"
3. 安装 APK
4. 添加小组件到桌面

## ⚙️ 自定义配置

如需修改构建配置，编辑 `.github/workflows/android.yml`：

```yaml
env:
  JAVA_VERSION: '17'           # Java 版本
  ANDROID_COMPILE_SDK: '35'    # 编译 SDK 版本
  ANDROID_BUILD_TOOLS: '35.0.0' # 构建工具版本
```

## 🔒 发布到应用商店（可选）

如需发布 Release 版本，需要：

1. 在 GitHub Secrets 中添加签名配置：
   - `KEYSTORE_FILE` - 签名文件 (base64)
   - `KEYSTORE_PASSWORD` - 密钥库密码
   - `KEY_ALIAS` - 密钥别名
   - `KEY_PASSWORD` - 密钥密码

2. 修改 workflow 添加签名配置

---

## ❓ 常见问题

**Q: 构建失败了怎么办？**
A: 点击 Actions → 失败的 workflow → 查看日志定位问题

**Q: 能构建小米应用商店版本吗？**
A: 需要配置360加固或自行添加签名信息

**Q: 如何添加其他开发者？**
A: GitHub 仓库 → Settings → Collaborators
