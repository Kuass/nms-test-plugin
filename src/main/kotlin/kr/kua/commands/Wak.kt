package kr.kua.commands

import kr.kua.Main
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

class Wak : CommandExecutor {

    override fun onCommand(
        sender: CommandSender, cmd: Command,
        commandLabel: String, args: Array<String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}플레이어만 사용할 수 있습니다.")
            return true
        }
        if (!sender.isOp) {
            sender.sendMessage("${ChatColor.RED}당신은 권한이 부족합니다.")
            return true
        }
        if (args.isEmpty()) helpMessage(sender)
        else {
            when (args[0]) {
                "s" -> {
                    val items = Bukkit.getServer().createInventory(null, 54, sender.name + "의 아이템")
                    for (`is` in sender.inventory.contents) {
                        if (`is` != null) {
                            items.addItem(`is`)
                        }
                    }
//                    for (`is` in sender.inventory.armorContents) {
//                        if (`is` != null) {
//                            items.addItem(`is`)
//                        }
//                    }
                    Main.plugin.corpses.spawnCorpse(sender, null, sender.location, items, 0).selectedSlot =
                        sender.inventory.heldItemSlot
                    sender.sendMessage("${ChatColor.YELLOW}Corpse of yourself spawned!")
                }

                "p", "player" -> {
                    if (args[1].isEmpty()) {
                        sender.sendMessage("${ChatColor.RED}Usage: /wak p/player [player]")
                        return true
                    }

                    val p = Bukkit.getServer().getPlayer(args[1])
                    if (p == null) {
                        sender.sendMessage("${ChatColor.RED}Player ${args[1]} is not online!")
                        return true
                    }

                    val items = Bukkit.getServer().createInventory(null, 54, p.name + "의 아이템")
                    for (`is` in p.inventory.contents) {
                        if (`is` != null) {
                            items.addItem(`is`)
                        }
                    }
//                    for (`is` in p.inventory.armorContents) {
//                        if (`is` != null) {
//                            items.addItem(`is`)
//                        }
//                    }
                    Main.plugin.corpses.spawnCorpse(p, null, p.location, items, 0).selectedSlot =
                        p.inventory.heldItemSlot
                    sender.sendMessage("${ChatColor.YELLOW}Spawned corpse of ${p.name}!")
                }

                "remove" -> {
                    val p: Player? = if (args.size == 1) sender.player else Bukkit.getServer().getPlayer(args[1])

                    if (p == null) {
                        sender.sendMessage("${ChatColor.RED}Player ${args[1]} is not online!")
                        return true
                    }

                    val filter = Main.plugin.corpses.allCorpses.filter { it.corpseName == p.name }
                    sender.sendMessage("${ChatColor.GREEN}Searched ${filter.size} Corpse.")
                    filter.map { Main.plugin.corpses.removeCorpse(it) }
                    sender.sendMessage("${ChatColor.GREEN}Remove Completed")
                }

                "removeall" -> {
                    val corpses = Main.plugin.corpses.allCorpses
                    var count = 0
                    corpses.map {
                        try {
                            Main.plugin.corpses.removeCorpse(it)
                            count++
                        }catch (e: Exception) {
                            sender.sendMessage("${ChatColor.RED}Error: ${e.message}")
                        }
                    }
                    sender.sendMessage("${ChatColor.GREEN}All corpses($count/${corpses.size}) removed!")
                }

                "list" -> {
                    sender.sendMessage(ChatColor.GREEN.toString() + "Corpses:")
                    for (c in Main.plugin.corpses.allCorpses) {
                        sender.sendMessage("${ChatColor.GREEN}${c.corpseName} at ${c.trueLocation.toString()}")
                    }
                }

                else -> helpMessage(sender)
            }
        }
        return true
    }

    private fun helpMessage(sender: CommandSender) {
        sender.sendMessage(ChatColor.GREEN.toString() + "Wak Commands:")
        sender.sendMessage(ChatColor.GREEN.toString() + "/wak s - Spawn a corpse of yourself")
        sender.sendMessage(ChatColor.GREEN.toString() + "/wak p/player <username> - Spawn a corpse of a player")
        sender.sendMessage(ChatColor.GREEN.toString() + "/wak remove <username> - Remove your corpse")
        sender.sendMessage(ChatColor.GREEN.toString() + "/wak removeall - Remove all corpses")
        sender.sendMessage(ChatColor.GREEN.toString() + "/wak list - List all corpses")
    }
}