package org.firstinspires.ftc.teamcode.biobuzz;

import com.qualcomm.robotcore.hardware.DcMotor;

/**
 * Drives one distance or turns one angle using two motors and a heading sensor.
 * An OpMode starts a command once, then calls update() on every loop until DONE or FAULT.
 * No method waits for motion to finish, so the SDK (software development kit) can
 * keep processing Stop and telemetry.
 * TagDrive uses this same class for the measured return from a camera approach.
 */
public final class StarterDrive {
    // Nominal drive encoder ticks per wheel revolution / wheel circumference in inches.
    public static final double TICKS_PER_INCH = 537.7 / (96.0 * Math.PI / 25.4);
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
    public static final double COMMAND_TIMEOUT_SECONDS = DriveMath.COMMAND_TIMEOUT_SECONDS;
    private static final double DISTANCE_TOLERANCE_INCHES = 0.5;
    private static final double MAXIMUM_HEADING_CORRECTION_POWER = 0.10;

    // An enum gives names to the three possible outcomes of update().
    public enum Result { RUNNING, DONE, FAULT }

    private final DcMotor leftMotor;
    private final DcMotor rightMotor;
    private final HeadingSource headingSensor;
    private final HeadingMath.Pose pose = new HeadingMath.Pose();
    private int poseLeftTicks;
    private int poseRightTicks;

    // These fields remember the command between calls to update().
    private boolean turning;
    private double goalHeading;
    private double commandStartedSeconds;
    private double maximumDrivePower;
    private double commandTimeout = COMMAND_TIMEOUT_SECONDS;
    private Result result = Result.DONE;
    private DriveMath.Completion completion;
    private String reason = "";

    public StarterDrive(DcMotor leftMotor, DcMotor rightMotor, HeadingSource headingSensor) {
        this.leftMotor = leftMotor;
        this.rightMotor = rightMotor;
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
        goalHeading = HeadingMath.wrap(headingSensor.degrees() + degrees);
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
        leftMotor.setTargetPosition(leftMotor.getCurrentPosition() + travelTicks);
        rightMotor.setTargetPosition(rightMotor.getCurrentPosition() + travelTicks);
        // Our update() calculates power itself; the motor controller does not seek the target.
        leftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rightMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        turning = false;
        goalHeading = headingSensor.degrees();
        commandTimeout = COMMAND_TIMEOUT_SECONDS;
        commandStartedSeconds = now;
        maximumDrivePower = requestedPower;
        completion = new DriveMath.Completion();
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
        if (now - commandStartedSeconds >= commandTimeout) {
            return fault("Drive/turn timeout: inspect heading, both encoders, and route");
        }

        double headingErrorDegrees = HeadingMath.wrap(goalHeading - yaw);
        double leftErrorInches = (leftMotor.getTargetPosition() - leftMotor.getCurrentPosition()) / TICKS_PER_INCH;
        double rightErrorInches = (rightMotor.getTargetPosition() - rightMotor.getCurrentPosition()) / TICKS_PER_INCH;
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
        if (completion.settledForRequiredTime(now, withinTolerance)) {
            stop();
            return result;
        }

        double turnPower;
        if (turning) {
            turnPower = HeadingMath.correction(goalHeading, yaw, TURN_PROPORTIONAL_GAIN, TURN_POWER);
        } else {
            turnPower = HeadingMath.correction(goalHeading, yaw,
                    HEADING_HOLD_PROPORTIONAL_GAIN, MAXIMUM_HEADING_CORRECTION_POWER);
        }
        if (headingReached) {
            turnPower = 0;
        }
        if (turning) {
            arcade(0, turnPower);
        } else {
            // Clockwise correction has the same sign during forward and backward travel.
            double leftPower = distancePower(leftErrorInches);
            double rightPower = distancePower(rightErrorInches);
            leftMotor.setPower(Math.max(-1, Math.min(1, leftPower + turnPower)));
            rightMotor.setPower(Math.max(-1, Math.min(1, rightPower - turnPower)));
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

    /** Apply a camera controller's powers while no distance/turn command is running. */
    public void arcade(double forward, double rotate) {
        double scale = Math.max(1, Math.abs(forward) + Math.abs(rotate));
        leftMotor.setPower((forward + rotate) / scale);
        rightMotor.setPower((forward - rotate) / scale);
    }

    public void stop() {
        leftMotor.setPower(0);
        rightMotor.setPower(0);
        leftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rightMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
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

    // Package-private helpers: TagDrive uses these, but student OpModes need not call them.
    DriveCheckpoint checkpoint() {
        return new DriveCheckpoint(leftMotor.getCurrentPosition(), rightMotor.getCurrentPosition(), headingSensor.degrees());
    }

    boolean wheelsPowered() {
        return Math.max(Math.abs(leftMotor.getPower()), Math.abs(rightMotor.getPower())) > 0.04;
    }

    Result startRecordedMove(double inches, double recordedHeading, boolean turnOnly,
                             double timeoutSeconds, double now) {
        begin(inches, DRIVE_POWER, now);
        goalHeading = recordedHeading;
        turning = turnOnly;
        commandTimeout = timeoutSeconds;
        return result;
    }
}
