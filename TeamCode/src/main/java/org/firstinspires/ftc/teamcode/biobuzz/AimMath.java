package org.firstinspires.ftc.teamcode.biobuzz;

/**
 * Small calculations used by HiveAim and TagApproach: valid readings, turn power, and range.
 * These static methods have no saved state or hardware, so tests can call them directly.
 */
public final class AimMath {
    private AimMath() {
    }

    public static boolean usable(double bearing, double range, double ageSeconds) {
        return Double.isFinite(bearing) && Double.isFinite(range) && range > 0
                && Double.isFinite(ageSeconds) && ageSeconds >= 0 && ageSeconds <= 0.25;
    }

    /** Multiply angle error by proportional gain, then limit power to the allowed range. */
    public static double turn(double bearing, double offset,
                              double proportionalGain, double maximumPower) {
        if (!Double.isFinite(bearing) || !Double.isFinite(offset)
                || !Double.isFinite(proportionalGain) || !Double.isFinite(maximumPower)
                || proportionalGain <= 0 || maximumPower <= 0) {
            return 0;
        }
        // Vendor arcadeDrive: positive rotation turns right; SDK positive bearing is left.
        double errorDegrees = bearing - offset;
        double requestedPower = -errorDegrees * proportionalGain;
        // Keep the request between -maximumPower and +maximumPower.
        return Math.max(-maximumPower, Math.min(maximumPower, requestedPower));
    }

    public static boolean inWindow(double range, double minimumRange, double maximumRange) {
        return Double.isFinite(range) && Double.isFinite(minimumRange) && Double.isFinite(maximumRange)
                && minimumRange > 0 && maximumRange >= minimumRange && range >= minimumRange && range <= maximumRange;
    }
}
