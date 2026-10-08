package org.firstinspires.ftc.teamcode.biobuzz.robot;

import com.qualcomm.robotcore.hardware.DcMotor;

/**
 * Drives one distance or turns one angle using the drive encoders and a heading sensor.
 * It works out how hard to push and which way to turn, then asks StarterRobot.arcadeDrive()
 * to move the wheels. That is the only place drive motors are powered.
 * An OpMode starts a command once, then calls update() on every loop until DONE or FAULT.
 * No method waits for motion to finish, so the SDK (software development kit) can
 * keep processing Stop and telemetry.
 */
public final class Drive {
    // TICKS PER INCH: how many encoder ticks a drive wheel counts while the robot rolls one inch.
    // There is one line per robot. Change ONLY the line for the robot you are using (ROBOT in
    // StarterRobot.java), and type your measured number in place of the starting number.
    public static final double TICKS_PER_INCH_BIOBUZZ_GOBILDA = 45.28;
    public static final double TICKS_PER_INCH_PRACTICE_GOBILDA = 45.28;
    public static final double TICKS_PER_INCH_PRACTICE_REV = 44.56;
    //
    // WHERE THE STARTING NUMBERS COME FROM
    // Each one is this formula, worked out once and rounded to two decimal places:
    //
    //   goBILDA:  537.7 / (Math.PI * 96 / 25.4)  = 45.28
    //   REV:      420   / (Math.PI * 3)          = 44.56
    //
    // In words: ticks in one turn of the wheel / inches the robot rolls in one turn of the wheel.
    //
    //   537.7 and 420   Ticks in one turn of the wheel. The encoder counts 28 ticks per turn of the
    //                   motor, and the gearbox turns the motor 19.2 times (goBILDA) or 15 times (REV)
    //                   for each turn of the wheel: 28 x 15 = 420, and 28 x 19.2 is 537.6. goBILDA
    //                   lists 537.7 because its exact gear ratio is a little over 19.2.
    //   96 and 3        The wheel's diameter: 96 millimetres (goBILDA) or 3 inches (REV).
    //   / 25.4          Turns millimetres into inches (25.4 mm = 1 inch): 96 / 25.4 = 3.780 inches.
    //   Math.PI         Java's name for pi, about 3.1416.
    //   Math.PI * diameter   The distance around the wheel (its circumference), which is how far the
    //                   robot rolls in one turn: 3.1416 x 3.780 = 11.87 inches; 3.1416 x 3 = 9.425 inches.
    //
    //   So: 537.7 / 11.87 = 45.28 and 420 / 9.425 = 44.56.
    //
    // The formula assumes the wheel never slips and is exactly the size on the box. Meeting 5 checks
    // the number with a tape measure; Meeting 10 works the formula out by hand. If they disagree,
    // trust the tape.

    // The code below uses the line for this robot. You do not need to change it.
    public static final double TICKS_PER_INCH = StarterRobot.pick(
            TICKS_PER_INCH_BIOBUZZ_GOBILDA, TICKS_PER_INCH_PRACTICE_GOBILDA, TICKS_PER_INCH_PRACTICE_REV);
    // Proportional gain (often written Kp): correction power = error * gain.
    // For turns: 10 degrees of error * 0.015 requests 0.15 power, before the power limit.
    public static final double TURN_PROPORTIONAL_GAIN = 0.015;
    // Power per degree of heading error while driving a straight segment.
    public static final double HEADING_HOLD_PROPORTIONAL_GAIN = 0.02;
    public static final double HEADING_TOLERANCE_DEGREES = 2;
    // Power per inch remaining: 2 inches * 0.06 requests 0.12 power.
    public static final double DISTANCE_PROPORTIONAL_GAIN = 0.06;
    public static final double DRIVE_POWER = 0.2;
    public static final double TURN_POWER = 0.18;
    // A drive or turn that has not finished after this long stops with a FAULT.
    public static final double COMMAND_TIMEOUT_SECONDS = 4.0;
    // The robot must stay inside its tolerance this long before a command counts as DONE.
    private static final double SETTLE_SECONDS = 0.25;
    private static final double DISTANCE_TOLERANCE_INCHES = 0.5;
    private static final double MAXIMUM_HEADING_CORRECTION_POWER = 0.10;

