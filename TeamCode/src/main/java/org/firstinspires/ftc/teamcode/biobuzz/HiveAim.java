package org.firstinspires.ftc.teamcode.biobuzz;

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
public final class HiveAim implements AutoCloseable {
    private String targetName = "UNSET";
    // Commissioning means checking and calibrating the assembled robot.
    // These gates control assisted motion/feed. Baseline TeleOp is manual.
    // Follow docs/biobuzz-quick-start.md before changing either setting.
    public static final boolean MOTION_ENABLED = false;
    public static final boolean FEED_ENABLED = false;
    // Initial tuning proposals, NOT measured/qualified robot settings.
    // Proportional gain (Kp): power per degree of aiming error.
    // An error of 10 degrees * 0.015 gives 0.15 power before the maximum power limit.
    public static final double TURN_PROPORTIONAL_GAIN = 0.015;
    public static final double MAXIMUM_TURN_POWER = 0.18;
    public static final double TOLERANCE_DEGREES = 2.0;
    public static final double OFFSET_DEGREES = 0.0;
    // Trial camera-XY window at 1250 ticks/s, NOT qualified: replace after shot trials.
    public static final double MINIMUM_RANGE_INCHES = 30.0;
    public static final double MAXIMUM_RANGE_INCHES = 34.0;
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
    private long alignedSince;

    public HiveAim(HardwareMap map) {
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
            alignedSince = 0;
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
                && AimMath.usable(bearing, range, ageSeconds);
        boolean within = aimRequested && fresh && Math.abs(bearing - OFFSET_DEGREES) <= TOLERANCE_DEGREES;
        if (!within) {
            alignedSince = 0;
        } else if (alignedSince == 0) {
            alignedSince = now;
        }
        aligned = within && (now - alignedSince) / 1e9 >= 0.3;
    }

    public double turn() {
        if (!MOTION_ENABLED || !fresh) {
            return 0;
        }
        return AimMath.turn(bearing, OFFSET_DEGREES,
                TURN_PROPORTIONAL_GAIN, MAXIMUM_TURN_POWER);
    }

    /** The caller must also check launcher speed and, in TeleOp, the driver buttons. */
    public boolean mayFeed() {
        return MOTION_ENABLED && FEED_ENABLED && aligned
                && AimMath.inWindow(range, MINIMUM_RANGE_INCHES, MAXIMUM_RANGE_INCHES);
    }

    @Override
    public void close() {
        camera.close();
    }
}
