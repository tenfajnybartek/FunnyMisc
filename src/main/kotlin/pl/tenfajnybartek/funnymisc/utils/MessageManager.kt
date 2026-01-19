package pl.tenfajnybartek.funnymisc.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin

/**
 * Manager obsługujący wiadomości z Adventure API (MiniMessage)
 */
class MessageManager(private val plugin: FunnyPlugin) {

    private val miniMessage = MiniMessage.miniMessage()
    private val config: FileConfiguration
        get() = plugin.config

    /**
     * Pobiera wiadomość z configu i parsuje ją przez MiniMessage
     */
    fun getMessage(path: String, vararg placeholders: Pair<String, String>): Component {
        val message = config.getString(path) ?: return Component.text("Missing message: $path")

        if (placeholders.isEmpty()) {
            return miniMessage.deserialize(message)
        }

        val resolvers = placeholders.map { (key, value) ->
            Placeholder.unparsed(key, value)
        }.toTypedArray()

        return miniMessage.deserialize(message, *resolvers)
    }

    /**
     * Wysyła wiadomość do gracza/console z prefiksem
     */
    fun sendMessage(sender: CommandSender, path: String, vararg placeholders: Pair<String, String>) {
        val prefix = getMessage("messages.prefix")
        val message = getMessage(path, *placeholders)
        sender.sendMessage(prefix.append(message))
    }

    /**
     * Wysyła wiadomość bez prefiksu
     */
    fun sendMessageNoPrefix(sender: CommandSender, path: String, vararg placeholders: Pair<String, String>) {
        val message = getMessage(path, *placeholders)
        sender.sendMessage(message)
    }

    /**
     * Wysyła raw Component do gracza
     */
    fun sendComponent(sender: CommandSender, component: Component) {
        sender.sendMessage(component)
    }

    /**
     * Parsuje tekst przez MiniMessage
     */
    fun parse(text: String): Component {
        return miniMessage.deserialize(text)
    }

    /**
     * Parsuje tekst przez MiniMessage z placeholderami
     */
    fun parse(text: String, vararg placeholders: Pair<String, String>): Component {
        if (placeholders.isEmpty()) {
            return miniMessage.deserialize(text)
        }

        val resolvers = placeholders.map { (key, value) ->
            Placeholder.unparsed(key, value)
        }.toTypedArray()

        return miniMessage.deserialize(text, *resolvers)
    }

    /**
     * Pobiera listę wiadomości (np. lore) i parsuje każdą przez MiniMessage
     */
    fun getMessageList(path: String): List<Component> {
        val messages = config.getStringList(path)
        return messages.map { miniMessage.deserialize(it) }
    }
}
