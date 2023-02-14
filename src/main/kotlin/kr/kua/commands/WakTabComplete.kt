package kr.kua.commands

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class WakTabComplete : TabCompleter {

    override fun onTabComplete(sender: CommandSender, command: Command, s: String, args: Array<String>): MutableList<String> {
        return if (args.size == 1) {
            mutableListOf("s", "p", "player", "remove", "removeall", "list")
        } else if (args.size == 2) {
            when(args[0]) {
                "p", "player", "remove" -> {
                    val list = mutableListOf<String>()
                    for (p in Bukkit.getOnlinePlayers()) {
                        list.add(p.name)
                    }
                    list
                }
                else -> mutableListOf()
            }
        } else {
            mutableListOf()
        }
    }

}