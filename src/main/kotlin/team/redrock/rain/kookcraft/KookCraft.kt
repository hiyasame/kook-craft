package team.redrock.rain.kookcraft

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import love.forte.simbot.component.kook.KookComponentBot
import love.forte.simbot.component.kook.event.KookChannelMessageEvent
import love.forte.simbot.component.kook.kookBots
import love.forte.simbot.component.kook.useKook
import love.forte.simbot.core.application.SimpleApplication
import love.forte.simbot.core.application.createSimpleApplication
import love.forte.simbot.core.event.listeners
import org.bukkit.Bukkit
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.registerBukkitListener
import taboolib.common.platform.function.submit
import taboolib.module.chat.colored
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import kotlin.coroutines.cancellation.CancellationException

object KookCraft : Plugin() {

    private val pluginScope = CoroutineScope(Dispatchers.IO)

    @Config(value = "config.yml")
    lateinit var config: Configuration
        private set

    private val kookSidePattern: String
        get() = config.getString("message-pattern.kook-side", "%name%: %message%")!!

    private val mcSidePattern: String
        get() = config.getString("message-pattern.mc-side", "&7%name% &8(Kook): &f%message%")!!.colored()

    private val onlineCountPattern: String
        get() = config.getString("online-count.pattern", "在线人数: %count%")!!

    private val token: String
        get() = config.getString("token", "")!!

    override fun onEnable() {
        pluginScope.launch {
            val application = createSimpleApplication {
                useKook()
                kookBots {
                    val bot = register("kook", token)
                    bot.setupChat()
                    bot.setupOnlineCount()
                    bot.start()
                }
            }
            application.setupChat()
        }
    }

    override fun onDisable() {
        pluginScope.cancel()
    }

    private fun KookComponentBot.setupOnlineCount() {
        fun refresh() {
            val onlineCountPatternPrefix = onlineCountPattern.split("%count%")[0]
            guildList.flatMap { it.channelList }
                .filter { it.name.startsWith(onlineCountPatternPrefix) }
                .forEach { channel ->
                    pluginScope.launch {
                        try {
                            channel.rename(
                                onlineCountPattern.replace("%count%", Bukkit.getOnlinePlayers().size.toString()),
                                token
                            )
                        } catch (e: CancellationException) {
                            throw e
                        } catch (t: Throwable) {
                            t.printStackTrace()
                        }
                    }
                }
        }
        registerBukkitListener(PlayerJoinEvent::class.java) {
            refresh()
        }
        registerBukkitListener(PlayerQuitEvent::class.java) {
            submit(delay = 20) {
                refresh()
            }
        }
    }

    private fun KookComponentBot.setupChat() {
        registerBukkitListener(AsyncPlayerChatEvent::class.java) { event ->
            guildList.flatMap {
                it.channelList
            }.filter { it.name == config.getString("channel") }.forEach { channel ->
                pluginScope.launch {
                    try {
                        channel.send(
                            kookSidePattern.replace("%name%", event.player.name)
                                .replace("%message%", event.message)
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                }
            }
        }
    }

    private fun SimpleApplication.setupChat() {
        eventListenerManager.listeners {
            KookChannelMessageEvent { e ->
                Bukkit.broadcastMessage(
                    mcSidePattern.replace("%name%", e.author().nickname)
                        .replace("%message%", e.messageContent.plainText)
                )
            }
        }
    }
}