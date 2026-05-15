package ru.garde.magnet

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
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.util.Locale

class MagnetPlugin : JavaPlugin() {
    companion object {
        private const val DEFAULT_LANGUAGE = "en"
        private val coreIdPattern = Regex("[a-z0-9_-]{1,32}")
    }

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
        magnetKey = NamespacedKey(this, "magnet")

        saveDefaultConfig()
        reloadConfig()
        config.options().copyDefaults(true)
        saveConfig()
        loadMessages()

        coreManager = MagnetCoreManager(this, ::getMagneticMultiplier)
        coreManager.load()

        startPortableMagnetTask()
        coreManager.start()

        logger.info("MagnetPlugin enabled")
    }

    override fun onDisable() {
        if (::coreManager.isInitialized) {
            coreManager.shutdown()
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

        return meta.persistentDataContainer.has(
            magnetKey,
            PersistentDataType.BYTE
        )
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

        item.itemMeta = meta
        return item
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

        if (args.isEmpty() || args[0].equals("help", ignoreCase = true)) {
            sendHelp(sender)
            return true
        }

        return when (args[0].lowercase(Locale.ROOT)) {
            "give" -> handleGive(sender)
            "core" -> handleCore(sender, args)
            else -> {
                send(sender, "command.unknown", NamedTextColor.RED)
                sendHelp(sender)
                true
            }
        }
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
            "reload" -> handleCoreReload(sender)
            else -> {
                send(sender, "command.core-usage", NamedTextColor.YELLOW)
                true
            }
        }
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
        if (targetBlock == null || !MagnetCoreManager.isCopperCoreMaterial(targetBlock.type)) {
            send(sender, "command.core-need-copper", NamedTextColor.RED)
            return true
        }

        val radius = parseRadius(sender, args.getOrNull(3)) ?: return true
        val strength = parseStrength(sender, args.getOrNull(4)) ?: return true
        val core = coreManager.create(id, targetBlock, radius, strength)

        send(
            sender,
            "command.core-created",
            NamedTextColor.GREEN,
            "id" to core.id,
            "world" to core.worldName,
            "x" to core.x.toString(),
            "y" to core.y.toString(),
            "z" to core.z.toString(),
            "radius" to formatDouble(core.radius),
            "strength" to formatDouble(core.strength)
        )

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
        if (!MagnetCoreManager.isCopperCoreMaterial(block.type)) {
            send(
                sender,
                "command.core-createat-not-copper",
                NamedTextColor.RED,
                "material" to block.type.name
            )
            return true
        }

        val core = coreManager.createAt(
            id = id,
            worldName = world.name,
            x = parsed.x,
            y = parsed.y,
            z = parsed.z,
            radius = parsed.radius,
            strength = parsed.strength
        )

        send(
            sender,
            "command.core-created",
            NamedTextColor.GREEN,
            "id" to core.id,
            "world" to core.worldName,
            "x" to core.x.toString(),
            "y" to core.y.toString(),
            "z" to core.z.toString(),
            "radius" to formatDouble(core.radius),
            "strength" to formatDouble(core.strength)
        )

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

        val radius = parseRadius(sender, args.getOrNull(firstNumberIndex + 3)) ?: return null
        val strength = parseStrength(sender, args.getOrNull(firstNumberIndex + 4)) ?: return null

        return ParsedCreateAt(worldName, x, y, z, radius, strength)
    }

    private fun parseRadius(sender: CommandSender, raw: String?): Double? {
        if (raw == null) return coreManager.defaultCoreRadius

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

    private fun parseStrength(sender: CommandSender, raw: String?): Double? {
        if (raw == null) return coreManager.defaultCoreStrength

        val strength = raw.toDoubleOrNull()
        if (strength == null || !java.lang.Double.isFinite(strength) || strength <= 0.0) {
            send(sender, "command.invalid-number", NamedTextColor.RED)
            return null
        }

        return strength
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

    private fun handleCoreList(sender: CommandSender): Boolean {
        val cores = coreManager.all()
        if (cores.isEmpty()) {
            send(sender, "command.core-list-empty", NamedTextColor.GRAY)
            return true
        }

        send(sender, "command.core-list-header", NamedTextColor.AQUA)
        for (core in cores) {
            sender.sendMessage(
                Component.text(
                    "- ${core.id}: ${core.worldName} ${core.x} ${core.y} ${core.z}, " +
                        "radius=${formatDouble(core.radius)}, strength=${formatDouble(core.strength)}"
                ).color(NamedTextColor.GRAY)
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
                "Core ${core.id}: ${core.worldName} ${core.x} ${core.y} ${core.z}, " +
                    "radius=${formatDouble(core.radius)}, strength=${formatDouble(core.strength)}"
            ).color(NamedTextColor.AQUA)
        )
        sender.sendMessage(
            Component.text(
                "World loaded=${inspection.worldLoaded}, chunk loaded=${inspection.chunkLoaded}, " +
                    "block=${inspection.blockType?.name ?: "UNKNOWN"}, copper=${inspection.copperCoreBlock}"
            ).color(if (inspection.copperCoreBlock) NamedTextColor.GREEN else NamedTextColor.RED)
        )
        sender.sendMessage(
            Component.text(
                "Nearby dropped items=${inspection.nearbyItems}, magnetic items=${inspection.magneticItems}"
            ).color(if (inspection.magneticItems > 0) NamedTextColor.GREEN else NamedTextColor.YELLOW)
        )

        return true
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
        val radius: Double,
        val strength: Double
    )
}
