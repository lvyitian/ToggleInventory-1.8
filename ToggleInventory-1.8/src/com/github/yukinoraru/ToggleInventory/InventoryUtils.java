
package com.github.yukinoraru.ToggleInventory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import net.minecraft.server.v1_8_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;

public final class InventoryUtils
{
  private InventoryUtils()
  {
  }

  private static String versionPrefix = "";
  private static Class<?> class_ItemStack;
  private static Class<?> class_NBTBase;
  private static Class<?> class_NBTTagCompound;
  private static Class<?> class_NBTTagList;
  private static Class<?> class_CraftInventoryCustom;
  private static Class<?> class_CraftItemStack;

  static {
    try {
      final String className = Bukkit.getServer().getClass().getName();
      final String[] packages = className.split("\\.");
      if (packages.length == 5) {
        InventoryUtils.versionPrefix = packages[3] + ".";
      }

      InventoryUtils.class_ItemStack = InventoryUtils.fixBukkitClass("net.minecraft.server.ItemStack");
      InventoryUtils.class_NBTBase = InventoryUtils.fixBukkitClass("net.minecraft.server.NBTBase");
      InventoryUtils.fixBukkitClass("net.minecraft.server.NBTCompressedStreamTools");
      InventoryUtils.class_NBTTagCompound = InventoryUtils.fixBukkitClass("net.minecraft.server.NBTTagCompound");
      InventoryUtils.class_NBTTagList = InventoryUtils.fixBukkitClass("net.minecraft.server.NBTTagList");
      InventoryUtils.class_CraftInventoryCustom = InventoryUtils
          .fixBukkitClass("org.bukkit.craftbukkit.inventory.CraftInventoryCustom");
      InventoryUtils.class_CraftItemStack = InventoryUtils
          .fixBukkitClass("org.bukkit.craftbukkit.inventory.CraftItemStack");
    } catch (final Throwable var2) {
      var2.printStackTrace();
    }

  }

  private static Class<?> fixBukkitClass(String className)
  {
    className = className.replace("org.bukkit.craftbukkit.", "org.bukkit.craftbukkit." + InventoryUtils.versionPrefix);
    className = className.replace("net.minecraft.server.", "net.minecraft.server." + InventoryUtils.versionPrefix);

    try {
      return Class.forName(className);
    } catch (final ClassNotFoundException var2) {
      var2.printStackTrace();
      return null;
    }
  }

  protected static Object getNMSCopy(final ItemStack stack)
  {
    Object nms = null;

    try {
      final Method copyMethod = InventoryUtils.class_CraftItemStack.getMethod("asNMSCopy", ItemStack.class);
      nms = copyMethod.invoke((Object) null, stack);
    } catch (final Throwable var3) {
      var3.printStackTrace();
    }

    return nms;
  }

  protected static Object getHandle(final ItemStack stack)
  {
    Object handle = null;

    try {
      final Field handleField = stack.getClass().getDeclaredField("handle");
      handleField.setAccessible(true);
      handle = handleField.get(stack);
    } catch (final Throwable var3) {
      var3.printStackTrace();
    }

    return handle;
  }

  protected static Object getTag(final Object mcItemStack)
  {
    Object tag = null;

    try {
      final Field tagField = InventoryUtils.class_ItemStack.getField("tag");
      tag = tagField.get(mcItemStack);
    } catch (final Throwable var3) {
      var3.printStackTrace();
    }

    return tag;
  }

  public static ItemStack getCopy(ItemStack stack)
  {
    if (stack == null) {
      return null;
    } else {
      try {
        final Object craft = InventoryUtils.getNMSCopy(stack);
        if (craft != null) {
          final Method mirrorMethod = InventoryUtils.class_CraftItemStack.getMethod("asCraftMirror", craft.getClass());
          stack = (ItemStack) mirrorMethod.invoke((Object) null, craft);
        }
      } catch (final Throwable var3) {
        var3.printStackTrace();
      }

      return stack;
    }
  }

  public static String getMeta(final ItemStack stack, final String tag, final String defaultValue)
  {
    final String result = InventoryUtils.getMeta(stack, tag);
    return result == null ? defaultValue : result;
  }

  public static String getMeta(final ItemStack stack, final String tag)
  {
    if (stack == null) {
      return null;
    } else {
      String meta = null;

      try {
        final Object craft = InventoryUtils.getHandle(stack);
        if (craft == null) {
          return null;
        }

        final Object tagObject = InventoryUtils.getTag(craft);
        if (tagObject == null) {
          return null;
        }

        final Method getStringMethod = InventoryUtils.class_NBTTagCompound.getMethod("getString", String.class);
        meta = (String) getStringMethod.invoke(tagObject, tag);
      } catch (final Throwable var6) {
        var6.printStackTrace();
      }

      return meta;
    }
  }

