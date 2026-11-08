package com.mortylovelly.uniquerelics.mixin;

import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SmithingScreenHandler.class)
public interface SmithingScreenHandlerAccessor {
    @Accessor("world")
    World uniqueRelics$getWorld();
}
