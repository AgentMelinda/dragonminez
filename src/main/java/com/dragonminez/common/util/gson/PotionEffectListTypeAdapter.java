package com.dragonminez.common.util.gson;

import com.dragonminez.common.util.types.items.PotionEffectDTO;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a potion's {@code mobEffects} in either the current or the pre-2.2 shape, and
 * always writes the current one.
 *
 * <p>Current shape — a list, so duration and amplifier can be stated separately:
 * <pre>"mobEffects": [{"effect": "minecraft:speed", "duration": 600, "amplifier": 1}]</pre>
 *
 * <p>Pre-2.2 shape — a map of effect id to a single number:
 * <pre>"mobEffects": {"minecraft:speed": 600}</pre>
 * That number was handed straight to {@code MobEffectInstance(MobEffect, int)}, whose
 * second parameter is the <em>duration</em>, so it is read back as a duration with a zero
 * amplifier. Files written before this change therefore keep behaving exactly as they did.
 *
 * <p>Applied via {@code @JsonAdapter} on the field rather than registered globally, so it
 * only affects this one field.
 */
public final class PotionEffectListTypeAdapter implements TypeAdapterFactory {

	@Override
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
		TypeAdapter<PotionEffectDTO> elementAdapter = gson.getAdapter(PotionEffectDTO.class);

		TypeAdapter<List<PotionEffectDTO>> adapter = new TypeAdapter<>() {

			@Override
			public void write(JsonWriter out, List<PotionEffectDTO> value) throws IOException {
				out.beginArray();
				for (PotionEffectDTO effect : value) {
					elementAdapter.write(out, effect);
				}
				out.endArray();
			}

			@Override
			public List<PotionEffectDTO> read(JsonReader in) throws IOException {
				List<PotionEffectDTO> effects = new ArrayList<>();

				if (in.peek() == JsonToken.BEGIN_OBJECT) {
					in.beginObject();
					while (in.hasNext()) {
						effects.add(PotionEffectDTO.fromLegacyEntry(in.nextName(), in.nextInt()));
					}
					in.endObject();
					return effects;
				}

				in.beginArray();
				while (in.hasNext()) {
					effects.add(elementAdapter.read(in));
				}
				in.endArray();
				return effects;
			}
		};

		@SuppressWarnings("unchecked")
		TypeAdapter<T> result = (TypeAdapter<T>) adapter.nullSafe();
		return result;
	}
}
