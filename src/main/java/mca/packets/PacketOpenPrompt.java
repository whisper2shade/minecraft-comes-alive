package mca.packets;

import io.netty.buffer.ByteBuf;
import mca.client.gui.GuiPrompt;
import mca.enums.EnumInteraction;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import radixcore.modules.net.AbstractPacket;

public class PacketOpenPrompt extends AbstractPacket<PacketOpenPrompt>
{
	private int senderId;
	private int targetId;
	private int interactionId;
	
	public PacketOpenPrompt()
	{
		//Required
	}

	public PacketOpenPrompt(EntityPlayer sender, EntityPlayer target, EnumInteraction interaction)
	{
		this.senderId = sender.getEntityId();
		this.targetId = target.getEntityId();
		this.interactionId = interaction.getId();
	}

	@Override
	public void fromBytes(ByteBuf byteBuf)
	{
		senderId = byteBuf.readInt();
		targetId = byteBuf.readInt();
		interactionId = byteBuf.readInt();
	}

	@Override
	public void toBytes(ByteBuf byteBuf)
	{
		byteBuf.writeInt(senderId);
		byteBuf.writeInt(targetId);
		byteBuf.writeInt(interactionId);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void processOnGameThread(PacketOpenPrompt packet, MessageContext context) 
	{
		EntityPlayer target = this.getPlayer(context);
		EntityPlayer sender = (EntityPlayer) target.world.getEntityByID(packet.senderId);
		EnumInteraction interaction = EnumInteraction.fromId(packet.interactionId);
		
		Minecraft.getMinecraft().displayGuiScreen(new GuiPrompt(sender, target, interaction));
	}
}
