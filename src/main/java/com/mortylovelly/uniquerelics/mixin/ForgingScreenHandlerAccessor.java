package com.mortylovelly.uniquerelics.mixin;

import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ForgingScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the vanilla smithing inputs/result inventory without depending on
 * screen slot numbers, which can be affected by handler layout changes.
 */
@Mixin(ForgingScreenHandler.class)
public interface ForgingScreenHandlerAccessor {
    @Accessor("input")
    Inventory uniqueRelics$getInput();

    @Accessor("output")
    CraftingResultInventory uniqueRelics$getOutput();
}
