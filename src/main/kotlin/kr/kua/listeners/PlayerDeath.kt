package kr.kua.listeners

import kr.kua.Main
import kr.kua.Util
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

    private fun yawToFacing(yaw: Float): Int {
        var facing = ROTATION_NORTH
        if (yaw >= -45 && yaw <= 45) {
            facing = ROTATION_SOUTH
        } else if (yaw in 45.0..135.0) {
            facing = ROTATION_WEST
        } else if (yaw <= -45 && yaw >= -135) {
            facing = ROTATION_EAST
        } else if (yaw <= -135 && yaw >= -225) {
            facing = ROTATION_NORTH
        } else if (yaw <= -225 && yaw >= -315) {
            facing = ROTATION_WEST
        } else if (yaw in 135.0..225.0) {
            facing = ROTATION_NORTH
        } else if (yaw in 225.0..315.0) {
            facing = ROTATION_EAST
        } else if (yaw >= 315) {
            facing = ROTATION_SOUTH
        } else if (yaw <= -315) {
            facing = ROTATION_SOUTH
        }
        return facing
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