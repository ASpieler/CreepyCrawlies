/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2015
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.CritterPet.Entities;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import Reika.CritterPet.Interfaces.TamedMob;
import Reika.CritterPet.Registry.CritterType;
import Reika.DragonAPI.Interfaces.Entity.TameHostile;
import Reika.DragonAPI.Libraries.ReikaEntityHelper;
import Reika.DragonAPI.Libraries.IO.ReikaChatHelper;
import Reika.DragonAPI.Libraries.Java.ReikaRandomHelper;
import Reika.DragonAPI.Libraries.Registry.ReikaParticleHelper;
import Reika.DragonAPI.Libraries.World.ReikaWorldHelper;

public abstract class EntitySlimeBase extends EntitySlime implements TamedMob, TameHostile {

	private CritterType base;
	private Entity entityToAttack;

	public EntitySlimeBase(World world, CritterType sp) {
		super(world);
		base = sp;
		this.setSize(1.8F*sp.size, 1.1F*sp.size/2);
		this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(this.getCritterMaxHealth());
		this.setHealth(this.getCritterMaxHealth());
		experienceValue = 0;
		height = 1.25F*sp.size;
		this.func_110163_bv();
		this.setSlimeSize(4);
	}

	@Override
	protected final void entityInit() {
		super.entityInit();
		dataWatcher.addObject(30, ""); //Set empty owner
	}

	private void setOwner(String owner) {
		dataWatcher.updateObject(30, owner);
	}

	public final void setOwner(EntityPlayer ep) {
		String owner = ep.getCommandSenderName();
		this.setOwner(owner);
	}

	public final String getMobOwner() {
		return dataWatcher.getWatchableObjectString(30);
	}

	public final boolean hasOwner() {
		String s = this.getMobOwner();
		return s != null && !s.isEmpty();
	}

	@Override
	public void spawnEffects() {
		World world = worldObj;
		double x = posX;
		double y = posY;
		double z = posZ;
		for (int i = 0; i < 12; i++) {
			double rx = ReikaRandomHelper.getRandomPlusMinus(x, 1);
			double rz = ReikaRandomHelper.getRandomPlusMinus(z, 1);
			ReikaParticleHelper.HEART.spawnAt(world, rx, y+this.getScaleFactor()*0.8, rz, 0, 0, 0);
			ReikaWorldHelper.splitAndSpawnXP(world, rx, y+0.5, rz, 1+rand.nextInt(5));
		}
		this.playSound("random.levelup", 1, 1);
	}

	public final float getScaleFactor() {
		return this.getSlimeSize()/2F;
	}

	@Override
	public final CritterType getBaseCritter() {
		return base;
	}

	@Override
	public final boolean isVanillaCritter() {
		return !this.isModCritter();
	}

	@Override
	public final boolean isModCritter() {
		return this.getBaseCritter().sourceMod != null;
	}

	@Override
	public final int getCritterMaxHealth() {
		return this.getBaseCritter().maxHealth*4;
	}

	@Override
	public final void faceEntity(Entity e, float a, float b) {
		if (!e.getCommandSenderName().equals(this.getMobOwner()))
			super.faceEntity(e, a, b);
	}

	@Override
	protected final int getJumpDelay()
	{
		return riddenByEntity != null ? 5 : super.getJumpDelay();
	}

	@Override
	protected final EntitySlime createInstance()
	{
		return super.createInstance();
	}

	@Override
	public final void setDead()
	{
		isDead = true;
	}

	@Override
	public final void onUpdate() {
		boolean preventDespawn = false;
		if (!worldObj.isRemote && worldObj.difficultySetting == EnumDifficulty.PEACEFUL) { //the criteria for mob despawn in peaceful
			preventDespawn = true;
			worldObj.difficultySetting = EnumDifficulty.EASY;
		}
		super.onUpdate();
		if (preventDespawn)
			worldObj.difficultySetting = EnumDifficulty.PEACEFUL;
		if (riddenByEntity != null)
			this.updateRider();
		if (riddenByEntity != null)
			this.followOwner();
	}

	@Override
	protected void updateEntityActionState()
	{
		if (entityToAttack != null)
			this.faceEntity(entityToAttack, 10.0F, 30.0F);
		super.updateEntityActionState();
	}

	protected abstract void updateRider();

	private void followOwner() {
		World world = worldObj;
		double x = posX;
		double y = posY;
		double z = posZ;
		Vec3 v = riddenByEntity.getLookVec();
		rotationPitch = 0;
	}

	protected abstract void applyAttackEffects(EntityLivingBase e);

	@Override
	public final void onCollideWithPlayer(EntityPlayer ep) {
		if (!ep.getCommandSenderName().equals(this.getMobOwner()))
			super.onCollideWithPlayer(ep);
	}

