package com.comze_instancelabs.mgwarlock;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.comze_instancelabs.minigamesapi.Arena;
import com.comze_instancelabs.minigamesapi.ArenaConfigStrings;
import com.comze_instancelabs.minigamesapi.ArenaSetup;
import com.comze_instancelabs.minigamesapi.ArenaState;
import com.comze_instancelabs.minigamesapi.MinigamesAPI;
import com.comze_instancelabs.minigamesapi.PluginInstance;
import com.comze_instancelabs.minigamesapi.commands.CommandHandler;
import com.comze_instancelabs.minigamesapi.config.ArenasConfig;
import com.comze_instancelabs.minigamesapi.config.DefaultConfig;
import com.comze_instancelabs.minigamesapi.config.MessagesConfig;
import com.comze_instancelabs.minigamesapi.config.StatsConfig;
import com.comze_instancelabs.minigamesapi.util.PlayerPickupItemHelper;
import com.comze_instancelabs.minigamesapi.util.Util;
import com.comze_instancelabs.minigamesapi.util.Validator;

public class Main extends JavaPlugin implements Listener {

	// allow custom arenas

	MinigamesAPI api = null;
	PluginInstance pli = null;
	static Main m = null;
	static int global_arenas_size = 30;

	HashMap<String, String> lastdamager = new HashMap<String, String>();

	public void onEnable() {
		m = this;
		api = MinigamesAPI.getAPI().setupAPI(this, "warlock", IArena.class, new ArenasConfig(this), new MessagesConfig(this), new IClassesConfig(this), new StatsConfig(this, false), new DefaultConfig(this, false), false);
		PluginInstance pinstance = api.pinstances.get(this);
		pli = pinstance;
		pinstance.addLoadedArenas(loadArenas(this, pinstance.getArenasConfig()));
		Bukkit.getPluginManager().registerEvents(this, this);
		pinstance.arenaSetup = new ArenaSetup();

		getConfig().addDefault("config.global_arenas_size", 30);
		getConfig().addDefault("config.bombs_blocks_damage", true);
		getConfig().options().copyDefaults(true);
		this.saveConfig();
		global_arenas_size = getConfig().getInt("config.global_arenas_size");
		
		new PlayerPickupItemHelper(this, this::onPlayerPickup);
	}

	public static ArrayList<Arena> loadArenas(JavaPlugin plugin, ArenasConfig cf) {
		ArrayList<Arena> ret = new ArrayList<Arena>();
		FileConfiguration config = cf.getConfig();
		if (!config.isSet("arenas")) {
			return ret;
		}
		for (String arena : config.getConfigurationSection(ArenaConfigStrings.ARENAS_PREFIX).getKeys(false)) {
			if (Validator.isArenaValid(plugin, arena, cf.getConfig())) {
				ret.add(initArena(arena));
			}
		}
		return ret;
	}

	public static IArena initArena(String arena) {
		IArena a = new IArena(m, arena);
		ArenaSetup s = MinigamesAPI.getAPI().pinstances.get(m).arenaSetup;
		a.init(Util.getSignLocationFromArena(m, arena), Util.getAllSpawns(m, arena), Util.getMainLobby(m), Util.getComponentForArena(m, arena, "lobby"), s.getPlayerCount(m, arena, true), s.getPlayerCount(m, arena, false), s.getArenaVIP(m, arena));
		a.setRadius(global_arenas_size);
		return a;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		CommandHandler ch = new CommandHandler();
		return ch.handleArgs(this, MinigamesAPI.getAPI().getPermissionGamePrefix("warlock"), "/" + cmd.getName(), sender, args);
	}