    // An enum gives names to the three possible outcomes of update().
    public enum Result { RUNNING, DONE, FAULT }

    private final StarterRobot robot;
    private final DcMotor leftMotor;
    private final DcMotor rightMotor;
    private final Heading.Source headingSensor;
    // Where each wheel's encoder should read when the current command is finished.
    private int leftTargetTicks;
    private int rightTargetTicks;
    private final Heading.Pose pose = new Heading.Pose();
    private int poseLeftTicks;
    private int poseRightTicks;

    // These fields remember the command between calls to update().
    private boolean turning;
    private double goalHeading;
    private double commandStartedSeconds;
    private double maximumDrivePower;
    private Result result = Result.DONE;
    private final HeldFor arrived = new HeldFor();
    private String reason = "";

    public Drive(StarterRobot robot, Heading.Source headingSensor) {
        this.robot = robot;
        // The motors are used here only to read their encoders.
        leftMotor = robot.leftDrive;
        rightMotor = robot.rightDrive;
        this.headingSensor = headingSensor;
        resetPoseAtStart();
    }

    /** Establish the local position origin after the OpMode resets yaw at Start. */
    public void resetPoseAtStart() {
        poseLeftTicks = leftMotor.getCurrentPosition();
        poseRightTicks = rightMotor.getCurrentPosition();
        pose.reset(headingSensor.degrees());
    }

    /** Estimate X/Y from wheel travel and heading; tire slip can make this inaccurate. */
    public void observePose() {
        double yaw = headingSensor.degrees();
        if (!Double.isFinite(yaw)) {
            return;
        }
        int leftTicks = leftMotor.getCurrentPosition();
        int rightTicks = rightMotor.getCurrentPosition();
        if (!Double.isFinite(pose.heading)) {
            pose.reset(yaw);
        }
        double leftTravelTicks = leftTicks - poseLeftTicks;
        double rightTravelTicks = rightTicks - poseRightTicks;
        double averageTravelTicks = (leftTravelTicks + rightTravelTicks) / 2;
        double averageTravelInches = averageTravelTicks / TICKS_PER_INCH;
        pose.update(averageTravelInches, yaw);
        poseLeftTicks = leftTicks;
        poseRightTicks = rightTicks;
    }

    public double xInches() {
        return pose.x;
    }

    public double yInches() {
        return pose.y;
    }

    public double headingDegrees() {
        return headingSensor.degrees();
    }

    /** Start once. Positive inches go forward; negative inches go backward. */
    public void driveInches(double inches, double now) {
        begin(inches, DRIVE_POWER, now);
    }

    /** Start once. Positive degrees turn right (clockwise); negative degrees turn left. */
    public void turnDegrees(double degrees, double now) {
        begin(0, TURN_POWER, now);
        if (result == Result.FAULT) {
            return;
        }
        if (!Double.isFinite(degrees) || Math.abs(degrees) > 180) {
            fault("Turn must be finite and within +/-180 degrees");
            return;
        }
        goalHeading = Heading.wrap(headingSensor.degrees() + degrees);
        turning = true;
    }

    /** Start once. Turn the shortest way to face this heading (the autonomous uses it to turn back). */
    public void turnToHeading(double heading, double now) {
        begin(0, TURN_POWER, now);
        if (result == Result.FAULT) {
            return;
        }
        if (!Double.isFinite(heading)) {
            fault("Heading to turn to is not a number");
            return;
        }
        goalHeading = Heading.wrap(heading);
        turning = true;
    }

