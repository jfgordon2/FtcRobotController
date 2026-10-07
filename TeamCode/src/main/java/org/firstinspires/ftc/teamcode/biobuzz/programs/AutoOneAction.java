package org.firstinspires.ftc.teamcode.biobuzz.programs;

import org.firstinspires.ftc.teamcode.biobuzz.robot.*;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * Meeting 9: the smallest useful autonomous. The robot does ONE thing by itself: drive.
 * start() gives the command once. loop() checks on it until it is DONE or has a FAULT.
 * Moving off the field wall in AUTO earns LEAVE, so this short program can score.
 *
 * AutoFollowRoute (the full autonomous) is this same program with a list of steps:
 * the same fields, the same checks in start(), and the same MOVE / DONE / FAULT states.
 */
@Autonomous(name = "Autonomous One Action", group = "BIOBUZZ")
public class AutoOneAction extends OpMode {
    // The one number to change. Positive drives forward; negative drives backward.
    static final double LEAVE_INCHES = 12;

    // The program is always in exactly one of these states.
    private enum State { MOVE, DONE, FAULT }

    private final StarterRobot robot = new StarterRobot();
    private final ElapsedTime timer = new ElapsedTime();
    private Heading.Hub heading;
    private Drive drive;
    private State state = State.FAULT;
    private String note = "Waiting for Start";

    @Override
    public void init() {
        robot.init(hardwareMap);
        telemetry.addData("Battery", robot.batteryStatus());
        heading = new Heading.Hub(hardwareMap);
        drive = new Drive(robot, heading);
        telemetry.addData("Plan", "Drive %.0f inches, then stop", LEAVE_INCHES);
    }

    @Override
    public void start() {
        // These two checks are the mentor's setup switches. Without them the robot stays still.
        if (!heading.ready()) {
            fail("Not moving: Hub direction is not confirmed in Heading.java");
            return;
        }
        if (!StarterRobot.MOTION_ENABLED) {
            fail("Not moving: MOTION_ENABLED is false in StarterRobot.java");
            return;
        }
        heading.resetAtStart();
        drive.resetPoseAtStart();
        timer.reset();
        // Give the command ONCE. It does not wait here for the robot to arrive.
        drive.driveInches(LEAVE_INCHES, timer.seconds());
        state = State.MOVE;
        note = "";
    }

    @Override
    public void loop() {
        // Every loop: do the work for the state we are in. Changing state decides the NEXT loop.
        switch (state) {
            case MOVE:
                // Ask the drive how the command is going.
                Drive.Result result = drive.update(timer.seconds());
                if (result == Drive.Result.DONE) {
                    state = State.DONE;
                } else if (result == Drive.Result.FAULT) {
                    fail(drive.reason());
                }
                break;
            default:
                // DONE or FAULT: nothing left to do. The robot stays stopped.
                break;
        }
        telemetry.addData("State", state);
        telemetry.addData("Note", note);
        telemetry.addData("Seconds", "%.1f", timer.seconds());
        telemetry.addData("Ticks left / right", "%d / %d",
                robot.leftDrive.getCurrentPosition(), robot.rightDrive.getCurrentPosition());
    }

    /** Stop everything and remember why. */
    private void fail(String message) {
        state = State.FAULT;
        note = message;
        robot.stop();
    }

    @Override
    public void stop() {
        robot.stop();
    }
}
