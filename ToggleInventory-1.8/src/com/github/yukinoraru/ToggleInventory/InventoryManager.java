
package com.github.yukinoraru.ToggleInventory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

public final class InventoryManager
{
  public static final String CONFIG_FILENAME_SPECIAL_INV = "special_inventories.yml";
  private final ToggleInventory plugin;

  public InventoryManager(final ToggleInventory plugin)
  {
    this.plugin = plugin;
  }

  public File getInventoryFile(final String playerName)
  {
    final String parentPath = this.plugin.getDataFolder() + File.separator + "players";
    final String childPath = playerName + ".yml";
    final File f = new File(parentPath);
    if (!f.isDirectory()) {
      f.mkdirs();
    }

    return new File(parentPath, childPath);
  }

  public File getDefaultSpecialInventoryFile()
  {
    return new File(this.plugin.getDataFolder(), "special_inventories.yml");
  }

  private void prepareFile(final File file) throws Exception
  {

    if (!file.exists()) {
      file.createNewFile();
    }

    final PrintWriter writer = new PrintWriter(file);
    writer.print("");
    writer.close();

  }

  private int getMaxInventoryIndex(final CommandSender player)
  {
    int max = -1;

    for (int i = 2; i <= 30; ++i) {
      final String permissionPath = String.format("toggle_inventory.%d", i);
      if (player.hasPermission(permissionPath)) {
        max = i;
      }
    }

    return max <= 1 ? 4 : max;
  }

  public int calcNextInventoryIndex(final int maxIndex, final int currentIndex, final boolean rotateDirection)
  {
    int nextIndex;
    if (rotateDirection) {
      nextIndex = (currentIndex + 1) > maxIndex ? 1 : currentIndex + 1;
    } else {
      nextIndex = (currentIndex - 1) <= 0 ? maxIndex : currentIndex - 1;
    }

    return nextIndex;
  }

  private FileConfiguration getPlayersFileConfiguration(final String playerName)
  {
    final File file = this.getInventoryFile(playerName);
    return YamlConfiguration.loadConfiguration(file);
  }

