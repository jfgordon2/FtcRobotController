package org.firstinspires.ftc.teamcode.biobuzz.programs;

import org.firstinspires.ftc.teamcode.biobuzz.robot.*;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * Student exercise: select ONE empty-robot drive or turn during INIT, then measure it.
 * start() issues the command once; loop() checks progress without blocking the SDK (software development kit).
 * StarterRobot sets up hardware; Drive controls the movement. No camera is needed.
 */
@Autonomous(name = "Tune Drive", group = "BIOBUZZ Tuning")
public class TuneDrive extends OpMode {
    private final StarterRobot robot = new StarterRobot();
    private Drive drive;
    private Heading.Hub heading;
    private final ElapsedTime timer = new ElapsedTime();
    private boolean chosen;
    private boolean turn;
    private boolean running;
    private boolean commandIssued;
    private double amount;
    private int initialLeft;
    private int initialRight;
    private double finishedSeconds;
    private String result = "Choose a command during INIT";

    @Override
    public void init() {
        robot.init(hardwareMap);
        heading = new Heading.Hub(hardwareMap);
        drive = new Drive(robot, heading);
    }

    @Override
    public void init_loop() {
        telemetry.addData("Battery", robot.batteryStatus());
        telemetry.addData("Hub logo / USB / confirmed", "%s / %s / %s",
                Heading.LOGO, Heading.USB, Heading.MOUNTING_CONFIRMED);
        telemetry.addData("Heading sensor (IMU), clockwise degrees", heading.degrees());
        telemetry.addData("Drive ticks per inch", "%.2f", Drive.TICKS_PER_INCH);
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
        if (!chosen || !StarterRobot.MOTION_ENABLED) {
            result = "Select a command and enable calibrated motion";
            return;
        }
        if (turn) {
            drive.turnDegrees(amount, timer.seconds());
        } else {
            drive.driveInches(amount, timer.seconds());
        }
        commandIssued = true;
        running = true;
    }

    @Override
    public void loop() {
        drive.observePose();
        if (running) {
            Drive.Result state = drive.update(timer.seconds());
            result = state + " " + drive.reason();
            running = state == Drive.Result.RUNNING;
            if (!running) {
                // Remember how long the move took. Routes need every move to finish within 4 seconds.
                finishedSeconds = timer.seconds();
            }
        } else {
            drive.stop();
        }
        telemetry.addData("Result", result);
        telemetry.addData("Seconds", "%.1f", running ? timer.seconds() : finishedSeconds);
        if (chosen) {
            telemetry.addData("Command", "%s %.1f", turn ? "Turn degrees" : "Drive inches", amount);
        }
        telemetry.addData("Drive ticks per inch", "%.2f", Drive.TICKS_PER_INCH);
        int leftTicks = robot.leftDrive.getCurrentPosition();
        int rightTicks = robot.rightDrive.getCurrentPosition();
        telemetry.addData("Tick change left / right", "%d / %d", leftTicks - initialLeft,
                rightTicks - initialRight);
        if (commandIssued && !turn) {
            telemetry.addData("Target tick change left / right", "%d / %d",
                    drive.leftTargetTicks() - initialLeft, drive.rightTargetTicks() - initialRight);
            telemetry.addData("Remaining ticks left / right", "%d / %d",
                    drive.leftTargetTicks() - leftTicks, drive.rightTargetTicks() - rightTicks);
        }
        telemetry.addData("Local X forward / Y right / heading", "%.1f / %.1f / %.1f",
                drive.xInches(), drive.yInches(), drive.headingDegrees());
        telemetry.addLine("After motion ends, record Result, targets, and counts before Stop; then measure distance/angle.");
    }

    @Override
    public void stop() {
        if (drive != null) {
            drive.stop();
        }
        robot.stop();
    }
}
