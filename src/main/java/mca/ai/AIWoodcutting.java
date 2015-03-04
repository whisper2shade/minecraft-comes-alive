package mca.ai;

import mca.api.ChoreRegistry;
import mca.core.Constants;
import mca.data.WatcherIDsHuman;
import mca.entity.EntityHuman;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import radixcore.data.BlockObj;
import radixcore.data.WatchedBoolean;
import radixcore.helpers.LogicHelper;
import radixcore.helpers.MathHelper;
import radixcore.math.Point3D;

public class AIWoodcutting extends AbstractToggleAI
{
	private WatchedBoolean isAIActive;
	private Point3D treeBasePoint;
	private int idOfTarget;
	private int yLevel;
	private int cutInterval;
	private int cutTimeLeft;
	private boolean doReplant;

	public AIWoodcutting(EntityHuman owner) 
	{
		super(owner);
		isAIActive = new WatchedBoolean(false, WatcherIDsHuman.IS_WOODCUTTING_ACTIVE, owner.getDataWatcherEx());
		treeBasePoint = new Point3D(0, 0, 0);
	}

	@Override
	public void setIsActive(boolean value) 
	{
		isAIActive.setValue(value);
	}

	@Override
	public boolean getIsActive() 
	{
		return isAIActive.getBoolean();
	}

	@Override
	public void onUpdateCommon() 
	{
	}

	@Override
	public void onUpdateClient() 
	{	
	}

	@Override
	public void onUpdateServer() 
	{
		if (treeBasePoint.iPosX == 0 && treeBasePoint.iPosY == 0 && treeBasePoint.iPosZ == 0)
		{
			final BlockObj block = ChoreRegistry.getWoodcuttingBlockById(idOfTarget);

			if (block == null) //Protect against NPE if IDs have changed for some reason.
			{
				isAIActive.setValue(false);
				return;
			}

			final Point3D point = LogicHelper.getNearestBlockPosWithMetadata(owner, block.getBlock(), block.getMeta(), 15);

			if (point != null)
			{
				//Follow the point down until logs are NOT found, so we have the base of the tree.
				while (owner.worldObj.getBlock(point.iPosX, point.iPosY, point.iPosZ) == block.getBlock())
				{
					point.iPosY--;

					if (point.iPosY <= 0) //Impose a limit on failure
					{
						break;
					}
				}

				//Follow back up and make sure we have the base. Not sure why, simply adding 1 caused issues every now and then.
				while (owner.worldObj.getBlock(point.iPosX, point.iPosY, point.iPosZ) != block.getBlock())
				{
					point.iPosY++;

					if (point.iPosY >= 255)
					{
						break;
					}
				}

				Point3D modifiedPoint = new Point3D(point.iPosX, point.iPosY, point.iPosZ);
				treeBasePoint = modifiedPoint;
			}

			else
			{
				notifyAssigningPlayer("There are no logs nearby.");
				isAIActive.setValue(false);
				return;
			}
		}

		else if (MathHelper.getDistanceToXYZ(treeBasePoint.dPosX, treeBasePoint.dPosY, treeBasePoint.dPosZ, owner.posX, owner.posY, owner.posZ) <= 2.5D || yLevel > 0)
		{
			cutTimeLeft--;
			owner.swingItem();

			if (cutTimeLeft <= 0)
			{
				cutTimeLeft = cutInterval;

				final BlockObj block = ChoreRegistry.getWoodcuttingBlockById(idOfTarget);
				owner.worldObj.setBlock(treeBasePoint.iPosX, treeBasePoint.iPosY + yLevel, treeBasePoint.iPosZ, Blocks.air);
				boolean addedToInventory = owner.getInventory().addItemStackToInventory(new ItemStack(block.getBlock(), 1, block.getMeta()));
				boolean toolBroken = owner.getInventory().damageItem(owner.getInventory().getBestItemOfTypeSlot(ItemAxe.class), 1);
				
				if (!addedToInventory)
				{
					notifyAssigningPlayer("My inventory is full.");
					isAIActive.setValue(false);
					return;
				}
				
				else if (toolBroken)
				{
					notifyAssigningPlayer("My axe has broken.");
					isAIActive.setValue(false);
					return;					
				}

				yLevel++;

				//Check that the next y level still contains a tree, reset if not.
				final Block nextBlock = owner.worldObj.getBlock(treeBasePoint.iPosX, treeBasePoint.iPosY + yLevel, treeBasePoint.iPosZ);
				final int nextBlockMeta = owner.worldObj.getBlockMetadata(treeBasePoint.iPosX, treeBasePoint.iPosY + yLevel, treeBasePoint.iPosZ); 

				if (ChoreRegistry.getIdOfWoodcuttingBlock(new BlockObj(nextBlock, nextBlockMeta)) == 0)
				{
					if (doReplant)
					{
						//TODO Set the base point to a sapling.
					}

					yLevel = 0;
					treeBasePoint = new Point3D(0, 0, 0);
				}
			}
		}

		else
		{
			for (Point3D point : LogicHelper.getNearbyBlocks(owner, Blocks.leaves, 1))
			{
				owner.worldObj.setBlock(point.iPosX, point.iPosY, point.iPosZ, Blocks.air);
			}

			for (Point3D point : LogicHelper.getNearbyBlocks(owner, Blocks.leaves2, 1))
			{
				owner.worldObj.setBlock(point.iPosX, point.iPosY, point.iPosZ, Blocks.air);				
			}
			
			if (owner.getNavigator().noPath())
			{
				owner.getNavigator().tryMoveToXYZ(treeBasePoint.dPosX, treeBasePoint.dPosY, treeBasePoint.dPosZ, Constants.SPEED_WALK);
			}
		}
	}

