package kr.kua.listeners

import kr.kua.Main
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent

class PlayerChangedWorld : Listener {
    @EventHandler
    fun onPlayerChangedWorld(e: PlayerChangedWorldEvent) {
        for (data in Main.plugin.corpses.allCorpses) {
            if (data.origLocation == null) {
                return
            }
            if (data.origLocation!!.world == e.player.world) {
                data.setCanSee(e.player, false)
                data.tickPlayerLater(35, e.player)
            }
        }
    }
}