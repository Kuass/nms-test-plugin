package kr.kua.listeners

import kr.kua.Main
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerRespawnEvent

class PlayerRespawn : Listener {
    @EventHandler
    fun onPlayerRespawn(e: PlayerRespawnEvent) {
        for (data in Main.plugin.corpses.allCorpses) {
            if (data.origLocation == null) {
                return
            }
            if (data.origLocation!!.world == e.respawnLocation.world) {
                data.setCanSee(e.player, false)
                data.tickPlayerLater(35, e.player)
            }
        }
    }
}