// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Garde1 / Gardeone12

package ru.garde.magnet

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.components.CustomModelDataComponent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.util.Locale

object MagnetPlugin : JavaPlugin() {
    private const val DEFAULT_LANGUAGE = "en"
    private const val PORTABLE_MAGNET_CUSTOM_MODEL_DATA = 9001001
    private const val PORTABLE_MAGNET_MODEL_NAMESPACE = "magnit"
    private const val PORTABLE_MAGNET_MODEL_PATH = "portable_magnet"
    private const val EXPECTED_PORTABLE_MAGNET_MODEL_KEY = "magnit:portable_magnet"
    private const val RESOURCE_PACK_CONFIG_SECTION = "resource-pack"
    private val coreIdPattern = Regex("[a-z0-9_-]{1,32}")
    private val portableMagnetItemModel = NamespacedKey(
        PORTABLE_MAGNET_MODEL_NAMESPACE,
        PORTABLE_MAGNET_MODEL_PATH
    )

    private lateinit var magnetKey: NamespacedKey
    private lateinit var coreManager: MagnetCoreManager

    private val supportedLanguages = setOf(DEFAULT_LANGUAGE, "ru")
    private val messages = mutableMapOf<String, YamlConfiguration>()

    private val ferromagneticMaterials = setOf(
        Material.IRON_INGOT,
        Material.RAW_IRON,
        Material.IRON_NUGGET,
        Material.IRON_BLOCK,
        Material.RAW_IRON_BLOCK,
        Material.IRON_ORE,
        Material.DEEPSLATE_IRON_ORE,

        Material.IRON_SWORD,
        Material.IRON_PICKAXE,
        Material.IRON_AXE,
        Material.IRON_SHOVEL,
        Material.IRON_HOE,

        Material.IRON_HELMET,
        Material.IRON_CHESTPLATE,
        Material.IRON_LEGGINGS,
        Material.IRON_BOOTS,
        Material.IRON_HORSE_ARMOR,

        Material.CHAINMAIL_HELMET,
        Material.CHAINMAIL_CHESTPLATE,
        Material.CHAINMAIL_LEGGINGS,
        Material.CHAINMAIL_BOOTS,

        Material.ANVIL,
        Material.CHIPPED_ANVIL,
        Material.DAMAGED_ANVIL,

        Material.IRON_BARS,
        Material.IRON_DOOR,
        Material.IRON_TRAPDOOR,
        Material.HEAVY_WEIGHTED_PRESSURE_PLATE,

        Material.HOPPER,
        Material.CAULDRON,
        Material.LANTERN,
        Material.SOUL_LANTERN,

        Material.BUCKET,
        Material.WATER_BUCKET,
        Material.LAVA_BUCKET,
        Material.MILK_BUCKET,
        Material.POWDER_SNOW_BUCKET,

        Material.SHEARS,
        Material.FLINT_AND_STEEL,
        Material.COMPASS,
        Material.RECOVERY_COMPASS,

        Material.RAIL,
        Material.POWERED_RAIL,
        Material.DETECTOR_RAIL,
        Material.ACTIVATOR_RAIL,

        Material.MINECART,
        Material.CHEST_MINECART,
        Material.FURNACE_MINECART,
        Material.HOPPER_MINECART,
        Material.TNT_MINECART
    )

    private val weakMagneticMaterials = setOf(
        Material.NETHERITE_INGOT,
        Material.NETHERITE_SCRAP,
        Material.NETHERITE_BLOCK,

        Material.NETHERITE_SWORD,
        Material.NETHERITE_PICKAXE,
        Material.NETHERITE_AXE,
        Material.NETHERITE_SHOVEL,
        Material.NETHERITE_HOE,

        Material.NETHERITE_HELMET,
        Material.NETHERITE_CHESTPLATE,
        Material.NETHERITE_LEGGINGS,
        Material.NETHERITE_BOOTS
    )

    override fun onEnable() {
        registerCommands()

        magnetKey = NamespacedKey(this, "magnet")

        saveDefaultConfig()
        reloadConfig()
        config.options().copyDefaults(true)
        saveConfig()
        loadMessages()
        validateResourcePackConfiguration()

        coreManager = MagnetCoreManager(this, ::getMagneticMultiplier)
        coreManager.load()
        server.pluginManager.registerEvents(MagnetStructureBreakListener(this, coreManager), this)

        startPortableMagnetTask()
        coreManager.start()

        logger.info("MagnetPlugin enabled")
    }

    override fun onDisable() {
        if (::coreManager.isInitialized) {
            coreManager.shutdown()
        }
    }

