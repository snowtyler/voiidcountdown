package voiidstudios.vct.managers;

import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.configs.MainConfigManager;

public class MessagesManager {
    private static String prefix;

	private MainConfigManager mainConfigManager;
	public MessagesManager(VoiidCountdownTimer plugin) {
		this.mainConfigManager = VoiidCountdownTimer.getConfigsManager().getMainConfigManager();
	}

    public static String getColoredMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getPrefix() {
		return prefix;
	}

	public static void setPrefix(String setprefix) {
		prefix = setprefix;
	}

	public void sendMessage(CommandSender sender, String message, boolean withPrefix){
		if(message == null || message.isEmpty()) return;
		boolean allowPrefix = withPrefix && mainConfigManager != null && mainConfigManager.isUsePrefix();
		if(allowPrefix){
			sender.sendMessage(getColoredMessage(VoiidCountdownTimer.prefix + message));
		}else{
			sender.sendMessage(getColoredMessage(message));
		}
	}

	public void sendConfigMessage(CommandSender sender, String path, boolean withPrefix, Map<String, String> replacements) {
		FileConfiguration config = mainConfigManager.getConfig();

		String message = config.getString(path);

		if (message != null && !message.isEmpty()) {
			if (replacements != null) {
				for (Map.Entry<String, String> entry : replacements.entrySet()) {
					message = message.replace(entry.getKey(), entry.getValue());
				}
			}

			boolean allowPrefix = withPrefix && mainConfigManager != null && mainConfigManager.isUsePrefix();
			if (allowPrefix) {
				sender.sendMessage(getColoredMessage(VoiidCountdownTimer.prefix + message));
			} else {
				sender.sendMessage(getColoredMessage(message));
			}
		}
	}
}
