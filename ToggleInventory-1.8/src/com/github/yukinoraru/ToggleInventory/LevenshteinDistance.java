
package com.github.yukinoraru.ToggleInventory;

public final class LevenshteinDistance
{
  private LevenshteinDistance() {}
  private static int minimum(final int a, final int b, final int c)
  {
    return Math.min(Math.min(a, b), c);
  }

  public static int computeLevenshteinDistance(final CharSequence str1, final CharSequence str2)
  {
    final int[][] distance = new int[str1.length() + 1][str2.length() + 1];

    int i;
    for (i = 0; i <= str1.length(); distance[i][0] = i++) {
      //none
    }

    for (i = 1; i <= str2.length(); distance[0][i] = i++) {
      //none
    }

    for (i = 1; i <= str1.length(); ++i) {
      for (int j = 1; j <= str2.length(); ++j) {
        distance[i][j] = LevenshteinDistance.minimum(distance[i - 1][j] + 1, distance[i][j - 1] + 1,
            distance[i - 1][j - 1] + (str1.charAt(i - 1) == str2.charAt(j - 1) ? 0 : 1));
      }
    }

    return distance[str1.length()][str2.length()];
  }

  public static int find(final String[] list, final String str)
  {
    int minDist = 0;
    int target = 0;
    boolean isFirst = true;

    for (int i = 0; i < list.length; ++i) {
      final String l = list[i];
      int dist = LevenshteinDistance.computeLevenshteinDistance(l, str);
      if (l.startsWith(str)) {
        dist -= str.length() * 2;
      }

      if (isFirst) {
        target = i;
        minDist = dist;
        isFirst = false;
      } else if (dist < minDist) {
        minDist = dist;
        target = i;
      }
    }

    return target;
  }
}
