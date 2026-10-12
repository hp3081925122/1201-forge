# Cindermaw Boss：Minecraft 1.21.1 NeoForge 迁移文档

## 1. 迁移目标

- 源项目：Minecraft 1.20.1 Forge 47.4.23。
- 目标项目：Minecraft 1.21.1 NeoForge 21.1.x，Java 21。
- 工作分支：`1211neoforge`。
- 工作树：`E:\模组\发cf的模组\生物\地狱猩猩boss\1211-neoforge`。
- 模组 ID：`cindermaw`。
- 显示名称：`Cindermaw Boss`。
- 迁移原则：保留现有注册 ID、实体行为、技能、装备、粒子、GeckoLib 模型/动画和资源引用；只改加载器、目标版本及目标 API 必需的调用方式。

## 2. 已确认的源项目内容

- 入口与注册：`Cindermaw.java`、`ModEntities.java`、`ModParticles.java`。
- 实体：`CindermawEntity`、`LavaBomb`、`MagmaEffect`。
- 客户端：实体/特效渲染器、粒子、客户端事件、GeckoLib 模型与动画。
- 物品：炉心、熔渊之心、熔渊锤、炉心胸甲及装备事件。
- 数据资源：配方、战利品表、配方进度、语言文件、模型、纹理和粒子 JSON。
- 当前源代码中仍存在 Forge 包引用，不能只替换 Gradle 配置后直接视为迁移完成。

## 3. 构建与元数据迁移

1. 以 `E:\模组\模组模板\1211-neoforge` 的 NeoForge UserDev 配置为基线。
2. 设置 Minecraft `1.21.1`、NeoForge `21.1.x`、Java `21`，保留代理配置和 UTF-8 编译设置。
3. 将 `META-INF/mods.toml` 替换为 `META-INF/neoforge.mods.toml`，版本范围遵循项目规则：最低版本 `0.0.1`，不写最高版本限制。
4. 更新 `pack.mcmeta` 到 1.21.1 对应的资源包格式。
5. 依赖重新核对 NeoForge 1.21.1 对应的 GeckoLib 版本和 CurseMaven 坐标；不能沿用只适用于 Forge 1.20.1 的文件编号。

## 4. Java API 迁移清单

- `net.minecraftforge.*` 注册、事件、Dist 和客户端事件包全部替换为 NeoForge 对应包。
- `DeferredRegister`、`RegistryObject`、实体/物品/粒子注册改为 NeoForge 1.21.1 推荐写法，并确认注册事件总线获取方式。
- `ForgeSpawnEggItem`、属性事件、创造模式标签事件改为 NeoForge 类型。
- `DistExecutor`、客户端事件订阅和渲染器注册改为 NeoForge 事件 API。
- 逐个核对实体构造器、属性、伤害、物品使用、装备、粒子和渲染相关签名；不使用未经当前依赖源码确认的方法。
- 检查 1.21.1 的物品组件、附魔标签、数据组件和实体数据变化；只在源代码实际使用处迁移。
- GeckoLib API 以目标版本依赖源码为准，保留当前动画控制器和实体实例缓存语义。

## 5. 资源与数据迁移清单

- 保留 `assets/cindermaw` 下模型、动画、纹理、语言和粒子资源路径。
- 检查 `pack.mcmeta`、配方、战利品表和进度 JSON 的 1.21.1 格式。
- 检查数据包目录是否需要从复数目录改为单数目录；仅按 1.21.1 实际格式修改，不改变注册 ID。
- 对所有 JSON 执行 UTF-8、语法和资源引用检查。
- 检查动画文件中使用的动画名必须覆盖 Java 控制器引用；缺失资源直接视为阻断问题。

## 6. 实施顺序

1. 完成本文件并冻结迁移范围。
2. 用模板替换 Gradle、NeoForge 元数据和基础目录。
3. 搬运当前源代码与资源，逐文件完成 Forge → NeoForge API 迁移。
4. 先运行 `processResources` 和 `compileJava`，修复编译/API 错误。
5. 再运行 `build`，检查产物内容、模组元数据、资源和数据文件。
6. 不自动启动 `runClient`；只有明确授权客户端测试时才启动。
7. 记录构建结果、剩余风险和需要用户实际测试的行为。

## 7. 验收门槛

- `processResources` 成功，且只生成 `neoforge.mods.toml`。
- `compileJava` 成功，无 Forge 包残留引用。
- `build` 成功，JAR 中包含实体、物品、动画、模型、纹理、粒子和数据资源。
- 静态检查确认动画资源存在、动画名称匹配、所有 JSON 可解析。
- 运行时行为、渲染效果、技能时序和存档兼容性作为独立客户端测试项报告，不以编译成功代替。

## 8. 主要风险

- NeoForge 1.21.1 与 Forge 1.20.1 的事件包、注册器和实体/物品 API 差异较大，批量替换可能造成隐性行为变化。
- Java 21 和 NeoForge UserDev 会暴露旧版泛型、构造器或客户端隔离问题。
- GeckoLib 目标版本若与当前动画控制器 API 不匹配，需要基于目标依赖源码调整，不能降级功能。
- 资源格式迁移错误可能在编译后才表现为加载失败，因此必须单独做 JSON 和 JAR 内容检查。