  public static void setMeta(final ItemStack stack, final String tag, final String value)
  {
    if (stack != null) {
      try {
        final Object craft = InventoryUtils.getHandle(stack);
        final Object tagObject = InventoryUtils.getTag(craft);
        final Method setStringMethod = InventoryUtils.class_NBTTagCompound.getMethod("setString", String.class,
            String.class);
        setStringMethod.invoke(tagObject, tag, value);
      } catch (final Throwable var6) {
        var6.printStackTrace();
      }

    }
  }

  public static void addGlow(final ItemStack stack)
  {
    if (stack != null) {
      try {
        final Object craft = InventoryUtils.getHandle(stack);
        final Object tagObject = InventoryUtils.getTag(craft);
        final Object enchList = InventoryUtils.class_NBTTagList.newInstance();
        final Method setMethod = InventoryUtils.class_NBTTagCompound.getMethod("set", String.class,
            InventoryUtils.class_NBTBase);
        setMethod.invoke(tagObject, "ench", enchList);
      } catch (final Throwable var5) {
        var5.printStackTrace();
      }

    }
  }

  public static String inventoryToString(final Inventory inventory)
  {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final DataOutputStream dataOutput = new DataOutputStream(outputStream);

    try {
      final NBTTagList itemList = new NBTTagList();

      for (int i = 0; i < inventory.getSize(); ++i) {
        final NBTTagCompound outputObject = new NBTTagCompound();
        Object craft = null;
        final CraftItemStack is = (CraftItemStack) inventory.getItem(i);
        if (is != null) {
          craft = InventoryUtils.getNMSCopy(is);
        } else {
          craft = null;
        }

        if ((craft != null) && InventoryUtils.class_ItemStack.isInstance(craft)) {
          outputObject.setByte("Slot", (byte) i);
          CraftItemStack.asNMSCopy(is).save(outputObject);
          itemList.add(outputObject);
        }
      }

      final NBTTagCompound tag = new NBTTagCompound();
      tag.set("Items", itemList);
      NBTCompressedStreamTools.a(tag, (DataOutput) dataOutput);
    } catch (final Throwable var8) {
      var8.printStackTrace();
    }

    return (new BigInteger(1, outputStream.toByteArray())).toString(32);
  }

  public static Inventory stringToInventory(final String data) throws Exception
  {
    Inventory inventory = null;

    try {
      final ByteArrayInputStream dataInput = new ByteArrayInputStream((new BigInteger(data, 32)).toByteArray());
      final InputStream inputStream = new DataInputStream(dataInput);
      final NBTTagCompound tagCompound = NBTCompressedStreamTools.a(inputStream);
      final NBTTagList itemList = tagCompound.getList("Items", 10);
      int maxSlot = 0;

      int i;
      for (i = 0; i < itemList.size(); ++i) {
        final int tmp = itemList.get(i).getByte("Slot") & 255;
        if (maxSlot < tmp) {
          maxSlot = tmp;
        }
      }

      inventory = InventoryUtils.createInventory((InventoryHolder) null, maxSlot + 1);

      for (i = 0; i < itemList.size(); ++i) {
        final NBTTagCompound tmpTagCompound = itemList.get(i);
        final int slot = tmpTagCompound.getByte("Slot") & 255;
        if ((slot >= 0) && (inventory != null) && (slot < inventory.getSize())) {
          final net.minecraft.server.v1_8_R3.ItemStack itemStack = net.minecraft.server.v1_8_R3.ItemStack.createStack(
              tmpTagCompound);
          final CraftItemStack craftItemStack = CraftItemStack.asCraftMirror(itemStack);
          inventory.setItem(slot, craftItemStack);
        }
      }

      return inventory;
    } catch (final Exception var12) {
      var12.printStackTrace();
      throw new Exception("Sorry, your inventory isn't compatible with this version. Clear and create new one.");
    }
  }

  public static Inventory createInventory(final InventoryHolder holder, final int size)
  {
    Inventory inventory = null;

    try {
      final Constructor<?> inventoryConstructor = InventoryUtils.class_CraftInventoryCustom
          .getConstructor(InventoryHolder.class, Integer.TYPE);
      inventory = (Inventory) inventoryConstructor.newInstance(holder, size);
    } catch (final Throwable var4) {
      var4.printStackTrace();
    }

    return inventory;
  }

  public static boolean inventorySetItem(final Inventory inventory, final int index, final ItemStack item)
  {
    try {
      final Method setItemMethod = InventoryUtils.class_CraftInventoryCustom.getMethod("setItem", Integer.TYPE,
          ItemStack.class);
      setItemMethod.invoke(inventory, index, item);
      return true;
    } catch (final Throwable var4) {
      var4.printStackTrace();
      return false;
    }
  }

  public static Inventory getArmorInventory(final PlayerInventory playerInventory)
  {
    final ItemStack[] armor = playerInventory.getArmorContents();
    final Inventory inventory = InventoryUtils.createInventory((InventoryHolder) null, armor.length);

    for (int i = 0; i < armor.length; ++i) {
      if (inventory != null) {
        inventory.setItem(i, armor[i]);
      }
    }

    return inventory;
  }
}
