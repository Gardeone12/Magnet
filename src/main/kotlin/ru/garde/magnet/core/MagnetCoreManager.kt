// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Garde1 / Gardeone12

package ru.garde.magnet.core

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Item
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.util.Locale
import kotlin.math.floor

class MagnetCoreManager(
    private val plugin: JavaPlugin,
    private val magneticMultiplier: (Material) -> Double?
) {
    companion object {
        private const val CONFIG_SECTION = "magnet-cores"
        private const val CORE_SIZE = "2x2x2"
        private const val MIN_FRAME_WARNING_BLOCKS = 8

        val defaultFrameMaterialNames = setOf(
            "STONE_BRICKS", "CRACKED_STONE_BRICKS", "CHISELED_STONE_BRICKS",
            "POLISHED_ANDESITE", "POLISHED_ANDESITE_STAIRS", "POLISHED_ANDESITE_SLAB",
            "DEEPSLATE_BRICKS", "DEEPSLATE_TILES", "POLISHED_DEEPSLATE", "BLACK_CONCRETE",
            "BLACKSTONE", "POLISHED_BLACKSTONE", "IRON_BLOCK", "COPPER_BLOCK", "CUT_COPPER",
            "WAXED_CUT_COPPER", "LIGHTNING_ROD"
        )
        val defaultFrameMaterials: Set<Material> = defaultFrameMaterialNames
            .mapNotNull(Material::matchMaterial)
            .toSet()
    }

    private val cores = linkedMapOf<String, MagnetCore>()
    private val blockIndex = mutableMapOf<String, FrameBlockRef>()
    private var task: BukkitTask? = null
    private var frameScanner = MagnetFrameScanner(MagnetFrameSettings(), defaultFrameMaterials)
    private val coreMaterialRegistry = CoreMaterialRegistry(plugin.logger)
    private val coresFile = File(plugin.dataFolder, "cores.yml")
    private var coresConfig = YamlConfiguration()

    var coreTaskPeriodTicks: Long = 2L
        private set
    var defaultCoreRadius: Double = 16.0
        private set
    var defaultCoreStrength: Double = 0.28
        private set
    var maxCoreRadius: Double = 64.0
        private set
    var minCoreStrength: Double = 0.05
        private set
    var maxCoreStrength: Double = 2.0
        private set
    var frameSettings: MagnetFrameSettings = MagnetFrameSettings()
        private set
    var frameMaterials: Set<Material> = defaultFrameMaterials
        private set

    fun load() {
        loadSettings()
        loadCores()
        rebuildBlockIndex()
    }

    fun reload() {
        plugin.reloadConfig()
        loadSettings()
        loadCores()
        rebuildBlockIndex()
        save()
        restartTask()
    }

    fun start() {
        restartTask()
    }

    fun shutdown() {
        task?.cancel()
        task = null
        save()
    }

    fun all(): List<MagnetCore> {
        return cores.values.toList()
    }

    fun exists(id: String): Boolean {
        return cores.containsKey(id)
    }

    fun get(id: String): MagnetCore? {
        return cores[id]
    }

    fun isCoreMaterial(type: Material): Boolean {
        return coreMaterialRegistry.isCoreMaterial(type)
    }

    fun coreMaterialProfiles(): List<CoreMaterialProfile> {
        return coreMaterialRegistry.allProfiles()
    }

    fun coreMaterialProfile(materialName: String): CoreMaterialProfile? {
        return coreMaterialRegistry.profileForMaterialName(materialName)
    }

    fun create(
        id: String,
        block: Block,
        radiusOverride: Double?,
        strengthOverride: Double?
    ): MagnetCoreCreateResult? {
        val structure = findCoreStructure(block) ?: return null

        return createFromStructure(
            id = id,
            worldName = structure.worldName,
            centerX = structure.centerX,
            centerY = structure.centerY,
            centerZ = structure.centerZ,
            coreBlocks = structure.coreBlocks,
            radiusOverride = radiusOverride,
            strengthOverride = strengthOverride
        )
    }

    fun createAt(
        id: String,
        worldName: String,
        x: Int,
        y: Int,
        z: Int,
        radiusOverride: Double?,
        strengthOverride: Double?
    ): MagnetCoreCreateResult? {
        val world = Bukkit.getWorld(worldName) ?: return null
        val structure = findCoreStructure(world.getBlockAt(x, y, z)) ?: return null

        return createFromStructure(
            id = id,
            worldName = structure.worldName,
            centerX = structure.centerX,
            centerY = structure.centerY,
            centerZ = structure.centerZ,
            coreBlocks = structure.coreBlocks,
            radiusOverride = radiusOverride,
            strengthOverride = strengthOverride
        )
    }

    fun findCoreStructure(targetBlock: Block): MagnetCoreStructure? {
        if (!isCoreMaterial(targetBlock.type)) return null

        for (originX in listOf(targetBlock.x, targetBlock.x - 1)) {
            for (originY in listOf(targetBlock.y, targetBlock.y - 1)) {
                for (originZ in listOf(targetBlock.z, targetBlock.z - 1)) {
                    val coreBlocks = buildCoreBlocks(originX, originY, originZ)

                    if (coreBlocks.all { position ->
                            isCoreMaterial(targetBlock.world.getBlockAt(position.x, position.y, position.z).type)
                        }
                    ) {
                        return MagnetCoreStructure(
                            worldName = targetBlock.world.name,
                            centerX = originX + 1.0,
                            centerY = originY + 1.0,
                            centerZ = originZ + 1.0,
                            coreBlocks = coreBlocks
                        )
                    }
                }
            }
        }

        return null
    }

    fun rescanFrame(id: String): MagnetFrameRescanResult? {
        plugin.reloadConfig()
        loadSettings()

        val core = cores[id] ?: return null
        val world = Bukkit.getWorld(core.worldName)

        if (world == null) {
            val reason = "World '${core.worldName}' is not loaded"
            val updated = core.copy(active = false, damaged = true, damageReason = reason)
            cores[id] = updated
            rebuildBlockIndex()
            save()

            return MagnetFrameRescanResult(
                core = updated,
                success = false,
                criticalCount = updated.criticalFrameBlocks.size,
                optionalCount = updated.optionalFrameBlocks.size,
                message = reason
            )
        }

        val statsRefresh = refreshCoreStats(core, world, "Invalid core material at")
        if (!statsRefresh.success) {
            val updated = statsRefresh.core
            cores[id] = updated
            rebuildBlockIndex()
            save()

            return MagnetFrameRescanResult(
                core = updated,
                success = false,
                criticalCount = updated.criticalFrameBlocks.size,
                optionalCount = updated.optionalFrameBlocks.size,
                message = statsRefresh.reason
            )
        }

        val scanResult = frameScanner.scanFrame(statsRefresh.core)
        val (updated, message) = statsRefresh.core.withScanResult(scanResult)
        cores[id] = updated
        rebuildBlockIndex()
        save()

        return MagnetFrameRescanResult(
            core = updated,
            success = updated.active && !updated.damaged,
            criticalCount = updated.criticalFrameBlocks.size,
            optionalCount = updated.optionalFrameBlocks.size,
            message = message
        )
    }

    fun refreshCore(id: String): MagnetCoreRefreshResult? {
        plugin.reloadConfig()
        loadSettings()

        return refreshCoreStatsById(id, "Invalid core material at")
    }

    fun reloadProfiles(): Int {
        plugin.reloadConfig()
        loadSettings()
        return refreshAllCoreStats()
    }

    fun setCoreOverride(id: String, enabled: Boolean): MagnetCoreRefreshResult? {
        val core = cores[id] ?: return null
        if (enabled) {
            val updated = core.copy(manualOverride = true)
            cores[id] = updated
            save()
            return MagnetCoreRefreshResult(updated, success = true, message = null)
        }

        cores[id] = core.copy(manualOverride = false)
        return refreshCoreStatsById(id, "Invalid core material at")
    }

    fun setCoreRadius(id: String, radius: Double): MagnetCoreRefreshResult? {
        val core = cores[id] ?: return null
        val updated = core.copy(radius = limitRadius(radius), manualOverride = true)
        cores[id] = updated
        save()

        return MagnetCoreRefreshResult(updated, success = true, message = null)
    }

    fun setCoreStrength(id: String, strength: Double): MagnetCoreRefreshResult? {
        val core = cores[id] ?: return null
        val updated = core.copy(strength = limitStrength(strength), manualOverride = true)
        cores[id] = updated
        save()

        return MagnetCoreRefreshResult(updated, success = true, message = null)
    }

    fun setMaterialProfileRadius(materialName: String, radius: Double): CoreMaterialProfile? {
        val material = Material.matchMaterial(materialName) ?: return null
        val path = "core-materials.${material.name}"
        if (!plugin.config.isConfigurationSection(path)) return null

        plugin.config.set("$path.base-radius", limitRadius(radius))
        plugin.saveConfig()
        loadSettings()
        refreshAllCoreStats()

        return coreMaterialRegistry.profileFor(material)
    }

    fun setMaterialProfileStrength(materialName: String, strength: Double): CoreMaterialProfile? {
        val material = Material.matchMaterial(materialName) ?: return null
        val path = "core-materials.${material.name}"
        if (!plugin.config.isConfigurationSection(path)) return null

        plugin.config.set("$path.base-strength", limitStrength(strength))
        plugin.saveConfig()
        loadSettings()
        refreshAllCoreStats()

        return coreMaterialRegistry.profileFor(material)
    }

    fun setMaterialProfilePriority(materialName: String, priority: Int): CoreMaterialProfile? {
        val material = Material.matchMaterial(materialName) ?: return null
        val path = "core-materials.${material.name}"
        if (!plugin.config.isConfigurationSection(path)) return null

        plugin.config.set("$path.priority", priority)
        plugin.saveConfig()
        loadSettings()
        refreshAllCoreStats()

        return coreMaterialRegistry.profileFor(material)
    }

    fun handlePlacedBlock(block: Block): MagnetCoreReplacementResult? {
        val ref = lookupStructureBlock(block) ?: return null
        if (ref.blockType != MagnetStructureBlockType.CORE) return null

        val refreshResult = refreshCoreStatsById(ref.coreId, "Invalid core material after replacement at")
            ?: return null

        return MagnetCoreReplacementResult(
            core = refreshResult.core,
            success = refreshResult.success,
            message = refreshResult.message,
            placedMaterialAllowed = isCoreMaterial(block.type)
        )
    }

    fun remove(id: String): MagnetCore? {
        val removed = cores.remove(id) ?: return null
        rebuildBlockIndex()
        save()
        return removed
    }

    fun inspect(id: String): MagnetCoreInspection? {
        val core = cores[id] ?: return null
        val world = Bukkit.getWorld(core.worldName)
        val chunkLoaded = world?.let { isEveryCoreChunkLoaded(it, core) } ?: false
        val blockTypes = if (world == null || !chunkLoaded) {
            emptyList()
        } else {
            core.coreBlocks.map { world.getBlockAt(it.x, it.y, it.z).type }
        }
        val coreComplete = blockTypes.size == 8 &&
            isCoreBlockLayout2x2x2(core.coreBlocks) &&
            blockTypes.all { isCoreMaterial(it) }
        val frameValidation = validateFrameDetailed(core)
        var nearbyItems = 0
        var magneticItems = 0

        if (world != null && chunkLoaded && coreComplete && frameValidation.valid) {
            val center = core.centerLocation(world)
            for (entity in world.getNearbyEntities(center, core.radius, core.radius, core.radius)) {
                if (entity !is Item) continue

                nearbyItems++
                if (magneticMultiplier(entity.itemStack.type) != null) {
                    magneticItems++
                }
            }
        }

        return MagnetCoreInspection(
            core = core,
            worldLoaded = world != null,
            chunkLoaded = chunkLoaded,
            coreComplete = coreComplete,
            frameComplete = frameValidation.valid,
            frameReason = frameValidation.reason,
            blockTypes = blockTypes,
            nearbyItems = nearbyItems,
            magneticItems = magneticItems
        )
    }

    fun limitRadius(radius: Double): Double {
        if (!java.lang.Double.isFinite(radius) || radius <= 0.0) return defaultCoreRadius

        return radius.coerceIn(1.0, maxCoreRadius)
    }

    fun limitStrength(strength: Double): Double {
        if (!java.lang.Double.isFinite(strength) || strength <= 0.0) return defaultCoreStrength

        return strength.coerceIn(minCoreStrength, maxCoreStrength)
    }

    fun validateFrame(core: MagnetCore): Boolean {
        return validateFrameDetailed(core).valid
    }

    fun lookupStructureBlock(block: Block): FrameBlockRef? {
        return blockIndex[blockKey(block.world.name, block.x, block.y, block.z)]
    }

    fun handleBrokenBlocks(blocks: Iterable<Block>): List<MagnetStructureDamage> {
        val candidates = linkedMapOf<String, MagnetDamageCandidate>()

        for (block in blocks) {
            val ref = lookupStructureBlock(block) ?: continue
            if (ref.blockType == MagnetStructureBlockType.FRAME_OPTIONAL) continue

            val candidate = MagnetDamageCandidate(
                coreId = ref.coreId,
                blockType = ref.blockType,
                worldName = block.world.name,
                position = CoreBlockPosition(block.x, block.y, block.z)
            )
            val current = candidates[ref.coreId]
            if (current == null || candidate.blockType == MagnetStructureBlockType.CORE) {
                candidates[ref.coreId] = candidate
            }
        }

        if (candidates.isEmpty()) return emptyList()

        val damages = candidates.values.mapNotNull(::damageCore)
        if (damages.isNotEmpty()) {
            save()
        }

        return damages
    }

    private fun createFromStructure(
        id: String,
        worldName: String,
        centerX: Double,
        centerY: Double,
        centerZ: Double,
        coreBlocks: List<CoreBlockPosition>,
        radiusOverride: Double?,
        strengthOverride: Double?
    ): MagnetCoreCreateResult {
        val world = Bukkit.getWorld(worldName)
        val materials = world?.let { loadedWorld ->
            coreBlocks.map { position ->
                loadedWorld.getBlockAt(position.x, position.y, position.z).type
            }
        }.orEmpty()
        val stats = coreMaterialRegistry.calculateStats(materials)
            ?: CoreStats("unknown", defaultCoreRadius, defaultCoreStrength)
        val calculatedRadius = limitRadius(stats.calculatedRadius)
        val calculatedStrength = limitStrength(stats.calculatedStrength)
        val manualOverride = radiusOverride != null || strengthOverride != null

        val core = MagnetCore(
            id = id,
            worldName = worldName,
            centerX = centerX,
            centerY = centerY,
            centerZ = centerZ,
            coreBlocks = coreBlocks,
            radius = radiusOverride?.let(::limitRadius) ?: calculatedRadius,
            strength = strengthOverride?.let(::limitStrength) ?: calculatedStrength,
            coreProfile = stats.dominantProfile,
            calculatedRadius = calculatedRadius,
            calculatedStrength = calculatedStrength,
            manualOverride = manualOverride,
            active = true,
            damaged = false,
            damageReason = null
        )
        val scanResult = frameScanner.scanFrame(core)
        val (updated, warning) = core.withScanResult(scanResult)

        cores[id] = updated
        rebuildBlockIndex()
        save()

        return MagnetCoreCreateResult(updated, warning)
    }

    private fun MagnetCore.withScanResult(scanResult: MagnetFrameScanResult): Pair<MagnetCore, String?> {
        val frame = scanResult.toFrame(frameSettings)
        if (!scanResult.success) {
            val reason = scanResult.message ?: "Frame scan failed"
            return copy(frame = frame, active = false, damaged = true, damageReason = reason) to reason
        }

        val frameBlockCount = frame.criticalBlocks.size + frame.optionalBlocks.size
        if (frame.criticalBlocks.isEmpty()) {
            val reason = "No critical frame blocks found"
            return copy(frame = frame, active = false, damaged = true, damageReason = reason) to reason
        }

        val warning = if (frameBlockCount < MIN_FRAME_WARNING_BLOCKS) {
            "Frame has only $frameBlockCount block(s). Check frame-materials or rescan after building the structure."
        } else {
            null
        }

        return copy(frame = frame, active = true, damaged = false, damageReason = null) to warning
    }

    private fun damageCore(candidate: MagnetDamageCandidate): MagnetStructureDamage? {
        val core = cores[candidate.coreId] ?: return null
        val reason = when (candidate.blockType) {
            MagnetStructureBlockType.CORE ->
                "Core block broken at ${candidate.worldName} ${candidate.position.x} ${candidate.position.y} ${candidate.position.z}"
            MagnetStructureBlockType.FRAME_CRITICAL ->
                "Critical frame block broken at ${candidate.worldName} ${candidate.position.x} ${candidate.position.y} ${candidate.position.z}"
            MagnetStructureBlockType.FRAME_OPTIONAL -> return null
        }

        val updated = core.copy(active = false, damaged = true, damageReason = reason)
        cores[candidate.coreId] = updated

        return MagnetStructureDamage(
            core = updated,
            blockType = candidate.blockType,
            position = candidate.position,
            reason = reason
        )
    }

    private fun loadSettings() {
        coreTaskPeriodTicks = plugin.config
            .getLong("settings.core-task-period-ticks", 2L)
            .coerceAtLeast(1L)
        defaultCoreRadius = positiveConfigDouble("settings.default-core-radius", 16.0)
            .coerceAtLeast(1.0)
        defaultCoreStrength = positiveConfigDouble("settings.default-core-strength", 0.28)
            .coerceAtLeast(0.01)
        maxCoreRadius = positiveConfigDouble("settings.max-core-radius", 64.0)
            .coerceAtLeast(1.0)
        minCoreStrength = positiveConfigDouble("settings.min-core-strength", 0.05)
            .coerceAtLeast(0.001)
        maxCoreStrength = positiveConfigDouble("settings.max-core-strength", 2.0)
            .coerceAtLeast(minCoreStrength)

        defaultCoreRadius = defaultCoreRadius.coerceAtMost(maxCoreRadius)
        defaultCoreStrength = defaultCoreStrength.coerceIn(minCoreStrength, maxCoreStrength)
        coreMaterialRegistry.load(plugin.config)
        plugin.saveConfig()
        frameSettings = MagnetFrameSettings(
            scanRadiusX = plugin.config.getInt("settings.frame-scan-radius-x", 12).coerceAtLeast(1),
            scanRadiusYDown = plugin.config.getInt("settings.frame-scan-radius-y-down", 8).coerceAtLeast(0),
            scanRadiusYUp = plugin.config.getInt("settings.frame-scan-radius-y-up", 8).coerceAtLeast(0),
            scanRadiusZ = plugin.config.getInt("settings.frame-scan-radius-z", 12).coerceAtLeast(1),
            maxFrameBlocks = plugin.config.getInt("settings.max-frame-blocks", 4096).coerceAtLeast(1),
            cornerPadding = plugin.config.getInt("settings.frame-corner-padding", 1).coerceAtLeast(0),
            ignoreFrameCorners = plugin.config.getBoolean("settings.ignore-frame-corners", true)
        )
        frameMaterials = loadFrameMaterials()
        frameScanner = MagnetFrameScanner(frameSettings, frameMaterials)
    }

    private fun loadFrameMaterials(): Set<Material> {
        val configured = plugin.config.getStringList("frame-materials")
        if (configured.isEmpty()) return defaultFrameMaterials

        val materials = linkedSetOf<Material>()
        val invalidConfiguredMaterials = mutableListOf<String>()
        for (name in configured) {
            val normalizedName = name.trim().uppercase(Locale.ROOT)
            val material = Material.matchMaterial(name)
            if (material == null || !material.isBlock || material == Material.AIR) {
                if (normalizedName !in defaultFrameMaterialNames) invalidConfiguredMaterials += name
                continue
            }
            materials += material
        }

        if (invalidConfiguredMaterials.isNotEmpty()) {
            plugin.logger.warning(
                "Ignoring invalid frame materials: ${invalidConfiguredMaterials.joinToString()}."
            )
        }
        return materials.ifEmpty { defaultFrameMaterials }
    }

    private fun loadCores() {
        cores.clear()
        coresConfig = YamlConfiguration.loadConfiguration(coresFile)

        val sourceConfig = if (coresConfig.isConfigurationSection(CONFIG_SECTION)) {
            coresConfig
        } else {
            plugin.config
        }
        val section = sourceConfig.getConfigurationSection(CONFIG_SECTION) ?: return
        for (id in section.getKeys(false)) {
            val path = "$CONFIG_SECTION.$id"
            val world = sourceConfig.getString("$path.world") ?: continue
            val core = loadConfiguredCore(sourceConfig, id, path, world)

            if (core == null) {
                plugin.logger.warning("Magnet core $id is invalid or incomplete. Expected 2x2x2 core blocks. Core disabled.")
                continue
            }

            cores[id] = validateLoadedCore(core)
        }
    }

    private fun loadConfiguredCore(config: FileConfiguration, id: String, path: String, worldName: String): MagnetCore? {
        val configuredRadius = config.getDouble("$path.radius", defaultCoreRadius)
        val configuredStrength = config.getDouble("$path.strength", defaultCoreStrength)
        val radius = limitRadius(configuredRadius)
        val strength = limitStrength(configuredStrength)
        val calculatedRadius = limitRadius(config.getDouble("$path.calculated-radius", radius))
        val calculatedStrength = limitStrength(config.getDouble("$path.calculated-strength", strength))
        val coreProfile = config.getString("$path.core-profile") ?: "unknown"
        val manualOverride = if (config.contains("$path.manual-override")) {
            config.getBoolean("$path.manual-override", false)
        } else if (config.contains("$path.calculated-radius") && config.contains("$path.calculated-strength")) {
            configuredRadius != calculatedRadius || configuredStrength != calculatedStrength
        } else {
            configuredRadius != defaultCoreRadius || configuredStrength != defaultCoreStrength
        }
        val configuredBlocks = config.getMapList("$path.core-blocks")
        val active = config.getBoolean("$path.active", true)
        val damaged = config.getBoolean("$path.damaged", false)
        val damageReason = config.getString("$path.damage-reason")
        val frame = loadConfiguredFrame(config, path)

        if (configuredBlocks.isNotEmpty()) {
            val coreBlocks = readBlockList(config, "$path.core-blocks")

            if (!isCoreBlockLayout2x2x2(coreBlocks)) return null

            val originX = coreBlocks.minOf { it.x }
            val originY = coreBlocks.minOf { it.y }
            val originZ = coreBlocks.minOf { it.z }
            val computedCenterX = originX + 1.0
            val computedCenterY = originY + 1.0
            val computedCenterZ = originZ + 1.0

            return MagnetCore(
                id = id,
                worldName = worldName,
                centerX = finiteConfigDouble(config, "$path.center.x", computedCenterX),
                centerY = finiteConfigDouble(config, "$path.center.y", computedCenterY),
                centerZ = finiteConfigDouble(config, "$path.center.z", computedCenterZ),
                coreBlocks = coreBlocks,
                frame = frame,
                radius = radius,
                strength = strength,
                coreProfile = coreProfile,
                calculatedRadius = calculatedRadius,
                calculatedStrength = calculatedStrength,
                manualOverride = manualOverride,
                active = active,
                damaged = damaged,
                damageReason = damageReason
            )
        }

        if (!config.contains("$path.x") ||
            !config.contains("$path.y") ||
            !config.contains("$path.z")
        ) {
            return null
        }

        val world = Bukkit.getWorld(worldName)
        if (world == null) {
            plugin.logger.warning("Legacy magnetic core '$id' references unloaded world '$worldName' and cannot be upgraded to 2x2x2.")
            return null
        }

        val block = world.getBlockAt(
            config.getInt("$path.x"),
            config.getInt("$path.y"),
            config.getInt("$path.z")
        )
        val structure = findCoreStructure(block)
        if (structure == null) {
            plugin.logger.warning("Legacy magnetic core '$id' is not part of a complete 2x2x2 configured core and was skipped.")
            return null
        }

        return MagnetCore(
            id = id,
            worldName = structure.worldName,
            centerX = structure.centerX,
            centerY = structure.centerY,
            centerZ = structure.centerZ,
            coreBlocks = structure.coreBlocks,
            frame = frame,
            radius = radius,
            strength = strength,
            coreProfile = coreProfile,
            calculatedRadius = calculatedRadius,
            calculatedStrength = calculatedStrength,
            manualOverride = manualOverride,
            active = active,
            damaged = damaged,
            damageReason = damageReason
        )
    }

    private fun loadConfiguredFrame(config: FileConfiguration, path: String): MagnetFrame {
        return MagnetFrame(
            scanRadiusX = config.getInt("$path.frame.scan-radius-x", frameSettings.scanRadiusX).coerceAtLeast(1),
            scanRadiusYDown = config.getInt("$path.frame.scan-radius-y-down", frameSettings.scanRadiusYDown).coerceAtLeast(0),
            scanRadiusYUp = config.getInt("$path.frame.scan-radius-y-up", frameSettings.scanRadiusYUp).coerceAtLeast(0),
            scanRadiusZ = config.getInt("$path.frame.scan-radius-z", frameSettings.scanRadiusZ).coerceAtLeast(1),
            criticalBlocks = readBlockList(config, "$path.frame.critical-blocks"),
            optionalBlocks = readBlockList(config, "$path.frame.optional-blocks")
        )
    }

    private fun readBlockList(config: FileConfiguration, path: String): List<CoreBlockPosition> {
        return config.getMapList(path).mapNotNull { block ->
            val x = (block["x"] as? Number)?.toInt() ?: return@mapNotNull null
            val y = (block["y"] as? Number)?.toInt() ?: return@mapNotNull null
            val z = (block["z"] as? Number)?.toInt() ?: return@mapNotNull null

            CoreBlockPosition(x, y, z)
        }.distinct()
    }

    private fun validateLoadedCore(core: MagnetCore): MagnetCore {
        val world = Bukkit.getWorld(core.worldName)
        if (world == null) {
            val reason = "World '${core.worldName}' is not loaded"
            plugin.logger.warning("Magnetic core '${core.id}' is inactive: $reason.")
            return core.copy(active = false, damaged = true, damageReason = reason)
        }

        val statsRefresh = refreshCoreStats(core, world, "Invalid core material at")
        if (!statsRefresh.success) {
            val reason = statsRefresh.reason ?: "Core blocks are missing or incomplete"
            plugin.logger.warning("Magnet core ${core.id} is invalid: $reason. Core disabled.")
            return statsRefresh.core
        }

        val refreshedCore = statsRefresh.core
        val frameValidation = validateFrameDetailed(refreshedCore)
        if (!frameValidation.valid) {
            plugin.logger.warning("Magnet core ${refreshedCore.id} frame is invalid: ${frameValidation.reason}. Core disabled.")
            return refreshedCore.copy(active = false, damaged = true, damageReason = frameValidation.reason)
        }

        return refreshedCore.copy(active = true, damaged = false, damageReason = null)
    }

    private fun refreshCoreStatsById(id: String, invalidMaterialPrefix: String): MagnetCoreRefreshResult? {
        val core = cores[id] ?: return null
        val world = Bukkit.getWorld(core.worldName)
        if (world == null) {
            val reason = "World '${core.worldName}' is not loaded"
            val updated = core.copy(active = false, damaged = true, damageReason = reason)
            cores[id] = updated
            rebuildBlockIndex()
            save()

            return MagnetCoreRefreshResult(updated, success = false, message = reason)
        }

        val statsRefresh = refreshCoreStats(core, world, invalidMaterialPrefix)
        val updated = if (statsRefresh.success) {
            statsRefresh.core.copy(active = true, damaged = false, damageReason = null)
        } else {
            statsRefresh.core
        }
        cores[id] = updated
        rebuildBlockIndex()
        save()

        return MagnetCoreRefreshResult(updated, statsRefresh.success, statsRefresh.reason)
    }

    private fun refreshAllCoreStats(): Int {
        var refreshed = 0
        val updatedCores = linkedMapOf<String, MagnetCore>()

        for ((id, core) in cores) {
            val world = Bukkit.getWorld(core.worldName)
            if (world == null) {
                updatedCores[id] = core.copy(
                    active = false,
                    damaged = true,
                    damageReason = "World '${core.worldName}' is not loaded"
                )
                continue
            }

            val statsRefresh = refreshCoreStats(core, world, "Invalid core material at")
            updatedCores[id] = if (statsRefresh.success) {
                refreshed++
                val damagedByCoreMaterial = core.damageReason?.startsWith("Invalid core material") == true
                if (!core.damaged || damagedByCoreMaterial) {
                    statsRefresh.core.copy(active = true, damaged = false, damageReason = null)
                } else {
                    statsRefresh.core.copy(
                        active = core.active,
                        damaged = core.damaged,
                        damageReason = core.damageReason
                    )
                }
            } else {
                statsRefresh.core
            }
        }

        cores.clear()
        cores.putAll(updatedCores)
        rebuildBlockIndex()
        save()

        return refreshed
    }

    private fun refreshCoreStats(core: MagnetCore, world: World, invalidMaterialPrefix: String): CoreStatsRefresh {
        if (!isCoreBlockLayout2x2x2(core.coreBlocks)) {
            val reason = "Core blocks are missing or incomplete"
            return CoreStatsRefresh(
                core = core.copy(active = false, damaged = true, damageReason = reason),
                success = false,
                reason = reason
            )
        }

        val materials = mutableListOf<Material>()
        for (position in core.coreBlocks) {
            val material = world.getBlockAt(position.x, position.y, position.z).type
            if (!isCoreMaterial(material)) {
                val reason = "$invalidMaterialPrefix ${world.name} ${position.x} ${position.y} ${position.z}"
                return CoreStatsRefresh(
                    core = core.copy(active = false, damaged = true, damageReason = reason),
                    success = false,
                    reason = reason
                )
            }

            materials += material
        }

        val stats = coreMaterialRegistry.calculateStats(materials)
        if (stats == null) {
            val reason = "Core blocks are missing or incomplete"
            return CoreStatsRefresh(
                core = core.copy(active = false, damaged = true, damageReason = reason),
                success = false,
                reason = reason
            )
        }

        val calculatedRadius = limitRadius(stats.calculatedRadius)
        val calculatedStrength = limitStrength(stats.calculatedStrength)
        val updated = core.copy(
            coreProfile = stats.dominantProfile,
            calculatedRadius = calculatedRadius,
            calculatedStrength = calculatedStrength,
            radius = if (core.manualOverride) limitRadius(core.radius) else calculatedRadius,
            strength = if (core.manualOverride) limitStrength(core.strength) else calculatedStrength
        )

        return CoreStatsRefresh(core = updated, success = true, reason = null)
    }

    private fun validateFrameDetailed(core: MagnetCore): MagnetFrameValidation {
        val world = Bukkit.getWorld(core.worldName)
            ?: return MagnetFrameValidation(false, "World '${core.worldName}' is not loaded")

        if (core.criticalFrameBlocks.isEmpty()) {
            return MagnetFrameValidation(false, "No critical frame blocks configured")
        }

        for (position in core.criticalFrameBlocks) {
            val material = world.getBlockAt(position.x, position.y, position.z).type
            if (material == Material.AIR || material !in frameMaterials) {
                return MagnetFrameValidation(
                    false,
                    "Critical frame block missing at ${core.worldName} ${position.x} ${position.y} ${position.z}"
                )
            }
        }

        return MagnetFrameValidation(true, null)
    }

    private fun positiveConfigDouble(path: String, fallback: Double): Double {
        val value = plugin.config.getDouble(path, fallback)
        return if (java.lang.Double.isFinite(value) && value > 0.0) value else fallback
    }

    private fun finiteConfigDouble(config: FileConfiguration, path: String, fallback: Double): Double {
        val value = config.getDouble(path, fallback)
        return if (java.lang.Double.isFinite(value)) value else fallback
    }

    private fun save() {
        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdirs()
        }

        coresConfig = YamlConfiguration()
        coresConfig.set(CONFIG_SECTION, null)
        if (cores.isEmpty()) {
            coresConfig.createSection(CONFIG_SECTION)
        }

        for (core in cores.values) {
            val path = "$CONFIG_SECTION.${core.id}"
            coresConfig.set("$path.world", core.worldName)
            coresConfig.set("$path.active", core.active)
            coresConfig.set("$path.damaged", core.damaged)
            coresConfig.set("$path.damage-reason", core.damageReason)
            coresConfig.set("$path.center.x", core.centerX)
            coresConfig.set("$path.center.y", core.centerY)
            coresConfig.set("$path.center.z", core.centerZ)
            coresConfig.set("$path.core-size", core.coreSize)
            coresConfig.set("$path.core-profile", core.coreProfile)
            coresConfig.set("$path.calculated-radius", core.calculatedRadius)
            coresConfig.set("$path.calculated-strength", core.calculatedStrength)
            coresConfig.set("$path.manual-override", core.manualOverride)
            coresConfig.set("$path.core-blocks", core.coreBlocks.toConfigList())
            coresConfig.set("$path.frame.scan-radius-x", core.frame.scanRadiusX)
            coresConfig.set("$path.frame.scan-radius-y-down", core.frame.scanRadiusYDown)
            coresConfig.set("$path.frame.scan-radius-y-up", core.frame.scanRadiusYUp)
            coresConfig.set("$path.frame.scan-radius-z", core.frame.scanRadiusZ)
            coresConfig.set("$path.frame.critical-blocks", core.criticalFrameBlocks.toConfigList())
            coresConfig.set("$path.frame.optional-blocks", core.optionalFrameBlocks.toConfigList())
            coresConfig.set("$path.radius", core.radius)
            coresConfig.set("$path.strength", core.strength)
        }
        coresConfig.save(coresFile)
    }

    private fun List<CoreBlockPosition>.toConfigList(): List<Map<String, Int>> {
        return map { block ->
            linkedMapOf(
                "x" to block.x,
                "y" to block.y,
                "z" to block.z
            )
        }
    }

    private fun rebuildBlockIndex() {
        blockIndex.clear()

        for (core in cores.values) {
            for (position in core.coreBlocks) {
                blockIndex[blockKey(core.worldName, position.x, position.y, position.z)] =
                    FrameBlockRef(core.id, MagnetStructureBlockType.CORE)
            }
            for (position in core.criticalFrameBlocks) {
                blockIndex[blockKey(core.worldName, position.x, position.y, position.z)] =
                    FrameBlockRef(core.id, MagnetStructureBlockType.FRAME_CRITICAL)
            }
            for (position in core.optionalFrameBlocks) {
                blockIndex[blockKey(core.worldName, position.x, position.y, position.z)] =
                    FrameBlockRef(core.id, MagnetStructureBlockType.FRAME_OPTIONAL)
            }
        }
    }

    private fun blockKey(worldName: String, x: Int, y: Int, z: Int): String {
        return "$worldName:$x:$y:$z"
    }

    private fun restartTask() {
        task?.cancel()
        task = Bukkit.getScheduler().runTaskTimer(
            plugin,
            Runnable { tick() },
            coreTaskPeriodTicks,
            coreTaskPeriodTicks
        )
    }

    private fun tick() {
        for (core in cores.values) {
            if (!core.active || core.damaged) continue

            attractItems(core)
        }
    }

    private fun attractItems(core: MagnetCore) {
        val world = Bukkit.getWorld(core.worldName) ?: return
        if (!isEveryCoreChunkLoaded(world, core)) return
        if (!isCorePresent(world, core)) return

        val center = core.centerLocation(world)
        val radiusSquared = core.radius * core.radius

        for (entity in world.getNearbyEntities(center, core.radius, core.radius, core.radius)) {
            if (entity !is Item) continue
            if (!entity.isValid || entity.isDead) continue

            val multiplier = magneticMultiplier(entity.itemStack.type) ?: continue
            val direction = center.toVector().subtract(entity.location.toVector())
            val distanceSquared = direction.lengthSquared()

            if (distanceSquared > radiusSquared || distanceSquared <= 0.04) continue

            entity.velocity = direction.normalize().multiply(core.strength * multiplier)
        }
    }

    private fun isEveryCoreChunkLoaded(world: World, core: MagnetCore): Boolean {
        return core.coreBlocks.all { world.isChunkLoaded(it.x shr 4, it.z shr 4) }
    }

    private fun isCorePresent(world: World, core: MagnetCore): Boolean {
        if (!isCoreBlockLayout2x2x2(core.coreBlocks)) return false

        return core.coreBlocks.all { position ->
            isCoreMaterial(world.getBlockAt(position.x, position.y, position.z).type)
        }
    }

    private fun isCoreBlockLayout2x2x2(coreBlocks: List<CoreBlockPosition>): Boolean {
        if (coreBlocks.size != 8) return false

        val xValues = coreBlocks.map { it.x }.toSet()
        val yValues = coreBlocks.map { it.y }.toSet()
        val zValues = coreBlocks.map { it.z }.toSet()

        return xValues.size == 2 &&
            yValues.size == 2 &&
            zValues.size == 2 &&
            xValues.maxOrNull() == xValues.minOrNull()?.plus(1) &&
            yValues.maxOrNull() == yValues.minOrNull()?.plus(1) &&
            zValues.maxOrNull() == zValues.minOrNull()?.plus(1)
    }

    private fun buildCoreBlocks(originX: Int, originY: Int, originZ: Int): List<CoreBlockPosition> {
        return buildList {
            for (dy in 0..1) {
                for (dz in 0..1) {
                    for (dx in 0..1) {
                        add(CoreBlockPosition(originX + dx, originY + dy, originZ + dz))
                    }
                }
            }
        }
    }
}

