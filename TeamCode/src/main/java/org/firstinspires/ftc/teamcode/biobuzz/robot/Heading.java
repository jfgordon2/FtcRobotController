package org.firstinspires.ftc.teamcode.biobuzz.robot;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.IMU;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;

/**
 * Everything about "which way is the robot facing?" in one file.
 * Heading is measured in degrees, and turning right (clockwise) makes it go UP.
 *
 *   1. The mentor's settings: how the Control Hub is mounted on this robot.
 *   2. Heading.Hub: reads the heading from the Hub's built-in sensor (the IMU).
 *   3. Angle arithmetic that Drive uses: wrap() and correction().
 *   4. Heading.Pose: a rough estimate of where the robot is, from wheel travel and heading.
 */
public final class Heading {
    // 1. HOW THE HUB IS MOUNTED. These are a starting guess. Look at the real robot, set them to
    // match, do the turn-by-hand check in the docs, and only then set MOUNTING_CONFIRMED to true.
    // Programs that drive by themselves stay still until it is true.
    public static final RevHubOrientationOnRobot.LogoFacingDirection LOGO =
            RevHubOrientationOnRobot.LogoFacingDirection.UP;
    public static final RevHubOrientationOnRobot.UsbFacingDirection USB =
            RevHubOrientationOnRobot.UsbFacingDirection.FORWARD;
    public static final boolean MOUNTING_CONFIRMED = false;

    /**
     * Anything that can report a heading. An interface is a promise that an object has this method.
     * Heading.Hub is the real sensor. The tests use a fake one that keeps the same promise.
     * NaN (Not a Number) means the heading is missing or too old to trust.
     */
    public interface Source {
        double degrees();
    }

    /** 2. The real heading sensor: the IMU (inertial measurement unit) inside the Control Hub. */
    public static final class Hub implements Source {
        // IMU is the SDK's type for the sensor; "imu" is its name in the robot configuration.
        private final IMU imu;
        private final boolean initialized;

        public Hub(HardwareMap map) {
            imu = map.get(IMU.class, "imu");
            initialized = imu.initialize(new IMU.Parameters(new RevHubOrientationOnRobot(LOGO, USB)));
        }

        @Override
        public double degrees() {
            if (!initialized) {
                return Double.NaN;
            }
            YawPitchRollAngles angles = imu.getRobotYawPitchRollAngles();
            double ageSeconds = (System.nanoTime() - angles.getAcquisitionTime()) / 1e9;
            double yaw = angles.getYaw(AngleUnit.DEGREES);
            boolean recentReading = ageSeconds >= 0 && ageSeconds <= 0.25;
            if (recentReading && Double.isFinite(yaw)) {
                // The SDK counts a left turn as positive. We count a right turn as positive.
                return -yaw;
            }
            return Double.NaN;
        }

        /** True when the mounting has been confirmed and the sensor is giving readings. */
        public boolean ready() {
            return MOUNTING_CONFIRMED && Double.isFinite(degrees());
        }

        /** Call the direction the robot faces right now "zero degrees". */
        public void resetAtStart() {
            imu.resetYaw();
        }
    }

    // 3. ANGLE ARITHMETIC. These never touch hardware.

    /** Put an angle in the range -180 to 180, so an error of 358 degrees becomes -2 degrees. */
    public static double wrap(double degrees) {
        // % is Java's remainder operator. Adding 360 before the second % also
        // handles negative inputs. Subtracting 180 puts the result around zero.
        return ((degrees + 180) % 360 + 360) % 360 - 180;
    }

    /** Turn power in proportion to the heading error, limited to +/-maximumPower. */
    public static double correction(double goal, double actual, double proportionalGain, double maximumPower) {
        double errorDegrees = wrap(goal - actual);
        double requestedPower = errorDegrees * proportionalGain;
        return Math.max(-maximumPower, Math.min(maximumPower, requestedPower));
    }

    /** 4. A rough position estimate: X forward from the start, Y to the right. Not field coordinates. */
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

    private Heading() {
    }
}
