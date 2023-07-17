package kr.kua.listeners

import kr.kua.Main
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent

class PlayerDeath : Listener {
    @EventHandler
    fun onPlayerDeath(e: PlayerDeathEvent) {
        val inv = Bukkit.getServer().createInventory(null, 54, e.entity.name + "의 아이템")
        val facing = yawToFacing(e.entity.location.yaw)

        for (itemStack in e.drops) {
            if (itemStack != null) {
                inv.addItem(itemStack)
            }
        }

        Main.plugin.corpses.spawnCorpse(
            e.entity, null,
            offsetLocationFacing(e.entity.location, facing), inv, facing
        ).selectedSlot = e.entity.inventory.heldItemSlot
        e.drops.clear()
    }

    private fun yawToFacing(yaw: Float): Int = when {
        yaw >= -45 && yaw <= 45 -> ROTATION_SOUTH
        yaw in 45.0..135.0 -> ROTATION_WEST
        yaw <= -45 && yaw >= -135 -> ROTATION_EAST
        yaw <= -135 && yaw >= -225 -> ROTATION_NORTH
        yaw <= -225 && yaw >= -315 -> ROTATION_WEST
        yaw in 135.0..225.0 -> ROTATION_NORTH
        yaw in 225.0..315.0 -> ROTATION_EAST
        yaw >= 315 -> ROTATION_SOUTH
        yaw <= -315 -> ROTATION_SOUTH
        else -> ROTATION_NORTH
    }

    private val offset = 2f

    //NON CLIPABLE LOCATION?!?!?
    private fun offsetLocationFacing(loc: Location, facing: Int): Location { //TODO: Fix
        var newLoc = loc.clone()
        newLoc = newLoc.add(0.0, 0.0, offset.toDouble())

//		if(facing == CorpseAPI.ROTATION_SOUTH) {
//			newLoc = newLoc.add(0, 0, offset);
//		}
//		else if(facing == CorpseAPI.ROTATION_EAST) {
//			newLoc = newLoc.add(offset, 0, 0);
//		}
//		else if(facing == CorpseAPI.ROTATION_NORTH) {
//			newLoc = newLoc.subtract(0, 0, offset);
//		}
//		else if(facing == CorpseAPI.ROTATION_WEST) {
//			newLoc = newLoc.subtract(offset, 0, 0);
//		}
        return newLoc
    }

    companion object {
        const val ROTATION_SOUTH = 0
        const val ROTATION_WEST = 1
        const val ROTATION_NORTH = 2
        const val ROTATION_EAST = 3
    }
}