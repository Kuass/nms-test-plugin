package kr.kua.listeners

import kr.kua.Main
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class PlayerJoin : Listener {
    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        Main.plugin.corpses.registerPacketListener(e.player)
        for (data in Main.plugin.corpses.allCorpses) {
            if (data.origLocation == null) {
                return
            }
            if (data.origLocation!!.world == e.player.location.world) {
                data.setCanSee(e.player, false)
                data.tickPlayerLater(35, e.player)
            }
        }
    }
}