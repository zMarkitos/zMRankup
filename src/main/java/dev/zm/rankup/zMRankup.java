package dev.zm.rankup;

import dev.zm.rankup.api.ZMRankupAPI;
import dev.zm.rankup.command.CommandManager;
import dev.zm.rankup.config.ConfigManager;
import dev.zm.rankup.config.MessageManager;
import dev.zm.rankup.hook.HookManager;
import dev.zm.rankup.listener.PlayerListener;
import dev.zm.rankup.menu.MenuListener;
import dev.zm.rankup.menu.MenuManager;
import dev.zm.rankup.permission.PermissionManager;
import dev.zm.rankup.rank.RankManager;
import dev.zm.rankup.rank.TemplateManager;
import dev.zm.rankup.requirement.RequirementRegistry;
import dev.zm.rankup.reward.RewardRegistry;
import dev.zm.rankup.storage.PlayerDataCache;
import dev.zm.rankup.storage.StorageManager;
import dev.zm.rankup.placeholder.PlaceholderListManager;
import dev.zm.rankup.system.SystemManager;
import dev.zm.rankup.task.AutoRankupTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class zMRankup extends JavaPlugin {

    private static zMRankup instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private PlaceholderListManager placeholderListManager;
    private StorageManager storageManager;
    private PlayerDataCache playerDataCache;
    private HookManager hookManager;
    private RequirementRegistry requirementRegistry;
    private RewardRegistry rewardRegistry;
    private TemplateManager templateManager;
    private SystemManager systemManager;
    private PermissionManager permissionManager;
    private RankManager rankManager;
    private MenuManager menuManager;
    private CommandManager commandManager;
    private dev.zm.rankup.util.VersionChecker versionChecker;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        instance = this;

        // Configuration
        this.configManager = new ConfigManager(this);
        this.configManager.loadAll();

        this.placeholderListManager = new PlaceholderListManager(this);
        this.placeholderListManager.load();
        
        this.messageManager = new MessageManager(this);

        // Hooks
        this.hookManager = new HookManager(this);
        this.hookManager.setup();

        // Storage
        this.storageManager = new StorageManager(this);
        this.storageManager.initialize();
        this.playerDataCache = new PlayerDataCache(this);

        // Registries
        this.requirementRegistry = new RequirementRegistry(this);
        this.requirementRegistry.registerDefaults();
        
        this.rewardRegistry = new RewardRegistry(this);

        // Core systems
        this.templateManager = new TemplateManager(this);
        this.templateManager.loadTemplates();
        
        this.systemManager = new SystemManager(this);
        this.systemManager.loadAll();

        this.permissionManager = new PermissionManager(this);
        this.permissionManager.load();
        
        this.rankManager = new RankManager(this);
        
        this.menuManager = new MenuManager(this);
        this.menuManager.loadMenuConfig();

        // Commands & Listeners
        this.commandManager = new CommandManager(this);
        this.commandManager.register();
        
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(), this);

        // Tasks
        int autoRankupInterval = this.configManager.getAutoRankupInterval();
        if (autoRankupInterval > 0) {
            new AutoRankupTask(this).runTaskTimer(this, 20L * autoRankupInterval, 20L * autoRankupInterval);
        }

        // API setup
        ZMRankupAPI.init(this);

        this.versionChecker = new dev.zm.rankup.util.VersionChecker(this);
        this.versionChecker.refresh();

        // Load data for online players (in case of reload)
        for (Player p : Bukkit.getOnlinePlayers()) {
            this.permissionManager.ensureAttachment(p);
            this.playerDataCache.load(p);
        }

        long timeTaken = System.currentTimeMillis() - startTime;
        printStartupMessage(timeTaken);
    }

    @Override
    public void onDisable() {
        long startTime = System.currentTimeMillis();
        if (this.playerDataCache != null) {
            this.playerDataCache.saveAll();
        }
        if (this.permissionManager != null) {
            this.permissionManager.clearAll();
        }
        if (this.storageManager != null) {
            this.storageManager.shutdown();
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        printShutdownMessage(timeTaken);
    }

    private void printStartupMessage(long ms) {
        org.bukkit.command.ConsoleCommandSender console = Bukkit.getConsoleSender();
        String version = getDescription().getVersion();
        String author = getDescription().getAuthors().isEmpty() ? "zMDev" : getDescription().getAuthors().get(0);
        int systems = systemManager.getAllSystems().size();
        int ranks = systemManager.getAllSystems().stream().mapToInt(s -> s.getAllRanks().size()).sum();
        String vaultStr = hookManager.isVaultEnabled() ? "<color:#2ECC71>Hooked" : "<color:#E74C3C>Not found";
        String papiStr = hookManager.isPlaceholderAPIEnabled() ? "<color:#2ECC71>Hooked" : "<color:#E74C3C>Not found";
        String autoRankupStr = configManager.getAutoRankupInterval() > 0 ? "<color:#2ECC71>Enabled" : "<color:#E74C3C>Disabled";
        
        console.sendMessage(dev.zm.rankup.util.ColorUtil.parse("<dark_gray>──────────────────────────────────────────"));
        console.sendMessage(dev.zm.rankup.util.ColorUtil.parse("<red>zMRankup <gray>v" + version + " <dark_gray>by <red>" + author));
        console.sendMessage(dev.zm.rankup.util.ColorUtil.parse(""));
        console.sendMessage(dev.zm.rankup.util.ColorUtil.parse("  <dark_gray>• <gray>Systems loaded: <color:#F1C40F>" + systems));
        console.sendMessage(dev.zm.rankup.util.ColorUtil.parse("  <dark_gray>• <gray>Ranks loaded: <color:#F1C40F>" + ranks));
        console.sendMessage(dev.zm.rankup.util.ColorUtil.parse("  <dark_gray>• <gray>Vault: " + vaultStr));
        console.sendMessage(dev.zm.rankup.util.ColorUtil.parse("  <dark_gray>• <gray>PlaceholderAPI: " + papiStr));
        console.sendMessage(dev.zm.rankup.util.ColorUtil.parse("  <dark_gray>• <gray>Auto-rankup: " + autoRankupStr));
        console.sendMessage(dev.zm.rankup.util.ColorUtil.parse(""));
        console.sendMessage(dev.zm.rankup.util.ColorUtil.parse("<color:#2ECC71>✓ <white>Plugin enabled successfully <gray>(" + ms + "ms)"));
        console.sendMessage(dev.zm.rankup.util.ColorUtil.parse("<dark_gray>──────────────────────────────────────────"));
    }

    private void printShutdownMessage(long ms) {
        org.bukkit.command.ConsoleCommandSender console = Bukkit.getConsoleSender();
        String version = getDescription().getVersion();
        console.sendMessage(dev.zm.rankup.util.ColorUtil.parse("<dark_gray>──────────────────────────────────────────"));
        console.sendMessage(dev.zm.rankup.util.ColorUtil.parse("<red>zMRankup <gray>v" + version + " <dark_gray>is shutting down..."));
        console.sendMessage(dev.zm.rankup.util.ColorUtil.parse("<color:#E74C3C>✗ <white>Plugin disabled successfully <gray>(" + ms + "ms)"));
        console.sendMessage(dev.zm.rankup.util.ColorUtil.parse("<dark_gray>──────────────────────────────────────────"));
    }

    public void reloadPlugin() {
        this.configManager.reload();
        this.placeholderListManager.reload();
        this.hookManager.setup(); // Try to hook again in case plugins loaded later
        this.templateManager.loadTemplates();
        this.systemManager.reload();
        this.permissionManager.reload();
        this.menuManager.loadMenuConfig();
        if (this.commandManager != null) {
            this.commandManager.reloadDynamicCommands();
        }
        this.permissionManager.refreshAllOnlinePlayers();
    }

    public static zMRankup getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public PlaceholderListManager getPlaceholderListManager() {
        return placeholderListManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public PlayerDataCache getPlayerDataCache() {
        return playerDataCache;
    }

    public HookManager getHookManager() {
        return hookManager;
    }

    public RequirementRegistry getRequirementRegistry() {
        return requirementRegistry;
    }

    public RewardRegistry getRewardRegistry() {
        return rewardRegistry;
    }

    public TemplateManager getTemplateManager() {
        return templateManager;
    }

    public SystemManager getSystemManager() {
        return systemManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public RankManager getRankManager() {
        return rankManager;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public dev.zm.rankup.util.VersionChecker getVersionChecker() {
        return versionChecker;
    }
}