	public void onPlayerPickup(PlayerPickupItemHelper.CustomPickupEvent event) {
		if (pli.global_players.containsKey(event.getPlayer().getName())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerDrop(final PlayerDropItemEvent event) {
		if (pli.global_players.containsKey(event.getPlayer().getName())) {
			if (event.getItemDrop().getItemStack().getType() == Material.FIREBALL) {
				Bukkit.getScheduler().runTaskLater(m, new Runnable() {
					public void run() {
						Location l = event.getItemDrop().getLocation();
						l.getWorld().createExplosion(l.getX(), l.getY(), l.getZ(), 3.5F, false, getConfig().getBoolean("config.bombs_blocks_damage"));
						event.getItemDrop().remove();
					}
				}, 60L);
			} else {
				event.setCancelled(true);
			}
		}
	}

	private HashMap<String, Integer> pusage = new HashMap<String, Integer>();

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		final Player p = event.getPlayer();
		if (pli.global_players.containsKey(p.getName())) {
			if (event.hasItem()) {
				if (event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
					final ItemStack item = event.getItem();
					if (item.getType() == Material.STONE_HOE) {
						shoot(item, event.getPlayer(), 0, 124, 6, 1);
					} else if (item.getType() == Material.IRON_HOE) {
						shoot(item, event.getPlayer(), 1, 242, 8, 2);
					} else if (item.getType() == Material.DIAMOND_HOE) {
						shoot(item, event.getPlayer(), 2, 1554, 12, 2);
					} else if (item.getType() == Material.FIREBALL) {
						final Item item_ = p.getWorld().dropItem(p.getLocation().clone().add(0D, 1D, 0D), new ItemStack(Material.FIREBALL));
						item_.setVelocity(p.getLocation().getDirection().multiply(0.5D));
						Bukkit.getScheduler().runTaskLater(m, new Runnable() {
							public void run() {
								Location l = item_.getLocation();
								l.getWorld().createExplosion(l.getX(), l.getY(), l.getZ(), 3.5F, false, getConfig().getBoolean("config.bombs_blocks_damage"));
								item_.remove();
							}
						}, 60L);
						p.getInventory().remove(new ItemStack(Material.FIREBALL, 2));
						p.updateInventory();
					}
				}
			}
		}
	}

	public void shoot(ItemStack item, final Player p, int id, int durability, int durability_temp, int eggcount) {
		if (item.getDurability() < durability) { // 124
			for (int i = 0; i < eggcount; i++) {
				p.launchProjectile(Egg.class);
			}
			item.setDurability((short) (item.getDurability() + durability_temp)); // 6
		} else {
			if (!pusage.containsKey(p.getName())) {
				p.sendMessage(ChatColor.RED + "Please wait 3 seconds before using this gun again!");
				Bukkit.getScheduler().runTaskLater(m, new Runnable() {
					public void run() {
						p.updateInventory();
						p.getInventory().clear();
						p.updateInventory();
						pli.getClassesHandler().getClass(p.getName());
						if (pusage.containsKey(p.getName())) {
							pusage.remove(p.getName());
						}
					}
				}, 20L * 3);
				pusage.put(p.getName(), id);
			}
		}
	}

	@EventHandler
	public void onEgg(PlayerEggThrowEvent event) {
		if (pli.global_players.containsKey(event.getPlayer().getName())) {
			event.setHatching(false);
		}
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player) {
			Player p = (Player) event.getEntity();
			if (pli.global_players.containsKey(p.getName())) {
				IArena a = (IArena) pli.global_players.get(p.getName());
				if (a.getArenaState() == ArenaState.INGAME) {
					if (event.getCause() == DamageCause.ENTITY_ATTACK) {
						p.setHealth(20D);
						event.setCancelled(true);
						return;
					}
				}
			}
		}
	}

	@EventHandler
	public void onBreak(BlockBreakEvent event) {
		if (pli.global_players.containsKey(event.getPlayer().getName())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
			Player p = (Player) event.getEntity();
			Player attacker = (Player) event.getDamager();
			if (pli.global_players.containsKey(p.getName()) && pli.global_players.containsKey(attacker.getName())) {
				IArena a = (IArena) pli.global_players.get(p.getName());
				if (a.getArenaState() == ArenaState.INGAME) {
					lastdamager.put(p.getName(), attacker.getName());
				}
			}
		}
	}

}
