package xyz.tehbrian.buildersutilities.commands;

import com.google.inject.Inject;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.tehbrian.buildersutilities.config.Lang;
import xyz.tehbrian.buildersutilities.user.UserManager;

public final class AdvancedFlyCommand implements CommandExecutor {

    private final UserManager userManager;
    private final Lang lang;

    @Inject
    public AdvancedFlyCommand(
            final @NonNull UserManager userManager,
            final @NonNull Lang lang
    ) {
        this.userManager = userManager;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (this.userManager.getUser(player).toggleAdvancedFlyEnabled()) {
                player.sendMessage(this.lang.c("messages.commands.advanced_fly.enabled"));
            } else {
                player.sendMessage(this.lang.c("messages.commands.advanced_fly.disabled"));
            }
        }
        return true;
    }

}
