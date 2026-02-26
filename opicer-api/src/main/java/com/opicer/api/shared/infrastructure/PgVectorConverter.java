package com.opicer.api.shared.infrastructure;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import org.postgresql.util.PGobject;

@Converter
public class PgVectorConverter implements AttributeConverter<float[], PGobject> {

	@Override
	public PGobject convertToDatabaseColumn(float[] attribute) {
		if (attribute == null) return null;
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (int i = 0; i < attribute.length; i++) {
			if (i > 0) sb.append(',');
			sb.append(attribute[i]);
		}
		sb.append(']');
		try {
			PGobject obj = new PGobject();
			obj.setType("vector");
			obj.setValue(sb.toString());
			return obj;
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to convert vector", e);
		}
	}

	@Override
	public float[] convertToEntityAttribute(PGobject dbData) {
		if (dbData == null || dbData.getValue() == null) return null;
		String value = dbData.getValue().trim();
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
}
