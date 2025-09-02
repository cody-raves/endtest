package cody.endtest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Endtest extends JavaPlugin {

    private FileConfiguration cfg;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cfg = getConfig();

        // Register events
        getServer().getPluginManager().registerEvents(new PortalListener(this), this);

        // Log End world status
        World end = findEndWorld();
        if (end == null) {
            getLogger().warning("No End world detected. Fallback: " +
                    cfg.getString("end_world_name_fallback", "world_the_end"));
        } else {
            getLogger().info("Detected End world: " + end.getName());
        }
    }

    public FileConfiguration cfg() { return cfg; }

    /** Return the End world if loaded; else try the configured fallback name. */
    public World findEndWorld() {
        for (World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() == World.Environment.THE_END) return w;
        }
        String name = cfg.getString("end_world_name_fallback", "world_the_end");
        return Bukkit.getWorld(name);
    }

    /** Configurable platform material with OBSIDIAN fallback. */
    public Material platformMaterial() {
        String mat = cfg.getString("platform_material", "OBSIDIAN");
        try {
            return Material.valueOf(mat);
        } catch (IllegalArgumentException ex) {
            getLogger().warning("Invalid platform_material '" + mat + "'. Using OBSIDIAN.");
            return Material.OBSIDIAN;
        }
    }
}
