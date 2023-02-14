package kr.kua.nms.v1_16_5R1

import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.PropertyMap
import com.mojang.datafixers.util.Pair
import kr.kua.Main
import kr.kua.Util.bedLocation
import kr.kua.Util.info
import kr.kua.Util.warning
import kr.kua.nms.CorpseData
import kr.kua.nms.Corpses
import net.minecraft.server.v1_16_R3.*
import net.minecraft.server.v1_16_R3.PacketPlayOutEntity.PacketPlayOutRelEntityMove
import net.minecraft.server.v1_16_R3.PacketPlayOutPlayerInfo.PlayerInfoData
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class NMSCorpses_v1_16_5_R1 : Corpses {
    private val corpses: MutableList<CorpseData>
    private fun cloneProfileWithRandomUUID(oldProf: GameProfile, name: String): GameProfile {
        val newProf = GameProfile(UUID.randomUUID(), name)
        newProf.properties.putAll(oldProf.properties)
        return newProf
    }

    private fun getClickableBlockUnderPlayer(loc: Location, addToYPos: Int): Location? {
        if (loc.blockY < 0) {
            return null
        }
        for (y in loc.blockY downTo 0) {
            val m = loc.world!!.getBlockAt(loc.blockX, y, loc.blockZ).type
            if (m.isSolid) {
                return Location(
                    loc.world, loc.x, (y + addToYPos).toDouble(),
                    loc.z
                )
            }
        }
        return null
    }

    override fun spawnCorpse(
        p: Player,
        overrideName: String?,
        loc: Location,
        items: Inventory,
        facing: Int
    ): CorpseData {
        val entityId = getNextEntityId()
        val prof = cloneProfileWithRandomUUID((p as CraftPlayer?)!!.profile, p.name)
        info("prof: $prof")

        val dw = clonePlayerDataWatcher(p, entityId)
        info("dw: $dw")

        val skinFlags = DataWatcherObject(16, DataWatcherRegistry.a)
        dw.set(skinFlags, 0x7F.toByte())
        info("skinFlags.a(): ${skinFlags.a()}")
        info("skinFlags.b(): ${skinFlags.b()}")

        val locUnder = getClickableBlockUnderPlayer(loc, 1)
        info("locUnder: $locUnder")

        val used = locUnder ?: loc
        used.yaw = loc.yaw
        used.pitch = loc.pitch
        info("used: $used")

        val data = NMSCorpseData(
            prof, used, dw, entityId,
            600 * 20, items, facing, p.name
        ) // 유지 시간 600초
        info("data: $data")

        if (p.killer != null) {
            data.killerUsername = p.killer!!.name
            data.killerUUID = p.killer!!.uniqueId
        }
        data.corpseName = p.name
        corpses.add(data)
        //		spawnSlimeForCorpse(data);
        return data
    }

    override fun removeCorpse(data: CorpseData) {
        corpses.remove(data)
        data.destroyCorpseFromEveryone()
        if (data.lootInventory != null) {
            data.lootInventory!!.clear()
            val close: List<HumanEntity> = ArrayList(
                data.lootInventory!!.viewers
            )
            for (p in close) {
                p.closeInventory()
            }
        }
    }

//    override val nextEntityId: Int
//        get() = try {
//            val entityCount = Entity::class.java.getDeclaredField("entityCount")
//            entityCount.isAccessible = true
//            val id = entityCount.getInt(null)
//            entityCount.setInt(null, id + 1)
//            id
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Math.round(Math.random() * Int.MAX_VALUE * 0.25).toInt()
//        }

    //Fix for lower versions
    override fun getNextEntityId(): Int {
        return getNextEntityIdAtomic().get()
    }

    //1.14 Change -- EntityCount is a AtomicInteger now
    private fun getNextEntityIdAtomic(): AtomicInteger {
        return try {
            val entityCount = Entity::class.java.getDeclaredField("entityCount")
            entityCount.isAccessible = true

            //Fix for final field
            val modifiersField = Field::class.java.getDeclaredField("modifiers")
            modifiersField.isAccessible = true
            modifiersField.setInt(entityCount, entityCount.modifiers and Modifier.FINAL.inv())
            val id = entityCount[null] as AtomicInteger
            id.incrementAndGet()
            entityCount[null] = id
            id
        } catch (e: IllegalAccessException) {
            warning("getNexEntityIdAtomic process while IllegalAccessException")
            AtomicInteger(Math.round(Math.random() * Int.MAX_VALUE * 0.25).toInt())
        } catch (e: java.lang.Exception) {
            warning("getNexEntityIdAtomic process while java.lang.Exception")
            AtomicInteger(Math.round(Math.random() * Int.MAX_VALUE * 0.25).toInt())
        }
    }

    inner class NMSCorpseData(
        private val prof: GameProfile, override val origLocation: Location,
        private val metadata: DataWatcher, override val entityId: Int, override var ticksLeft: Int,
        override val lootInventory: Inventory?, override var rotation: Int,
        var corpsesName: String
    ) : CorpseData {
        private val canSee: MutableMap<Player, Boolean>
        private val tickLater: MutableMap<Player, Int>
        override var inventoryView: InventoryView? = null
        override var selectedSlot = 0

        //        override var corpseName: String
        override var killerUsername: String? = null
        override var killerUUID: UUID? = null

        init {
            canSee = HashMap()
            tickLater = HashMap()
            if (rotation > 3 || rotation < 0) {
                rotation = 0
            }
        }

        private fun convertBukkitToMc(stack: ItemStack?): net.minecraft.server.v1_16_R3.ItemStack {
            return CraftItemStack.asNMSCopy(stack)
            /*
			if(stack == null){
				return new ItemStack(Item.getById(0));
			}
			ItemStack temp = new ItemStack(Item.getById(stack.getTypeId()), stack.getAmount());
			temp.setData((int)stack.getData().getData());
			if(stack.getEnchantments().size() >= 1) {
				temp.addEnchantment(Enchantment.c(0), 1);//Dummy enchantment
			}
			return temp;
			 */
        }

        override fun setCanSee(p: Player, b: Boolean) {
            this.canSee[p] = java.lang.Boolean.valueOf(b)
        }

        override fun canSee(p: Player): Boolean {
            return canSee[p]!!
        }

        fun removeFromMap(p: Player) {
            canSee.remove(p)
        }

        override fun mapContainsPlayer(p: Player): Boolean {
            return canSee.containsKey(p)
        }

        override val playersWhoSee: MutableSet<Player>
            get() = canSee.keys

        override fun removeAllFromMap(toRemove: MutableList<Player?>) {
            canSee.keys.removeAll(toRemove.toSet())
        }

        private val spawnPacket: PacketPlayOutNamedEntitySpawn
            get() {
                val packet = PacketPlayOutNamedEntitySpawn()
                try {
                    val a = packet.javaClass.getDeclaredField("a")
                    a.isAccessible = true
                    a[packet] = entityId
                    val b = packet.javaClass.getDeclaredField("b")
                    b.isAccessible = true
                    b[packet] = prof.id
                    val c = packet.javaClass.getDeclaredField("c")
                    c.isAccessible = true
                    c.setDouble(packet, origLocation.x)
                    val d = packet.javaClass.getDeclaredField("d")
                    d.isAccessible = true
                    d.setDouble(packet, origLocation.y + 1.0f / 16.0f)
                    val e = packet.javaClass.getDeclaredField("e")
                    e.isAccessible = true
                    e.setDouble(packet, origLocation.z)
                    val f = packet.javaClass.getDeclaredField("f")
                    f.isAccessible = true
                    f.setByte(packet, (origLocation.yaw * 256.0f / 360.0f).toInt().toByte())
                    val g = packet.javaClass.getDeclaredField("g")
                    g.isAccessible = true
                    g.setByte(packet, (origLocation.pitch * 256.0f / 360.0f).toInt().toByte())


//				Field i = packet.getClass().getDeclaredField("h");
//				i.setAccessible(true);
//				i.set(packet, metadata);
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return packet
            }
        private val movePacket: PacketPlayOutRelEntityMove
            //		public PacketPlayOutBed getBedPacket() {
            get() = PacketPlayOutRelEntityMove(
                entityId, 0.toShort(), (-61.8).toInt().toShort(), 0.toShort(), false
            )
        private val infoPacket: PacketPlayOutPlayerInfo
            get() {
                val packet = PacketPlayOutPlayerInfo(
                    PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER
                )
                try {
                    val b = packet.javaClass.getDeclaredField("b")
                    b.isAccessible = true
                    val data = b[packet] as MutableList<PlayerInfoData>
                    data.add(
                        packet.PlayerInfoData(
                            prof, -1,
                            EnumGamemode.SURVIVAL, ChatMessage("[CR]")
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return packet
            }
        private val removeInfoPacket: PacketPlayOutPlayerInfo
            get() {
                val packet = PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER)
                try {
                    val b = packet.javaClass.getDeclaredField("b")
                    b.isAccessible = true
                    val data = b[packet] as MutableList<PlayerInfoData>
                    data.add(
                        packet.PlayerInfoData(
                            prof, -1,
                            EnumGamemode.SURVIVAL, ChatMessage("[CR]")
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return packet
            }
        override val trueLocation: Location
            get() = origLocation.clone().add(0.0, 0.1, 0.0)
        val entityMetadataPacket: PacketPlayOutEntityMetadata
            //		public PacketPlayOutEntityEquipment getEquipmentPacket(EnumItemSlot slot, ItemStack stack){
            get() = PacketPlayOutEntityMetadata(entityId, metadata, false)

        @Suppress("deprecation")
        override fun resendCorpseToEveryone() {
            val spawnPacket = spawnPacket
            //PacketPlayOutBed bedPacket = getBedPacket();
            val movePacket = movePacket
            val infoPacket = infoPacket
            val removeInfo = removeInfoPacket
            //Defining the list of Pairs with EnumItemSlot and (NMS) ItemStack
            val equipmentList: MutableList<Pair<EnumItemSlot, net.minecraft.server.v1_16_R3.ItemStack>> = ArrayList()
            equipmentList.add(
                Pair(
                    EnumItemSlot.HEAD, convertBukkitToMc(
                        lootInventory!!.getItem(1)
                    )
                )
            )
            equipmentList.add(
                Pair(
                    EnumItemSlot.CHEST, convertBukkitToMc(
                        lootInventory.getItem(2)
                    )
                )
            )
            equipmentList.add(
                Pair(
                    EnumItemSlot.LEGS, convertBukkitToMc(
                        lootInventory.getItem(3)
                    )
                )
            )
            equipmentList.add(
                Pair(
                    EnumItemSlot.FEET, convertBukkitToMc(
                        lootInventory.getItem(4)
                    )
                )
            )
            equipmentList.add(
                Pair(
                    EnumItemSlot.MAINHAND, convertBukkitToMc(
                        lootInventory.getItem(selectedSlot + 45)
                    )
                )
            )
            equipmentList.add(
                Pair(
                    EnumItemSlot.OFFHAND, convertBukkitToMc(
                        lootInventory.getItem(7)
                    )
                )
            )

            //Creating the packet
            val entityEquipment = PacketPlayOutEntityEquipment(entityId, equipmentList)
            val toSend = origLocation.world!!.players
            for (p in toSend) {
                val conn = (p as CraftPlayer).handle.playerConnection
                val bedLocation = bedLocation(origLocation)
                // You don't need beds since we force the sleeping position in makePlayerSleep (Check it out)
                p.sendBlockChange(
                    bedLocation,
                    Material.RED_BED, rotation.toByte()
                )
                conn.sendPacket(infoPacket)
                conn.sendPacket(spawnPacket)
                //conn.sendPacket(bedPacket);
                conn.sendPacket(movePacket)
                // The EntityMetadataPacket is sent from here.
                makePlayerSleep(p, conn, getBlockPositionFromBukkitLocation(bedLocation), metadata)

                // Why resend the packet? This is part of the reason why corpses spawn standing up.
                //conn.sendPacket(getEntityMetadataPacket());
//				if(ConfigData.shouldRenderArmor()) {
                conn.sendPacket(entityEquipment)
                //				}
            }
            Bukkit.getServer().scheduler
                .scheduleSyncDelayedTask(Main.plugin, {
                    for (p in toSend) {
                        (p as CraftPlayer).handle.playerConnection
                            .sendPacket(removeInfo)
                    }
                }, 40L)
        }

        @Suppress("deprecation")
        override fun resendCorpseToPlayer(p: Player) {
            val spawnPacket = spawnPacket
            val movePacket = movePacket
            val infoPacket = infoPacket
            val removeInfo = removeInfoPacket

            //Defining the list of Pairs with EnumItemSlot and (NMS) ItemStack
            val equipmentList: MutableList<Pair<EnumItemSlot, net.minecraft.server.v1_16_R3.ItemStack>> = ArrayList()
            equipmentList.add(
                Pair(
                    EnumItemSlot.HEAD, convertBukkitToMc(
                        lootInventory!!.getItem(1)
                    )
                )
            )
            equipmentList.add(
                Pair(
                    EnumItemSlot.CHEST, convertBukkitToMc(
                        lootInventory.getItem(2)
                    )
                )
            )
            equipmentList.add(
                Pair(
                    EnumItemSlot.LEGS, convertBukkitToMc(
                        lootInventory.getItem(3)
                    )
                )
            )
            equipmentList.add(
                Pair(
                    EnumItemSlot.FEET, convertBukkitToMc(
                        lootInventory.getItem(4)
                    )
                )
            )
            equipmentList.add(
                Pair(
                    EnumItemSlot.MAINHAND, convertBukkitToMc(
                        lootInventory.getItem(selectedSlot + 45)
                    )
                )
            )
            equipmentList.add(
                Pair(
                    EnumItemSlot.OFFHAND, convertBukkitToMc(
                        lootInventory.getItem(7)
                    )
                )
            )

            //Creating the packet
            val entityEquipment = PacketPlayOutEntityEquipment(entityId, equipmentList)
            val conn = (p as CraftPlayer?)!!.handle.playerConnection
            val bedLocation = bedLocation(origLocation)
            p!!.sendBlockChange(bedLocation, Material.RED_BED, rotation.toByte())
            conn.sendPacket(infoPacket)
            conn.sendPacket(spawnPacket)
            //conn.sendPacket(bedPacket);
            conn.sendPacket(movePacket)
            // The EntityMetadataPacket is sent from here.
            makePlayerSleep(p, conn, getBlockPositionFromBukkitLocation(bedLocation), metadata)

            // Why resend the packet? This is part of the reason why corpses spawn standing up.
            //conn.sendPacket(getEntityMetadataPacket());
//			if(ConfigData.shouldRenderArmor()) {
            conn.sendPacket(entityEquipment)
            //			}
            Bukkit.getServer().scheduler
                .scheduleSyncDelayedTask(
                    Main.plugin,
                    { (p as CraftPlayer?)!!.handle.playerConnection.sendPacket(removeInfo) },
                    40L
                )
        }

        private fun getBlockPositionFromBukkitLocation(loc: Location): BlockPosition {
            return BlockPosition(loc.blockX, loc.blockY, loc.blockZ)
        }

        private fun makePlayerSleep(p: Player, conn: PlayerConnection, bedPos: BlockPosition, playerDW: DataWatcher) {
            val entityPlayer: EntityPlayer = CustomEntityPlayer(p, prof)
            entityPlayer.e(entityId) //sets the entity id
            try {
                //Set the datawatcher field on the newly crafted entity -- uses reflection
//				Field dwField = EntityPlayer.class.getField("datawatcher");
//				dwField.setAccessible(true);
//				dwField.set(entityPlayer, playerDW);

//				 These lines force an entity player into the sleeping position
                val poseF = Entity::class.java.getDeclaredField("POSE")
                poseF.isAccessible = true
                val POSE = poseF[null] as DataWatcherObject<EntityPose>
                entityPlayer.dataWatcher.set(POSE, EntityPose.SLEEPING)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            entityPlayer.entitySleep(bedPos) //go to sleep
            conn.sendPacket(PacketPlayOutEntityMetadata(entityPlayer.id, entityPlayer.dataWatcher, false))
        }

        @Suppress("deprecation")
        override fun destroyCorpseFromPlayer(p: Player) {
//			PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(entityId);
//			((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
//			Block b = Util.bedLocation(loc).getBlock();
//			boolean removeBed = true;
//			for (CorpseData cd : getAllCorpses()) {
//				if (cd != this
//						&& Util.bedLocation(cd.getOrigLocation())
//						.getBlock().getLocation()
//						.equals(b.getLocation())) {
//					removeBed = false;
//					break;
//				}
//			}
//			if (removeBed) {
//				p.sendBlockChange(b.getLocation(), b.getType(), b.getData());
//			}
            val packet = PacketPlayOutEntityDestroy(
                entityId
            )
            (p as CraftPlayer?)!!.handle.playerConnection.sendPacket(packet)
            val b = origLocation.clone().subtract(0.0, 2.0, 0.0).block
            var removeBed = true
            for (cd in allCorpses) {
                if (cd !== this
                    && (cd!!.origLocation!!.clone().subtract(0.0, 2.0, 0.0)
                        .block.location
                            == b.location)
                ) {
                    removeBed = false
                    break
                }
            }
            if (removeBed) {
                p.sendBlockChange(b.location, b.type, b.data)
            }
        }

        @Suppress("deprecation")
        override fun destroyCorpseFromEveryone() {
//			PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(
//					entityId);
//			Block b = loc.clone().subtract(0, 2, 0).getBlock();
//			boolean removeBed = true;
//			for (CorpseData cd : getAllCorpses()) {
//				if (cd != this
//						&& Util.bedLocation(cd.getOrigLocation())
//						.getBlock().getLocation()
//						.equals(b.getLocation())) {
//					removeBed = false;
//					break;
//				}
//			}
//			for (Player p : loc.getWorld().getPlayers()) {
//				((CraftPlayer) p).getHandle().playerConnection
//						.sendPacket(packet);
//				if (removeBed) {
//					p.sendBlockChange(b.getLocation(), b.getType(), b.getData());
//				}
//			}
            val packet = PacketPlayOutEntityDestroy(
                entityId
            )
            val b = origLocation.clone().subtract(0.0, 2.0, 0.0).block
            var removeBed = true
            for (cd in allCorpses) {
                if (cd !== this
                    && (cd!!.origLocation!!.clone().subtract(0.0, 2.0, 0.0)
                        .block.location
                            == b.location)
                ) {
                    removeBed = false
                    break
                }
            }
            for (p in origLocation.world!!.players) {
                (p as CraftPlayer).handle.playerConnection
                    .sendPacket(packet)
                if (removeBed) {
                    p.sendBlockChange(b.location, b.type, b.data)
                }
            }
        }

        override fun tickPlayerLater(ticks: Int, p: Player) {
            tickLater[p] = Integer.valueOf(ticks)
        }

        override fun getPlayerTicksLeft(p: Player): Int {
            return tickLater[p]!!
        }

        override fun stopTickingPlayer(p: Player) {
            tickLater.remove(p)
        }

        override fun isTickingPlayer(p: Player): Boolean {
            return tickLater.containsKey(p)
        }

        override val playersTicked: MutableSet<Player>
            get() = tickLater.keys

        override fun setSelectedSlot(slot: Int): CorpseData {
            selectedSlot = slot
            return this
        }

        override lateinit var corpseName: String

        override val profilePropertiesJson: String
            get() {
                val pmap = prof.properties
                val element = PropertyMap.Serializer().serialize(pmap, null, null)
                return element.toString()
            }
    }

    private var tickNumber = 0

    init {
        corpses = ArrayList()
        Bukkit.getServer().scheduler
            .scheduleSyncRepeatingTask(Main.plugin, { tick() }, 0L, 1L)
    }

    private fun tick() {
        ++tickNumber
        val toRemoveCorpses: MutableList<CorpseData> = ArrayList()
        for (data in corpses) {
            val worldPlayers = data.origLocation!!.world!!.players
            for (p in worldPlayers) {
                if (data.isTickingPlayer(p)) {
                    val ticks = data.getPlayerTicksLeft(p)
                    if (ticks > 0) {
                        data.tickPlayerLater(ticks - 1, p)
                        continue
                    } else {
                        data.stopTickingPlayer(p)
                    }
                }
                if (data.mapContainsPlayer(p)) {
                    if (isInViewDistance(p, data) && !data.canSee(p)) {
                        object : BukkitRunnable() {
                            override fun run() {
                                data.resendCorpseToPlayer(p)
                            }
                        }.runTaskLater(Main.plugin, 2)
                        data.setCanSee(p, true)
                    } else if (!isInViewDistance(p, data) && data.canSee(p)) {
                        data.destroyCorpseFromPlayer(p)
                        data.setCanSee(p, false)
                    }
                } else if (isInViewDistance(p, data)) {
                    object : BukkitRunnable() {
                        override fun run() {
                            data.resendCorpseToPlayer(p)
                        }
                    }.runTaskLater(Main.plugin, 2)
                    data.setCanSee(p, true)
                } else {
                    data.setCanSee(p, false)
                }
            }
            if (data.ticksLeft >= 0) {
                if (data.ticksLeft == 0) {
                    toRemoveCorpses.add(data)
                } else {
                    data.ticksLeft = data.ticksLeft - 1
                }
            }
            val toRemove: MutableList<Player?> = ArrayList()
            for (pl in data.playersWhoSee) {
                if (!worldPlayers.contains(pl)) {
                    toRemove.add(pl)
                }
            }
            data.removeAllFromMap(toRemove)
            toRemove.clear()
            val set = data.playersTicked
            for (pl in set) {
                if (!worldPlayers.contains(pl)) {
                    toRemove.add(pl)
                }
            }
            set.removeAll(toRemove.toSet())
            toRemove.clear()
        }
        for (data in toRemoveCorpses) {
            removeCorpse(data)
        }
    }

    fun isInViewDistance(p: Player?, data: CorpseData?): Boolean {
        val p1loc = p!!.location
        val p2loc = data!!.trueLocation
        val minX = p2loc!!.x - 45
        val minY = p2loc.y - 45
        val minZ = p2loc.z - 45
        val maxX = p2loc.x + 45
        val maxY = p2loc.y + 45
        val maxZ = p2loc.z + 45
        return p1loc.x >= minX && p1loc.x <= maxX && p1loc.y >= minY && p1loc.y <= maxY && p1loc.z >= minZ && p1loc.z <= maxZ
    }

    override val allCorpses: MutableList<CorpseData>
        get() = corpses

    override fun registerPacketListener(p: Player) {
        PacketIn_v1_16_5_R1.registerListener(p)
    }

    override fun addNbtTagsToSlime(slime: LivingEntity) {
        slime.setAI(false)
        val nmsEntity = (slime as CraftEntity?)!!.handle
        nmsEntity.isSilent = true
        nmsEntity.isInvulnerable = true
        nmsEntity.isNoGravity = true
    }

    internal class CustomEntityPlayer(p: Player, prof: GameProfile?) : EntityPlayer(
        (p.world as CraftWorld).handle.minecraftServer,
        (p.world as CraftWorld).handle,
        prof,
        PlayerInteractManager((p.world as CraftWorld).handle)
    )

    companion object {
        fun clonePlayerDataWatcher(player: Player, currentEntId: Int): DataWatcher {
//		EntityHuman h = new EntityHuman(
//				((CraftWorld) player.getWorld()).getHandle(),
//				((CraftPlayer) player).getProfile()) {
//			public void sendMessage(IChatBaseComponent arg0) {
//				return;
//			}
//
//			public boolean a(int arg0, String arg1) {
//				return false;
//			}
//
//			public BlockPosition getChunkCoordinates() {
//				return null;
//			}
//
//			public boolean v() {
//				return false;
//			}
//		};
//		h.d(currentEntId);
//		return h.getDataWatcher();
            val loc = player.location
            val h: EntityHuman = object : EntityHuman(
                (player.world as CraftWorld).handle,
                BlockPosition(loc.x, loc.y, loc.z),
                loc.yaw,
                (player as CraftPlayer?)!!.profile
            ) {
                override fun getChunkCoordinates(): BlockPosition? {
                    return null
                }

                override fun isSpectator(): Boolean {
                    return false
                }

                override fun isCreative(): Boolean {
                    return false
                }
            }
            h.e(currentEntId)
            return h.dataWatcher
        }
    }
}