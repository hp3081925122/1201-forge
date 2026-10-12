package org.hp.cindermaw.entity;

// 技能时间线：服务端结算、动画与客户端特效共用同一套刻数。
public enum CindermawAttack {
    NONE(0, "", 0, 0, 0, 0, 0, false),
    // 双拳砸地：第 16 刻命中，扇形熔岩裂缝向前蔓延
    SLAM(1, "slam", 32, 0.0, 5.5, 70, 10, false),
    // 左右两连横扫：第 11、24 刻命中
    SWEEP(2, "sweep", 40, 0.0, 5.0, 60, 10, false),
    // 炉心喷焰：第 14 至 50 刻持续喷射
    BREATH(3, "breath", 64, 0.0, 9.0, 150, 7, false),
    // 火山齐射：第 12、24、36 刻各发射一轮熔岩弹
    BARRAGE(4, "barrage", 60, 6.0, 30.0, 170, 8, false),
    // 陨落重踏：第 12 刻起跳，落地后切换到 LAND
    LEAP(5, "leap", 90, 7.0, 20.0, 220, 6, false),
    // 落地硬直
    LAND(6, "leap_land", 24, 0, 0, 0, 0, false),
    // 熔核新星：第 22 刻释放贴地火墙
    NOVA(7, "nova", 48, 0.0, 16.0, 260, 7, true),
    // 转阶段咆哮：第 26 刻热浪推开周围目标
    ROAR(8, "roar", 50, 0, 0, 0, 0, false);

    public final int id;
    public final String anim;
    public final int duration;
    public final double minRange;
    public final double maxRange;
    public final int cooldown;
    public final int weight;
    public final boolean phase2Only;

    CindermawAttack(int id, String anim, int duration, double minRange, double maxRange, int cooldown, int weight, boolean phase2Only) {
        this.id = id;
        this.anim = anim;
        this.duration = duration;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.cooldown = cooldown;
        this.weight = weight;
        this.phase2Only = phase2Only;
    }

    // 按同步编号还原技能，未知编号视为空闲
    public static CindermawAttack byId(int id) {
        for (CindermawAttack attack : values()) {
            if (attack.id == id) {
                return attack;
            }
        }
        return NONE;
    }
}
