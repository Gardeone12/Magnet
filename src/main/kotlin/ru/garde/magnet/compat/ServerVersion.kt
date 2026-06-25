// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Garde1 / Gardeone12

package ru.garde.magnet.compat

import org.bukkit.Bukkit

internal data class MinecraftVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<MinecraftVersion> {
    override fun compareTo(other: MinecraftVersion): Int {
        return compareValuesBy(this, other, MinecraftVersion::major, MinecraftVersion::minor, MinecraftVersion::patch)
    }

    override fun toString(): String = "$major.$minor.$patch"
}

internal object ServerVersion {
    val minimumSupported = MinecraftVersion(1, 16, 5)

    fun current(): MinecraftVersion? = parse(Bukkit.getBukkitVersion())

    fun parse(raw: String): MinecraftVersion? {
        val match = VERSION_PATTERN.find(raw) ?: return null
        return MinecraftVersion(
            major = match.groupValues[1].toIntOrNull() ?: return null,
            minor = match.groupValues[2].toIntOrNull() ?: return null,
            patch = match.groupValues[3].toIntOrNull() ?: 0
        )
    }

    private val VERSION_PATTERN = Regex("""(\d+)\.(\d+)(?:\.(\d+))?""")
}
