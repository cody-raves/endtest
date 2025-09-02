package cody.endtest;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

public class PortalListener implements Listener {

    private final Endtest plugin;

    public PortalListener(Endtest plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.END_PORTAL) return;

        Player player = event.getPlayer();
        Location from = event.getFrom();

        World end = plugin.findEndWorld();
        if (end == null) {
            plugin.getLogger().warning("End world not found; keeping vanilla destination.");
            return;
        }

        // Keep same X/Z, choose Y by config
        double x = from.getX();
        double z = from.getZ();

        boolean keepSameY = plugin.cfg().getBoolean("keep_same_y", false);
        int yMin = plugin.cfg().getInt("y_min", 10);
        int yMax = plugin.cfg().getInt("y_max", 240);

        double y = keepSameY ? from.getY() : plugin.cfg().getInt("fixed_y", 80);
        y = Math.max(yMin, Math.min(yMax, y));

        // Preserve orientation
        float yaw = from.getYaw();
        float pitch = from.getPitch();

        Location target = new Location(end, x, y, z, yaw, pitch);

        // Safety handling
        if (!isSafe(target)) {
            if (plugin.cfg().getBoolean("build_platform_if_unsafe", true)) {
                target = buildSafetyPlatform(target);
            } else {
                target = findNearestSafe(target, 16);
            }
        }

        event.setTo(target);
        event.setCanCreatePortal(false); // don't let vanilla mess with it
    }

    // --- safety utils ---

    private boolean isSafe(Location loc) {
        if (loc.getWorld() == null) return false;

        Location feet = loc.clone();
        Location head = loc.clone().add(0, 1, 0);
        Location below = loc.clone().add(0, -1, 0);

        Material feetType = feet.getBlock().getType();
        Material headType = head.getBlock().getType();
        Material belowType = below.getBlock().getType();

        // Inside world limits
        if (loc.getY() < loc.getWorld().getMinHeight() + 1 ||
                loc.getY() > loc.getWorld().getMaxHeight() - 2) return false;

        boolean spaceClear = isPassable(feetType) && isPassable(headType);
        boolean groundSolid = belowType.isSolid();

        return spaceClear && groundSolid;
    }

    private boolean isPassable(Material m) {
        return m.isAir() || !m.isSolid();
    }

    private Location buildSafetyPlatform(Location center) {
        World w = center.getWorld();
        if (w == null) return center;

        int size = Math.max(1, plugin.cfg().getInt("platform_size", 3));
        if (size % 2 == 0) size++; // make odd for symmetry
        int half = size / 2;

        int y = Math.max(w.getMinHeight() + 1,
                Math.min(w.getMaxHeight() - 2, (int) Math.round(center.getY())));
        Material pad = plugin.platformMaterial();

        // Floor
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                Block b = w.getBlockAt((int) Math.floor(center.getX()) + dx,
                        y - 1,
                        (int) Math.floor(center.getZ()) + dz);
                b.setType(pad, false);
            }
        }

        // Clear headroom
        int headroom = Math.max(2, plugin.cfg().getInt("clear_headroom", 3));
        for (int dy = 0; dy < headroom; dy++) {
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    Block b = w.getBlockAt((int) Math.floor(center.getX()) + dx,
                            y + dy,
                            (int) Math.floor(center.getZ()) + dz);
                    if (b.getType().isSolid()) b.setType(Material.AIR, false);
                }
            }
        }

        return new Location(w, center.getX(), y, center.getZ(), center.getYaw(), center.getPitch());
    }

    private Location findNearestSafe(Location start, int maxUp) {
        World w = start.getWorld();
        if (w == null) return start;

        Location test = start.clone();
        for (int i = 0; i < maxUp; i++) {
            if (isSafe(test)) return test;
            test.add(new Vector(0, 1, 0));
        }

        // Try a reasonable default Y and scan up again
        test = new Location(w, start.getX(), 80, start.getZ(), start.getYaw(), start.getPitch());
        for (int i = 0; i < maxUp; i++) {
            if (isSafe(test)) return test;
            test.add(0, 1, 0);
        }
        return start;
    }
}
