// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Garde1 / Gardeone12

package ru.garde.magnet.compat

import org.bukkit.Bukkit

internal data class MagnetCompatibility(
    val bukkitVersion: String,
    val minecraftVersion: MinecraftVersion?,
    val itemModelApi: Boolean,
    val customModelDataComponentApi: Boolean,
    val legacyCustomModelDataApi: Boolean
) {
    fun summary(): String {
        return "Compatibility: server=$bukkitVersion, minecraft=$minecraftVersion, " +
            "itemModelApi=$itemModelApi, customModelDataComponentApi=$customModelDataComponentApi, " +
            "legacyCustomModelDataApi=$legacyCustomModelDataApi"
    }

    companion object {
        fun detect(itemModelCompatibility: ItemModelCompatibility): MagnetCompatibility {
            return MagnetCompatibility(
                bukkitVersion = Bukkit.getBukkitVersion(),
                minecraftVersion = ServerVersion.current(),
                itemModelApi = itemModelCompatibility.hasItemModelApi,
                customModelDataComponentApi = itemModelCompatibility.hasCustomModelDataComponentApi,
                legacyCustomModelDataApi = itemModelCompatibility.hasLegacyCustomModelDataApi
            )
        }
    }
}
