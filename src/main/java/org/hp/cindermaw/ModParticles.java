package org.hp.cindermaw;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// 自绘像素粒子：火星、烟团、火焰、熔岩碎块与闪光，全部忽略粒子距离限制。
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Cindermaw.MOD_ID);

    public static final RegistryObject<SimpleParticleType> EMBER = PARTICLES.register("ember", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> CINDER_SMOKE = PARTICLES.register("cinder_smoke", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> BLAZE_FLAME = PARTICLES.register("blaze_flame", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> MAGMA_CHUNK = PARTICLES.register("magma_chunk", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> HEAT_FLASH = PARTICLES.register("heat_flash", () -> new SimpleParticleType(true));

    private ModParticles() {
    }
}
