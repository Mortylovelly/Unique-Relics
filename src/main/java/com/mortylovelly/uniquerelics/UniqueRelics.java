package com.mortylovelly.uniquerelics;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UniqueRelics implements ModInitializer {
    public static final String MOD_ID = "unique_relics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Unique Relics initialized: smithing trim relic metadata is enabled.");
    }
}