class MagnetFrameScanner(
    private val settings: MagnetFrameSettings,
    private val frameMaterials: Set<Material>
) {
    fun scanFrame(core: MagnetCore): MagnetFrameScanResult {
        val world = Bukkit.getWorld(core.worldName)
            ?: return MagnetFrameScanResult(
                success = false,
                message = "World '${core.worldName}' is not loaded"
            )

        val scanBounds = MagnetFrameBounds(
            minX = floor(core.centerX - settings.scanRadiusX).toInt(),
            maxX = floor(core.centerX + settings.scanRadiusX).toInt(),
            minY = floor(core.centerY - settings.scanRadiusYDown).toInt(),
            maxY = floor(core.centerY + settings.scanRadiusYUp).toInt(),
            minZ = floor(core.centerZ - settings.scanRadiusZ).toInt(),
            maxZ = floor(core.centerZ + settings.scanRadiusZ).toInt()
        )
        val coreBlockSet = core.coreBlocks.toSet()
        val frameBlocks = mutableListOf<CoreBlockPosition>()

        for (x in scanBounds.minX..scanBounds.maxX) {
            for (y in scanBounds.minY..scanBounds.maxY) {
                for (z in scanBounds.minZ..scanBounds.maxZ) {
                    val position = CoreBlockPosition(x, y, z)
                    if (position in coreBlockSet) continue

                    val material = world.getBlockAt(x, y, z).type
                    if (material == Material.AIR || material !in frameMaterials) continue

                    frameBlocks += position
                    if (frameBlocks.size > settings.maxFrameBlocks) {
                        return MagnetFrameScanResult(
                            success = false,
                            bounds = scanBounds,
                            message = "Frame scan exceeded max-frame-blocks (${settings.maxFrameBlocks})"
                        )
                    }
                }
            }
        }

        if (frameBlocks.isEmpty()) {
            return MagnetFrameScanResult(success = true, bounds = scanBounds)
        }

        val frameBounds = MagnetFrameBounds(
            minX = frameBlocks.minOf { it.x },
            maxX = frameBlocks.maxOf { it.x },
            minY = frameBlocks.minOf { it.y },
            maxY = frameBlocks.maxOf { it.y },
            minZ = frameBlocks.minOf { it.z },
            maxZ = frameBlocks.maxOf { it.z }
        )
        val optionalBlocks = mutableListOf<CoreBlockPosition>()
        val criticalBlocks = mutableListOf<CoreBlockPosition>()

        for (position in frameBlocks) {
            if (isOptionalCorner(position, frameBounds)) {
                optionalBlocks += position
            } else {
                criticalBlocks += position
            }
        }

        return MagnetFrameScanResult(
            success = true,
            criticalFrameBlocks = criticalBlocks,
            optionalFrameBlocks = optionalBlocks,
            bounds = frameBounds
        )
    }

    private fun isOptionalCorner(position: CoreBlockPosition, bounds: MagnetFrameBounds): Boolean {
        if (!settings.ignoreFrameCorners) return false

        val padding = settings.cornerPadding
        val minX = position.x <= bounds.minX + padding
        val maxX = position.x >= bounds.maxX - padding
        val minZ = position.z <= bounds.minZ + padding
        val maxZ = position.z >= bounds.maxZ - padding

        return (minX && minZ) ||
            (maxX && minZ) ||
            (minX && maxZ) ||
            (maxX && maxZ)
    }
}

