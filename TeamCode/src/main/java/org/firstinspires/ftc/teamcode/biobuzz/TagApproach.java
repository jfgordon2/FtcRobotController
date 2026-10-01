package org.firstinspires.ftc.teamcode.biobuzz;

/**
 * Decides forward/turn power requests from camera range and bearing.
 * TagDrive calls update() each loop, applies these requests, and records the return path.
 * ALIGN faces the tag, RANGE adjusts distance, and FINAL_ALIGN applies the shot offset.
 * This class has no motor dependencies, so its decisions can be tested off the robot.
 */
public final class TagApproach {
    public static final double TIMEOUT_SECONDS = 4.0;
    public static final double RECOVERY_STEP_SECONDS = 2.0;
    // Proportional gain (Kp): power per degree of aiming error.
    // An error of 10 degrees * 0.015 gives 0.15 power before the maximum power limit.
    public static final double TURN_PROPORTIONAL_GAIN = 0.015;
    public static final double MAXIMUM_TURN_POWER = 0.18;
    // Power per inch of distance error: 2 inches * 0.035 gives 0.07 forward power.
    public static final double RANGE_PROPORTIONAL_GAIN = 0.035;
    public static final double MAXIMUM_FORWARD_POWER = 0.15;
    public static final double RANGE_TOLERANCE_INCHES = 0.5;
    public static final double BEARING_TOLERANCE_DEGREES = 2;
    public static final double MAXIMUM_TRANSLATION_INCHES = 6;
    public static final double MAXIMUM_WHEEL_TRAVEL_INCHES = 12;
    public enum Stage {
        ALIGN, RANGE, FINAL_ALIGN, DONE, FAULT
    }

    private Stage stage = Stage.ALIGN;
    private final String target;
    private final double rangeGoal;
    private final double bearingGoal;
    private final double started;
    // NaN (Not a Number) means the current phase has not started settling.
    private double settled = Double.NaN;
    private String reason = "";
    // Output requests from the latest update, each expressed as motor power.
    public double forward;
    public double turn;

    public TagApproach(String target, double range, double bearing, double now) {
        this.target = target;
        rangeGoal = range;
        bearingGoal = bearing;
        started = now;
        if (target == null || target.equals("UNSET") || !Double.isFinite(range) || range <= 0
                || !Double.isFinite(bearing) || Math.abs(bearing) > 10 || !Double.isFinite(now)) {
            fail("Invalid tag destination/range/bearing");
        }
    }

    public Stage update(double now, String observedTarget, boolean fresh, double range, double bearing,
            double translationInches, double wheelTravelInches) {
        forward = 0;
        turn = 0;
        if (stage == Stage.DONE || stage == Stage.FAULT) {
            return stage;
        }
        if (!Double.isFinite(now) || now < started || now - started >= TIMEOUT_SECONDS) {
            return fail("Tag positioning timeout");
        }
        if (!target.equals(observedTarget) || !fresh || !Double.isFinite(range)
                || range <= 0 || !Double.isFinite(bearing)) {
            return fail("Selected tag missing, stale, or invalid");
        }
        if (!Double.isFinite(translationInches) || !Double.isFinite(wheelTravelInches)
                || translationInches >= MAXIMUM_TRANSLATION_INCHES || wheelTravelInches >= MAXIMUM_WHEEL_TRAVEL_INCHES) {
            return fail("Tag travel limit reached");
        }
        double rangeError = range - rangeGoal;
        if (stage == Stage.ALIGN) {
            if (settled(now, Math.abs(bearing) <= BEARING_TOLERANCE_DEGREES, 0.2)) {
                next(Stage.RANGE);
            } else if (Math.abs(bearing) > BEARING_TOLERANCE_DEGREES) {
                turn = AimMath.turn(bearing, 0, TURN_PROPORTIONAL_GAIN, MAXIMUM_TURN_POWER);
            }
        } else if (stage == Stage.RANGE) {
            // No curved path: keeping this segment straight makes its measured reverse reproducible.
            if (Math.abs(bearing) > 8) {
                return fail("Target moved off straight approach; reposition start");
            }
            if (settled(now, Math.abs(rangeError) <= RANGE_TOLERANCE_INCHES, 0.2)) {
                next(Stage.FINAL_ALIGN);
            } else if (Math.abs(rangeError) > RANGE_TOLERANCE_INCHES) {
                forward = Math.max(-MAXIMUM_FORWARD_POWER, Math.min(MAXIMUM_FORWARD_POWER, rangeError * RANGE_PROPORTIONAL_GAIN));
            }
        } else {
            if (Math.abs(rangeError) > RANGE_TOLERANCE_INCHES) {
                return fail("Range changed during final alignment");
            }
            if (settled(now, Math.abs(bearing - bearingGoal) <= BEARING_TOLERANCE_DEGREES, 0.3)) {
                next(Stage.DONE);
            } else if (Math.abs(bearing - bearingGoal) > BEARING_TOLERANCE_DEGREES) {
                turn = AimMath.turn(bearing, bearingGoal, TURN_PROPORTIONAL_GAIN, MAXIMUM_TURN_POWER);
            }
        }
        return stage;
    }

    private boolean settled(double now, boolean within, double duration) {
        if (!within) {
            settled = Double.NaN;
            return false;
        }
        if (Double.isNaN(settled)) {
            settled = now;
        }
        return now - settled >= duration;
    }

    private void next(Stage next) {
        stage = next;
        settled = Double.NaN;
        forward = 0;
        turn = 0;
    }

    private Stage fail(String message) {
        stage = Stage.FAULT;
        reason = message;
        forward = 0;
        turn = 0;
        return stage;
    }

    public Stage stage() {
        return stage;
    }

    public String reason() {
        return reason;
    }
}
