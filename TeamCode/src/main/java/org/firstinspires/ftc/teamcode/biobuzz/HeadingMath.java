package org.firstinspires.ftc.teamcode.biobuzz;

/**
 * Angle calculations and a local position estimate used by StarterDrive.
 * Angles are clockwise-positive degrees. These calculations never command hardware.
 */
public final class HeadingMath {
    private HeadingMath() {
    }

    /** Put an angle in [-180, 180), so 358 degrees of error becomes -2 degrees. */
    public static double wrap(double degrees) {
        // % is Java's remainder operator. Adding 360 before the second % also
        // handles negative inputs. Subtracting 180 puts the result around zero.
        return ((degrees + 180) % 360 + 360) % 360 - 180;
    }

    /** Request power in proportion to heading error, limited to +/-maximumPower. */
    public static double correction(double goal, double actual, double proportionalGain, double maximumPower) {
        double errorDegrees = wrap(goal - actual);
        double requestedPower = errorDegrees * proportionalGain;
        return Math.max(-maximumPower, Math.min(maximumPower, requestedPower));
    }

    /** Local dead reckoning: X forward from start, Y right; not official field coordinates. */
    public static final class Pose {
        public double x;
        public double y;
        public double heading;

        public void reset(double degrees) {
            x = 0;
            y = 0;
            heading = degrees;
        }

        public void update(double inches, double degrees) {
            // Approximate travel using the direction halfway between the old
            // heading and the new one. Java's sin/cos functions require radians.
            double headingChangeDegrees = wrap(degrees - heading);
            double middleDegrees = heading + headingChangeDegrees / 2;
            double middleRadians = Math.toRadians(middleDegrees);
            x += inches * Math.cos(middleRadians);
            y += inches * Math.sin(middleRadians);
            heading = degrees;
        }
    }
}
