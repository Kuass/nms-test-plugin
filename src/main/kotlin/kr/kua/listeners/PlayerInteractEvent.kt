package kr.kua.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent

class PlayerInteractEvent : Listener {
    @EventHandler
    fun onPlayerInteractEvent(e: PlayerInteractEvent) {
        val p = e.player
        p.sendMessage("action: ${e.action}")

    }
}