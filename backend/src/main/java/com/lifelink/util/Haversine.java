package com.lifelink.util;

/**
 * Great-circle distance calculation using the Haversine formula.
 */
public final class Haversine {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private Haversine() {
    }

    /**
     * Distance in kilometres between two coordinates.
     *
     * @return distance in km, or {@code Double.POSITIVE_INFINITY} if either point is invalid
     */
    public static double distanceKm(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return Double.POSITIVE_INFINITY;
        }
        if (!isValidLatitude(lat1) || !isValidLatitude(lat2)
                || !isValidLongitude(lon1) || !isValidLongitude(lon2)) {
            return Double.POSITIVE_INFINITY;
        }

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double sinLat = Math.sin(dLat / 2);
        double sinLon = Math.sin(dLon / 2);

        double a = sinLat * sinLat
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * sinLon * sinLon;
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Estimated arrival minutes for a donor given a straight-line distance.
     * Accounts for traffic overhead on top of a 50 km/h average.
     */
    public static int estimatedArrivalMinutes(double distanceKm) {
        if (Double.isInfinite(distanceKm)) {
            return 45;
        }
        return Math.max(30, (int) Math.round(distanceKm / 50.0 * 60.0) + 15);
    }

    public static boolean isValidLatitude(double lat) {
        return lat >= -90 && lat <= 90;
    }

    public static boolean isValidLongitude(double lon) {
        return lon >= -180 && lon <= 180;
    }
}