	@Override
	public void reset() 
	{

	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) 
	{
		nbt.setBoolean("isAIActive", isAIActive.getBoolean());

		treeBasePoint.writeToNBT("treeBasePoint", nbt);
		nbt.setInteger("idOfTarget", idOfTarget);
		nbt.setInteger("yLevel", yLevel);
		nbt.setInteger("cutInterval", cutInterval);
		nbt.setInteger("cutTimeLeft", cutTimeLeft);
		nbt.setBoolean("doReplant", doReplant);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) 
	{
		isAIActive.setValue(nbt.getBoolean("isAIActive"));

		treeBasePoint = Point3D.readFromNBT("treeBasePoint", nbt);
		idOfTarget = nbt.getInteger("idOfTarget");
		yLevel = nbt.getInteger("yLevel");
		cutInterval = nbt.getInteger("cutInterval");
		cutTimeLeft = nbt.getInteger("cutTimeLeft");
		doReplant = nbt.getBoolean("doReplant");
	}

	public void startWoodcutting(EntityPlayer player, BlockObj block, boolean doReplant)
	{
		Integer id = ChoreRegistry.getIdOfWoodcuttingBlock(block);
		this.idOfTarget = id == null ? 0 : id;

		this.assigningPlayer = player.getUniqueID().toString();
		this.yLevel = 0;
		this.doReplant = doReplant;
		this.cutInterval = calculateCutInterval();		
		this.cutTimeLeft = cutInterval;		
		this.isAIActive.setValue(true);
	}

	private int calculateCutInterval()
	{
		ItemStack bestAxe = owner.getInventory().getBestItemOfType(ItemAxe.class);
		int returnAmount = -1;
		
		if (bestAxe != null)
		{
			Item item = bestAxe.getItem();
			ToolMaterial material = ToolMaterial.valueOf(((ItemAxe) bestAxe.getItem()).getToolMaterialName());	

			switch (material)
			{
			case WOOD:
				returnAmount = 40;
				break;
			case STONE:
				returnAmount = 30;
				break;
			case IRON:
				returnAmount = 25;
				break;
			case EMERALD:
				returnAmount = 10;
				break;
			case GOLD:
				returnAmount = 5;
				break;
			default:
				returnAmount = 25;
				break;
			}
			
			owner.setHeldItem(item);
		}

		else
		{
			returnAmount = 60;
			owner.setHeldItem(null);
		}
		
		return returnAmount;
	}
}
