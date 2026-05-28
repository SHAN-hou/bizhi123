# 粒子动态壁纸 (Particle Live Wallpaper)

一个基于 Android 的粒子流动动态壁纸应用，使用 Kotlin 编写。

## 功能特性

- **粒子动画**：流畅的粒子漂浮效果，带有光晕渲染
- **连接线**：距离较近的粒子之间会自动绘制半透明连接线，呈现科技感网络效果
- **触摸互动**：触摸屏幕时粒子会向触摸点聚集
- **多种配色**：科技蓝、极光绿、梦幻紫、烈焰红、星空金
- **可调参数**：粒子数量、运动速度、是否显示连接线、是否开启触摸互动

## 项目结构

```
app/src/main/
├── java/com/example/livewallpaper/
│   ├── ParticleWallpaperService.kt   # 核心壁纸服务（粒子引擎）
│   └── SettingsActivity.kt           # 壁纸设置页面
├── res/
│   ├── xml/
│   │   ├── wallpaper.xml             # 壁纸声明
│   │   └── wallpaper_preferences.xml # 设置项定义
│   └── values/
│       ├── strings.xml
│       ├── arrays.xml
│       └── colors.xml
└── AndroidManifest.xml
```

## 构建与安装

### 前提条件
- Android Studio (Arctic Fox 或更高版本)
- Android SDK 34
- Kotlin 1.9+

### 步骤
1. 用 Android Studio 打开项目文件夹 `AndroidLiveWallpaper`
2. 等待 Gradle 同步完成
3. 连接 Android 设备或启动模拟器
4. 点击 Run 按钮构建并安装

### 设置动态壁纸
1. 长按桌面空白处 → 选择「壁纸」
2. 找到「粒子动态壁纸」
3. 点击「设置壁纸」
4. 点击齿轮图标可自定义粒子参数

## 核心原理

Android 动态壁纸的核心是 `WallpaperService`：

1. **继承 `WallpaperService`**，重写 `onCreateEngine()` 返回自定义 Engine
2. **Engine 内部** 通过 `SurfaceHolder.lockCanvas()` 获取画布，绘制每一帧
3. **使用 `Handler.postDelayed()`** 实现 ~60fps 的绘制循环
4. **在 `AndroidManifest.xml`** 中声明 Service，并添加 `BIND_WALLPAPER` 权限
5. **通过 `wallpaper.xml`** 元数据关联设置页面

## 自定义扩展建议

- 替换粒子为图片（星星、雪花等）
- 添加陀螺仪感应，让粒子随手机倾斜方向流动
- 添加昼夜模式，根据时间自动切换配色
- 使用 OpenGL ES 替代 Canvas 以获得更好的性能
