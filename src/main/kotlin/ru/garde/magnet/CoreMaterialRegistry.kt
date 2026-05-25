package ru.garde.magnet

import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import java.util.logging.Logger

class CoreMaterialRegistry(
    private val logger: Logger
) {
    companion object {
        private const val CONFIG_SECTION = "core-materials"

        val defaultProfiles = listOf(
            DefaultCoreMaterialProfile("COPPER_BLOCK", "copper", 16.0, 0.28, 10),
            DefaultCoreMaterialProfile("EXPOSED_COPPER", "copper", 15.0, 0.26, 10),
            DefaultCoreMaterialProfile("WEATHERED_COPPER", "copper", 14.0, 0.24, 10),
            DefaultCoreMaterialProfile("OXIDIZED_COPPER", "copper", 13.0, 0.22, 10),
            DefaultCoreMaterialProfile("WAXED_COPPER_BLOCK", "copper", 16.0, 0.28, 10),
            DefaultCoreMaterialProfile("WAXED_EXPOSED_COPPER", "copper", 15.0, 0.26, 10),
            DefaultCoreMaterialProfile("WAXED_WEATHERED_COPPER", "copper", 14.0, 0.24, 10),
            DefaultCoreMaterialProfile("WAXED_OXIDIZED_COPPER", "copper", 13.0, 0.22, 10),
            DefaultCoreMaterialProfile("RAW_COPPER_BLOCK", "raw_copper", 18.0, 0.30, 12),
            DefaultCoreMaterialProfile("CUT_COPPER", "tuned_copper", 17.0, 0.29, 12),
            DefaultCoreMaterialProfile("WAXED_CUT_COPPER", "tuned_copper", 17.0, 0.29, 12),
            DefaultCoreMaterialProfile("IRON_BLOCK", "iron", 20.0, 0.36, 20),
            DefaultCoreMaterialProfile("RAW_IRON_BLOCK", "raw_iron", 21.0, 0.38, 22),
            DefaultCoreMaterialProfile("ANVIL", "dense_iron", 18.0, 0.42, 23),
            DefaultCoreMaterialProfile("CHIPPED_ANVIL", "dense_iron", 17.0, 0.38, 22),
            DefaultCoreMaterialProfile("DAMAGED_ANVIL", "dense_iron", 16.0, 0.34, 21),
            DefaultCoreMaterialProfile("GOLD_BLOCK", "gold", 24.0, 0.24, 18),
            DefaultCoreMaterialProfile("RAW_GOLD_BLOCK", "raw_gold", 25.0, 0.25, 19),
            DefaultCoreMaterialProfile("REDSTONE_BLOCK", "redstone", 26.0, 0.32, 25),
            DefaultCoreMaterialProfile("LAPIS_BLOCK", "lapis", 22.0, 0.30, 16),
            DefaultCoreMaterialProfile("DIAMOND_BLOCK", "diamond", 28.0, 0.44, 35),
            DefaultCoreMaterialProfile("EMERALD_BLOCK", "emerald", 27.0, 0.40, 34),
            DefaultCoreMaterialProfile("AMETHYST_BLOCK", "amethyst", 30.0, 0.34, 30),
            DefaultCoreMaterialProfile("PRISMARINE", "prismarine", 23.0, 0.31, 17),
            DefaultCoreMaterialProfile("DARK_PRISMARINE", "dark_prismarine", 25.0, 0.35, 20),
            DefaultCoreMaterialProfile("OBSIDIAN", "obsidian", 18.0, 0.50, 32),
            DefaultCoreMaterialProfile("CRYING_OBSIDIAN", "unstable_obsidian", 24.0, 0.52, 33),
            DefaultCoreMaterialProfile("NETHERITE_BLOCK", "netherite", 32.0, 0.60, 50),
            DefaultCoreMaterialProfile("LODESTONE", "lodestone", 34.0, 0.64, 55),
            DefaultCoreMaterialProfile("BEACON", "beacon", 36.0, 0.58, 52),
            DefaultCoreMaterialProfile("RESPAWN_ANCHOR", "unstable_anchor", 38.0, 0.62, 53)
        )
    }

    private val profilesByMaterial = linkedMapOf<Material, CoreMaterialProfile>()

    fun load(config: FileConfiguration) {
        writeMissingDefaultProfiles(config)

        val section = config.getConfigurationSection(CONFIG_SECTION)
        profilesByMaterial.clear()

        if (section != null) {
            for (materialName in section.getKeys(false)) {
                val material = Material.matchMaterial(materialName)
                if (material == null || !material.isBlock || material == Material.AIR) {
                    logger.warning("Ignoring invalid core material '$materialName'.")
                    continue
                }

                val path = "$CONFIG_SECTION.$materialName"
                val profile = config.getString("$path.profile")?.trim().orEmpty()
                val baseRadius = config.getDouble("$path.base-radius", Double.NaN)
                val baseStrength = config.getDouble("$path.base-strength", Double.NaN)
                val priority = config.getInt("$path.priority", 0)

                if (profile.isEmpty()) {
                    logger.warning("Ignoring core material '$materialName' because profile is empty.")
                    continue
                }
                if (!baseRadius.isFinite() || baseRadius <= 0.0) {
                    logger.warning("Ignoring core material '$materialName' because base-radius is invalid.")
                    continue
                }
                if (!baseStrength.isFinite() || baseStrength <= 0.0) {
                    logger.warning("Ignoring core material '$materialName' because base-strength is invalid.")
                    continue
                }

                profilesByMaterial[material] = CoreMaterialProfile(
                    material = material,
                    profile = profile,
                    baseRadius = baseRadius,
                    baseStrength = baseStrength,
                    priority = priority
                )
            }
        }

        if (profilesByMaterial.isEmpty()) {
            logger.warning("No valid core-materials configured. Falling back to built-in core materials.")
            for (profile in defaultProfiles) {
                val material = Material.matchMaterial(profile.materialName)
                if (material == null || !material.isBlock || material == Material.AIR) {
                    logger.warning("Ignoring unavailable built-in core material '${profile.materialName}'.")
                    continue
                }

                profilesByMaterial[material] = profile.toProfile(material)
            }
        }
    }

    fun isCoreMaterial(material: Material): Boolean {
        return material in profilesByMaterial
    }

    fun profileFor(material: Material): CoreMaterialProfile? {
        return profilesByMaterial[material]
    }

    fun profileForMaterialName(materialName: String): CoreMaterialProfile? {
        val material = Material.matchMaterial(materialName) ?: return null
        return profileFor(material)
    }

    fun allProfiles(): List<CoreMaterialProfile> {
        return profilesByMaterial.values.sortedBy { it.material.name }
    }

    fun calculateStats(materials: List<Material>): CoreStats? {
        if (materials.size != 8) return null

        val profiles = materials.map { profileFor(it) ?: return null }
        val dominantProfile = profiles
            .groupBy { it.profile }
            .map { (profile, matches) ->
                DominantProfileCandidate(
                    profile = profile,
                    count = matches.size,
                    priority = matches.maxOf { it.priority }
                )
            }
            .sortedWith(
                compareByDescending<DominantProfileCandidate> { it.count }
                    .thenByDescending { it.priority }
                    .thenBy { it.profile }
            )
            .first()
            .profile

        return CoreStats(
            dominantProfile = dominantProfile,
            calculatedRadius = profiles.map { it.baseRadius }.average(),
            calculatedStrength = profiles.map { it.baseStrength }.average()
        )
    }

    private fun writeMissingDefaultProfiles(config: FileConfiguration) {
        for (profile in defaultProfiles) {
            val material = Material.matchMaterial(profile.materialName)
            if (material == null || !material.isBlock || material == Material.AIR) {
                logger.warning("Skipping default core material '${profile.materialName}' because it is not available in this server version.")
                continue
            }

            val path = "$CONFIG_SECTION.${profile.materialName}"
            if (!config.contains("$path.profile")) {
                config.set("$path.profile", profile.profile)
            }
            if (!config.contains("$path.base-radius")) {
                config.set("$path.base-radius", profile.baseRadius)
            }
            if (!config.contains("$path.base-strength")) {
                config.set("$path.base-strength", profile.baseStrength)
            }
            if (!config.contains("$path.priority")) {
                config.set("$path.priority", profile.priority)
            }
        }
    }

    private data class DominantProfileCandidate(
        val profile: String,
        val count: Int,
        val priority: Int
    )
}

data class DefaultCoreMaterialProfile(
    val materialName: String,
    val profile: String,
    val baseRadius: Double,
    val baseStrength: Double,
    val priority: Int
) {
    fun toProfile(material: Material): CoreMaterialProfile {
        return CoreMaterialProfile(
            material = material,
            profile = profile,
            baseRadius = baseRadius,
            baseStrength = baseStrength,
            priority = priority
        )
    }
}

data class CoreMaterialProfile(
    val material: Material,
    val profile: String,
    val baseRadius: Double,
    val baseStrength: Double,
    val priority: Int
)

data class CoreStats(
    val dominantProfile: String,
    val calculatedRadius: Double,
    val calculatedStrength: Double
)
