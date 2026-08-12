package com.dragonminez.mixin.sable;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

/**
 * Mixin: do not escalate foreign UDP noise on Sable's server UDP channel.
 */
@Mixin(targets = "dev.ryanhcode.sable.network.udp.handler.SableUDPChannelHandlerServer", remap = false)
public abstract class SableUDPChannelHandlerServerMixin {

	@Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
	private void dragonminez$ignoreForeignUdpNoise(ChannelHandlerContext ctx, Throwable cause, CallbackInfo ci) {
		if (isInvalidPacketIdNoise(cause)) {
			ci.cancel();
		}
	}

	static boolean isInvalidPacketIdNoise(Throwable cause) {
		Throwable t = cause;
		while (t != null) {
			if (t instanceof DecoderException || t instanceof IOException) {
				String msg = t.getMessage();
				if (msg != null && msg.contains("invalid packet ID")) {
					return true;
				}
			}
			t = t.getCause();
		}
		return false;
	}
}
