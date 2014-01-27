package au.com.addstar.pandora.modules;

import java.util.HashSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class PVPHandler implements Module, Listener
{
	private HashSet<PotionEffectType> mBadEffects = new HashSet<PotionEffectType>();
	private WorldGuardPlugin mWorldGuard;
	
	public PVPHandler()
	{
		mBadEffects.add(PotionEffectType.BLINDNESS);
		mBadEffects.add(PotionEffectType.CONFUSION);
		mBadEffects.add(PotionEffectType.HARM);
		mBadEffects.add(PotionEffectType.HUNGER);
		mBadEffects.add(PotionEffectType.POISON);
		mBadEffects.add(PotionEffectType.SLOW);
		mBadEffects.add(PotionEffectType.SLOW_DIGGING);
		mBadEffects.add(PotionEffectType.WEAKNESS);
		mBadEffects.add(PotionEffectType.WITHER);
	}
	
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onSplashPotion(PotionSplashEvent event)
	{
		if(!(event.getPotion().getShooter() instanceof Player) || event.getPotion().getShooter().hasMetadata("NPC"))
			return;
		
		boolean bad = false;
		for(PotionEffect effect : event.getPotion().getEffects())
		{
			if(mBadEffects.contains(effect.getType()))
				bad = true;
		}
		
		if(!bad)
			return;
		
		Player thrower = (Player)event.getPotion().getShooter();
		boolean warned = false;
		
		RegionManager manager = mWorldGuard.getRegionManager(event.getPotion().getWorld());
		if(manager == null)
			return;
		
		for(LivingEntity ent : event.getAffectedEntities())
		{
			if(!(ent instanceof Player) || ent.hasMetadata("NPC"))
				continue;
			
			if(ent.equals(thrower))
				continue;
			
			ApplicableRegionSet regions = manager.getApplicableRegions(ent.getLocation());
			if(!regions.allows(DefaultFlag.PVP))
			{
				if(!warned)
				{
					thrower.sendMessage(ChatColor.RED + "PVP is not allowed in this area");
					warned = true;
				}
				event.setIntensity(ent, 0D);
			}
		}
	}
	
	private boolean handleBlockPlace(Block block, Player player, Material placeMaterial)
	{
		RegionManager manager = mWorldGuard.getRegionManager(block.getWorld());
		if(manager == null)
			return false;
		
		Location playerLoc = new Location(null, 0, 0, 0);
		Location blockLoc = block.getLocation();
		
		ApplicableRegionSet regions = manager.getApplicableRegions(blockLoc);
		if(regions.allows(DefaultFlag.PVP))
			return false;
		
		List<Entity> entities = player.getNearbyEntities(10, 10, 10);
		
		for(Entity entity : entities)
		{
			if(!(entity instanceof Player) || entity.hasMetadata("NPC"))
				continue;
			
			entity.getLocation(playerLoc);
			if(playerLoc.distanceSquared(blockLoc) < 9)
			{
				player.sendMessage(ChatColor.RED + "PVP is not allowed in this area!");
				player.sendMessage(ChatColor.GRAY + "You placed " + placeMaterial.toString() + " too close to another player.");
				return true;
			}
		}
		
		return false;
	}
	
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onPlaceFire(BlockPlaceEvent event)
	{
		if(event.getBlock().getType() != Material.FIRE && event.getBlock().getType() != Material.LAVA && event.getBlock().getType() != Material.STATIONARY_LAVA)
			return;
		
		event.setCancelled(handleBlockPlace(event.getBlock(), event.getPlayer(), event.getBlock().getType()));
	}
	
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onPlaceLava(PlayerBucketEmptyEvent event)
	{
		if(event.getBucket() != Material.LAVA_BUCKET)
			return;
		
		event.setCancelled(handleBlockPlace(event.getBlockClicked(), event.getPlayer(), Material.LAVA));
	}
	
	@Override
	public void onEnable()
	{
		mWorldGuard = (WorldGuardPlugin)Bukkit.getPluginManager().getPlugin("WorldGuard");
	}

	@Override
	public void onDisable()
	{
	}

	@Override
	public void setPandoraInstance( MasterPlugin plugin ) {}

}