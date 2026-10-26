package com.opicer.api.opic.application;

/**
 * Turns a street address into map coordinates. Implementations never throw for "no result" or provider trouble;
 * they report it in the {@link Result} so saving a center never depends on the provider being up.
 */
public interface Geocoder {

	Result geocode(String address);

	/** Either both coordinates, or a short Korean reason for the admin. */
	record Result(Double latitude, Double longitude, String failure) {

		public static Result found(double latitude, double longitude) {
			return new Result(latitude, longitude, null);
		}

		public static Result failed(String reason) {
			return new Result(null, null, reason);
		}

		public boolean isFound() {
			return failure == null;
		}
	}
}
