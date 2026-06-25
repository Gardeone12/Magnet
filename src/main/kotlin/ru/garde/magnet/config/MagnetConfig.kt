// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Garde1 / Gardeone12

package ru.garde.magnet.config

import org.bukkit.plugin.java.JavaPlugin
import ru.garde.magnet.portable.MagnetItemFactory
import java.util.Locale

internal class MagnetConfig(
    private val plugin: JavaPlugin
) {
    fun resourcePack(): ResourcePackConfig {
        val invalidSection = plugin.config.contains(RESOURCE_PACK_CONFIG_SECTION) &&
            plugin.config.getConfigurationSection(RESOURCE_PACK_CONFIG_SECTION) == null
        val section = plugin.config.getConfigurationSection(RESOURCE_PACK_CONFIG_SECTION)

        return ResourcePackConfig(
            invalidSection = invalidSection,
            enabled = section?.getBoolean("enabled", false) ?: false,
            mode = section?.getString("mode", "external")
                ?.trim()
                ?.lowercase(Locale.ROOT)
                .orEmpty(),
            url = section?.getString("url")
                ?.trim()
                .orEmpty(),
            file = section?.getString("file")
                ?.trim()
                .orEmpty(),
            modelKey = section?.getString("model-key", MagnetItemFactory.EXPECTED_PORTABLE_MAGNET_MODEL_KEY)
                ?.trim()
                .orEmpty()
        )
    }

    companion object {
        private const val RESOURCE_PACK_CONFIG_SECTION = "resource-pack"
    }
}

internal data class ResourcePackConfig(
    val invalidSection: Boolean,
    val enabled: Boolean,
    val mode: String,
    val url: String,
    val file: String,
    val modelKey: String
)
