
package com.github.yukinoraru.ToggleInventory;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class ToggleInventory extends JavaPlugin implements Listener
{
  protected Logger log;
  protected InventoryManager inventoryManager;

  @Override
  public void onEnable()
  {
    this.log = this.getLogger();
    this.saveDefaultConfig();
    this.getConfig().options().copyDefaults(true);
    this.saveConfig();

    this.log.info("Update check was skipped.");

    this.inventoryManager = new InventoryManager(this);
    final File spInvFile = this.inventoryManager.getDefaultSpecialInventoryFile();
    if (!spInvFile.exists()) {
      this.saveResource(spInvFile.getName(), false);
    }

    if (!this.getConfig().getBoolean("disable-mcstats", false)) {
      try {
        this.log.info("MCStats enabled. You can disable via config.yml.");
        final Metrics metrics = new Metrics(this);
        metrics.start();
      } catch (final IOException var4) {
        // ignore
      }
    } else {
      this.log.info("MCStats disabled.");
    }

  }

  @Override
  public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args)
  {
    Player player = null;
    String playerName = null;
    if (!(sender instanceof Player)) {
      sender.sendMessage("You must be a player!");
      return false;
    } else {
      player = (Player) sender;
      playerName = player.getName();
      final boolean isToggleInvCommand = "togglei".equalsIgnoreCase(cmd.getName());
      final boolean isReverse = "toggleir".equalsIgnoreCase(cmd.getName());
      if (!isToggleInvCommand && !isReverse) {
        final boolean isTogglelInvSpecialCommand = "toggleis".equalsIgnoreCase(cmd.getName());
        final boolean isTISReverse = "toggleisr".equalsIgnoreCase(cmd.getName());
        if (isTogglelInvSpecialCommand || isTISReverse) {
          if (!player.hasPermission("toggle_inventory.toggle_special")) {
            this.outputError("You don't have permission to toggle special inventories.", player);
            return true;
          }

          try {
            String name;
            if ((args.length >= 1) && ("add".equals(args[0]) || "delete".equals(args[0]))) {
              name = args.length == 2 ? args[1] : null;
              if (name == null) {
                player.sendMessage("USAGE: /tis [add|delete] [name]");
                return true;
              }

              if ("add".equals(args[0])) {
                this.inventoryManager.saveSpecialInventory(player, name);
                player.sendMessage(ChatColor.DARK_GREEN
                    + String.format("Add %s to special inventories.", ChatColor.GREEN + name + ChatColor.DARK_GREEN));
              } else if ("delete".equals(args[0])) {
                this.inventoryManager.deleteSpecialInventory(this.inventoryManager.getInventoryFile(playerName), name);
                player.sendMessage(ChatColor.DARK_GREEN + String.format("Delete %s from special inventories.",
                    ChatColor.GREEN + name + ChatColor.DARK_GREEN));
              }

              return true;
            }

            if ((args.length >= 1) && ("add-default".equals(args[0]) || "delete-default".equals(args[0]))) {
              name = args.length == 2 ? args[1] : null;
              if (name == null) {
                player.sendMessage("USAGE: /tis [add-deafult|delete-default] [name]");
                return true;
              }

              if ("add-default".equals(args[0])) {
                this.inventoryManager.saveSpecialInventory(player, name);
                player.sendMessage(ChatColor.DARK_GREEN + String.format("Add %s to default special inventories.",
                    ChatColor.GREEN + name + ChatColor.DARK_GREEN));
              } else if ("delete-default".equals(args[0])) {
                this.inventoryManager.deleteSpecialInventory(this.inventoryManager.getDefaultSpecialInventoryFile(),
                    name);
                player.sendMessage(ChatColor.DARK_GREEN + String.format("Delete %s from default special inventories.",
                    ChatColor.GREEN + name + ChatColor.DARK_GREEN));
              }

              return true;
            }

            if ((args.length >= 1) && "copy".equals(args[0])) {
              final int destinationIndex = args.length == 3 ? Integer.parseInt(args[2]) : -1;
              final String spInvName = args.length == 3 ? args[1] : null;
              if (destinationIndex > 0) {
                this.inventoryManager.copySpInvToNormalInventory(player, spInvName, destinationIndex);
                if (destinationIndex == this.inventoryManager.getCurrentInventoryIndex(playerName)) {
                  this.inventoryManager.restoreInventory(player);
                }

                player.sendMessage(String.format("'%s' was copied to '%s' successfully!",
                    ChatColor.GREEN + spInvName + ChatColor.RESET,
                    ChatColor.BOLD + String.valueOf(destinationIndex) + ChatColor.RESET));
                return true;
              }

              player.sendMessage("USAGE: /tis copy [special inventory name] [invenotry index]");
              return true;
            }

            boolean isForce;
            if ((args.length >= 1) && "reset".equals(args[0])) {
              isForce = (args.length == 2) && "-f".equals(args[1]);
              if (isForce) {
                this.inventoryManager.initializeSPInvFromDefault(playerName);
                player.sendMessage(ChatColor.GOLD + "All special inventory were reset!");
                return true;
              }

              player.sendMessage(ChatColor.GOLD + "WARNING: All special inventory will be reset by default.");
              player.sendMessage(ChatColor.GOLD + "If you want to continue operation, retype " + ChatColor.DARK_RED
                  + "'/tis reset -f'");
              return true;
            }

            if ((args.length >= 1) && "reset-default".equals(args[0])) {
              isForce = (args.length == 2) && "-f".equals(args[1]);
              if (isForce) {
                this.saveResource(this.inventoryManager.getDefaultSpecialInventoryFile().getName(), true);
                player.sendMessage(ChatColor.GOLD + "Default special inventory were reset!");
                return true;
              }

              player.sendMessage(ChatColor.GOLD + "WARNING: Default special inventory will be reset by default.");
              player.sendMessage(ChatColor.GOLD + "If you want to continue operation, retype " + ChatColor.DARK_RED
                  + "'/tis reset-default -f'");
              return true;
            }

            if (this.inventoryManager.isFirstUseForToggleInventorySpecial(playerName)) {
              this.inventoryManager.initializeSPInvFromDefault(playerName);
              this.inventoryManager.setSpecialInventoryUsingStatusForFirstUse(playerName, false);
            }

            if ((args.length == 1) && (args[0].length() > 0)) {
              this.inventoryManager.toggleSpecialInventory(player, args[0]);
            } else {
              this.inventoryManager.toggleSpecialInventory(player, !isTISReverse);
            }

            player.sendMessage(this.inventoryManager.makeSpecialInventoryMessage(player) + " inventory toggled.");
          } catch (final Exception var14) {
            this.outputError(var14.getMessage(), player);
            var14.printStackTrace();
          }
        }

        return true;
      } else if (!player.hasPermission("toggle_inventory.toggle")) {
        this.outputError("You don't have permission to toggle inventory.", player);
        return true;
      } else if ((args.length >= 1) && (args[0].length() > 0) && args[0].startsWith("h")) {
        player.sendMessage("USAGE1: /ti - toggle inventory like a ring");
        player.sendMessage("USAGE2: /it - toggle inventory like a ring (reverse)");
        player.sendMessage("Advanced: /ti [enable|disable] gamemode - (you can toggle with gamemode)");
        return true;
      } else {
        if ((args.length >= 2) && (args[0].length() > 0) && (args[1].length() > 0)) {
          try {
            if (args[0].startsWith("e")) {
              if (args[1].startsWith("g")) {
                this.inventoryManager.setGameModeSaving(playerName, true);
                player.sendMessage("[ToggleInventory] Game mode toggle is enabled.");
              }

              return true;
            }

            if (args[0].startsWith("d")) {
              if (args[1].startsWith("g")) {
                this.inventoryManager.setGameModeSaving(playerName, false);
                player.sendMessage("[ToggleInventory] Game mode toggle is disabled.");
              }

              return true;
            }
          } catch (final IOException var13) {
            this.outputError("Something went wrong! (gamemode enable option)", player);
          }
        }

        try {
          if (this.inventoryManager.getSpecialInventoryUsingStatus(playerName)) {
            this.inventoryManager.restoreInventory(player);
            this.inventoryManager.setSpecialInventoryUsingStatus(playerName, false);
          } else if ((args.length >= 1) && (args[0].length() > 0)) {
            final int index = Integer.parseInt(args[0]);
            this.inventoryManager.toggleInventory(player, index);
          } else {
            this.inventoryManager.toggleInventory(player, !isReverse);
          }

          player.sendMessage(this.inventoryManager.makeInventoryMessage(player) + " inventory toggled.");
        } catch (final NumberFormatException var15) {
          this.outputError("Index must be a number.", player);
        } catch (final Exception var16) {
          this.outputError(var16.getMessage(), player);
        }

        return true;
      }
    }
  }

  @Override
  public void onDisable()
  {
    // unimplemented
  }

  private void outputError(final String msg)
  {
    this.getLogger().warning(msg);
  }

  private void outputError(final String msg, final CommandSender sender)
  {
    sender.sendMessage(ChatColor.RED + "[ERROR] " + msg);
    this.outputError(msg);
  }
}
