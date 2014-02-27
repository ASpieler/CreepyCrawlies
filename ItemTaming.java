/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2014
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.SpiderPet;

import java.util.List;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import Reika.DragonAPI.Interfaces.IndexedItemSprites;
import Reika.SpiderPet.Registry.SpiderType;

public class ItemTaming extends Item implements IndexedItemSprites {

	public ItemTaming(int par1) {
		super(par1);
		this.setCreativeTab(CreativeTabs.tabTools);
	}

	@Override
	public boolean onItemUse(ItemStack is, EntityPlayer ep, World world, int x, int y, int z, int par7, float par8, float par9, float par10)
	{
		return false;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack is, World world, EntityPlayer ep)
	{
		return is;
	}

	@Override
	public void addInformation(ItemStack is, EntityPlayer ep, List li, boolean verbose) {
		int i = is.getItemDamage();
		if (i == 0) {
			li.add("Missing a taming item.");
		}
		else {
			SpiderType type = SpiderType.spiderList[i-1];
			String name = type.getName();
			Item item = type.tamingItem;
			li.add("Loaded with "+item.getItemDisplayName(is));
			li.add("Ready to tame a "+name+" Spider.");
		}
	}

	@Override
	public void getSubItems(int id, CreativeTabs tab, List li) {
		int num = 1+SpiderType.spiderList.length;
		for (int i = 0; i < num; i++) {
			li.add(new ItemStack(id, 1, i));
		}
	}

	@Override
	public void registerIcons(IconRegister ico) {

	}

	@Override
	public boolean itemInteractionForEntity(ItemStack is, EntityPlayer ep, EntityLivingBase elb)
	{
		return TamingController.TameSpider(elb, ep);
	}

	@Override
	public int getItemSpriteIndex(ItemStack is) {
		return is.getItemDamage()+1;
	}

	@Override
	public String getTexture(ItemStack is) {
		return "Textures/items.png";
	}

	@Override
	public Class getTextureReferenceClass() {
		return SpiderPet.class;
	}

}
