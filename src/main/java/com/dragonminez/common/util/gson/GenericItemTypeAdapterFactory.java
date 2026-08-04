package com.dragonminez.common.util.gson;

import com.dragonminez.common.util.types.items.EnchantedBookDTO;
import com.dragonminez.common.util.types.items.EnchantedItemDTO;
import com.dragonminez.common.util.types.items.GenericItemDTO;
import com.dragonminez.common.util.types.items.LingeringPotionDTO;
import com.dragonminez.common.util.types.items.PotionDTO;
import com.dragonminez.common.util.types.items.SplashPotionDTO;
import com.dragonminez.common.util.types.items.TippedArrowDTO;
import com.dragonminez.common.util.types.items.TrimmedArmorDTO;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Polymorphic (de)serialisation for the {@link GenericItemDTO} hierarchy, keyed on the
 * {@code "itemType"} discriminator.
 *
 * <p>This is a {@link TypeAdapterFactory} rather than a {@code JsonSerializer}/
 * {@code JsonDeserializer} pair on purpose. The delegate adapter is obtained through
 * {@link Gson#getDelegateAdapter}, so the concrete subtype is (de)serialised by the
 * <em>same</em> Gson instance this factory is registered on and therefore inherits every
 * other adapter on it — most importantly {@link ResourceLocationTypeAdapter}. Building a
 * throwaway {@code new GsonBuilder().create()} inside the adapter, as the previous
 * implementation did, silently dropped those adapters and wrote resource locations as
 * {@code {"namespace": ..., "path": ...}} objects. It also allocated a whole Gson per
 * element parsed.
 *
 * <p>Only the base type is matched, so delegating to a concrete subtype cannot recurse.
 */
public final class GenericItemTypeAdapterFactory implements TypeAdapterFactory {

	private static final String TYPE_FIELD = "itemType";

	@Override
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
		if (type.getRawType() != GenericItemDTO.class) {
			return null;
		}

		final TypeAdapterFactory self = this;
		TypeAdapter<T> adapter = new TypeAdapter<>() {

			@Override
			public void write(JsonWriter out, T value) throws IOException {
				@SuppressWarnings("unchecked")
				TypeAdapter<Object> delegate =
						(TypeAdapter<Object>) gson.getDelegateAdapter(self, TypeToken.get(value.getClass()));
				gson.toJson(delegate.toJsonTree(value), out);
			}

			@Override
			public T read(JsonReader in) throws IOException {
				JsonElement element = JsonParser.parseReader(in);
				if (!element.isJsonObject()) {
					throw new JsonSyntaxException("Expected an item object but found: " + element);
				}

				JsonObject object = element.getAsJsonObject();
				String itemType = GenericItemDTO.ITEM_TYPE;
				if (object.has(TYPE_FIELD) && object.get(TYPE_FIELD).isJsonPrimitive()) {
					itemType = object.get(TYPE_FIELD).getAsString();
				}

				Class<? extends GenericItemDTO> target = classForType(itemType);
				if (target == null) {
					throw new JsonSyntaxException("Unknown item type: '" + itemType + "'");
				}

				@SuppressWarnings("unchecked")
				TypeAdapter<GenericItemDTO> delegate =
						(TypeAdapter<GenericItemDTO>) gson.getDelegateAdapter(self, TypeToken.get(target));

				GenericItemDTO parsed = delegate.fromJsonTree(object);
				if (parsed != null) {
					// Fail loudly here, while we still know which file we are reading,
					// rather than mid-tick when the item is handed to a player.
					parsed.validate();
				}

				@SuppressWarnings("unchecked")
				T result = (T) parsed;
				return result;
			}
		};

		return adapter.nullSafe();
	}

	public static Class<? extends GenericItemDTO> classForType(String type) {
		if (type == null) {
			return null;
		}
		return switch (type) {
			case "enchanted_book" -> EnchantedBookDTO.class;
			case "enchanted_item" -> EnchantedItemDTO.class;
			case "generic_item" -> GenericItemDTO.class;
			case "lingering_potion" -> LingeringPotionDTO.class;
			case "potion" -> PotionDTO.class;
			case "splash_potion" -> SplashPotionDTO.class;
			case "tipped_arrow" -> TippedArrowDTO.class;
			case "trimmed_armor" -> TrimmedArmorDTO.class;
			default -> null;
		};
	}
}
