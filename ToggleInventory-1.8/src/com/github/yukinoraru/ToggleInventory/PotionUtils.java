
package com.github.yukinoraru.ToggleInventory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.potion.PotionEffect;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

public final class PotionUtils
{
  private PotionUtils()
  {
  }

  static String serializePotion(final Collection<PotionEffect> collection) throws IOException
  {
    StringBuilder result = new StringBuilder();

    HashMap<Object,Object> tmp;
    for (final Iterator<PotionEffect> var3 = collection.iterator(); var3
        .hasNext(); result.append(PotionUtils.serialize(tmp) + ",")) {
      final PotionEffect effect = (PotionEffect) var3.next();
      tmp = new HashMap<>(effect.serialize());
    }

    return result.toString();
  }

  static Collection<PotionEffect> deserializePotion(final String effectsInString) throws IOException, ClassNotFoundException
  {
    final Collection<PotionEffect> result = new ArrayList<>();
    if ((effectsInString != null) && (effectsInString.length() != 0)) {
      String[] var5;
      final int var4 = (var5 = effectsInString.split(",")).length;

      for (int var3 = 0; var3 < var4; ++var3) {
        final String effectInSring = var5[var3];
        if ((effectInSring != null) && (effectInSring.length() != 0)) {
          @SuppressWarnings("unchecked")
          final Map<String, Object> tmp = (Map<String,Object>) PotionUtils.deserialize(effectInSring);
          final PotionEffect potionEffect = new PotionEffect(tmp);
          result.add(potionEffect);
        }
      }

      return result;
    } else {
      return result;
    }
  }

  static Object deserialize(final String s) throws IOException, ClassNotFoundException
  {
    final byte[] data = Base64Coder.decode(s);
    final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
    final Object o = ois.readObject();
    ois.close();
    return o;
  }

  static String serialize(final Serializable o) throws IOException
  {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(o);
    oos.close();
    return new String(Base64Coder.encode(baos.toByteArray()));
  }
}
