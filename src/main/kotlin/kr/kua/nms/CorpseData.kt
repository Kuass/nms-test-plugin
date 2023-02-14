package kr.kua.nms

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView
import java.util.*

interface CorpseData {
    fun resendCorpseToEveryone()
    fun resendCorpseToPlayer(p: Player)
    fun destroyCorpseFromEveryone()
    fun destroyCorpseFromPlayer(p: Player)
    fun mapContainsPlayer(p: Player): Boolean
    val rotation: Int
    fun setCanSee(p: Player, b: Boolean)
    fun canSee(p: Player): Boolean
    val playersWhoSee: MutableSet<Player>
    fun removeAllFromMap(toRemove: MutableList<Player?>)
    val trueLocation: Location?
    val origLocation: Location?
    var ticksLeft: Int
    fun tickPlayerLater(ticks: Int, p: Player)
    fun getPlayerTicksLeft(p: Player): Int
    fun isTickingPlayer(p: Player): Boolean
    fun stopTickingPlayer(p: Player)
    val playersTicked: MutableSet<Player>
    val entityId: Int
    val lootInventory: Inventory?
    var inventoryView: InventoryView?
    var selectedSlot: Int
    fun setSelectedSlot(slot: Int): CorpseData?
    var corpseName: String
    val killerUsername: String?
    val killerUUID: UUID?
    val profilePropertiesJson: String?
}