	@Override
	protected final boolean interact(EntityPlayer ep)
	{
		ItemStack is = ep.getCurrentEquippedItem();
		String owner = this.getMobOwner();
		if (owner == null || owner.isEmpty()) {
			if (is != null && is.getItem() == base.tamingItem) {
				owner = ep.getCommandSenderName();
				//ReikaChatHelper.writeString("This critter had no owner! You are now the owner.");
				return true;
			}
			return false;
		}
		if (ep.getCommandSenderName().equals(owner)) {
			if (is != null) {
				if (is.getItem() == Items.slime_ball && this.getHealth() < this.getMaxHealth()) {
					this.heal(8);
					if (!ep.capabilities.isCreativeMode)
						is.stackSize--;
					return true;
				}
				if (is.getItem() == Items.name_tag) {
					return false;
				}
				boolean flag = super.interact(ep);
				if (flag)
					return true;
			}
			if (!worldObj.isRemote) {
				if (riddenByEntity != null && riddenByEntity.equals(ep)) {
					ep.dismountEntity(this);
				}
				else {
					ep.mountEntity(this);
				}
			}
			return true;
		}
		else {
			ReikaChatHelper.writeString("You do not own this critter.");
			return false;
		}
	}

	@Override
	public void heal(float par1)
	{
		super.heal(par1);

		this.playLivingSound();

		double x = posX;
		double y = posY;
		double z = posZ;
		for (int i = 0; i < 6; i++) {
			double rx = ReikaRandomHelper.getRandomPlusMinus(x, 1);
			double rz = ReikaRandomHelper.getRandomPlusMinus(z, 1);
			ReikaParticleHelper.HEART.spawnAt(worldObj, rx, y+this.getScaleFactor()*0.8, rz, 0, 0, 0);
			//ReikaWorldHelper.splitAndSpawnXP(worldObj, rx, y+0.5, rz, 1+rand.nextInt(5));
		}
	}

	@Override
	public final boolean attackEntityFrom(DamageSource dsc, float par2)
	{
		if (this.isEntityInvulnerable()) {
			return false;
		}
		else if (!this.canBeHurtBy(dsc)) {
			return false;
		}
		else if (super.attackEntityFrom(dsc, par2)) {
			Entity entity = dsc.getEntity();
			if (riddenByEntity != entity && ridingEntity != entity) {
				if (entity != this && !entity.getCommandSenderName().equals(this.getMobOwner()))
					entityToAttack = entity;
				return true;
			}
			else
				return true;
		}
		else
			return false;
	}

	public abstract boolean canBeHurtBy(DamageSource dsc);

	@Override
	public final String getCommandSenderName() {
		return this.hasCustomNameTag() ? this.getCustomNameTag() : this.getDefaultName();
	}

	private final String getDefaultName() {
		String owner = this.getMobOwner();
		String sg = base.name;
		if (owner == null || owner.isEmpty())
			return sg;
		else
			return owner+"'s "+sg;
	}

	@Override
	protected final boolean isMovementBlocked()
	{
		return false;
	}

	@Override
	public final boolean getAlwaysRenderNameTagForRender()
	{
		return riddenByEntity == null;
	}

	@Override
	public final int getTalkInterval()
	{
		return riddenByEntity != null ? 960 : 320;
	}

	public final float getStepSoundVolume() {
		return this.getSoundVolume();
	}

	@Override
	protected final float getSoundVolume()
	{
		return riddenByEntity != null ? 0.05F : 0.15F;
	}

	@Override
	public final double getMountedYOffset()
	{
		return this.getSlimeSize()/2F+0.0625F;
	}

	@Override
	public final boolean shouldRenderInPass(int pass) {
		return pass <= 1;
	}

	@Override
	public final float getShadowSize()
	{
		return width*4;
	}

	@Override
	public final boolean canBePushed() {
		return false;
	}

	@Override
	public final String toString() {
		return this.getCommandSenderName()+" @ "+String.format("%.1f, %.1f, %.1f", posX, posY, posZ);
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound NBT)
	{
		super.writeEntityToNBT(NBT);

		String s = this.getMobOwner();
		if (s == null)
			NBT.setString("Owner", "");
		else
			NBT.setString("Owner", s);
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound NBT)
	{
		super.readEntityFromNBT(NBT);
		String s = NBT.getString("Owner");

		if (s.length() > 0) {
			this.setOwner(s);
		}
	}

	@Override
	protected final Item getDropItem()
	{
		return null;
	}

	@Override
	protected final void dropFewItems(boolean par1, int par2)
	{

	}

	@Override
	protected void collideWithNearbyEntities()
	{
		List list = worldObj.getEntitiesWithinAABBExcludingEntity(this, boundingBox.expand(0.2, 0.0D, 0.2));

		if (list != null && !list.isEmpty())
		{
			for (int i = 0; i < list.size(); ++i)
			{
				Entity entity = (Entity)list.get(i);

				if (entity.canBePushed())
				{
					this.collideWithEntity(entity);
				}
			}
		}
	}

	@Override
	protected void collideWithEntity(Entity e)
	{
		e.applyEntityCollision(this);
		if (e instanceof EntityLivingBase && !(e instanceof TamedMob)) {
			if (ReikaEntityHelper.isHostile((EntityLivingBase)e) || this.isNonOwnerPlayer(e)) {
				//this.attackEntity(e, this.getAttackDamage());
				e.attackEntityFrom(DamageSource.causeMobDamage(this), this.getAttackStrength());
				this.applyAttackEffects((EntityLivingBase)e);
			}
		}
	}

	@Override
	public boolean shouldDismountInWater(Entity rider) {
		return false;
	}

	protected final boolean isNonOwnerPlayer(Entity e) {
		return e instanceof EntityPlayer && !e.getCommandSenderName().equals(this.getMobOwner());
	}

}
