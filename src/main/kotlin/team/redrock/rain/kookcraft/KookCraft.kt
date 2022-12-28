package team.redrock.rain.kookcraft

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import love.forte.simbot.component.kook.event.KookChannelMessageEvent
import love.forte.simbot.component.kook.kookBots
import love.forte.simbot.component.kook.useKook
import love.forte.simbot.core.application.createSimpleApplication
import love.forte.simbot.core.event.listeners
import org.bukkit.Bukkit
import org.bukkit.event.player.AsyncPlayerChatEvent
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.registerBukkitListener
import taboolib.module.chat.colored
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import kotlin.coroutines.cancellation.CancellationException

//@RuntimeDependencies(
//    RuntimeDependency(
//        value = "!love.forte.simbot.component:simbot-component-kook-core:3.0.0.0-alpha.3",
//        relocate = ["!kotlin.", "!kotlin@kotlin_version_escape@."]
//    ),
//    RuntimeDependency(
//        value = "!love.forte.simbot:simbot-core:3.0.0-M5",
//        relocate = ["!kotlin.", "!kotlin@kotlin_version_escape@."]
//    ),
//    RuntimeDependency(
//        value = "!org.jetbrains.kotlin:kotlin-reflect:1.7.10",
//        relocate = ["!kotlin.", "!kotlin@kotlin_version_escape@."]
//    )
//)
object KookCraft : Plugin() {

    private val pluginScope = CoroutineScope(Dispatchers.IO)

    @Config(value = "config.yml")
    lateinit var config: Configuration
        private set

    private val kookSidePattern: String
        get() = config.getString("message-pattern.kook-side", "%name%: %message%")!!

    private val mcSidePattern: String
        get() = config.getString("message-pattern.mc-side", "&7%name% &8(Kook): &f%message%")!!.colored()

    override fun onEnable() {
        pluginScope.launch {
            val application = createSimpleApplication {
                useKook()
                kookBots {
                    val bot = register("kook", config.getString("token", "")!!)
                    registerBukkitListener(AsyncPlayerChatEvent::class.java) { event ->
                        bot.guildList.flatMap {
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
                    bot.start()
                }
            }
            application.eventListenerManager.listeners {
                KookChannelMessageEvent { e ->
                    Bukkit.broadcastMessage(
                        mcSidePattern.replace("%name%", e.author().nickname)
                            .replace("%message%", e.messageContent.plainText)
                    )
                }
            }
        }
    }

    override fun onDisable() {
        pluginScope.cancel()
    }
}