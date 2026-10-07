package org.firstinspires.ftc.teamcode.biobuzz.programs;

import org.firstinspires.ftc.teamcode.biobuzz.robot.*;
import org.firstinspires.ftc.teamcode.biobuzz.settings.*;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * Meeting 5 exercise: find the launcher speed that hits the target from each distance.
 * D-pad up/down changes the requested speed. Hold right bumper to spin, tap Y for one short shot.
 * Write the speed, the distance, and what happened on the worksheet. One change at a time.
 */
@TeleOp(name = "Tune Shooter", group = "BIOBUZZ Tuning")
public class TuneShooter extends OpMode {
    private final StarterRobot robot = new StarterRobot();
    private final TargetSelection selection = new TargetSelection();
    private final ElapsedTime clock = new ElapsedTime();
    private final Launcher launcher = new Launcher(robot);
    private TagCamera aim;
    private double speed = ShooterCalibration.START_SPEED;
    private boolean wasUp;
    private boolean wasDown;
    private boolean wasY;

    @Override
    public void init() {
        robot.init(hardwareMap);
        if (ShooterCalibration.USE_CAMERA) {
            aim = new TagCamera(hardwareMap); // A missing "Webcam 1" shows as an error here.
        }
    }

    @Override
    public void init_loop() {
        telemetry.addData("Battery", robot.batteryStatus());
        telemetry.addLine("INIT: D-pad LEFT/RIGHT = red/blue; UP/DOWN = SCORING (far side) / AUDIENCE cell.");
        telemetry.addLine("Pick the tags that are printed on your target.");
        if (aim != null) {
            selection.update(gamepad1, true);
            aim.setTarget(selection.targetName());
            aim.update(false);
            telemetry.addData("Target", selection.targetName());
            telemetry.addData("Tag seen", aim.fresh);
            telemetry.addData("Camera range inches / bearing degrees", "%.1f / %.1f", aim.range, aim.bearing);
        } else {
            telemetry.addLine("Camera is off (USE_CAMERA = false). Aim by hand; use the tape measure.");
        }
        telemetry.addLine("START: UP/DOWN changes speed. Hold RB to spin. Tap Y to feed. Hold A to aim.");
    }

    @Override
    public void start() {
        // A D-pad button still held from INIT must be released before it changes the speed.
        wasUp = gamepad1.dpad_up;
        wasDown = gamepad1.dpad_down;
        clock.reset();
    }

    @Override
    public void loop() {
        // 1. One new D-pad press changes the requested speed by one step.
        if (gamepad1.dpad_up && !wasUp) {
            speed = Math.min(ShooterCalibration.MAXIMUM_SPEED, speed + ShooterCalibration.SPEED_STEP);
        }
        if (gamepad1.dpad_down && !wasDown) {
            speed = Math.max(ShooterCalibration.SPEED_STEP, speed - ShooterCalibration.SPEED_STEP);
        }
        wasUp = gamepad1.dpad_up;
        wasDown = gamepad1.dpad_down;

        // 2. Drive slowly with the sticks, or hold A to turn toward the selected tag.
        //    The robot holds still while the launcher is spinning.
        boolean spinning = gamepad1.right_bumper;
        if (aim != null) {
            aim.update(gamepad1.a);
        }
        if (spinning) {
            robot.arcadeDrive(0, 0);
        } else if (gamepad1.a && aim != null) {
            robot.arcadeDrive(0, aim.turn());
        } else {
            robot.arcadeDrive(-gamepad1.left_stick_y * Drive.DRIVE_POWER,
                    gamepad1.right_stick_x * Drive.TURN_POWER);
        }

        // 3. Hold right bumper to spin the launcher at the requested speed.
        // 4. Tap Y for one short shot. The feeder waits until the wheel is at speed.
        boolean yPressed = gamepad1.y && !wasY;
        wasY = gamepad1.y;
        if (!spinning) {
            launcher.stop();
        } else {
            if (yPressed) {
                launcher.startShot(clock.seconds(), speed, ShooterCalibration.FEED_PULSE_SECONDS);
            }
            if (launcher.shooting()) {
                launcher.updateShot(clock.seconds(), true);
            } else {
                robot.setLauncherSpeed(speed);
            }
        }
        double measured = robot.launcher.getVelocity();

        // 5. Show the numbers to write on the worksheet.
        telemetry.addData("Speed requested / measured (ticks per second)", "%.0f / %.0f", speed, measured);
        telemetry.addData("At speed", robot.launcherAtSpeed(speed));
        if (aim != null) {
            telemetry.addData("Tag seen / aimed", "%s / %s", aim.fresh, aim.aligned);
            telemetry.addData("Camera range inches / bearing degrees", "%.1f / %.1f", aim.range, aim.bearing);
        } else {
            telemetry.addLine("Camera off: write the tape distance.");
        }
    }

    @Override
    public void stop() {
        try {
            robot.stop();
        } finally {
            if (aim != null) {
                aim.close();
            }
        }
    }
}
