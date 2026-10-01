package org.firstinspires.ftc.teamcode.biobuzz;

/**
 * Compares the camera view with the measured view saved on one route card.
 * StarterBotPositionAuto uses matches() before starting; status() explains it in telemetry.
 * This optional check never moves the robot or resets its estimated position.
 */
public final class StartTagCheck {
    public final boolean enabled;
    public final double rangeInches;
    public final double bearingDegrees;

    public StartTagCheck(boolean enabled, double range, double bearing) {
        this.enabled = enabled;
        rangeInches = range;
        bearingDegrees = bearing;
    }

    public boolean matches(boolean fresh, double range, double bearing) {
        return enabled && fresh && Double.isFinite(range) && Double.isFinite(bearing)
                && Double.isFinite(rangeInches) && rangeInches > 0 && Double.isFinite(bearingDegrees)
                && Math.abs(range - rangeInches) <= 2
                && Math.abs(HeadingMath.wrap(bearing - bearingDegrees)) <= 3;
    }

    public String status(boolean fresh, double range, double bearing) {
        if (!enabled) {
            return "NOT CONFIGURED: use physical start marks";
        }
        if (matches(fresh, range, bearing)) {
            return "MATCH: also verify marks and cell state";
        }
        return "MISMATCH/MISSING: reposition by hand before countdown";
    }
}
