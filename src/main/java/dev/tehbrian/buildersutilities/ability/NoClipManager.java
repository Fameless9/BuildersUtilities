package dev.tehbrian.buildersutilities.ability;

import com.google.inject.Inject;
import dev.tehbrian.buildersutilities.BuildersUtilities;
import dev.tehbrian.buildersutilities.user.User;
import dev.tehbrian.buildersutilities.user.UserService;
import dev.tehbrian.buildersutilities.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class NoClipManager implements Listener {

  private final BuildersUtilities buildersUtilities;
  private final UserService userService;

  private final HashMap<UUID, GameMode> previousGameMode = new HashMap<>();
  private final List<UUID> playersOnNoClip = new ArrayList<>();

  @Inject
  public NoClipManager(
      final BuildersUtilities buildersUtilities,
      final UserService userService
  ) {
    this.buildersUtilities = buildersUtilities;
    this.userService = userService;
  }

  public void start() {
    Bukkit.getScheduler().runTaskTimer(this.buildersUtilities, this::checkForBlocks, 0, 1);
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerGameModeChange(final PlayerGameModeChangeEvent event) {
    if (this.playersOnNoClip.contains(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
      return;
    }
    this.previousGameMode.put(event.getPlayer().getUniqueId(), event.getNewGameMode());
  }

  @SuppressWarnings("deprecation") // <- no alternative to Player#isOnGround
  private void checkForBlocks() {
    for (final User user : this.userService.getUserMap().values()) {
      if (!this.isEligible(user)) {
        continue;
      }

      final Player player = user.getPlayer();
      assert player != null;
      // ^ null-checked in isEligible(user)
      final UUID uuid = user.getPlayer().getUniqueId();

      final boolean isOnGround = player.isOnGround();
      final boolean noClip;
      boolean tp = false;

      if (!this.playersOnNoClip.contains(uuid)) {
        if (isOnGround && player.isSneaking()) {
          noClip = true;
        } else {
          noClip = this.shouldNoClip(player);
          tp = isOnGround;
        }

        if (!noClip) {
          continue;
        }

        this.previousGameMode.put(uuid, player.getGameMode());
        this.playersOnNoClip.add(uuid);
        player.setGameMode(GameMode.SPECTATOR);
        if (tp) {
          player.teleport(player.getLocation());
        }
        continue;
      }
      if (isOnGround || this.shouldNoClip(player)) {
        continue;
      }

      player.setGameMode(this.previousGameMode.getOrDefault(uuid, GameMode.CREATIVE));
      this.playersOnNoClip.remove(uuid);
    }
  }

  private boolean shouldNoClip(final Player player) {
    final Location playerLocation = player.getLocation();
    final double checkRadius = 0.4;

    for (double x = -checkRadius; x <= checkRadius; x += 0.1) {
      for (double z = -checkRadius; z <= checkRadius; z += 0.1) {
        final Location checkLocation1 = playerLocation.clone().add(x, 0, z);
        final Location checkLocation2 = playerLocation.clone().add(x, 1, z);
        final Location checkLocation3 = playerLocation.clone().add(x, 1.9, z);
        if (checkLocation1.getBlock().getType().isCollidable()
          || checkLocation2.getBlock().getType().isCollidable()
          || checkLocation3.getBlock().getType().isCollidable()) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isEligible(final User user) {
    return user != null && user.noclipEnabled() && user.getPlayer() != null
        && user.getPlayer().isOnline() && user.getPlayer().hasPermission(Permissions.NOCLIP);
  }
}
