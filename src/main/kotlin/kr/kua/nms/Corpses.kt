package kr.kua.nms

import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

interface Corpses {
    fun spawnCorpse(p: Player, overrideName: String?, loc: Location, items: Inventory, facing: Int): CorpseData
    fun removeCorpse(data: CorpseData)
    fun getNextEntityId(): Int
	val allCorpses: List<CorpseData>
    fun registerPacketListener(p: Player)
    fun addNbtTagsToSlime(slime: LivingEntity)
}