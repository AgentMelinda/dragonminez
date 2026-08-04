package com.dragonminez.common.util.types.items;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * A tipped arrow. {@code Items.TIPPED_ARROW} is an {@code ArrowItem}, not a
 * {@code PotionItem}, but it reads the same {@code Potion} and
 * {@code custom_potion_effects} tags — see {@link PotionDTO#applyPotionData}.
 */
@Getter
@Setter
@NoArgsConstructor
public class TippedArrowDTO extends PotionDTO {

	public static final String ITEM_TYPE = "tipped_arrow";
	private static final ResourceLocation TIPPED_ARROW_ID = new ResourceLocation("minecraft", "tipped_arrow");

	public TippedArrowDTO(ResourceLocation potion, int count, List<PotionEffectDTO> mobEffects) {
		super(ITEM_TYPE, TIPPED_ARROW_ID, count, potion, mobEffects);
	}
}