  private void setPlayerConfig(final String playerName, final String section, final Object obj) throws IOException
  {
    final File file = this.getInventoryFile(playerName);
    final FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);
    fileConfiguration.set(section, obj);
    fileConfiguration.save(file);
  }

  public String makeInventoryMessage(final CommandSender player)
  {
    final StringBuilder msg = new StringBuilder(ChatColor.GRAY + "[");
    final String playerName = player.getName();
    final int invCurrentIndex = this.getCurrentInventoryIndex(playerName);
    final int maxIndex = this.getMaxInventoryIndex(player);

    for (int i = 1; i <= maxIndex; ++i) {
      msg.append((i == invCurrentIndex ? ChatColor.WHITE : ChatColor.GRAY) + Integer.toString(i));
      msg.append(ChatColor.RESET + " ");
    }

    return msg.toString() + ChatColor.GRAY + "] ";
  }

  public String makeSpecialInventoryMessage(final CommandSender player) throws Exception
  {
    final StringBuilder msg = new StringBuilder(ChatColor.GRAY + "[");
    final String playerName = player.getName();
    final String invCurrentName = this.getCurrentSpecialInventoryIndex(playerName);
    final String[] list = this.getListSpecialInventory(this.getInventoryFile(playerName));

    for (final String element : list) {
      msg.append((element.equals(invCurrentName) ? ChatColor.GREEN : ChatColor.DARK_GREEN) + element);
      msg.append(ChatColor.RESET + " ");
    }

    return msg.toString() + ChatColor.GRAY + "] ";
  }

  private String[] getListSpecialInventory(final File specialInventoryFile) throws Exception
  {
    final YamlConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(specialInventoryFile);

    try {
      final Set<?> nameList = fileConfiguration.getConfigurationSection("special_inventories").getKeys(false);
      final String[] tmp = nameList.toArray(new String[0]);
      if (tmp.length == 0) {
        throw new Exception();
      } else {
        return tmp;
      }
    } catch (final Exception var5) {
      return new String[0];
    }
  }

  public String getNextSpecialInventory(final String[] list, final String name, final boolean rotateDirection)
  {
    String nextInvName = null;

    for (int i = 0; i < list.length; ++i) {
      if (list[i].equals(name)) {
        if (rotateDirection) {
          if ((i + 1) < list.length) {
            nextInvName = list[i + 1];
          } else {
            nextInvName = list[0];
          }
        } else if ((i - 1) >= 0) {
          nextInvName = list[i - 1];
        } else {
          nextInvName = list[list.length - 1];
        }
        break;
      }
    }

    return nextInvName == null ? list[0] : nextInvName;
  }

  private boolean isExistSpecialInv(final String playerName, final String specialInventoryName) throws Exception
  {
    final String[] list = this.getListSpecialInventory(this.getInventoryFile(playerName));

    final int var7 = list.length;
    boolean isMatched = false;
    final String[] var8 = list;
    for (int var6 = 0; var6 < var7; ++var6) {
      final String name = var8[var6];
      if (name.equals(specialInventoryName)) {
        isMatched = true;
        break;
      }
    }

    return isMatched;
  }

  public void saveInventory(final Player player, final String index, final boolean isSpecialInventory) throws Exception
  {
    final PlayerInventory inventory = player.getInventory();
    final Inventory inventoryArmor = InventoryUtils.getArmorInventory(inventory);
    final String serializedInventoryContents = InventoryUtils.inventoryToString(inventory);
    final String serializedInventoryArmor = InventoryUtils.inventoryToString(inventoryArmor);
    final String serializedPotion = PotionUtils.serializePotion(player.getActivePotionEffects());
    String sectionPathContents;
    String sectionPathArmor;
    String sectionPathGameMode;
    String sectionPathPotion;
    if (isSpecialInventory) {
      sectionPathContents = this.getSectionPathForSPInvContents(index);
      sectionPathArmor = this.getSectionPathForSPInvArmor(index);
      sectionPathGameMode = this.getSectionPathForSPInvGameMode(index);
      sectionPathPotion = this.getSectionPathForSPInvPotion(index);
    } else {
      final int tmp = Integer.parseInt(index);
      sectionPathContents = this.getSectionPathForUserContents(tmp);
      sectionPathArmor = this.getSectionPathForUserArmor(tmp);
      sectionPathGameMode = this.getSectionPathForUserInvGameMode(tmp);
      sectionPathPotion = this.getSectionPathForUserInvPotion(tmp);
    }

    final File inventoryFile = this.getInventoryFile(player.getName());
    final FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(inventoryFile);
    fileConfiguration.set(sectionPathContents, serializedInventoryContents);
    fileConfiguration.set(sectionPathArmor, serializedInventoryArmor);
    fileConfiguration.set(sectionPathGameMode, player.getGameMode().name());
    fileConfiguration.set(sectionPathPotion, serializedPotion);
    this.prepareFile(inventoryFile);
    fileConfiguration.save(inventoryFile);
  }

  private void loadInventory(final Player player, final String index, final boolean isSpecialInventory) throws Exception
  {
    final File inventoryFile = this.getInventoryFile(player.getName());
    final FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(inventoryFile);
    String sectionPathContents;
    String sectionPathArmor;
    String sectionPathGameMode;
    String sectionPathPotion;
    if (isSpecialInventory) {
      sectionPathContents = this.getSectionPathForSPInvContents(index);
      sectionPathArmor = this.getSectionPathForSPInvArmor(index);
      sectionPathGameMode = this.getSectionPathForSPInvGameMode(index);
      sectionPathPotion = this.getSectionPathForSPInvPotion(index);
    } else {
      final int tmp = Integer.parseInt(index);
      sectionPathContents = this.getSectionPathForUserContents(tmp);
      sectionPathArmor = this.getSectionPathForUserArmor(tmp);
      sectionPathGameMode = this.getSectionPathForUserInvGameMode(tmp);
      sectionPathPotion = this.getSectionPathForUserInvPotion(tmp);
    }

    final String serializedInventoryContents = fileConfiguration.getString(sectionPathContents);
    final String serializedInventoryArmor = fileConfiguration.getString(sectionPathArmor);
    final PlayerInventory inventory = player.getInventory();
    inventory.clear();
    inventory.setArmorContents((ItemStack[]) null);

    try {
      Inventory deserialized_inv_armor;
      if (serializedInventoryContents != null) {
        deserialized_inv_armor = InventoryUtils.stringToInventory(serializedInventoryContents);
        int i = 0;
        ItemStack[] var18;
        final int var17 = (var18 = deserialized_inv_armor.getContents()).length;

        for (int var16 = 0; var16 < var17; ++var16) {
          final ItemStack item = var18[var16];
          ++i;
          if (item != null) {
            inventory.setItem(i - 1, item);
          }
        }
      }

      if (serializedInventoryArmor != null) {
        deserialized_inv_armor = InventoryUtils.stringToInventory(serializedInventoryArmor);
        final ItemStack[] tmp = deserialized_inv_armor.getContents();
        if (tmp != null) {
          inventory.setArmorContents(tmp);
        }
      }
    } catch (final Exception var20) {
      Files.delete(inventoryFile.toPath());
      throw var20;
    }

    String effectsInString;
    if (this.isEnableGameModeSaving(player.getName())) {
      effectsInString = fileConfiguration.getString(sectionPathGameMode);
      if ((effectsInString != null) && (effectsInString.length() > 0)) {
        player.setGameMode(GameMode.valueOf(effectsInString));
      }
    }

    final Iterator<?> var25 = player.getActivePotionEffects().iterator();

    while (var25.hasNext()) {
      final PotionEffect effect = (PotionEffect) var25.next();
      player.removePotionEffect(effect.getType());
    }

    effectsInString = fileConfiguration.getString(sectionPathPotion);

    try {
      player.addPotionEffects(PotionUtils.deserializePotion(effectsInString));
    } catch (final Exception var19) {
      var19.printStackTrace();
    }

  }

  public void toggleInventory(final CommandSender player, final boolean rotateDirection) throws Exception
  {
    final String playerName = player.getName();
    final int maxIndex = this.getMaxInventoryIndex(player);
    final int currentIndex = this.getCurrentInventoryIndex(playerName);
    final int nextIndex = this.calcNextInventoryIndex(maxIndex, currentIndex, rotateDirection);
    this.toggleInventory(player, nextIndex);
  }

  public void toggleInventory(final CommandSender player, final int index) throws Exception
  {
    final String playerName = player.getName();
    final int maxIndex = this.getMaxInventoryIndex(player);
    final int currentIndex = this.getCurrentInventoryIndex(playerName);
    if (index > maxIndex) {
      throw new IndexOutOfBoundsException(String.format("Max inventory index is %d", maxIndex));
    } else if (index <= 0) {
      throw new Exception("Inventory index is wrong.");
    } else if (currentIndex == index) {
      throw new Exception("It's current inventory.");
    } else {
      this.saveInventory((Player) player, String.valueOf(currentIndex), false);
      this.loadInventory((Player) player, String.valueOf(index), false);
      this.setCurrentInventoryIndex(playerName, index);
    }
  }

  public void toggleSpecialInventory(final CommandSender player, final String inventoryName) throws Exception
  {
    final String playerName = player.getName();
    if (!this.getSpecialInventoryUsingStatus(playerName)) {
      final int currentIndex = this.getCurrentInventoryIndex(playerName);
      this.saveInventory((Player) player, String.valueOf(currentIndex), false);
    }

    final String[] list = this.getListSpecialInventory(this.getInventoryFile(playerName));
    if (list.length == 0) {
      throw new Exception(
          "Your special inventory is empty.\n Try '/tis add' or '/tis reset -f', '/tis reset-default -f'");
    } else {
      final int index = LevenshteinDistance.find(list, inventoryName);
      final String specialInvName = list[index];
      this.loadSpecialInventory((Player) player, specialInvName);
      this.setCurrentSpecialInventoryIndex(playerName, specialInvName);
      this.setSpecialInventoryUsingStatus(playerName, true);
    }
  }

  public void toggleSpecialInventory(final CommandSender player, final boolean rotateDirection) throws Exception
  {
    final String playerName = player.getName();
    final String currentSpIndex = this.getCurrentSpecialInventoryIndex(playerName);
    final String[] list = this.getListSpecialInventory(this.getInventoryFile(playerName));

    try {
      final String nextSpIndex = this.getSpecialInventoryUsingStatus(playerName)
          ? this.getNextSpecialInventory(list, currentSpIndex, rotateDirection)
          : currentSpIndex;
      this.toggleSpecialInventory(player, nextSpIndex);
    } catch (final NullPointerException var7) {
      this.toggleSpecialInventory(player, (String) null);
    }

  }

  public void initializeSPInvFromDefault(final String playerName) throws Exception
  {
    this.deleteAllSPInventoryFromUser(playerName);
    final File defaultFile = this.getDefaultSpecialInventoryFile();
    final File playerFile = this.getInventoryFile(playerName);
    final FileConfiguration defaultFileConfiguration = YamlConfiguration.loadConfiguration(defaultFile);
    final FileConfiguration playerFileConfiguration = YamlConfiguration.loadConfiguration(playerFile);
    final String[] defaultSPInvList = this.getListSpecialInventory(defaultFile);
    if (defaultSPInvList.length == 0) {
      throw new Exception("There are no default special inventories in special_inventories.yml. Please check it.");
    } else {
      final String[] var10 = defaultSPInvList;
      final int var9 = defaultSPInvList.length;

      for (int var8 = 0; var8 < var9; ++var8) {
        final String name = var10[var8];
        playerFileConfiguration.set(this.getSectionPathForSPInvContents(name),
            defaultFileConfiguration.get(this.getSectionPathForSPInvContents(name)));
        playerFileConfiguration.set(this.getSectionPathForSPInvArmor(name),
            defaultFileConfiguration.get(this.getSectionPathForSPInvArmor(name)));
        playerFileConfiguration.set(this.getSectionPathForSPInvPotion(name),
            defaultFileConfiguration.get(this.getSectionPathForSPInvPotion(name)));
        playerFileConfiguration.set(this.getSectionPathForSPInvGameMode(name),
            defaultFileConfiguration.get(this.getSectionPathForSPInvGameMode(name)));
      }

      playerFileConfiguration.save(playerFile);
    }
  }

  public void copySpInvToNormalInventory(final CommandSender player, final String specialInventoryName,
      final int destinationIndex) throws Exception
  {
    final String playerName = player.getName();
    final int maxIndex = this.getMaxInventoryIndex(player);
    if ((destinationIndex > 0) && (destinationIndex <= maxIndex)) {
      if (!this.isExistSpecialInv(playerName, specialInventoryName)) {
        throw new Exception(String.format("No such special inventory found: '%s'", specialInventoryName));
      } else {
        final File playerInventoryFile = this.getInventoryFile(playerName);
        final File specialInventoryFile = this.getInventoryFile(playerName);
        final FileConfiguration playerFileConfiguration = YamlConfiguration.loadConfiguration(playerInventoryFile);
        final FileConfiguration spinvFileConfiguration = YamlConfiguration.loadConfiguration(specialInventoryFile);
        final String playerSectionPathContents = this.getSectionPathForUserContents(destinationIndex);
        final String playerSectionPathArmor = this.getSectionPathForUserArmor(destinationIndex);
        final String playerSectionPathPotion = this.getSectionPathForUserInvPotion(destinationIndex);
        final String playerSectionPathGameMode = this.getSectionPathForUserInvGameMode(destinationIndex);
        final String spinvSectionPathContents = this.getSectionPathForSPInvContents(specialInventoryName);
        final String spinvSectionPathArmor = this.getSectionPathForSPInvArmor(specialInventoryName);
        final String spinvSectionPathPotion = this.getSectionPathForSPInvPotion(specialInventoryName);
        final String spinvSectionPathGameMode = this.getSectionPathForSPInvGameMode(specialInventoryName);
        playerFileConfiguration.set(playerSectionPathContents, spinvFileConfiguration.get(spinvSectionPathContents));
        playerFileConfiguration.set(playerSectionPathArmor, spinvFileConfiguration.get(spinvSectionPathArmor));
        playerFileConfiguration.set(playerSectionPathPotion, spinvFileConfiguration.get(spinvSectionPathPotion));
        playerFileConfiguration.set(playerSectionPathGameMode, spinvFileConfiguration.get(spinvSectionPathGameMode));
        playerFileConfiguration.save(playerInventoryFile);
      }
    } else {
      throw new Exception("Wrong destination index.");
    }
  }

  public void restoreInventory(final CommandSender player) throws Exception
  {
    final int index = this.getCurrentInventoryIndex(((Player) player).getName());
    this.loadInventory((Player) player, index);
  }

  public void deleteSpecialInventory(final File file, final String name) throws IOException
  {
    final FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);
    this.plugin.getLogger().warning(this.getSectionPathForSPInvRoot(name));
    fileConfiguration.set(this.getSectionPathForSPInvRoot(name), (Object) null);
    fileConfiguration.save(file);
  }

  private void deleteAllSPInventoryFromUser(final String playerName) throws Exception
  {
    final File playerFile = this.getInventoryFile(playerName);
    final String[] playerSPInvList = this.getListSpecialInventory(playerFile);
    if (playerSPInvList.length != 0) {
      final String[] var7 = playerSPInvList;
      final int var6 = playerSPInvList.length;

      for (int var5 = 0; var5 < var6; ++var5) {
        final String name = var7[var5];
        this.deleteSpecialInventory(playerFile, name);
      }
    }

  }

  public void saveSpecialInventory(final Player player, final String name) throws Exception
  {
    this.saveInventory(player, name, true);
  }

  private void loadInventory(final Player player, final int index) throws Exception
  {
    this.loadInventory(player, String.valueOf(index), false);
  }

  private void loadSpecialInventory(final Player player, final String name) throws Exception
  {
    this.loadInventory(player, name, true);
  }

  public boolean getSpecialInventoryUsingStatus(final String playerName)
  {
    return this.getPlayersFileConfiguration(playerName).getBoolean("sp_using");
  }

  public void setSpecialInventoryUsingStatus(final String playerName, final boolean isUsing) throws IOException
  {
    this.setPlayerConfig(playerName, "sp_using", isUsing);
  }

  private String getCurrentSpecialInventoryIndex(final String playerName)
  {
    return this.getPlayersFileConfiguration(playerName).getString("sp_current", "");
  }

  private void setCurrentSpecialInventoryIndex(final String playerName, final String name) throws IOException
  {
    this.setPlayerConfig(playerName, "sp_current", name);
  }

  public int getCurrentInventoryIndex(final String playerName)
  {
    return this.getPlayersFileConfiguration(playerName).getInt("current", 1);
  }

  private void setCurrentInventoryIndex(final String playerName, final int index) throws IOException
  {
    this.setPlayerConfig(playerName, "current", index);
  }

  public boolean isFirstUseForToggleInventorySpecial(final String playerName)
  {
    return this.getPlayersFileConfiguration(playerName).getBoolean("sp_firstuse", true);
  }

  public void setSpecialInventoryUsingStatusForFirstUse(final String playerName, final boolean isFirstUse)
      throws IOException
  {
    this.setPlayerConfig(playerName, "sp_firstuse", isFirstUse);
  }

  public boolean isEnableGameModeSaving(final String playerName)
  {
    return this.getPlayersFileConfiguration(playerName).getBoolean("enable_gamemode_toggle", false);
  }

  public void setGameModeSaving(final String playerName, final boolean enable) throws IOException
  {
    this.setPlayerConfig(playerName, "enable_gamemode_toggle", enable);
  }

  private String getSectionPathForSPInvPotion(final String name)
  {
    return String.format("%s.potion", this.getSectionPathForSPInvRoot(name));
  }

  private String getSectionPathForUserInvPotion(final int index)
  {
    return String.format("inv%d.potion", index);
  }

  private String getSectionPathForSPInvGameMode(final String name)
  {
    return String.format("%s.gamemode", this.getSectionPathForSPInvRoot(name));
  }

  private String getSectionPathForUserInvGameMode(final int index)
  {
    return String.format("inv%d.gamemode", index);
  }

  private String getSectionPathForUserContents(final int index)
  {
    return String.format("inv%d.contents", index);
  }

  private String getSectionPathForUserArmor(final int index)
  {
    return String.format("inv%d.armor", index);
  }

  private String getSectionPathForSPInvRoot(final String name)
  {
    return String.format("special_inventories.%s", name);
  }

  private String getSectionPathForSPInvContents(final String name)
  {
    return String.format("%s.contents", this.getSectionPathForSPInvRoot(name));
  }

  private String getSectionPathForSPInvArmor(final String name)
  {
    return String.format("%s.armor", this.getSectionPathForSPInvRoot(name));
  }
}