    private fun registerCommands() {
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(
                "magnet",
                "Gives magnet item and manages magnetic cores",
                listOf("magnit"),
                BasicCommand { source, args ->
                    handleMagnetCommand(source.sender, args)
                }
            )
        }
    }

    private fun loadMessages() {
        for (language in supportedLanguages) {
            saveResource("lang/$language.yml", false)
            messages[language] = YamlConfiguration.loadConfiguration(
                dataFolder.resolve("lang/$language.yml")
            )
        }
    }

    private fun startPortableMagnetTask() {
        Bukkit.getScheduler().runTaskTimer(this, Runnable {
            for (player in Bukkit.getOnlinePlayers()) {
                if (!hasMagnet(player)) continue

                attractItemsToPlayer(player)
            }
        }, 20L, 2L)
    }

    private fun attractItemsToPlayer(player: Player) {
        val radius = 7.0
        val baseSpeed = 0.35

        val magnetLocation = player.location.clone().add(0.0, 1.0, 0.0)

        for (entity in player.getNearbyEntities(radius, radius, radius)) {
            if (entity !is Item) continue
            if (!entity.isValid || entity.isDead) continue

            val itemStack = entity.itemStack
            val magneticMultiplier = getMagneticMultiplier(itemStack.type) ?: continue

            val direction = magnetLocation.toVector()
                .subtract(entity.location.toVector())

            if (direction.length() <= 0.3) continue

            entity.velocity = direction
                .normalize()
                .multiply(baseSpeed * magneticMultiplier)
        }
    }

    private fun getMagneticMultiplier(material: Material): Double? {
        return when {
            material in ferromagneticMaterials -> 1.0
            material in weakMagneticMaterials -> 0.5
            else -> null
        }
    }

    private fun hasMagnet(player: Player): Boolean {
        return isMagnet(player.inventory.itemInMainHand) ||
            isMagnet(player.inventory.itemInOffHand)
    }

    private fun isMagnet(item: ItemStack?): Boolean {
        if (item == null || item.type == Material.AIR) return false

        val meta = item.itemMeta ?: return false

        val isMagnet = meta.persistentDataContainer.has(
            magnetKey,
            PersistentDataType.BYTE
        )

        if (isMagnet && !hasPortableMagnetVisuals(meta)) {
            applyPortableMagnetVisuals(meta)
            item.itemMeta = meta
        }

        return isMagnet
    }

    private fun createMagnet(language: String): ItemStack {
        val item = ItemStack(Material.AMETHYST_SHARD)
        val meta = item.itemMeta

        meta.displayName(
            Component.text(message(language, "item.name"))
                .color(NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false)
        )

        meta.lore(
            messageList(language, "item.lore").mapIndexed { index, line ->
                Component.text(line)
                    .color(if (index == 0) NamedTextColor.GRAY else NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false)
            }
        )

        meta.persistentDataContainer.set(
            magnetKey,
            PersistentDataType.BYTE,
            1
        )

        applyPortableMagnetVisuals(meta)

        item.itemMeta = meta
        return item
    }

    private fun hasPortableMagnetVisuals(meta: ItemMeta): Boolean {
        return meta.hasItemModel() && meta.itemModel == portableMagnetItemModel
    }

    private fun applyPortableMagnetVisuals(meta: ItemMeta) {
        meta.setItemModel(portableMagnetItemModel)

        val customModelData = meta.customModelDataComponent
        customModelData.setFloats(listOf(PORTABLE_MAGNET_CUSTOM_MODEL_DATA.toFloat()))
        customModelData.setStrings(listOf(portableMagnetItemModel.asString()))
        meta.setCustomModelDataComponent(customModelData)
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!command.name.equals("magnet", ignoreCase = true)) {
            return false
        }

        return handleMagnetCommand(sender, args)
    }

    private fun handleMagnetCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.isEmpty() || args[0].equals("help", ignoreCase = true)) {
            sendHelp(sender)
            return true
        }

        return when (args[0].lowercase(Locale.ROOT)) {
            "give" -> handleGive(sender)
            "reload" -> handleReload(sender)
            "debug" -> handleDebug(sender, args)
            "core" -> handleCore(sender, args)
            "profile" -> handleProfile(sender, args)
            else -> {
                send(sender, "command.unknown", NamedTextColor.RED)
                sendHelp(sender)
                true
            }
        }
    }

    private fun handleReload(sender: CommandSender): Boolean {
        loadMessages()
        coreManager.reload()
        validateResourcePackConfiguration()
        send(sender, "command.reloaded", NamedTextColor.GREEN)
        return true
    }

    private fun handleGive(sender: CommandSender): Boolean {
        if (sender !is Player) {
            send(sender, "command.player-only", NamedTextColor.RED)
            return true
        }

        val language = languageFor(sender)

        sender.inventory.addItem(createMagnet(language))
        send(sender, "command.received", NamedTextColor.GREEN)

        return true
    }

    private fun handleCore(sender: CommandSender, args: Array<out String>): Boolean {
        return when (args.getOrNull(1)?.lowercase(Locale.ROOT)) {
            "create" -> handleCoreCreate(sender, args)
            "createat" -> handleCoreCreateAt(sender, args)
            "remove", "delete" -> handleCoreRemove(sender, args)
            "list" -> handleCoreList(sender)
            "info", "check" -> handleCoreInfo(sender, args)
            "rescan" -> handleCoreRescan(sender, args)
            "refresh" -> handleCoreRefresh(sender, args)
            "override" -> handleCoreOverride(sender, args)
            "set" -> handleCoreSet(sender, args)
            "reload" -> handleCoreReload(sender)
            else -> {
                send(sender, "command.core-usage", NamedTextColor.YELLOW)
                true
            }
        }
    }

    private fun handleProfile(sender: CommandSender, args: Array<out String>): Boolean {
        return when (args.getOrNull(1)?.lowercase(Locale.ROOT)) {
            "list" -> handleProfileList(sender)
            "info" -> handleProfileInfo(sender, args)
            "set" -> handleProfileSet(sender, args)
            "reload" -> handleProfileReload(sender)
            else -> {
                send(sender, "command.profile-usage", NamedTextColor.YELLOW)
                true
            }
        }
    }

    private fun handleDebug(sender: CommandSender, args: Array<out String>): Boolean {
        return when (args.getOrNull(1)?.lowercase(Locale.ROOT)) {
            "item" -> handleDebugItem(sender)
            else -> {
                send(sender, "command.debug-usage", NamedTextColor.YELLOW)
                true
            }
        }
    }

    private fun handleDebugItem(sender: CommandSender): Boolean {
        if (sender !is Player) {
            send(sender, "command.player-only", NamedTextColor.RED)
            return true
        }

        val item = sender.inventory.itemInMainHand
        val meta = item.itemMeta
        val hasMarker = meta?.persistentDataContainer?.has(
            magnetKey,
            PersistentDataType.BYTE
        ) == true
        val isPortableMagnet = item.type == Material.AMETHYST_SHARD && hasMarker
        val itemModelKey = if (meta?.hasItemModel() == true) {
            meta.itemModel?.asString() ?: "none"
        } else {
            "none"
        }
        val customModelData = if (meta?.hasCustomModelDataComponent() == true) {
            formatCustomModelData(meta.customModelDataComponent)
        } else {
            "none"
        }
        val legacyCustomModelData = meta?.let { legacyCustomModelData(it)?.toString() } ?: "none"

        sender.sendMessage(Component.text("Portable Magnet item debug").color(NamedTextColor.AQUA))
        sender.sendMessage(Component.text("Is portable magnet: $isPortableMagnet").color(debugColor(isPortableMagnet)))
        sender.sendMessage(Component.text("Base material: ${item.type.name}").color(NamedTextColor.GRAY))
        sender.sendMessage(Component.text("PersistentDataContainer marker: $hasMarker").color(debugColor(hasMarker)))
        sender.sendMessage(Component.text("Item model key: $itemModelKey").color(debugColor(itemModelKey == EXPECTED_PORTABLE_MAGNET_MODEL_KEY)))
        sender.sendMessage(Component.text("Expected model key: $EXPECTED_PORTABLE_MAGNET_MODEL_KEY").color(NamedTextColor.GRAY))
        sender.sendMessage(Component.text("Custom model data component: $customModelData").color(NamedTextColor.GRAY))
        sender.sendMessage(Component.text("Legacy custom model data: $legacyCustomModelData").color(NamedTextColor.GRAY))
        sender.sendMessage(Component.text("Plugin version: ${pluginMeta.version}").color(NamedTextColor.GRAY))

        return true
    }

    private fun handleCoreCreate(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            send(sender, "command.player-only", NamedTextColor.RED)
            return true
        }

        val id = args.getOrNull(2)?.lowercase(Locale.ROOT)
        if (id == null) {
            send(sender, "command.core-create-usage", NamedTextColor.YELLOW)
            return true
        }

        if (!coreIdPattern.matches(id)) {
            send(sender, "command.core-id-invalid", NamedTextColor.RED)
            return true
        }

        if (coreManager.exists(id)) {
            send(sender, "command.core-already-exists", NamedTextColor.RED, "id" to id)
            return true
        }

        val targetBlock = sender.getTargetBlockExact(10)
        if (targetBlock == null || !coreManager.isCoreMaterial(targetBlock.type)) {
            send(sender, "command.core-need-material", NamedTextColor.RED)
            return true
        }

        val radiusOverride = args.getOrNull(3)?.let { parseRadius(sender, it) ?: return true }
        val strengthOverride = args.getOrNull(4)?.let { parseStrength(sender, it) ?: return true }
        val result = coreManager.create(id, targetBlock, radiusOverride, strengthOverride)
        if (result == null) {
            send(sender, "command.core-2x2x2-not-found", NamedTextColor.RED)
            return true
        }
        val core = result.core

        send(
            sender,
            "command.core-created",
            NamedTextColor.GREEN,
            "id" to core.id,
            "world" to core.worldName,
            "x" to formatDouble(core.centerX),
            "y" to formatDouble(core.centerY),
            "z" to formatDouble(core.centerZ),
            "radius" to formatDouble(core.radius),
            "strength" to formatDouble(core.strength),
            "profile" to core.coreProfile,
            "calculatedRadius" to formatDouble(core.calculatedRadius),
            "calculatedStrength" to formatDouble(core.calculatedStrength),
            "override" to core.manualOverride.toString(),
            "critical" to core.criticalFrameBlocks.size.toString(),
            "optional" to core.optionalFrameBlocks.size.toString()
        )
        sendFrameWarning(sender, result.frameWarning)

        return true
    }

    private fun handleCoreCreateAt(sender: CommandSender, args: Array<out String>): Boolean {
        val id = args.getOrNull(2)?.lowercase(Locale.ROOT)
        if (id == null) {
            send(sender, "command.core-createat-usage", NamedTextColor.YELLOW)
            return true
        }

        if (!coreIdPattern.matches(id)) {
            send(sender, "command.core-id-invalid", NamedTextColor.RED)
            return true
        }

        if (coreManager.exists(id)) {
            send(sender, "command.core-already-exists", NamedTextColor.RED, "id" to id)
            return true
        }

        val parsed = parseCreateAtArguments(sender, args) ?: return true
        val world = Bukkit.getWorld(parsed.worldName)
        if (world == null) {
            send(sender, "command.world-not-found", NamedTextColor.RED, "world" to parsed.worldName)
            return true
        }

        val block = world.getBlockAt(parsed.x, parsed.y, parsed.z)
        if (!coreManager.isCoreMaterial(block.type)) {
            send(
                sender,
                "command.core-createat-not-material",
                NamedTextColor.RED,
                "material" to block.type.name
            )
            return true
        }

        val result = coreManager.createAt(
            id = id,
            worldName = world.name,
            x = parsed.x,
            y = parsed.y,
            z = parsed.z,
            radiusOverride = parsed.radiusOverride,
            strengthOverride = parsed.strengthOverride
        )
        if (result == null) {
            send(sender, "command.core-2x2x2-not-found", NamedTextColor.RED)
            return true
        }
        val core = result.core

        send(
            sender,
            "command.core-created",
            NamedTextColor.GREEN,
            "id" to core.id,
            "world" to core.worldName,
            "x" to formatDouble(core.centerX),
            "y" to formatDouble(core.centerY),
            "z" to formatDouble(core.centerZ),
            "radius" to formatDouble(core.radius),
            "strength" to formatDouble(core.strength),
            "profile" to core.coreProfile,
            "calculatedRadius" to formatDouble(core.calculatedRadius),
            "calculatedStrength" to formatDouble(core.calculatedStrength),
            "override" to core.manualOverride.toString(),
            "critical" to core.criticalFrameBlocks.size.toString(),
            "optional" to core.optionalFrameBlocks.size.toString()
        )
        sendFrameWarning(sender, result.frameWarning)

        return true
    }

    private fun parseCreateAtArguments(
        sender: CommandSender,
        args: Array<out String>
    ): ParsedCreateAt? {
        val firstNumberIndex = args.indexOfFirstFrom(3) { it.toIntOrNull() != null }
        if (firstNumberIndex == -1) {
            send(sender, "command.core-createat-usage", NamedTextColor.YELLOW)
            return null
        }

        val worldName = if (firstNumberIndex == 3) {
            val player = sender as? Player
            if (player == null) {
                send(sender, "command.core-createat-console-usage", NamedTextColor.YELLOW)
                return null
            }

            player.world.name
        } else {
            args[3]
        }

        val x = args.getOrNull(firstNumberIndex)?.toIntOrNull()
        val y = args.getOrNull(firstNumberIndex + 1)?.toIntOrNull()
        val z = args.getOrNull(firstNumberIndex + 2)?.toIntOrNull()
        if (x == null || y == null || z == null) {
            send(sender, "command.core-createat-usage", NamedTextColor.YELLOW)
            return null
        }

        val radiusOverride = args.getOrNull(firstNumberIndex + 3)?.let {
            parseRadius(sender, it) ?: return null
        }
        val strengthOverride = args.getOrNull(firstNumberIndex + 4)?.let {
            parseStrength(sender, it) ?: return null
        }

        return ParsedCreateAt(worldName, x, y, z, radiusOverride, strengthOverride)
    }

    private fun parseRadius(sender: CommandSender, raw: String): Double? {
        val requested = raw.toDoubleOrNull()
        if (requested == null || !java.lang.Double.isFinite(requested) || requested <= 0.0) {
            send(sender, "command.invalid-number", NamedTextColor.RED)
            return null
        }

        val limited = coreManager.limitRadius(requested)
        if (limited < requested) {
            send(
                sender,
                "command.core-radius-clamped",
                NamedTextColor.YELLOW,
                "radius" to formatDouble(limited)
            )
        }

        return limited
    }

    private fun parseStrength(sender: CommandSender, raw: String): Double? {
        val requested = raw.toDoubleOrNull()
        if (requested == null || !java.lang.Double.isFinite(requested) || requested <= 0.0) {
            send(sender, "command.invalid-number", NamedTextColor.RED)
            return null
        }

        val limited = coreManager.limitStrength(requested)
        if (limited != requested) {
            send(
                sender,
                "command.core-strength-clamped",
                NamedTextColor.YELLOW,
                "strength" to formatDouble(limited)
            )
        }

        return limited
    }

    private fun handleCoreRemove(sender: CommandSender, args: Array<out String>): Boolean {
        val id = args.getOrNull(2)?.lowercase(Locale.ROOT)
        if (id == null) {
            send(sender, "command.core-remove-usage", NamedTextColor.YELLOW)
            return true
        }

        if (coreManager.remove(id) == null) {
            send(sender, "command.core-not-found", NamedTextColor.RED, "id" to id)
            return true
        }

        send(sender, "command.core-removed", NamedTextColor.GREEN, "id" to id)
        return true
    }

    private fun handleCoreRescan(sender: CommandSender, args: Array<out String>): Boolean {
        val id = args.getOrNull(2)?.lowercase(Locale.ROOT)
        if (id == null) {
            send(sender, "command.core-rescan-usage", NamedTextColor.YELLOW)
            return true
        }

        val result = coreManager.rescanFrame(id)
        if (result == null) {
            send(sender, "command.core-not-found", NamedTextColor.RED, "id" to id)
            return true
        }

        if (!result.success) {
            send(
                sender,
                "command.core-frame-rescan-failed",
                NamedTextColor.RED,
                "id" to result.core.id,
                "reason" to (result.message ?: "unknown")
            )
            return true
        }

        send(
            sender,
            "command.core-frame-rescanned",
            NamedTextColor.GREEN,
            "id" to result.core.id,
            "critical" to result.criticalCount.toString(),
            "optional" to result.optionalCount.toString()
        )
        sendFrameWarning(sender, result.message)
        return true
    }

    private fun handleCoreRefresh(sender: CommandSender, args: Array<out String>): Boolean {
        val id = args.getOrNull(2)?.lowercase(Locale.ROOT)
        if (id == null) {
            send(sender, "command.core-refresh-usage", NamedTextColor.YELLOW)
            return true
        }

        val result = coreManager.refreshCore(id)
        if (result == null) {
            send(sender, "command.core-not-found", NamedTextColor.RED, "id" to id)
            return true
        }

        if (!result.success) {
            send(
                sender,
                "command.core-refresh-failed",
                NamedTextColor.RED,
                "id" to result.core.id,
                "reason" to (result.message ?: "unknown")
            )
            return true
        }

        send(
            sender,
            "command.core-refreshed",
            NamedTextColor.GREEN,
            "id" to result.core.id,
            "profile" to result.core.coreProfile,
            "radius" to formatDouble(result.core.radius),
            "strength" to formatDouble(result.core.strength)
        )
        return true
    }

    private fun handleCoreOverride(sender: CommandSender, args: Array<out String>): Boolean {
        val id = args.getOrNull(2)?.lowercase(Locale.ROOT)
        val enabled = args.getOrNull(3)?.toBooleanStrictOrNull()
        if (id == null || enabled == null) {
            send(sender, "command.core-override-usage", NamedTextColor.YELLOW)
            return true
        }

        val result = coreManager.setCoreOverride(id, enabled)
        if (result == null) {
            send(sender, "command.core-not-found", NamedTextColor.RED, "id" to id)
            return true
        }
        if (!result.success) {
            send(
                sender,
                "command.core-refresh-failed",
                NamedTextColor.RED,
                "id" to result.core.id,
                "reason" to (result.message ?: "unknown")
            )
            return true
        }

        send(
            sender,
            "command.core-override-updated",
            NamedTextColor.GREEN,
            "id" to result.core.id,
            "override" to result.core.manualOverride.toString(),
            "radius" to formatDouble(result.core.radius),
            "strength" to formatDouble(result.core.strength)
        )
        return true
    }

    private fun handleCoreSet(sender: CommandSender, args: Array<out String>): Boolean {
        val id = args.getOrNull(2)?.lowercase(Locale.ROOT)
        val field = args.getOrNull(3)?.lowercase(Locale.ROOT)
        val value = args.getOrNull(4)
        if (id == null || field == null || value == null) {
            send(sender, "command.core-set-usage", NamedTextColor.YELLOW)
            return true
        }

        val result = when (field) {
            "radius" -> {
                val radius = parseRadius(sender, value) ?: return true
                coreManager.setCoreRadius(id, radius)
            }
            "strength" -> {
                val strength = parseStrength(sender, value) ?: return true
                coreManager.setCoreStrength(id, strength)
            }
            else -> {
                send(sender, "command.core-set-usage", NamedTextColor.YELLOW)
                return true
            }
        }

        if (result == null) {
            send(sender, "command.core-not-found", NamedTextColor.RED, "id" to id)
            return true
        }

        send(
            sender,
            "command.core-set-updated",
            NamedTextColor.GREEN,
            "id" to result.core.id,
            "radius" to formatDouble(result.core.radius),
            "strength" to formatDouble(result.core.strength)
        )
        return true
    }

    private fun handleCoreList(sender: CommandSender): Boolean {
        val cores = coreManager.all()
        if (cores.isEmpty()) {
            send(sender, "command.core-list-empty", NamedTextColor.GRAY)
            return true
        }

        send(sender, "command.core-list-header", NamedTextColor.AQUA)
        for (core in cores) {
            val activity = if (core.active && !core.damaged) "active" else "inactive"
            val frameState = if (core.damaged) {
                "damaged"
            } else if (core.criticalFrameBlocks.isNotEmpty()) {
                "ok"
            } else {
                "missing"
            }
            val line = if (core.damaged) {
                "${core.id} | $activity | damaged | reason: ${core.damageReason ?: "unknown"}"
            } else {
                "${core.id} | $activity | profile: ${core.coreProfile} | " +
                    "radius: ${formatDouble(core.radius)} | strength: ${formatDouble(core.strength)} | " +
                    "calculated: ${formatDouble(core.calculatedRadius)}/${formatDouble(core.calculatedStrength)} | " +
                    "override: ${core.manualOverride} | frame: $frameState | " +
                    "critical: ${core.criticalFrameBlocks.size} | optional: ${core.optionalFrameBlocks.size}"
            }
            sender.sendMessage(
                Component.text(line).color(if (core.damaged) NamedTextColor.RED else NamedTextColor.GRAY)
            )
        }

        return true
    }

    private fun handleCoreInfo(sender: CommandSender, args: Array<out String>): Boolean {
        val id = args.getOrNull(2)?.lowercase(Locale.ROOT)
        if (id == null) {
            send(sender, "command.core-info-usage", NamedTextColor.YELLOW)
            return true
        }

        val inspection = coreManager.inspect(id)
        if (inspection == null) {
            send(sender, "command.core-not-found", NamedTextColor.RED, "id" to id)
            return true
        }

        val core = inspection.core
        sender.sendMessage(
            Component.text(
                "Core ${core.id}:"
            ).color(NamedTextColor.AQUA)
        )
        sender.sendMessage(
            Component.text(
                "Status: ${if (core.active && !core.damaged) "active" else "inactive"}, damaged=${core.damaged}"
            ).color(if (core.active && !core.damaged) NamedTextColor.GREEN else NamedTextColor.RED)
        )
        sender.sendMessage(
            Component.text(
                "World: ${core.worldName}, center: " +
                    "${formatDouble(core.centerX)} ${formatDouble(core.centerY)} ${formatDouble(core.centerZ)}"
            ).color(NamedTextColor.GRAY)
        )
        sender.sendMessage(
            Component.text(
                "Profile: ${core.coreProfile}, radius: ${formatDouble(core.radius)}, " +
                    "strength: ${formatDouble(core.strength)}, calculated: " +
                    "${formatDouble(core.calculatedRadius)} / ${formatDouble(core.calculatedStrength)}, " +
                    "override: ${core.manualOverride}, core-size: ${core.coreSize}"
            ).color(NamedTextColor.GRAY)
        )
        sender.sendMessage(
            Component.text(
                "World loaded=${inspection.worldLoaded}, chunk loaded=${inspection.chunkLoaded}, " +
                    "core complete=${inspection.coreComplete}"
            ).color(if (inspection.coreComplete) NamedTextColor.GREEN else NamedTextColor.RED)
        )
        sender.sendMessage(
            Component.text(
                "Frame complete=${inspection.frameComplete}, critical=${core.criticalFrameBlocks.size}, " +
                    "optional=${core.optionalFrameBlocks.size}, reason=${inspection.frameReason ?: core.damageReason ?: "none"}"
            ).color(if (inspection.frameComplete && !core.damaged) NamedTextColor.GREEN else NamedTextColor.RED)
        )
        sender.sendMessage(Component.text("Core blocks:").color(NamedTextColor.AQUA))
        for ((index, position) in core.coreBlocks.withIndex()) {
            val material = inspection.blockTypes.getOrNull(index)?.name ?: "UNKNOWN"
            sender.sendMessage(
                Component.text("- ${core.worldName} ${position.x} ${position.y} ${position.z} $material")
                    .color(if (material == "UNKNOWN") NamedTextColor.RED else NamedTextColor.GRAY)
            )
        }
        sender.sendMessage(
            Component.text(
                "Nearby dropped items=${inspection.nearbyItems}, magnetic items=${inspection.magneticItems}"
            ).color(if (inspection.magneticItems > 0) NamedTextColor.GREEN else NamedTextColor.YELLOW)
        )

        return true
    }

    private fun handleProfileList(sender: CommandSender): Boolean {
        val profiles = coreManager.coreMaterialProfiles()
        if (profiles.isEmpty()) {
            send(sender, "command.profile-list-empty", NamedTextColor.GRAY)
            return true
        }

        send(sender, "command.profile-list-header", NamedTextColor.AQUA)
        for (profile in profiles) {
            sender.sendMessage(
                Component.text(
                    "${profile.material.name} | profile: ${profile.profile} | " +
                        "radius: ${formatDouble(profile.baseRadius)} | " +
                        "strength: ${formatDouble(profile.baseStrength)} | priority: ${profile.priority}"
                ).color(NamedTextColor.GRAY)
            )
        }
        return true
    }

    private fun handleProfileInfo(sender: CommandSender, args: Array<out String>): Boolean {
        val materialName = args.getOrNull(2)
        if (materialName == null) {
            send(sender, "command.profile-info-usage", NamedTextColor.YELLOW)
            return true
        }

        val profile = coreManager.coreMaterialProfile(materialName)
        if (profile == null) {
            send(sender, "command.profile-not-found", NamedTextColor.RED, "material" to materialName.uppercase(Locale.ROOT))
            return true
        }

        sender.sendMessage(
            Component.text(
                "${profile.material.name}: profile=${profile.profile}, " +
                    "radius=${formatDouble(profile.baseRadius)}, " +
                    "strength=${formatDouble(profile.baseStrength)}, priority=${profile.priority}"
            ).color(NamedTextColor.AQUA)
        )
        return true
    }

    private fun handleProfileSet(sender: CommandSender, args: Array<out String>): Boolean {
        val materialName = args.getOrNull(2)
        val field = args.getOrNull(3)?.lowercase(Locale.ROOT)
        val value = args.getOrNull(4)
        if (materialName == null || field == null || value == null) {
            send(sender, "command.profile-set-usage", NamedTextColor.YELLOW)
            return true
        }

        val profile = when (field) {
            "radius" -> {
                val radius = parseRadius(sender, value) ?: return true
                coreManager.setMaterialProfileRadius(materialName, radius)
            }
            "strength" -> {
                val strength = parseStrength(sender, value) ?: return true
                coreManager.setMaterialProfileStrength(materialName, strength)
            }
            "priority" -> {
                val priority = value.toIntOrNull()
                if (priority == null) {
                    send(sender, "command.invalid-number", NamedTextColor.RED)
                    return true
                }
                coreManager.setMaterialProfilePriority(materialName, priority)
            }
            else -> {
                send(sender, "command.profile-set-usage", NamedTextColor.YELLOW)
                return true
            }
        }

        if (profile == null) {
            send(sender, "command.profile-not-found", NamedTextColor.RED, "material" to materialName.uppercase(Locale.ROOT))
            return true
        }

        send(
            sender,
            "command.profile-updated",
            NamedTextColor.GREEN,
            "material" to profile.material.name,
            "profile" to profile.profile,
            "radius" to formatDouble(profile.baseRadius),
            "strength" to formatDouble(profile.baseStrength),
            "priority" to profile.priority.toString()
        )
        return true
    }

    private fun handleProfileReload(sender: CommandSender): Boolean {
        val refreshed = coreManager.reloadProfiles()
        send(
            sender,
            "command.profile-reloaded",
            NamedTextColor.GREEN,
            "count" to refreshed.toString()
        )
        return true
    }

    private fun sendFrameWarning(sender: CommandSender, warning: String?) {
        if (warning == null) return

        send(
            sender,
            "command.core-frame-warning",
            NamedTextColor.YELLOW,
            "warning" to warning
        )
    }

    private fun handleCoreReload(sender: CommandSender): Boolean {
        coreManager.reload()
        send(sender, "command.core-reloaded", NamedTextColor.GREEN)
        return true
    }

    private fun sendHelp(sender: CommandSender) {
        for (line in messageList(languageFor(sender), "command.help")) {
            sender.sendMessage(Component.text(line).color(NamedTextColor.GRAY))
        }
    }

    private fun validateResourcePackConfiguration() {
        if (config.contains(RESOURCE_PACK_CONFIG_SECTION) &&
            config.getConfigurationSection(RESOURCE_PACK_CONFIG_SECTION) == null
        ) {
            logger.warning("resource-pack must be a configuration section.")
            return
        }

        val section = config.getConfigurationSection(RESOURCE_PACK_CONFIG_SECTION) ?: return
        val configuredModelKey = section.getString("model-key", EXPECTED_PORTABLE_MAGNET_MODEL_KEY)
            ?.trim()
            .orEmpty()

        if (configuredModelKey.isBlank()) {
            logger.warning(
                "resource-pack.model-key is empty; expected $EXPECTED_PORTABLE_MAGNET_MODEL_KEY."
            )
        } else if (configuredModelKey != EXPECTED_PORTABLE_MAGNET_MODEL_KEY) {
            logger.warning(
                "resource-pack.model-key is '$configuredModelKey', but Portable Magnet uses " +
                    "$EXPECTED_PORTABLE_MAGNET_MODEL_KEY. A namespace mismatch can cause missing texture."
            )
        }

        if (!section.getBoolean("enabled", false)) return

        val mode = section.getString("mode", "external")
            ?.trim()
            ?.lowercase(Locale.ROOT)
            .orEmpty()

        when (mode) {
            "external" -> {
                val url = section.getString("url")
                    ?.trim()
                    .orEmpty()
                if (url.isBlank()) {
                    logger.warning("resource-pack is enabled in config, but resource-pack.url is empty.")
                }
            }
            "builtin", "built-in", "local" -> {
                val fileName = section.getString("file")
                    ?.trim()
                    .orEmpty()
                if (fileName.isBlank()) {
                    logger.warning("resource-pack mode '$mode' is enabled, but resource-pack.file is empty.")
                    return
                }

                val file = dataFolder.resolve(fileName)
                if (!file.isFile) {
                    logger.warning(
                        "resource-pack mode '$mode' is enabled, but file '$fileName' was not found in " +
                            dataFolder.absolutePath + "."
                    )
                }
            }
            else -> {
                logger.warning("Unknown resource-pack.mode '$mode'. Use external or builtin.")
            }
        }
    }

    private fun formatCustomModelData(component: CustomModelDataComponent): String {
        val parts = mutableListOf<String>()
        if (component.floats.isNotEmpty()) {
            parts += "floats=${component.floats.joinToString()}"
        }
        if (component.strings.isNotEmpty()) {
            parts += "strings=${component.strings.joinToString()}"
        }
        if (component.flags.isNotEmpty()) {
            parts += "flags=${component.flags.joinToString()}"
        }
        if (component.colors.isNotEmpty()) {
            parts += "colors=${component.colors.joinToString()}"
        }

        return parts.joinToString("; ").ifBlank { "empty" }
    }

    @Suppress("DEPRECATION")
    private fun legacyCustomModelData(meta: ItemMeta): Int? {
        return if (meta.hasCustomModelData()) meta.customModelData else null
    }

    private fun debugColor(ok: Boolean): NamedTextColor {
        return if (ok) NamedTextColor.GREEN else NamedTextColor.RED
    }

    private fun languageFor(sender: CommandSender): String {
        val player = sender as? Player ?: return DEFAULT_LANGUAGE
        val language = player.locale().language.lowercase(Locale.ROOT)
        return if (language in supportedLanguages) language else DEFAULT_LANGUAGE
    }

    private fun message(language: String, path: String): String {
        return messages[language]?.getString(path)
            ?: messages[DEFAULT_LANGUAGE]?.getString(path)
            ?: path
    }

    private fun messageList(language: String, path: String): List<String> {
        val localized = messages[language]?.getStringList(path).orEmpty()
        if (localized.isNotEmpty()) return localized

        val fallback = messages[DEFAULT_LANGUAGE]?.getStringList(path).orEmpty()
        return fallback.ifEmpty { listOf(path) }
    }

    private fun send(
        sender: CommandSender,
        path: String,
        color: NamedTextColor,
        vararg replacements: Pair<String, String>
    ) {
        var text = message(languageFor(sender), path)
        for ((key, value) in replacements) {
            text = text.replace("{$key}", value)
        }

        sender.sendMessage(Component.text(text).color(color))
    }

    private fun formatDouble(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
    }

    private inline fun Array<out String>.indexOfFirstFrom(
        startIndex: Int,
        predicate: (String) -> Boolean
    ): Int {
        for (index in startIndex until size) {
            if (predicate(this[index])) return index
        }

        return -1
    }

    private data class ParsedCreateAt(
        val worldName: String,
        val x: Int,
        val y: Int,
        val z: Int,
        val radiusOverride: Double?,
        val strengthOverride: Double?
    )
}
