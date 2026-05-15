package ru.garde.magnet

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Item
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

class MagnetCoreManager(
    private val plugin: JavaPlugin,
    private val magneticMultiplier: (Material) -> Double?
) {
    companion object {
        private const val CONFIG_SECTION = "magnet-cores"

        val copperCoreMaterials = setOf(
            Material.COPPER_BLOCK,
            Material.EXPOSED_COPPER,
            Material.WEATHERED_COPPER,
            Material.OXIDIZED_COPPER,
            Material.WAXED_COPPER_BLOCK,
            Material.WAXED_EXPOSED_COPPER,
            Material.WAXED_WEATHERED_COPPER,
            Material.WAXED_OXIDIZED_COPPER
        )

        fun isCopperCoreMaterial(type: Material): Boolean {
            if (type in copperCoreMaterials) return true

            return type.isBlock &&
                type.name.contains("COPPER") &&
                !type.name.endsWith("_ORE") &&
                type != Material.RAW_COPPER_BLOCK
        }
    }

    private val cores = linkedMapOf<String, MagnetCore>()
    private var task: BukkitTask? = null

    var coreTaskPeriodTicks: Long = 2L
        private set
    var defaultCoreRadius: Double = 16.0
        private set
    var defaultCoreStrength: Double = 0.28
        private set
    var maxCoreRadius: Double = 64.0
        private set

    fun load() {
        loadSettings()
        loadCores()
    }

    fun reload() {
        plugin.reloadConfig()
        load()
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

    fun create(id: String, block: Block, radius: Double, strength: Double): MagnetCore {
        return createAt(
            id = id,
            worldName = block.world.name,
            x = block.x,
            y = block.y,
            z = block.z,
            radius = radius,
            strength = strength
        )
    }

    fun createAt(
        id: String,
        worldName: String,
        x: Int,
        y: Int,
        z: Int,
        radius: Double,
        strength: Double
    ): MagnetCore {
        val core = MagnetCore(
            id = id,
            worldName = worldName,
            x = x,
            y = y,
            z = z,
            radius = limitRadius(radius),
            strength = strength
        )

        cores[id] = core
        save()
        return core
    }

    fun remove(id: String): MagnetCore? {
        val removed = cores.remove(id) ?: return null
        save()
        return removed
    }

    fun inspect(id: String): MagnetCoreInspection? {
        val core = cores[id] ?: return null
        val world = Bukkit.getWorld(core.worldName)
        val chunkLoaded = world?.isChunkLoaded(core.x shr 4, core.z shr 4) ?: false
        val blockType = world?.getBlockAt(core.x, core.y, core.z)?.type
        var nearbyItems = 0
        var magneticItems = 0

        if (world != null && chunkLoaded && blockType != null) {
            val center = world.getBlockAt(core.x, core.y, core.z).location.add(0.5, 0.5, 0.5)
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
            blockType = blockType,
            copperCoreBlock = blockType?.let(::isCopperCoreMaterial) ?: false,
            nearbyItems = nearbyItems,
            magneticItems = magneticItems
        )
    }

    fun limitRadius(radius: Double): Double {
        if (!java.lang.Double.isFinite(radius) || radius <= 0.0) return defaultCoreRadius

        return radius.coerceIn(1.0, maxCoreRadius)
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

        defaultCoreRadius = defaultCoreRadius.coerceAtMost(maxCoreRadius)
    }

    private fun loadCores() {
        cores.clear()

        val section = plugin.config.getConfigurationSection(CONFIG_SECTION) ?: return
        for (id in section.getKeys(false)) {
            val path = "$CONFIG_SECTION.$id"
            val world = plugin.config.getString("$path.world") ?: continue

            cores[id] = MagnetCore(
                id = id,
                worldName = world,
                x = plugin.config.getInt("$path.x"),
                y = plugin.config.getInt("$path.y"),
                z = plugin.config.getInt("$path.z"),
                radius = limitRadius(plugin.config.getDouble("$path.radius", defaultCoreRadius)),
                strength = positiveConfigDouble("$path.strength", defaultCoreStrength)
                    .coerceAtLeast(0.01)
            )
        }
    }

    private fun positiveConfigDouble(path: String, fallback: Double): Double {
        val value = plugin.config.getDouble(path, fallback)
        return if (java.lang.Double.isFinite(value) && value > 0.0) value else fallback
    }

    private fun save() {
        plugin.config.set(CONFIG_SECTION, null)
        if (cores.isEmpty()) {
            plugin.config.createSection(CONFIG_SECTION)
        }

        for (core in cores.values) {
            val path = "$CONFIG_SECTION.${core.id}"
            plugin.config.set("$path.world", core.worldName)
            plugin.config.set("$path.x", core.x)
            plugin.config.set("$path.y", core.y)
            plugin.config.set("$path.z", core.z)
            plugin.config.set("$path.radius", core.radius)
            plugin.config.set("$path.strength", core.strength)
        }
        plugin.saveConfig()
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
            attractItems(core)
        }
    }

    private fun attractItems(core: MagnetCore) {
        val world = Bukkit.getWorld(core.worldName) ?: return
        if (!world.isChunkLoaded(core.x shr 4, core.z shr 4)) return

        val block = world.getBlockAt(core.x, core.y, core.z)
        if (!isCopperCoreMaterial(block.type)) return

        val center = block.location.add(0.5, 0.5, 0.5)
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
}

data class MagnetCore(
    val id: String,
    val worldName: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val radius: Double,
    val strength: Double
)

data class MagnetCoreInspection(
    val core: MagnetCore,
    val worldLoaded: Boolean,
    val chunkLoaded: Boolean,
    val blockType: Material?,
    val copperCoreBlock: Boolean,
    val nearbyItems: Int,
    val magneticItems: Int
)
