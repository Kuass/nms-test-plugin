package kr.kua

import kr.entree.spigradle.annotations.PluginMain
import kr.kua.commands.Wak
import kr.kua.commands.WakTabComplete
import kr.kua.listeners.*
import kr.kua.nms.Corpses
import kr.kua.nms.v1_16_5R1.NMSCorpses_v1_16_5_R1
import org.bukkit.Bukkit
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.JavaPluginLoader
import java.io.File


@PluginMain
class Main : JavaPlugin {
    constructor() : super()

    // Constructor for MockBukkit. if you don't need it, use: "class SpigradleSample() : JavaPlugin()" without both constructors.
    constructor(loader: JavaPluginLoader, description: PluginDescriptionFile, dataFolder: File, file: File?)
            : super(loader, description, dataFolder, file ?: File(""))

    lateinit var corpses: Corpses

    companion object {
        lateinit var plugin: Main
//        private lateinit var plugin: Main
    }

    override fun onEnable() {
        plugin = this
        corpses = NMSCorpses_v1_16_5_R1()

        if(!Bukkit.getPluginManager().isPluginEnabled("FurnitureLib")) {
            Bukkit.getPluginManager().disablePlugin(this);
        }

        val pm = server.pluginManager
        pm.registerEvents(PlayerJoin(), this)
        pm.registerEvents(PlayerRespawn(), this)
        pm.registerEvents(PlayerChangedWorld(), this)
        pm.registerEvents(PlayerDeath(), this)
        pm.registerEvents(PlayerInteractEvent(), this)

        getCommand("wak")!!.setExecutor(Wak())
        this.getCommand("wak")!!.tabCompleter = WakTabComplete()

        logger.info("Wakgood Plugin은 성공적으로 로드되었습니다.");
    }

    override fun onDisable() {

    }

}