data class MagnetCore(
    val id: String,
    val worldName: String,
    val centerX: Double,
    val centerY: Double,
    val centerZ: Double,
    val coreBlocks: List<CoreBlockPosition>,
    val frame: MagnetFrame = MagnetFrame(),
    val radius: Double,
    val strength: Double,
    val coreProfile: String = "unknown",
    val calculatedRadius: Double = radius,
    val calculatedStrength: Double = strength,
    val manualOverride: Boolean = false,
    val active: Boolean = true,
    val damaged: Boolean = false,
    val damageReason: String? = null
) {
    val coreSize: String = "2x2x2"
    val coreType: String = coreSize
    val criticalFrameBlocks: List<CoreBlockPosition>
        get() = frame.criticalBlocks
    val optionalFrameBlocks: List<CoreBlockPosition>
        get() = frame.optionalBlocks
    val frameBlocks: List<CoreBlockPosition>
        get() = criticalFrameBlocks + optionalFrameBlocks

    fun centerLocation(world: World): Location {
        return Location(world, centerX, centerY, centerZ)
    }
}

data class MagnetFrame(
    val scanRadiusX: Int = 0,
    val scanRadiusYDown: Int = 0,
    val scanRadiusYUp: Int = 0,
    val scanRadiusZ: Int = 0,
    val criticalBlocks: List<CoreBlockPosition> = emptyList(),
    val optionalBlocks: List<CoreBlockPosition> = emptyList(),
    val bounds: MagnetFrameBounds? = null
)

