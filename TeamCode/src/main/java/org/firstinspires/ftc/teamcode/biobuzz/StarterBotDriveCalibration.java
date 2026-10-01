package org.firstinspires.ftc.teamcode.biobuzz;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * Student exercise: select ONE empty-robot drive or turn during INIT, then measure it.
 * start() issues the command once; loop() checks progress without blocking the SDK (software development kit).
 * StarterRobot sets up hardware; StarterDrive controls the movement. No camera is needed.
 */
@Disabled
@Autonomous(name = "BIOBUZZ Measure Drive or Turn", group = "BIOBUZZ Calibration")
public class StarterBotDriveCalibration extends OpMode {
    private final StarterRobot robot = new StarterRobot();
    private StarterDrive drive;
    private HubHeading heading;
    private final ElapsedTime timer = new ElapsedTime();
    private boolean chosen;
    private boolean turn;
    private boolean running;
    private double amount;
    private int initialLeft;
    private int initialRight;
    private String result = "Choose a command during INIT";

    @Override
    public void init() {
        robot.init(hardwareMap);
        heading = new HubHeading(hardwareMap);
        drive = new StarterDrive(robot.leftDrive, robot.rightDrive, heading);
    }

    @Override
    public void init_loop() {
        telemetry.addData("Hub logo / USB / confirmed", "%s / %s / %s",
                HubHeading.LOGO, HubHeading.USB, HubHeading.MOUNTING_CONFIRMED);
        telemetry.addData("Heading sensor (IMU), clockwise degrees", heading.degrees());
        if (gamepad1.dpad_up && !gamepad1.dpad_down && !gamepad1.dpad_left && !gamepad1.dpad_right) {
            chooseDriveInches(24);
        }
        if (gamepad1.dpad_down && !gamepad1.dpad_up && !gamepad1.dpad_left && !gamepad1.dpad_right) {
            chooseDriveInches(-24);
        }
        if (gamepad1.dpad_left && !gamepad1.dpad_right && !gamepad1.dpad_up && !gamepad1.dpad_down) {
            chooseTurnDegrees(-90);
        }
        if (gamepad1.dpad_right && !gamepad1.dpad_left && !gamepad1.dpad_up && !gamepad1.dpad_down) {
            chooseTurnDegrees(90);
        }
        telemetry.addLine("EMPTY robot: UP/DOWN = +/-24 inches; LEFT/RIGHT = -/+90 degrees");
        String selected = "NONE";
        if (chosen) {
            if (turn) {
                selected = "Turn degrees " + amount;
            } else {
                selected = "Drive inches " + amount;
            }
        }
        telemetry.addData("Selected", selected);
    }

    private void chooseDriveInches(double inches) {
        chosen = true;
        turn = false;
        amount = inches;
    }

    private void chooseTurnDegrees(double degrees) {
        chosen = true;
        turn = true;
        amount = degrees;
    }

    @Override
    public void start() {
        if (!heading.ready()) {
            result = "Heading sensor (IMU) mounting unconfirmed or heading unavailable";
            return;
        }
        heading.resetAtStart();
        drive.resetPoseAtStart();
        initialLeft = robot.leftDrive.getCurrentPosition();
        initialRight = robot.rightDrive.getCurrentPosition();
        timer.reset();
        if (!chosen || !HiveAim.MOTION_ENABLED) {
            result = "Select a command and enable calibrated motion";
            return;
        }
        if (turn) {
            drive.turnDegrees(amount, timer.seconds());
        } else {
            drive.driveInches(amount, timer.seconds());
        }
        running = true;
    }

    @Override
    public void loop() {
        drive.observePose();
        telemetry.addData("Local X forward / Y right / heading", "%.1f / %.1f / %.1f",
                drive.xInches(), drive.yInches(), drive.headingDegrees());
        if (running) {
            StarterDrive.Result state = drive.update(timer.seconds());
            result = state + " " + drive.reason();
            running = state == StarterDrive.Result.RUNNING;
        } else {
            drive.stop();
        }
        telemetry.addData("Result", result);
        telemetry.addData("Tick change left / right", "%d / %d", robot.leftDrive.getCurrentPosition() - initialLeft,
                robot.rightDrive.getCurrentPosition() - initialRight);
        telemetry.addLine("After motion ends, record counts, press Stop, then measure actual distance/angle.");
    }

    @Override
    public void stop() {
        if (drive != null) {
            drive.stop();
        }
        robot.stop();
    }
}
