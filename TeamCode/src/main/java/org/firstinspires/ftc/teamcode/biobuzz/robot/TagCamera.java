package org.firstinspires.ftc.teamcode.biobuzz.robot;

import org.firstinspires.ftc.teamcode.biobuzz.settings.*;
import android.util.Size;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagClusterDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

/**
 * Reads the selected hive cluster and checks freshness, alignment, and the shot range.
 * The OpMode calls update() each loop; turn() suggests power and mayFeed() checks permission.
 * This class owns the camera, but never moves a motor or feeder. Call close() at Stop.
 * Moving hive tags support camera-relative aiming, not fixed field localization.
 */
public final class TagCamera implements AutoCloseable {
    private String targetName = "UNSET";
    // Aiming values. These are starting guesses until they have been tried on our robot.
    // TeleOp and autonomous both aim with these, so the robot aims the same way in each.
    // Proportional gain (Kp): power per degree of aiming error.
    // An error of 10 degrees * 0.015 gives 0.15 power before the maximum power limit.
    public static final double TURN_PROPORTIONAL_GAIN = 0.015;
    public static final double MAXIMUM_TURN_POWER = 0.18;
    public static final double TOLERANCE_DEGREES = 2.0;
    // If shots land left or right of where the camera aims, correct it here.
    public static final double OFFSET_DEGREES = 0.0;
    // The bearing must stay inside the tolerance this long before the robot counts as aimed.
    public static final double ALIGNED_SECONDS = 0.3;
    private final AprilTagProcessor tags;
    private final VisionPortal camera;
    public AprilTagClusterDetection target;
    public boolean fresh;
    public boolean aligned;
    // SDK (software development kit) camera values: bearing in degrees.
    // XY range combines sideways X and forward Y distance; it excludes vertical Z.
    // NaN (Not a Number) below means the selected target has no measurement.
    public double bearing;
    public double range;
    public double ageSeconds;
    private final HeldFor onTarget = new HeldFor();

    public TagCamera(HardwareMap map) {
        tags = new AprilTagProcessor.Builder()
                .setTagLibrary(AprilTagGameDatabase.getBioBuzzTagLibrary())
                .setOutputUnits(DistanceUnit.INCH, AngleUnit.DEGREES).build();
        camera = new VisionPortal.Builder()
                .setCamera(map.get(WebcamName.class, "Webcam 1"))
                .setCameraResolution(new Size(640, 480)).addProcessor(tags).build();
    }

    public void setTarget(String name) {
        if (!targetName.equals(name)) {
            targetName = name;
            target = null;
            fresh = false;
            aligned = false;
            onTarget.reset();
            bearing = Double.NaN;
            range = Double.NaN;
        }
    }

    public String targetName() {
        return targetName;
    }

    /** Read the camera now. Alignment dwell runs only while aiming is requested. */
    public void update(boolean aimRequested) {
        target = null;
        for (AprilTagDetection detection : tags.getDetections()) {
            if (detection instanceof AprilTagClusterDetection) {
                AprilTagClusterDetection cluster = (AprilTagClusterDetection) detection;
                if (cluster.metadata != null && targetName.equals(cluster.metadata.name) && cluster.ftcPose != null) {
                    target = cluster;
                    break;
                }
            }
        }
        long now = System.nanoTime();
        if (target == null) {
            // No selected cluster was found in the camera's latest results.
            bearing = Double.NaN;
            range = Double.NaN;
            ageSeconds = Double.POSITIVE_INFINITY;
        } else {
            bearing = target.ftcPose.bearing;
            range = target.ftcPose.range;
            // A nanosecond is one billionth of a second.
            ageSeconds = (now - target.frameAcquisitionNanoTime) / 1e9;
        }
        fresh = camera.getCameraState() == VisionPortal.CameraState.STREAMING
                && usable(bearing, range, ageSeconds);
        boolean within = aimRequested && fresh && Math.abs(bearing - OFFSET_DEGREES) <= TOLERANCE_DEGREES;
        aligned = onTarget.longEnough(now / 1e9, within, ALIGNED_SECONDS);
    }

    /** True when a camera reading is a real, recent measurement (at most a quarter second old). */
    static boolean usable(double bearing, double range, double ageSeconds) {
        return Double.isFinite(bearing) && Double.isFinite(range) && range > 0
                && Double.isFinite(ageSeconds) && ageSeconds >= 0 && ageSeconds <= 0.25;
    }

    /** Turn power that brings a camera bearing to the goal bearing: error times gain, then limited. */
    public static double turnPower(double bearingDegrees, double goalDegrees) {
        if (!Double.isFinite(bearingDegrees) || !Double.isFinite(goalDegrees)) {
            return 0;
        }
        // arcadeDrive: positive rotate turns right. The camera's positive bearing is to the left.
        double errorDegrees = bearingDegrees - goalDegrees;
        double requestedPower = -errorDegrees * TURN_PROPORTIONAL_GAIN;
        return Math.max(-MAXIMUM_TURN_POWER, Math.min(MAXIMUM_TURN_POWER, requestedPower));
    }

    /** Turn power toward the selected target, or zero when it cannot be seen or motion is off. */
    public double turn() {
        if (!StarterRobot.MOTION_ENABLED || !fresh) {
            return 0;
        }
        return turnPower(bearing, OFFSET_DEGREES);
    }

    /** The caller must also check launcher speed and, in TeleOp, the driver buttons. */
    public boolean mayFeed() {
        // Our measured table knows which ranges we can shoot from. An empty table allows none.
        return StarterRobot.MOTION_ENABLED && StarterRobot.FEED_ENABLED && aligned
                && ShooterCalibration.covers(range);
    }

    @Override
    public void close() {
        camera.close();
    }
}