data class MagnetFrameSettings(
    val scanRadiusX: Int = 12,
    val scanRadiusYDown: Int = 8,
    val scanRadiusYUp: Int = 8,
    val scanRadiusZ: Int = 12,
    val maxFrameBlocks: Int = 4096,
    val cornerPadding: Int = 1,
    val ignoreFrameCorners: Boolean = true
)

data class MagnetFrameBounds(
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int,
    val minZ: Int,
    val maxZ: Int
)

data class MagnetFrameScanResult(
    val success: Boolean,
    val criticalFrameBlocks: List<CoreBlockPosition> = emptyList(),
    val optionalFrameBlocks: List<CoreBlockPosition> = emptyList(),
    val bounds: MagnetFrameBounds? = null,
    val message: String? = null
) {
    fun toFrame(settings: MagnetFrameSettings): MagnetFrame {
        return MagnetFrame(
            scanRadiusX = settings.scanRadiusX,
            scanRadiusYDown = settings.scanRadiusYDown,
            scanRadiusYUp = settings.scanRadiusYUp,
            scanRadiusZ = settings.scanRadiusZ,
            criticalBlocks = criticalFrameBlocks,
            optionalBlocks = optionalFrameBlocks,
            bounds = bounds
        )
    }
}

data class CoreBlockPosition(
    val x: Int,
    val y: Int,
    val z: Int
)

