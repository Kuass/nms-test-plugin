package kr.kua.nms.v1_16_5R1

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import kr.kua.Main
import kr.kua.Util
import net.minecraft.server.v1_16_R3.PacketPlayInUseEntity
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer
import org.bukkit.entity.Player

class PacketIn_v1_16_5_R1(private val p: Player) : ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is PacketPlayInUseEntity) {
            Bukkit.getServer().scheduler.runTask(Main.plugin, Runnable {
                Util.info("packetPlayInUseEntity: ${msg.a()}")
                if (msg.b() == PacketPlayInUseEntity.EnumEntityUseAction.INTERACT_AT) {
                    Util.warning("Interact at: " + getId(msg))
                    for (cd in Main.plugin.corpses.allCorpses) {
                        if (cd.entityId == getId(msg)) {
                            p.openInventory(cd.lootInventory!!)
                            break
                        }
                    }
                }
            })
        }
        super.channelRead(ctx, msg)
    }

    private fun getId(packet: PacketPlayInUseEntity): Int {
        return try {
            val afield = packet.javaClass.getDeclaredField("a")
            afield.isAccessible = true
            val id = afield.getInt(packet)
            afield.isAccessible = false
            id
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    companion object {
        fun registerListener(p: Player) {
            val c = getChannel(p) ?: throw NullPointerException("Couldn't get channel??")
            c.pipeline().addBefore("packet_handler", "packet_in_listener", PacketIn_v1_16_5_R1(p))
        }

        private fun getChannel(p: Player): Channel? {
            val nm = (p as CraftPlayer?)!!.handle.playerConnection.networkManager
            try {
                return nm.channel
                /*
                Field ifield = nm.getClass().getDeclaredField("channel");
                ifield.setAccessible(true);
                Channel c = (Channel) ifield.get(nm);
                ifield.setAccessible(false);
                return c;
                 */
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }
}