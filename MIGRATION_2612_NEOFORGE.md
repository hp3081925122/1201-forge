# Cindermaw Boss 26.1.2 NeoForge 迁移记录

源分支 `1211neoforge`，基线提交 `195def5`。只改了独立工作树 `2612-neoforge`。1.20.1 与 1.21.1 工作树未动。Boss 技能、双阶段、动画、粒子、熔岩弹、地面特效、召唤物、锤、胸甲、配方和掉落都保留。

## 工具链

- Minecraft 26.1.2，NeoForge 26.1.2.95，ModDevGradle 2.0.140，Gradle 9.4，Java 25
- GeckoLib `curse.maven:geckolib-388172:7913641`（5.5.1，包名 `com.geckolib`）
- `neoforge.mods.toml` 中 GeckoLib 依赖下限改为 `[5.5.1,)`
- `compileJava` 与 `jar` 均已通过。产物：`build/libs/cindermaw-26.1.2-neoforge-1.0.0.jar`
- CurseForge 项目 1740723 已接受文件 [9145947](https://www.curseforge.com/minecraft/mc-mods/cindermaw/files/9145947)，显示名 `26.1.2-neoforge-1.0.0`。公开文件接口当时仍返回空，还不能当成已审核通过
- 开发客户端已进过主菜单和世界。召唤 Boss 后服务端和客户端各崩一次，原因和修法见下文。发光、骨骼和特效还没在游戏里看过

## 二阶段发光闪烁

1.20.1 的 `Phase2GlowLayer` 把脉动亮度作为顶点 RGB 传入重绘。1.21.1 和迁移前的 26.1.2 代码算出了同样的 `pulse`，却把颜色写成 `0xFFFFFFFF`，所以二阶段发光贴图一直全亮。

26.1.2 的修法在 `Phase2GlowLayer`：`pulse = strength * (0.55 + 0.45 * sin(time * 0.16))`，再压成 `0xAARRGGBB` 写入 `DataTickets.RENDER_COLOR`，然后用 `RenderTypes.eyes` 提交。`submitRenderTasks` 会在返回前记下这个颜色，提交后再把原来的颜色写回去。眼睛渲染是全亮的，闪烁来自顶点颜色。咆哮 26 刻内爬升、死亡按 `deathTime / 40` 淡出，和 1.20.1 相同。

## 渲染

- `GeoEntityRenderer<CindermawEntity, CindermawRenderState>`，层用 `withRenderLayer`
- 自定义状态必须实现 `GeoRenderState`。GeckoLib 的 mixin 只在运行时把接口加到 `EntityRenderState` 上，javac 看不见
- 状态自己保存 GeckoLib 数据表，并覆盖 `addGeckolibData` / `getGeckolibData` / `getOrDefaultGeckolibData`，避免和 mixin 字段各写各的
- 待机转头改到 `preRenderPass` 的 head bone updater。26.1 的 `LivingEntityRenderState.yRot` 已经是头相对身体的偏角，不能再减 `bodyRot`。攻击中和死亡时不转头
- 模型朝向读 `bodyRot`。自定义渲染状态必须在自己的数据表没有这项时，继续用 GeckoLib 的 `OverridingDataTicket` 取原版字段。漏掉时身体朝向恒为 0，模型和技能前后相反
- 手臂、炉心、喷口、头、身体在 GeckoLib 已经变换到骨骼枢轴的 per-bone 回调里采样，不再二次套矩阵。喷口火焰画在枢轴原点，单面 `entityCutout`，和 1.21.1 的 `entityCutoutNoCull` 一样
- 喷焰、横扫用插值后的实体坐标加上这个偏移。广告牌用镜头的 `leftVector` / `upVector`。顶点光照用 `setLight(0xF000F0)`，不再把打包光照塞进 `setUv2` 的第一个参数
- 26.1 的 `RenderTypes.eyes` 是半透明混合，1.21.1 的眼睛管线是叠加。顶点颜色已经按透明度预乘，地面光斑、警告圈、熔岩池和火柱改走从 `EYES` 复制的叠加管线 `cindermaw:pipeline/additive_glow`，不写深度。熔岩弹岩石仍用半透明发光
- 横扫、吐息、喷流仍在 `postRenderPass` 里用这一帧的骨骼位置画。吐息朝向使用本次渲染的 partial tick
- 熔岩弹和地面特效改为 `extractRenderState` + `submit`。顶点在延迟回调里使用捕获的 pose。广告牌用相机旋转的共轭求左右和上方向
- 粒子改为 `SingleQuadParticle`，`Layer.TRANSLUCENT`，火焰类粒子仍用满亮度

## 实体与物品

- `AnimationState` 改为 `AnimationTest`。控制器不再接收实体参数，速度用 `setControllerSpeed`
- GeckoLib 5 的 `isMoving()` 用 `walkAnimation.speed()` 对比 0.015。闲逛水平位移大约 0.01 时过不了，主控制器会一直停在待机。行走改看 `walkAnimation.speed() > 0.002`，再退回到水平位移平方大于 `2.5E-5`
- `hurt` 改为 `hurtOrSimulate`；Boss 咆哮无敌覆盖 `hurtServer`
- 存档改为 `ValueInput` / `ValueOutput`。`CompoundTag` 读取改为 `getIntOr` / `getFloatOr` / `getBooleanOr` / `getDoubleOr`，整数数组用 `Optional`
- 同步 NBT 使用 `SyncedTag`，codec 为 `ByteBufCodecs.TRUSTED_COMPOUND_TAG`，`copy()` 会复制标签。这个序列化器注册在 `NeoForgeRegistries.ENTITY_DATA_SERIALIZERS`，名字是 `cindermaw:compound_tag`。26.1 的原版 `EntityDataSerializers.registerSerializer` 拒绝模组序列化器；不注册时，熔岩弹和地面特效一生成就抛 `Unregistered serializer`
- 生成使用 `EntitySpawnReason.TRIGGERED`。实体类型 `build` 接收 `ResourceKey`
- 位置设置改为 `snapTo`。跳跃不再写已删除的 `hasImpulse`，速度仍由 `setDeltaMovement` 设置，立即同步可能比旧版弱
- `causeFallDamage` 的距离参数改为 `double`。客户端判断改为 `isClientSide()` / `FMLEnvironment.getDist()`
- 五个物品都走 `DeferredRegister.Items.registerItem`。普通 `register` 不会调用 `Properties.setId`，构造物品时会在 `effectiveDescriptionId` 空指针
- 熔渊锤是 `Item`，属性来自 `properties.sword(ToolMaterial.NETHERITE, 7, -3.2F)`。下界合金伤害加成为 4，总攻击加成为 11，攻速修正 -3.2，耐久仍走下界合金。游戏内提示为 12 点攻击、0.8 攻速。蓄力 16 刻，冷却 80，消耗 3 耐久，三道长 7、伤害 9 的裂缝保留
- 蓄力姿势返回 `ItemUseAnimation.TRIDENT`。26.1 的 `SPEAR` 是新的突刺，不再是旧版三叉戟后拉
- 右键返回 `InteractionResult.CONSUME`，松手返回 boolean。提示用 `TooltipDisplay` 和 `Consumer`
- 胸甲不再注册 `ArmorMaterial`。材质记录：耐久基数 37（胸甲 592，与下界合金相同）、胸甲防御 9、附魔 16、韧性 3.5、击退抗性 0.15。修理标签 `cindermaw:repairs_cindermaw_armor` 只含熔渊之心
- 26.1.2 不注册刷怪蛋。召唤仍用熔渊炉核，或命令 `/summon cindermaw:cindermaw`
- 空间不足时的提示改为 `sendOverlayMessage`
- 炉心胸甲的火焰免疫和 35% 近战反击仍在 `CindermawGearEvents`

## 数据

目录改为单数：`recipe/`、`advancement/`、`loot_table/`。物品标签本来就是单数。

- 配方结果改为 `{"id":"...","count":N}`，合成键和锻造模板/底材/添加物改为物品 ID 字符串
- 抢夺数量改为 `minecraft:enchanted_count_increase`
- 炉核掉落：玩家击杀，且 `random_chance_with_enchanted_bonus`。无抢夺 0.15；有抢夺时线性 `base` 0.20、`per_level_above_first` 0.05，对应旧的 0.15 + 0.05 × 抢夺等级
- 进度里的物品谓词由数组改为字符串
- 每个物品增加 `assets/cindermaw/items/<id>.json`
- 装备定义 `assets/cindermaw/equipment/cindermaw.json`，贴图复制到 `textures/entity/equipment/humanoid/cindermaw.png`

`pack.mcmeta` 不再使用 `pack_format` 34。资源包格式是 84.0，数据包格式是 101.1。模组包同时包含两边，因此写成 `min_format [84, 0]`、`max_format [101, 1]`。NeoForge 在两边都按这个范围判断兼容。这个范围没有在游戏里加载过。

## GeckoLib 5 资源路径

5.5.1 只扫描 `assets/<命名空间>/geckolib/models/` 和 `geckolib/animations/`。缓存键会去掉 `geckolib/models/`、`geckolib/animations/` 以及 `.geo.json`、`.animation.json`。模型和动画都放成 `cindermaw.geo.json`、`cindermaw.animation.json`，`CindermawModel` 返回的 ID 是 `cindermaw:cindermaw`。旧的 `geo/` 和 `animations/` 不会被加载。启动日志里 “Loaded 0 models and 0 animations” 就是这个原因。主控制器过渡长度是 4，动画全部解析失败时 `AnimationTimeline.create` 会对空列表调用 `getLast`，Boss 第一次渲染就崩。

## 尚未在游戏中确认的点

- 二阶段发光是否按 1.20.1 的节奏闪
- 骨骼采样和喷口火焰是否对齐
- 配方、抢夺掉落、胸甲贴图是否被数据包接受
- 跳跃速度在没有 `hasImpulse` 时的同步
