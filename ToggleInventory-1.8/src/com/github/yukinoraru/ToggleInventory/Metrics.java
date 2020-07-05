
package com.github.yukinoraru.ToggleInventory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.scheduler.BukkitTask;

public class Metrics
{
  private final Plugin plugin;
  private final Set<Object> graphs = Collections.<Object>synchronizedSet(new HashSet<Object>());
  private final Metrics.Graph defaultGraph = new Metrics.Graph("Default", (Metrics.Graph) null);
  private final YamlConfiguration configuration;
  private final File configurationFile;
  private final String guid;
  private final boolean debug;
  private final Object optOutLock = new Object();
  private volatile BukkitTask task;

  public Metrics(final Plugin plugin) throws IOException
  {
    if (plugin == null) {
      throw new IllegalArgumentException("Plugin cannot be null");
    } else {
      this.plugin = plugin;
      this.configurationFile = this.getConfigFile();
      this.configuration = YamlConfiguration.loadConfiguration(this.configurationFile);
      this.configuration.addDefault("opt-out", false);
      this.configuration.addDefault("guid", UUID.randomUUID().toString());
      this.configuration.addDefault("debug", false);
      if (this.configuration.get("guid", (Object) null) == null) {
        this.configuration.options().header("http://mcstats.org").copyDefaults(true);
        this.configuration.save(this.configurationFile);
      }

      this.guid = this.configuration.getString("guid");
      this.debug = this.configuration.getBoolean("debug", false);
    }
  }

  public Metrics.Graph createGraph(final String name)
  {
    if (name == null) {
      throw new IllegalArgumentException("Graph name cannot be null");
    } else {
      final Metrics.Graph graph = new Metrics.Graph(name, (Metrics.Graph) null);
      this.graphs.add(graph);
      return graph;
    }
  }

  public void addGraph(final Metrics.Graph graph)
  {
    if (graph == null) {
      throw new IllegalArgumentException("Graph cannot be null");
    } else {
      this.graphs.add(graph);
    }
  }

  public void addCustomData(final Metrics.AbstractPlotter abstractPlotter)
  {
    if (abstractPlotter == null) {
      throw new IllegalArgumentException("AbstractPlotter cannot be null");
    } else {
      this.defaultGraph.addPlotter(abstractPlotter);
      this.graphs.add(this.defaultGraph);
    }
  }

  public boolean start()
  {
    synchronized (this.optOutLock) {
      if (this.isOptOut()) {
        return false;
      } else if (this.task != null) {
        return true;
      } else {
        this.task = this.plugin.getServer().getScheduler().runTaskTimerAsynchronously(this.plugin, new Runnable()
        {
          private boolean firstPost = true;

          public void run()
          {
            try {
              synchronized (Metrics.this.optOutLock) {
                if (Metrics.this.isOptOut() && (Metrics.this.task != null)) {
                  Metrics.this.task.cancel();
                  Metrics.this.task = null;
                  final Iterator<?> var3 = Metrics.this.graphs.iterator();

                  while (var3.hasNext()) {
                    final Metrics.Graph graph = (Metrics.Graph) var3.next();
                    graph.onOptOut();
                  }
                }
              }

              Metrics.this.postPlugin(!this.firstPost);
              this.firstPost = false;
            } catch (final IOException var5) {
              if (Metrics.this.debug) {
                Bukkit.getLogger().log(Level.INFO, "[Metrics] " + var5.getMessage());
              }
            }

          }
        }, 0L, 12000L);
        return true;
      }
    }
  }

  public boolean isOptOut()
  {
    synchronized (this.optOutLock) {
      try {
        this.configuration.load(this.getConfigFile());
      } catch (final IOException var3) {
        if (this.debug) {
          Bukkit.getLogger().log(Level.INFO, "[Metrics] " + var3.getMessage());
        }

        return true;
      } catch (final InvalidConfigurationException var4) {
        if (this.debug) {
          Bukkit.getLogger().log(Level.INFO, "[Metrics] " + var4.getMessage());
        }

        return true;
      }

      return this.configuration.getBoolean("opt-out", false);
    }
  }

  public void enable() throws IOException
  {
    synchronized (this.optOutLock) {
      if (this.isOptOut()) {
        this.configuration.set("opt-out", false);
        this.configuration.save(this.configurationFile);
      }

      if (this.task == null) {
        this.start();
      }

    }
  }

  public void disable() throws IOException
  {
    synchronized (this.optOutLock) {
      if (!this.isOptOut()) {
        this.configuration.set("opt-out", true);
        this.configuration.save(this.configurationFile);
      }

      if (this.task != null) {
        this.task.cancel();
        this.task = null;
      }

    }
  }

  public File getConfigFile()
  {
    final File pluginsFolder = this.plugin.getDataFolder().getParentFile();
    return new File(new File(pluginsFolder, "PluginMetrics"), "config.yml");
  }

