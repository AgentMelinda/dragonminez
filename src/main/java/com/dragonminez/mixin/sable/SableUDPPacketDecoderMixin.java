package com.dragonminez.mixin.sable;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mixin soft-compat for Sable UDP decoder — <b>no home-grown packet-id table</b>.
 *
 * <p>Upstream already does the correct check:
 * {@code packetID >= SableUDPPacketType.VALUES.length} (enum-driven).
 * That throws {@code IOException: Received an invalid packet ID: …} for foreign UDP
 * (e.g. legacy query {@code 254}).
 *
 * <p>We only turn that throw into a silent drop. When Sable adds enum constants,
 * {@code VALUES.length} grows and valid ids keep working — we never hardcode 6 or 32.
 */
@Mixin(targets = "dev.ryanhcode.sable.network.udp.SableUDPPacketDecoder", remap = false)
public abstract class SableUDPPacketDecoderMixin {

	/**
	 * Sable's decode builds {@code new IOException(...)} only for ids outside
	 * {@link dev.ryanhcode.sable.network.udp.SableUDPPacketType#VALUES}. Cancel that path
	 * so the datagram is discarded without killing the UDP pipeline.
	 */
	@Inject(
			method = "decode(Lio/netty/channel/ChannelHandlerContext;Lio/netty/channel/socket/DatagramPacket;Ljava/util/List;)V",
			at = @At(value = "NEW", target = "java/io/IOException"),
			cancellable = true,
			remap = false,
			require = 0
	)
	private void dragonminez$softDropUnknownEnumId(
			ChannelHandlerContext ctx,
			DatagramPacket msg,
			List<?> out,
			CallbackInfo ci
	) {
		// Enum length check already failed in the original method — trust VALUES, just don't throw.
		ci.cancel();
	}
}
