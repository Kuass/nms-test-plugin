package kr.kua

import kr.kua.nms.CorpseData
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.nio.file.Files

object Util {
    private const val prefix = "[WakGood] "

    //	public static int getNearestMultipleOfNumberCeil(int number, int multiple) {
    //		try{
    //			return (int) (multiple * Math.ceil((double) number / multiple));
    //		}catch(Exception ex){
    //			new ReportError(ex);
    //		}
    //		return -1;
    //	}
    fun info(text: String) {
        Bukkit.getServer().logger.info(prefix + text)
    }

    fun warning(text: String) {
        Bukkit.getServer().logger.warning(prefix + text)
    }

    fun severe(text: String) {
        Bukkit.getServer().logger.severe(prefix + text)
    }

    fun cinfo(text: String) {
        var text = text
        text = text.replace("&".toRegex(), "�")
        Bukkit.getConsoleSender().sendMessage(prefix + text)
    }

    fun isWithinRadius(p: Player, radius: Double, corpseLocation: Location): Boolean {
        return isWithinRadius(p.location, radius, corpseLocation)
    }

    fun isWithinRadius(playerLoc: Location, radius: Double, corpseLocation: Location): Boolean {
        if (playerLoc.world != corpseLocation.world) {
            return false
        }

        val dis =
            Math.sqrt((corpseLocation.x - playerLoc.x) * (corpseLocation.x - playerLoc.x) + (corpseLocation.z - playerLoc.z) * (corpseLocation.z - playerLoc.z))
        return dis <= radius
    }

    fun isCorpseInChunk(chunk: Chunk, cd: CorpseData): Boolean {
        return if (cd.trueLocation == null) false
        else cd.trueLocation!!.chunk == chunk
    }

    fun removeCorpsesInRadius(p: Player, radius: Double): ArrayList<CorpseData> {
        val returnCorpses = ArrayList<CorpseData>()
        val iWantToRemove = ArrayList<CorpseData>()
        for (cd in Main.plugin.corpses.allCorpses) {
            val l = cd.origLocation
            if (l == null) {
                warning("CorpseData.origLocation is null! (CorpseData.entityId: " + cd.entityId + ")")
            } else {
                if (isWithinRadius(p, radius, l)) {
                    iWantToRemove.add(cd)
                }
            }
        }
        for (cd in iWantToRemove) {
            returnCorpses.add(cd)
            Main.plugin.corpses.removeCorpse(cd)
        }
        return returnCorpses
    }

    fun getCorpseInRadius(loc: Location, radius: Double): CorpseData? {
        for (cd in Main.plugin.corpses.allCorpses) {
            val l = cd.origLocation
            if (l == null) {
                warning("CorpseData.origLocation is null! (CorpseData.entityId: " + cd.entityId + ")")
            } else if (isWithinRadius(loc, radius, l)) {
                return cd
            }
        }
        return null
    }

    fun removeAllCorpses(world: World): ArrayList<CorpseData> {
        val returnCorpses = ArrayList<CorpseData>()
        val iWantToRemove = ArrayList<CorpseData>()
        for (cd in Main.plugin.corpses.allCorpses) {
            if (cd.trueLocation?.world == world) {
                iWantToRemove.add(cd)
            }
        }
        for (cd in iWantToRemove) {
            returnCorpses.add(cd)
            Main.plugin.corpses.removeCorpse(cd)
        }
        return returnCorpses
    }

    fun isInventoryEmpty(i: Inventory): Boolean {
        for (item in i.contents) {
            if (item != null) return false
        }
        return true
    }

    fun isCorpseInventory(iv: InventoryView?): CorpseData? {
        for (cd in Main.plugin.corpses.allCorpses) {
            if (iv != null && cd.inventoryView != null && cd.inventoryView == iv) {
                return cd
            }
        }
        return null
    }

    fun removeCorpse(cd: CorpseData) {
        Main.plugin.corpses.removeCorpse(cd)
    }

    fun callEvent(event: Event?) {
        Bukkit.getServer().pluginManager.callEvent(event!!)
    }

    fun commaSep(list: ArrayList<String?>): String? {
        var result: String? = ""
        for (i in list.indices) {
            if (i != 0) {
                result += ", "
            }
            result += list[i]
        }
        return result
    }

    @JvmStatic
	fun bedLocation(loc: Location): Location {
        val l = loc.clone()
        l.y = bedLocation().toDouble()
        return l
    }

    fun bedLocation(): Int {
        return 1
    }

    fun copyFiles(source: File, dest: File): Boolean {
        return try {
            Files.copy(source.toPath(), dest.toPath())
            true
        } catch (e: Exception) {
            false
        }
    }

    @Throws(Exception::class)
    fun setFinalStatic(theObj: Any?, field: Field, newValue: Any?) {
        field.isAccessible = true
        val modifiersField = Field::class.java.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
        field[theObj] = newValue
    }

    fun <E : Enum<E>?> prettyPrintGenericEnum(clazz: Class<E>): String {
        val commaSeperatedValidMsgTypes = StringBuilder()
        for (msgType in clazz.enumConstants) {
            commaSeperatedValidMsgTypes.append(msgType!!.name + ", ")
        }
        return commaSeperatedValidMsgTypes.toString()
    }
}