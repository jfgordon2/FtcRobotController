package org.firstinspires.ftc.teamcode.biobuzz.settings;

import org.firstinspires.ftc.teamcode.biobuzz.robot.*;

/**
 * Shooter settings and our measured shooting table: camera range in inches -> launcher speed.
 * Launcher speed is always encoder ticks per second, the same unit goBILDA's code uses.
 * Between two measured rows the speed is interpolated (a straight line between the dots).
 * Outside the measured rows there is no answer: the robot refuses to shoot instead of guessing.
 */
public final class ShooterCalibration {
    // true = read distance and aim with "Webcam 1". false = tape measure only, aim by hand.
    public static final boolean USE_CAMERA = true;

    // The tuner starts at the robot's normal speed and changes by one step per D-pad press.
    public static final double START_SPEED = StarterRobot.LAUNCHER_SPEED;
    public static final double SPEED_STEP = 25;
    // Mentor-approved top speed for testing. The tuner and the table never go above it.
    public static final double MAXIMUM_SPEED = 1800;
    // How long the feeder runs for one tap of Y. It is a time, not a ball counter.
    public static final double FEED_PULSE_SECONDS = 0.20;

    // OUR MEASURED TABLE. Empty means "not measured yet", and the robot will not feed by itself.
    // Copy rows from the shooting worksheet: closest range first, one speed per range.
    // Example shape only (these are NOT measurements): {36, 44, 52} and {1100, 1175, 1250}.
    // Measure again whenever the robot, launcher, camera mount, or target changes.
    public static final double[] RANGE_INCHES = {};
    public static final double[] SPEED_TICKS_PER_SECOND = {};

    /** Launcher speed for this camera range from our table. NaN (Not a Number) means "do not shoot". */
    public static double speedFor(double range) {
        return speedFor(range, RANGE_INCHES, SPEED_TICKS_PER_SECOND, MAXIMUM_SPEED);
    }

    /** True when our table has measured rows on both sides of this camera range. */
    public static boolean covers(double range) {
        return Double.isFinite(speedFor(range));
    }

    /** What is wrong with our table, in words. Empty text means nothing is wrong. */
    public static String tableProblem() {
        return problem(RANGE_INCHES, SPEED_TICKS_PER_SECOND, MAXIMUM_SPEED);
    }

    // The two methods below do the work for any table, so the tests can try made-up tables.

    static String problem(double[] ranges, double[] speeds, double maximumSpeed) {
        if (!Double.isFinite(maximumSpeed) || maximumSpeed <= 0) {
            return "Invalid maximum speed";
        }
        if (ranges.length < 2 || speeds.length != ranges.length) {
            return "Need at least two measured rows (range and speed)";
        }
        for (int i = 0; i < ranges.length; i++) {
            boolean rangeBad = !Double.isFinite(ranges[i]) || ranges[i] <= 0
                    || (i > 0 && ranges[i] <= ranges[i - 1]);
            boolean speedBad = !Double.isFinite(speeds[i]) || speeds[i] <= 0 || speeds[i] > maximumSpeed;
            if (rangeBad || speedBad) {
                return "Row " + (i + 1) + ": ranges must increase; speeds must be above 0 and below the cap";
            }
        }
        return "";
    }

    static double speedFor(double range, double[] ranges, double[] speeds, double maximumSpeed) {
        if (!problem(ranges, speeds, maximumSpeed).isEmpty() || !Double.isFinite(range)
                || range < ranges[0] || range > ranges[ranges.length - 1]) {
            return Double.NaN;
        }
        for (int i = 0; i < ranges.length - 1; i++) {
            if (range <= ranges[i + 1]) {
                // How far is this range between row i and row i + 1? 0 = at row i; 1 = at the next row.
                double fraction = (range - ranges[i]) / (ranges[i + 1] - ranges[i]);
                return speeds[i] + fraction * (speeds[i + 1] - speeds[i]);
            }
        }
        return Double.NaN;
    }

    private ShooterCalibration() { }
}
