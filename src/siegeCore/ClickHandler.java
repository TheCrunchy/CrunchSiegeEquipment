package siegeCore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ArmorStand.LockType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.EulerAngle;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.actions.TownyDestroyEvent;
import com.palmergames.bukkit.towny.object.TownBlock;

import CrunchProjectiles.ExplosiveProjectile;

public class ClickHandler implements Listener {

	public float MinDelay = 5;

	public static HashMap<UUID, ExplosiveProjectile> projectiles = new HashMap<UUID, ExplosiveProjectile>();
	@EventHandler
	public void onExplode(EntityExplodeEvent e) {
		for (Entity entity : e.getEntity().getNearbyEntities(e.getYield() + 1, e.getYield()+ 1, e.getYield()+ 1)) {
			if(entity instanceof ArmorStand) {
				ArmorStand stand = (ArmorStand) entity;
				if (CrunchSiegeCore.towny != null) {
					TownBlock block = TownyAPI.getInstance().getTownBlock(stand.getLocation());
					if (block != null) {
						if (block.hasTown()) {
							if (!block.getTownBlockOwner().getPermissions().explosion) {
								return;
							}
						}
					}
				}
				stand.remove();
			}
		}
	}

	@EventHandler
	public void onHit(ProjectileHitEvent event) {
		if ((event.getEntity() instanceof Snowball) && projectiles.containsKey(event.getEntity().getUniqueId())) {
			ExplosiveProjectile proj = projectiles.get(event.getEntity().getUniqueId());
			Entity snowball = event.getEntity();
			Snowball ball = (Snowball) snowball;
			Player player = (Player) ball.getShooter();
			if (player != null) {
				player.sendMessage("Distance to impact: " + String.format("%.2f",player.getLocation().distance(ball.getLocation())));
			}
			Location loc = snowball.getLocation();
			World world = event.getEntity().getWorld();
			//	world.createExplosion(loc, proj.Radius, proj.DoFire);
			Entity tnt = event.getEntity().getWorld().spawnEntity(loc, EntityType.PRIMED_TNT);

			projectiles.remove(event.getEntity().getUniqueId());

			if (proj.PlaceBlocks) {
				TNTPrimed tntEnt = (TNTPrimed) tnt;
				tntEnt.setYield(0);
				tntEnt.setFuseTicks(0);
				if (CrunchSiegeCore.towny != null) {
					TownBlock block = TownyAPI.getInstance().getTownBlock(loc);
					if (block != null) {
						if (block.hasTown()) {
							if (!block.getTownBlockOwner().getPermissions().explosion) {
								return;
							}
						}
					}
				}
				if (event.getHitBlock() != null) {
					List<Block> Blocks = sphere(event.getHitBlock().getLocation(), (int) proj.ExplodePower);
					for (int i = 0; i < proj.BlocksToPlaceAmount; i++) {
						Block replace = getRandomElement(Blocks);
						replace.setType(Material.COBWEB);
					}
				}
			}
			else {
				TNTPrimed tntEnt = (TNTPrimed) tnt;
				tntEnt.setYield(proj.ExplodePower);
				tntEnt.setFuseTicks(0);
			}

		}
	}

	public ArrayList<Block> sphere(final Location center, final int radius) {
		ArrayList<Block> sphere = new ArrayList<Block>();
		for (int Y = -radius; Y < radius; Y++)
			for (int X = -radius; X < radius; X++)
				for (int Z = -radius; Z < radius; Z++)
					if (Math.sqrt((X * X) + (Y * Y) + (Z * Z)) <= radius) {
						final Block block = center.getWorld().getBlockAt(X + center.getBlockX(), Y + center.getBlockY(), Z + center.getBlockZ());
						if (block.getType() == Material.AIR) {
							sphere.add(block);
						}
					}
		return sphere;
	}

	public Block getRandomElement(List<Block> list)
	{
		Random rand = new Random();
		return list.get(rand.nextInt(list.size()));
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void BlockPlaceEvent(org.bukkit.event.block.BlockPlaceEvent event) {
		Player thePlayer = event.getPlayer();
		if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.CARVED_PUMPKIN)
		{
			ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
			if (item.getItemMeta() != null && item.getItemMeta().hasCustomModelData()) {
				int customModel = item.getItemMeta().getCustomModelData();
				Boolean created = CrunchSiegeCore.CreateTrebuchet(thePlayer, customModel, event.getBlockAgainst().getLocation());
				if (created) {
					item.setAmount(item.getAmount() - 1);
					thePlayer.getInventory().setItemInMainHand(item);
					thePlayer.sendMessage("Equipment spawned!");
				}
				else {
					thePlayer.sendMessage("Equipment could not be spawned, is it enabled?");
				}
				event.setCancelled(true);
			}
		}
	}


