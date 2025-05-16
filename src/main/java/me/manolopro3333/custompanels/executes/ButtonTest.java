package me.manolopro3333.custompanels.executes;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;


@SuppressWarnings("removal")
public class ButtonTest {
    public static void execute(LevelAccessor world, String efecto) {
        for (Entity entity : new ArrayList<>(world.players())) {
            if (entity instanceof LivingEntity living) {
                MobEffect effectToRemove = BuiltInRegistries.MOB_EFFECT.get(new ResourceLocation(efecto));
                if (effectToRemove != null) {
                    living.removeEffect(effectToRemove);
                }
            }
        }
    }
}