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
  public NoClipManager(final BuildersUtilities buildersUtilities, final UserService userService) {
    this.buildersUtilities = buildersUtilities;
    this.userService = userService;
  }

  public void start() {
    Bukkit.getScheduler().runTaskTimer(this.buildersUtilities, this::checkForUpdates, 0, 1);
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerGameModeChange(final PlayerGameModeChangeEvent event) {
    if (this.playersOnNoClip.contains(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
      return;
    }
    this.previousGameMode.put(event.getPlayer().getUniqueId(), event.getNewGameMode());
  }

  private void checkForUpdates() {
    for (final User user : this.userService.getUserMap().values()) {
      if (!this.isEligible(user)) {
        continue;
      }

      final Player player = user.getPlayer();
      assert player != null; // <- null-checked in isEligible(user)
      final UUID uuid = user.getPlayer().getUniqueId();
      final boolean noClip = this.shouldNoClip(player);

      if (!this.playersOnNoClip.contains(uuid) && noClip) {
        this.previousGameMode.put(uuid, player.getGameMode());
        this.playersOnNoClip.add(uuid);
        player.setGameMode(GameMode.SPECTATOR);
        continue;
      }

      if (!noClip) {
        player.setGameMode(this.previousGameMode.getOrDefault(uuid, GameMode.CREATIVE));
        this.playersOnNoClip.remove(uuid);
      }
    }
  }

  @SuppressWarnings("deprecation") // <- no alternative to Player#isOnGround
  private boolean shouldNoClip(final Player player) {
    if (player.isSneaking() && player.isOnGround()) {
      return true;
    }

    final Location playerLocation = player.getLocation();

    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < 4; j++) {
        boolean negative = i == 0;
        boolean ignoreX = j == 0 || j == 3;
        boolean ignoreZ = j == 1 || j == 3;

        final boolean checkLocation1 = playerLocation
            .clone()
            .add(ignoreX ? 0 : negative ? -0.4 : 0.4, 0, ignoreZ ? 0 : negative ? 0.4 : -0.4)
            .getBlock()
            .isCollidable();
        final boolean checkLocation2 = playerLocation
            .clone()
            .add(ignoreX ? 0 : negative ? -0.4 : 0.4, 1, ignoreZ ? 0 : negative ? 0.4 : -0.4)
            .getBlock()
            .isCollidable();
        final boolean checkLocation3 = playerLocation
            .clone()
            .add(ignoreX ? 0 : negative ? -0.4 : 0.4, 1.9, ignoreZ ? 0 : negative ? 0.4 : -0.4)
            .getBlock()
            .isCollidable();

        if (checkLocation1 || checkLocation2 || checkLocation3) {
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
