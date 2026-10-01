package org.firstinspires.ftc.teamcode.biobuzz;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.IMU;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;

/**
 * Reads the Hub IMU (inertial measurement unit, the orientation sensor) and converts its yaw to clockwise-positive degrees for StarterDrive.
 * Yaw is rotation about the vertical axis; heading is the direction the robot faces.
 * Configure the physical mounting first; the OpMode resets zero on its marked start.
 * Missing, stale, or invalid readings return NaN (Not a Number, used here for an unavailable reading).
 */
public final class HubHeading implements HeadingSource {
    // ASSUMPTION for commissioning; inspect the assembled Hub before confirming.
    public static final RevHubOrientationOnRobot.LogoFacingDirection LOGO =
            RevHubOrientationOnRobot.LogoFacingDirection.UP;
    public static final RevHubOrientationOnRobot.UsbFacingDirection USB =
            RevHubOrientationOnRobot.UsbFacingDirection.FORWARD;
    public static final boolean MOUNTING_CONFIRMED = false;
    // IMU is the SDK (software development kit) type; "imu" below is its configured name.
    private final IMU imu;
    private final boolean initialized;

    public HubHeading(HardwareMap map) {
        imu = map.get(IMU.class, "imu");
        initialized = imu.initialize(new IMU.Parameters(new RevHubOrientationOnRobot(LOGO, USB)));
    }

    @Override
    public double degrees() {
        if (!initialized) {
            return Double.NaN;
        }
        YawPitchRollAngles angles = imu.getRobotYawPitchRollAngles();
        double age = (System.nanoTime() - angles.getAcquisitionTime()) / 1e9;
        double yaw = angles.getYaw(AngleUnit.DEGREES);
        boolean recentReading = age >= 0 && age <= 0.25;
        if (recentReading && Double.isFinite(yaw)) {
            // SDK yaw is positive to the left; our drive commands are positive to the right.
            return -yaw;
        }
        return Double.NaN;
    }

    public boolean ready() {
        return MOUNTING_CONFIRMED && Double.isFinite(degrees());
    }

    public void resetAtStart() {
        imu.resetYaw();
    }
}