	public void Shoot(Player player, long delay) {
		float actualDelay = delay;
		Boolean FirstShot = true;
		for (Entity ent : CrunchSiegeCore.TrackedStands.get(player.getUniqueId())){{

			if (ent == null || ent.isDead()) {
				continue;
			}
			double distance = player.getLocation().distance(ent.getLocation());
			if (distance >= 250) {
				
				continue;
			}
			SiegeEquipment siege = CrunchSiegeCore.equipment.get(ent.getUniqueId());

			if (siege.isLoaded()) {
				siege.Fire(player, actualDelay);
				actualDelay += delay;
			}
			else {
				player.sendMessage("Equipment is not loaded");
			}
			//	player.sendMessage(String.format("�e" +actualDelay));

		}
		}
	}


	@EventHandler
	public void rightClick(PlayerInteractEvent event) {
		Player player = event.getPlayer();

		ItemStack ItemInHand = event.getPlayer().getInventory().getItemInMainHand();
		if (ItemInHand == null) {
			return;
		}

		//		if (ItemInHand.getType() == Material.PAPER) {
		//			ItemMeta meta = ItemInHand.getItemMeta();
		//			if (meta.hasCustomModelData() && meta.getCustomModelData() == 505050505) {
		//				CrunchSiegeCore.CreateTrebuchet(player);
		//				ItemInHand.setAmount(ItemInHand.getAmount() - 1);
		//				return;
		//			}
		//		}


		if (event.getAction() == Action.LEFT_CLICK_AIR) {
			if (ItemInHand.getType() != Material.CLOCK) {
				return;
			}

			Shoot(player, 6);

		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onSignChange(SignChangeEvent event){
		String topline = event.getLine(0);
		if (topline == null) topline = "";
		Player player = event.getPlayer();
		String toplinetrimmed = topline.trim();
		if (toplinetrimmed.equals("[Cannon]")) {
			SaveCannons(player, event.getBlock());
		}

	}

	@EventHandler
	public void damage(EntityDamageByEntityEvent event){
		if (event.getEntity() instanceof ArmorStand) {
			if (event.getDamager() instanceof Projectile) {
				event.setDamage(0);
				event.setCancelled(true);
			}
		}
	}



	public static void TakeControl(Player player, Entity entity) {
		LivingEntity living = (LivingEntity) entity;
		if (CrunchSiegeCore.TrackedStands.containsKey(player.getUniqueId())) {
			List<Entity> entities = CrunchSiegeCore.TrackedStands.get(player.getUniqueId());
			if (entities.contains(entity)) {
				return;
			}
		}

		if (living.getEquipment().getHelmet() != null && living.getEquipment().getHelmet().getType() == Material.CARVED_PUMPKIN) {
			if (living.getEquipment() == null || living.getEquipment().getHelmet() == null || living.getEquipment().getHelmet().getItemMeta() == null) {
				return;
			}

			ArmorStand stand = (ArmorStand) entity;
			SiegeEquipment equip;

			if (CrunchSiegeCore.equipment.containsKey(entity.getUniqueId())) {
				equip = CrunchSiegeCore.equipment.get(entity.getUniqueId());
				if (equip == null || !equip.Enabled) {
					return;
				}
			}
			else {
				equip = CrunchSiegeCore.CreateClone(living.getEquipment().getHelmet().getItemMeta().getCustomModelData());
				if (equip == null || !equip.Enabled) {
					return;
				}
				equip.AmmoHolder = new EquipmentMagazine();
				equip.Entity = entity;
				equip.EntityId = entity.getUniqueId();
			}
			stand.addEquipmentLock(EquipmentSlot.HEAD, LockType.REMOVING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.LEGS, LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.CHEST, LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.FEET, LockType.ADDING_OR_CHANGING);
			stand.setBasePlate(false);
			if (CrunchSiegeCore.TrackedStands.containsKey(player.getUniqueId())) {
				List<Entity> entities = CrunchSiegeCore.TrackedStands.get(player.getUniqueId());
				entities.add(entity);
				CrunchSiegeCore.TrackedStands.put(player.getUniqueId(), entities);
			}
			else {
				List<Entity> newList = new ArrayList<Entity>();
				newList.add(entity);
				CrunchSiegeCore.TrackedStands.put(player.getUniqueId(), newList);
			}
			CrunchSiegeCore.equipment.put(entity.getUniqueId(), equip);
			player.sendMessage("�eNow controlling the equipment.");
		}
	}

	public void SaveCannons(Player player, Block block) {
		List<String> Ids = new ArrayList<String>();
		if (!CrunchSiegeCore.TrackedStands.containsKey(player.getUniqueId())) {
			return;
		}
		for (Entity ent : CrunchSiegeCore.TrackedStands.get(player.getUniqueId())) {

			Ids.add(ent.getUniqueId().toString());
		}
		TileState state = (TileState) block.getState();
		NamespacedKey key = new NamespacedKey(CrunchSiegeCore.plugin, "cannons");		
		state.getPersistentDataContainer().set(key, PersistentDataType.STRING, String.join(",", Ids));
		state.update();
		player.sendMessage("Saving cannons!");
	}

	public void AimUp(Player player, float amount) {

		for (Entity ent : CrunchSiegeCore.TrackedStands.get(player.getUniqueId())) {
			if (ent.isDead()) {
				continue;
			}

			DoAimUp(ent, amount, player);

		}
	}

	public void DoAimUp(Entity ent, float amount, Player player) {
		Location loc = ent.getLocation();
		ArmorStand stand = (ArmorStand) ent;
		//	player.sendMessage(String.format("" + loc.getPitch()));
		if (loc.getPitch() == -85 || loc.getPitch() - amount < -85) {
			return;
		}
		loc.setPitch((float) (loc.getPitch() - amount));
		SiegeEquipment equipment = CrunchSiegeCore.equipment.get(ent.getUniqueId());
		if (equipment != null) {

			equipment.ShowFireLocation(player);   
			if (equipment.RotateStandHead) {
				stand.setHeadPose(new EulerAngle(loc.getDirection().getY()*(-1),0,0));
			}

		}



		ent.teleport(loc);
	}

	public void DoAimDown(Entity ent, float amount, Player player) {
		Location loc = ent.getLocation();
		ArmorStand stand = (ArmorStand) ent;
		//	player.sendMessage(String.format("" + loc.getPitch()));
		if (loc.getPitch() == 85 || loc.getPitch() + amount > 85) {
			return;
		}
		SiegeEquipment equipment = CrunchSiegeCore.equipment.get(ent.getUniqueId());
		if (equipment != null) {

			equipment.ShowFireLocation(player);   
			if (equipment.RotateStandHead) {
				stand.setHeadPose(new EulerAngle(loc.getDirection().getY()*(-1),0,0));

			}
		}
		loc.setPitch((float) (loc.getPitch() + amount));

		ent.teleport(loc);
	}
	NamespacedKey key = new NamespacedKey(CrunchSiegeCore.plugin, "cannons");	
	@EventHandler
	public void DeathEvent(EntityDeathEvent event) {
		Boolean removeStands = false;
		List<ItemStack> items = event.getDrops();
		if (event.getEntity() instanceof ArmorStand) {
			if (event.getEntity().getPersistentDataContainer().has(key,  PersistentDataType.STRING)) {
				Entity base = Bukkit.getEntity(UUID.fromString(event.getEntity().getPersistentDataContainer().get(key, PersistentDataType.STRING)));
				base.remove();
			}
			if (CrunchSiegeCore.equipment.containsKey(event.getEntity().getUniqueId())) {
				removeStands = true;
			}
			else {
				for (ItemStack i : items) {
					if (i.getType() == Material.CARVED_PUMPKIN && i.hasItemMeta() && i.getItemMeta().hasCustomModelData()) {
						if (CrunchSiegeCore.DefinedEquipment.containsKey(i.getItemMeta().getCustomModelData())) {
							removeStands = true;
							break;
						}

					}
				}
			}
			if (removeStands) {
				for (ItemStack i : items) {
					if (i.getType() == Material.ARMOR_STAND) {
						i.setAmount(0);
						return;
					}
				}	
			}
		}
	}

	public void AimDown(Player player, float amount) {

		for (Entity ent : CrunchSiegeCore.TrackedStands.get(player.getUniqueId())) {
			if (ent.isDead()) {
				continue;
			}

			DoAimDown(ent, amount, player);
		}
	}

	public void LoadCannonsWithPowder(Player player) {
		for (Entity ent : CrunchSiegeCore.TrackedStands.get(player.getUniqueId())) {
			if (ent.isDead()) {
				continue;
			}

			SiegeEquipment equipment = CrunchSiegeCore.equipment.get(ent.getUniqueId());
			if (equipment != null) {
				equipment.LoadFuel(player);
			}
		}
	}

	public void LoadCannonsWithProjectile(Player player, ItemStack projectile) {
		for (Entity ent : CrunchSiegeCore.TrackedStands.get(player.getUniqueId())) {
			if (ent.isDead()) {
				continue;
			}

			SiegeEquipment equipment = CrunchSiegeCore.equipment.get(ent.getUniqueId());
			if (equipment != null) {
				equipment.LoadProjectile(player, projectile);
			}
		}
	}

	@EventHandler
	public void onPlayerClickSign(PlayerInteractEvent event){
		Player player = event.getPlayer();
		if(event.getClickedBlock() != null && event.getClickedBlock().getType().toString().contains("SIGN")){
			Sign sign = (Sign) event.getClickedBlock().getState();
			if(sign.getLine(0).equalsIgnoreCase( "[Fire]") && event.getAction() == Action.RIGHT_CLICK_BLOCK){
				if (!CrunchSiegeCore.TrackedStands.containsKey(player.getUniqueId())) {
					return;
				}
				try {
					Long delay = Long.parseLong(sign.getLine(1));
					if (delay < 6){
						delay = 6l;
					}
					Shoot(player, delay);
				} catch (Exception e) {
					Shoot(player, 6);
				}
				return;
			}
			if(sign.getLine(0).equalsIgnoreCase( "[Load]")){
				if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
					LoadCannonsWithPowder(player);
					return;
				}
				if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
					//load proj
					ItemStack itemInHand = player.getInventory().getItemInMainHand();
					if (itemInHand == null) {
						return;
					}


					LoadCannonsWithProjectile(player, player.getInventory().getItemInMainHand());
					return;
				}
			}

			if(sign.getLine(0).equalsIgnoreCase( "[Aim]")){
				if (!CrunchSiegeCore.TrackedStands.containsKey(player.getUniqueId())) {
					return;
				}
				float amount;
				try {
					amount = Float.parseFloat(sign.getLine(1));
					if (player.isSneaking()) {
						AimDown(player, amount);
					}
					else {
						AimUp(player, amount);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					player.sendMessage("Could not parse number on second line.");
				} 

				return;
			}

			if(sign.getLine(0).equalsIgnoreCase( "[Cannon]")){
				if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
					CrunchSiegeCore.TrackedStands.remove(player.getUniqueId());
					player.sendMessage("Releasing the equipment!");
					return;
				}

				if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
					if (player.isSneaking()) {
						SaveCannons(player, event.getClickedBlock());
						return;
					}

					NamespacedKey key = new NamespacedKey(CrunchSiegeCore.plugin, "cannons");		
					TileState state = (TileState)  sign.getBlock().getState();
					CrunchSiegeCore.TrackedStands.remove(player.getUniqueId());
					List<UUID> temp = new ArrayList<UUID>();
					if (!state.getPersistentDataContainer().has(key,  PersistentDataType.STRING)) {
						return;
					}
					String[] split = state.getPersistentDataContainer().get(key, PersistentDataType.STRING).replace("[", "").replace("]", "").split(",");
					for (String s : split) {
						temp.add(UUID.fromString(s.trim()));
					}
					for (UUID Id : temp) {
						List<Entity> entities = new ArrayList<Entity>();
						Entity ent = Bukkit.getEntity(Id);
						if (ent != null) {
							TakeControl(player, ent);
						}
					}
				}
			}

		}
	}

	@EventHandler
	public void onEntityClick(PlayerInteractAtEntityEvent event) {

		Player player = event.getPlayer();
		ItemStack itemInHand = player.getInventory().getItemInMainHand();
		Entity entity = event.getRightClicked();
		if (entity == null) {
			return;
		}
		if (entity.getType() == EntityType.ARMOR_STAND){
			TakeControl(player, entity);
			if (CrunchSiegeCore.equipment.containsKey(entity.getUniqueId())) {
				if (itemInHand.getType() == Material.RECOVERY_COMPASS) {
					if (player.isSneaking()) {
						DoAimDown(entity, 1, player);
					}
					else {
						DoAimUp(entity, 1, player);
					}
					return;
				}
			
				SiegeEquipment equipment = CrunchSiegeCore.equipment.get(entity.getUniqueId());
		
				if (itemInHand == null || itemInHand.getType() == Material.AIR) {
					ArmorStand stand = (ArmorStand) entity;
					if (stand.isInvisible()) {
						stand.setInvisible(false);
						player.sendMessage("Equipment is now breakable");
					}
					else{
						if (equipment.AllowInvisibleStand) {
							stand.setInvisible(true);
							player.sendMessage("Equipment is no longer breakable");
						}
					}
				}
				if (itemInHand.getType().equals(equipment.FuelMaterial)) {
					if (!equipment.LoadFuel(player)) {
						player.sendMessage("Could not load Fuel.");
					}
				}
				if (itemInHand.getType().equals(Material.FLINT)) {
					if (equipment.isLoaded()) {
						equipment.Fire(player, 6);
					}
					else {
						player.sendMessage("Equipment is not loaded");
					}
					return;
				}

				if (equipment.Projectiles.containsKey(itemInHand.getType()) && equipment.AmmoHolder.LoadedProjectile == 0){
					equipment.AmmoHolder.LoadedProjectile = 1;
					equipment.AmmoHolder.MaterialName = (Material) itemInHand.getType();
					player.sendMessage("Adding projectile to equipment");
					itemInHand.setAmount(itemInHand.getAmount() - 1);
				}
				return;
			}
		}
	}
}