    private void begin(double inches, double requestedPower, double now) {
        stop();
        if (!Double.isFinite(inches) || Math.abs(inches) > 96
                || !Double.isFinite(TICKS_PER_INCH) || TICKS_PER_INCH <= 0
                || !Double.isFinite(headingSensor.degrees())) {
            result = Result.FAULT;
            reason = "Invalid drive calibration/command";
            return;
        }
        int travelTicks = (int) Math.round(inches * TICKS_PER_INCH);
        leftTargetTicks = leftMotor.getCurrentPosition() + travelTicks;
        rightTargetTicks = rightMotor.getCurrentPosition() + travelTicks;
        turning = false;
        goalHeading = headingSensor.degrees();
        commandStartedSeconds = now;
        maximumDrivePower = requestedPower;
        arrived.reset();
        reason = "";
        result = Result.RUNNING;
    }

    /** Call each loop using seconds from the same timer used to start the command. */
    public Result update(double now) {
        if (result != Result.RUNNING) {
            return result;
        }
        observePose();
        double yaw = headingSensor.degrees();
        if (!Double.isFinite(yaw)) {
            return fault("Heading sensor (IMU) missing/stale: stop and reinitialize");
        }
        if (now - commandStartedSeconds >= COMMAND_TIMEOUT_SECONDS) {
            return fault("Drive/turn timeout: inspect heading, both encoders, and route");
        }

        double headingErrorDegrees = Heading.wrap(goalHeading - yaw);
        double leftErrorInches = (leftTargetTicks - leftMotor.getCurrentPosition()) / TICKS_PER_INCH;
        double rightErrorInches = (rightTargetTicks - rightMotor.getCurrentPosition()) / TICKS_PER_INCH;
        boolean headingReached = Math.abs(headingErrorDegrees) <= HEADING_TOLERANCE_DEGREES;
        boolean distanceReached = Math.abs(leftErrorInches) <= DISTANCE_TOLERANCE_INCHES
                && Math.abs(rightErrorInches) <= DISTANCE_TOLERANCE_INCHES;
        // A turn needs only heading; a drive needs both wheel distances AND heading.
        boolean withinTolerance;
        if (turning) {
            withinTolerance = headingReached;
        } else {
            withinTolerance = headingReached && distanceReached;
        }
        if (arrived.longEnough(now, withinTolerance, SETTLE_SECONDS)) {
            stop();
            return result;
        }

        double turnPower;
        if (turning) {
            turnPower = Heading.correction(goalHeading, yaw, TURN_PROPORTIONAL_GAIN, TURN_POWER);
        } else {
            turnPower = Heading.correction(goalHeading, yaw,
                    HEADING_HOLD_PROPORTIONAL_GAIN, MAXIMUM_HEADING_CORRECTION_POWER);
        }
        if (headingReached) {
            turnPower = 0;
        }
        if (turning) {
            robot.arcadeDrive(0, turnPower);
        } else {
            // Each wheel wants its own push to finish its own distance. The average of the two
            // is the forward part; half their difference is extra turning. Clockwise heading
            // correction has the same sign during forward and backward travel.
            double leftPush = distancePower(leftErrorInches);
            double rightPush = distancePower(rightErrorInches);
            double forward = (leftPush + rightPush) / 2;
            double rotate = turnPower + (leftPush - rightPush) / 2;
            robot.arcadeDrive(forward, rotate);
        }
        return result;
    }

    private double distancePower(double errorInches) {
        if (Math.abs(errorInches) <= DISTANCE_TOLERANCE_INCHES) {
            return 0;
        }
        double requestedPower = errorInches * DISTANCE_PROPORTIONAL_GAIN;
        return Math.max(-maximumDrivePower, Math.min(maximumDrivePower, requestedPower));
    }

    public void stop() {
        robot.arcadeDrive(0, 0);
        result = Result.DONE;
    }

    private Result fault(String message) {
        stop();
        result = Result.FAULT;
        reason = message;
        return result;
    }

    public String reason() {
        return reason;
    }

    // Package-private: only the tests use these.
    int leftTargetTicks() {
        return leftTargetTicks;
    }

    int rightTargetTicks() {
        return rightTargetTicks;
    }
}
