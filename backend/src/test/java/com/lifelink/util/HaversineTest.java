package com.lifelink.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HaversineTest {

    @Test
    void samePointIsZero() {
        assertEquals(0.0, Haversine.distanceKm(52.52, 13.405, 52.52, 13.405), 0.001);
    }

    @Test
    void londonToParisIsAbout343Km() {
        double distance = Haversine.distanceKm(51.5074, -0.1278, 48.8566, 2.3522);
        assertEquals(343.0, distance, 10.0);
    }

    @Test
    void nullCoordinatesGiveInfinity() {
        assertTrue(Double.isInfinite(Haversine.distanceKm(null, 13.4, 52.5, 13.4)));
        assertTrue(Double.isInfinite(Haversine.distanceKm(52.5, 13.4, null, 13.4)));
    }

    @Test
    void invalidCoordinatesGiveInfinity() {
        assertTrue(Double.isInfinite(Haversine.distanceKm(95.0, 13.4, 52.5, 13.4)));
        assertTrue(Double.isInfinite(Haversine.distanceKm(52.5, 200.0, 52.5, 13.4)));
    }

    @Test
    void estimatedArrivalHasMinimum() {
        assertEquals(30, Haversine.estimatedArrivalMinutes(0.0));
        assertEquals(45, Haversine.estimatedArrivalMinutes(Double.POSITIVE_INFINITY));
        assertTrue(Haversine.estimatedArrivalMinutes(10.0) >= 30);
    }
}