data class MagnetCoreStructure(
    val worldName: String,
    val centerX: Double,
    val centerY: Double,
    val centerZ: Double,
    val coreBlocks: List<CoreBlockPosition>
)

data class MagnetCoreCreateResult(
    val core: MagnetCore,
    val frameWarning: String?
)

data class MagnetFrameRescanResult(
    val core: MagnetCore,
    val success: Boolean,
    val criticalCount: Int,
    val optionalCount: Int,
    val message: String?
)

data class MagnetCoreRefreshResult(
    val core: MagnetCore,
    val success: Boolean,
    val message: String?
)

data class MagnetCoreReplacementResult(
    val core: MagnetCore,
    val success: Boolean,
    val message: String?,
    val placedMaterialAllowed: Boolean
)

data class MagnetCoreInspection(
    val core: MagnetCore,
    val worldLoaded: Boolean,
    val chunkLoaded: Boolean,
    val coreComplete: Boolean,
    val frameComplete: Boolean,
    val frameReason: String?,
    val blockTypes: List<Material>,
    val nearbyItems: Int,
    val magneticItems: Int
)

data class FrameBlockRef(
    val coreId: String,
    val blockType: MagnetStructureBlockType
)

enum class MagnetStructureBlockType {
    CORE,
    FRAME_CRITICAL,
    FRAME_OPTIONAL
}

data class MagnetStructureDamage(
    val core: MagnetCore,
    val blockType: MagnetStructureBlockType,
    val position: CoreBlockPosition,
    val reason: String
)

private data class MagnetFrameValidation(
    val valid: Boolean,
    val reason: String?
)

private data class CoreStatsRefresh(
    val core: MagnetCore,
    val success: Boolean,
    val reason: String?
)

private data class MagnetDamageCandidate(
    val coreId: String,
    val blockType: MagnetStructureBlockType,
    val worldName: String,
    val position: CoreBlockPosition
)