  private void postPlugin(final boolean isPing) throws IOException
  {
    final PluginDescriptionFile description = this.plugin.getDescription();
    final String pluginName = description.getName();
    final boolean onlineMode = Bukkit.getServer().getOnlineMode();
    final String pluginVersion = description.getVersion();
    final String serverVersion = Bukkit.getVersion();
    final int playersOnline = Bukkit.getServer().getOnlinePlayers().size();
    final StringBuilder data = new StringBuilder();
    data.append(Metrics.encode("guid")).append('=').append(Metrics.encode(this.guid));
    Metrics.encodeDataPair(data, "version", pluginVersion);
    Metrics.encodeDataPair(data, "server", serverVersion);
    Metrics.encodeDataPair(data, "players", Integer.toString(playersOnline));
    Metrics.encodeDataPair(data, "revision", String.valueOf(6));
    final String osname = System.getProperty("os.name");
    String osarch = System.getProperty("os.arch");
    final String osversion = System.getProperty("os.version");
    final String java_version = System.getProperty("java.version");
    final int coreCount = Runtime.getRuntime().availableProcessors();
    if ("amd64".equals(osarch)) {
      osarch = "x86_64";
    }

    Metrics.encodeDataPair(data, "osname", osname);
    Metrics.encodeDataPair(data, "osarch", osarch);
    Metrics.encodeDataPair(data, "osversion", osversion);
    Metrics.encodeDataPair(data, "cores", Integer.toString(coreCount));
    Metrics.encodeDataPair(data, "online-mode", Boolean.toString(onlineMode));
    Metrics.encodeDataPair(data, "java_version", java_version);
    if (isPing) {
      Metrics.encodeDataPair(data, "ping", "true");
    }

    synchronized (this.graphs) {
      final Iterator<?> iter = this.graphs.iterator();

      while (true) {
        if (!iter.hasNext()) {
          break;
        }

        final Metrics.Graph graph = (Metrics.Graph) iter.next();
        final Iterator<?> var18 = graph.getPlotters().iterator();

        while (var18.hasNext()) {
          final Metrics.AbstractPlotter abstractPlotter = (Metrics.AbstractPlotter) var18.next();
          final String key = String.format("C%s%s%s%s", "~~", graph.getName(), "~~", abstractPlotter.getColumnName());
          final String value = Integer.toString(abstractPlotter.getValue());
          Metrics.encodeDataPair(data, key, value);
        }
      }
    }

    final URL url = new URL("http://mcstats.org" + String.format("/report/%s", Metrics.encode(pluginName)));
    URLConnection connection;
    if (this.isMineshafterPresent()) {
      connection = url.openConnection(Proxy.NO_PROXY);
    } else {
      connection = url.openConnection();
    }

    connection.setDoOutput(true);
    final OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
    writer.write(data.toString());
    writer.flush();
    final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    final String response = reader.readLine();
    writer.close();
    reader.close();
    if ((response != null) && !response.startsWith("ERR")) {
      if (response.contains("OK This is your first update this hour")) {
        synchronized (this.graphs) {
          final Iterator<?> iter = this.graphs.iterator();

          while (iter.hasNext()) {
            final Metrics.Graph graph = (Metrics.Graph) iter.next();
            final Iterator<?> var23 = graph.getPlotters().iterator();

            while (var23.hasNext()) {
              final Metrics.AbstractPlotter abstractPlotter = (Metrics.AbstractPlotter) var23.next();
              abstractPlotter.reset();
            }
          }
        }
      }

    } else {
      throw new IOException(response);
    }
  }

  private boolean isMineshafterPresent()
  {
    try {
      Class.forName("mineshafter.MineServer");
      return true;
    } catch (final Exception var2) {
      return false;
    }
  }

  private static void encodeDataPair(final StringBuilder buffer, final String key, final String value)
      throws UnsupportedEncodingException
  {
    buffer.append('&').append(Metrics.encode(key)).append('=').append(Metrics.encode(value));
  }

  private static String encode(final String text) throws UnsupportedEncodingException
  {
    return URLEncoder.encode(text, "UTF-8");
  }

  public static class Graph
  {
    private final String name;
    private final Set<Object> plotters;

    private Graph(final String name)
    {
      this.plotters = new LinkedHashSet<>();
      this.name = name;
    }

    public String getName()
    {
      return this.name;
    }

    public void addPlotter(final Metrics.AbstractPlotter abstractPlotter)
    {
      this.plotters.add(abstractPlotter);
    }

    public void removePlotter(final Metrics.AbstractPlotter abstractPlotter)
    {
      this.plotters.remove(abstractPlotter);
    }

    public Set<Object> getPlotters()
    {
      return Collections.unmodifiableSet(this.plotters);
    }

    @Override
    public int hashCode()
    {
      return this.name.hashCode();
    }

    @Override
    public boolean equals(final Object object)
    {
      if (!(object instanceof Metrics.Graph)) {
        return false;
      } else {
        final Metrics.Graph graph = (Metrics.Graph) object;
        return graph.name.equals(this.name);
      }
    }

    protected void onOptOut()
    {
      //unimplemented
    }

    // $FF: synthetic method
    Graph(final String var1, final Metrics.Graph var2)
    {
      this(var1);
    }
  }

  public abstract static class AbstractPlotter
  {
    private final String name;

    public AbstractPlotter()
    {
      this("Default");
    }

    public AbstractPlotter(final String name)
    {
      this.name = name;
    }

    public abstract int getValue();

    public String getColumnName()
    {
      return this.name;
    }

    public void reset()
    {
    }

    @Override
    public int hashCode()
    {
      return this.getColumnName().hashCode();
    }

    @Override
    public boolean equals(final Object object)
    {
      if (!(object instanceof Metrics.AbstractPlotter)) {
        return false;
      } else {
        final Metrics.AbstractPlotter abstractPlotter = (Metrics.AbstractPlotter) object;
        return abstractPlotter.name.equals(this.name) && (abstractPlotter.getValue() == this.getValue());
      }
    }
  }
}
