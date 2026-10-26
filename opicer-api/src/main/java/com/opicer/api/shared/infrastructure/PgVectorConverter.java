package com.opicer.api.shared.infrastructure;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@code float[]} to pgvector's text form {@code [a,b,...]}. Writes must cast the bound text to
 * {@code vector} (the entity column uses {@code @ColumnTransformer(write = "cast(? as vector)")}, native
 * queries use {@code cast(:param as vector)}); reads come back as that text form.
 */
@Converter
public class PgVectorConverter implements AttributeConverter<float[], String> {

	@Override
	public String convertToDatabaseColumn(float[] attribute) {
		return toLiteral(attribute);
	}

	@Override
	public float[] convertToEntityAttribute(String dbData) {
		if (dbData == null) return null;
		String value = dbData.trim();
		if (value.startsWith("[") && value.endsWith("]")) {
			value = value.substring(1, value.length() - 1);
		}
		if (value.isBlank()) return new float[0];
		String[] parts = value.split(",");
		float[] out = new float[parts.length];
		for (int i = 0; i < parts.length; i++) {
			out[i] = Float.parseFloat(parts[i].trim());
		}
		return out;
	}

	/** pgvector text literal, or null. */
	public static String toLiteral(float[] vector) {
		if (vector == null) return null;
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (int i = 0; i < vector.length; i++) {
			if (i > 0) sb.append(',');
			sb.append(vector[i]);
		}
		sb.append(']');
		return sb.toString();
	}
}
