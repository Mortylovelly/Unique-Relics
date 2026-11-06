package com.mortylovelly.uniquerelics;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entrypoint for Unique Relics.
 *
 * The repository currently contains project scaffolding only.
 * Gameplay systems will be added in later development steps.
 */
public final class UniqueRelics implements ModInitializer {
    public static final String MOD_ID = "unique_relics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Unique Relics initialized. Gameplay features are not implemented yet.");
    }
}
