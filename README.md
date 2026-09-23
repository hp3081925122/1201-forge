# 熔渊灾厄（Cindermaw Boss）

这是 Minecraft 1.20.1 Forge 47.4.23 的独立工程，使用 Java 17 和 GeckoLib 4.8.2。当前已接入熔渊炉心的 GeckoLib 几何模型、基础贴图、发光遮罩、骨骼层级、实体注册和刷怪蛋。

开发世界中可以使用 `/summon cindermaw:cindermaw` 召唤实体，或从创造模式的生成蛋页取出“熔渊炉心刷怪蛋”。当前模型资源没有动作动画，战斗技能和自然生成将在后续阶段加入。

```powershell
$env:JAVA_HOME = 'C:\Users\30819\.jdks\ms-17.0.18'
.\gradlew.bat build
.\gradlew.bat runClient
```
