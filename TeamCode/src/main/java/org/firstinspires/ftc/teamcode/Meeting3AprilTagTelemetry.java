package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;
import java.util.List;

@TeleOp(name = "M3 AprilTag Telemetry", group = "BIOBUZZ")
public class Meeting3AprilTagTelemetry extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        // SETUP: the configured webcam name must match exactly.
        AprilTagProcessor tags = AprilTagProcessor.easyCreateWithDefaults();
        VisionPortal camera = VisionPortal.easyCreateWithDefaults(
                hardwareMap.get(WebcamName.class, "Webcam 1"), tags);

        // The finally block releases the camera even if we Stop during INIT.
        try {
            telemetry.addLine("Camera opening. Press Start when ready.");
            telemetry.update();
            waitForStart();

            while (opModeIsActive()) {
                List<AprilTagDetection> detections = tags.getDetections();
                telemetry.addData("Camera", camera.getCameraState());
                telemetry.addData("Detections", detections.size());
                if (detections.isEmpty()) {
                    telemetry.addLine("No tags visible yet.");
                }

                // READ: single tags and clusters share the same pose fields.
                for (AprilTagDetection detection : detections) {
                    if (detection.ftcPose != null) {
                        // SHOW: default units are inches and degrees.
                        telemetry.addLine("--- Target pose ---");
                        // Range: camera-to-target distance in the X-Y plane (inches).
                        // It ignores height (Z): sqrt(X*X + Y*Y).
                        telemetry.addData("Range (in)", "%.1f", detection.ftcPose.range);
                        // Bearing: left/right angle (degrees); left +, right -, centered 0.
                        telemetry.addData("Bearing (deg)", "%.1f", detection.ftcPose.bearing);
                        // Elevation: up/down angle (degrees); above +, below -, level 0.
                        telemetry.addData("Elevation (deg)", "%.1f", detection.ftcPose.elevation);
                        telemetry.addData("X / Y / Z (in)", "%.1f / %.1f / %.1f",
                                detection.ftcPose.x, detection.ftcPose.y, detection.ftcPose.z);
                    } else {
                        telemetry.addLine("Tag seen; no distance. Check tag library/size.");
                    }
                }
                telemetry.update();
                sleep(50);
            }
        } finally {
            camera.close();
        }
    }
}
