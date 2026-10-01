package org.firstinspires.ftc.teamcode.biobuzz;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * Advanced empty-robot exercise: approach a selected tag, then reverse those movements.
 * HiveAim supplies camera readings; TagDrive owns approach/return; StarterDrive moves motors.
 * This OpMode has no shooting or parking leg. Read drive calibration first.
 */
@Disabled
@Autonomous(name = "BIOBUZZ Measure Tag Approach", group = "BIOBUZZ Calibration")
public class StarterBotTagCalibration extends OpMode {
    private final StarterRobot robot = new StarterRobot();
    private StarterDrive drive;
    private TagDrive tagDrive;
    private HubHeading heading;
    private HiveAim aim;
    private final TargetSelection selection = new TargetSelection();
    private final ElapsedTime timer = new ElapsedTime();
    private enum State {
        POSITION, RETURN, STOPPED
    }

    private State state = State.STOPPED;
    private String reason = "Select a target; empty robot only";

    @Override
    public void init() {
        robot.init(hardwareMap);
        heading = new HubHeading(hardwareMap);
        drive = new StarterDrive(robot.leftDrive, robot.rightDrive, heading);
        tagDrive = new TagDrive(drive);
        aim = new HiveAim(hardwareMap);
    }

    @Override
    public void init_loop() {
        telemetry.addData("Hub logo / USB / confirmed", "%s / %s / %s",
                HubHeading.LOGO, HubHeading.USB, HubHeading.MOUNTING_CONFIRMED);
        telemetry.addData("Heading sensor (IMU), clockwise degrees", heading.degrees());
        selection.update(gamepad1.dpad_left, gamepad1.dpad_right, gamepad1.dpad_up, gamepad1.dpad_down, true);
        aim.setTarget(selection.clusterName());
        aim.update(false);
        telemetry.addLine("LEFT/RIGHT: red/blue; UP/DOWN: far/audience. Empty robot approaches, then returns.");
        show();
    }

    @Override
    public void start() {
        if (!heading.ready()) {
            reason = "Heading sensor (IMU) mounting unconfirmed or heading unavailable";
            return;
        }
        heading.resetAtStart();
        drive.resetPoseAtStart();
        selection.lockAlliance();
        timer.reset();
        if (!selection.ready() || !HiveAim.MOTION_ENABLED) {
            reason = "Select target and enable motion first";
            return;
        }
        tagDrive.startApproach(selection.clusterName(), StarterBotPositionAuto.TAG_RANGE_INCHES, HiveAim.OFFSET_DEGREES, 0);
        state = State.POSITION;
    }

    @Override
    public void loop() {
        drive.observePose();
        telemetry.addData("Local X forward / Y right / heading", "%.1f / %.1f / %.1f",
                drive.xInches(), drive.yInches(), drive.headingDegrees());
        aim.update(true);
        if (timer.seconds() >= 12) {
            tagDrive.stop();
            state = State.STOPPED;
            reason = "Calibration deadline";
        }
        if (state == State.POSITION) {
            StarterDrive.Result result = tagDrive.updateApproach(timer.seconds(),
                    aim.targetName(), aim.fresh, aim.range, aim.bearing);
            if (result != StarterDrive.Result.RUNNING) {
                reason = result + " " + tagDrive.reason();
                if (tagDrive.canReturn()) {
                    tagDrive.startReturn(timer.seconds());
                    state = State.RETURN;
                } else {
                    tagDrive.stop();
                    state = State.STOPPED;
                }
            }
        } else if (state == State.RETURN) {
            StarterDrive.Result result = tagDrive.updateReturn(timer.seconds());
            if (result != StarterDrive.Result.RUNNING) {
                if (result == StarterDrive.Result.FAULT) {
                    reason = "Return failed: " + tagDrive.reason();
                }
                state = State.STOPPED;
            }
        } else {
            tagDrive.stop();
        }
        show();
    }

    private void show() {
        telemetry.addData("State / reason", "%s / %s", state, reason);
        telemetry.addData("Target / fresh", "%s / %s", selection.clusterName(), aim.fresh);
        telemetry.addData("Range goal / actual inches", "%.1f / %.1f", StarterBotPositionAuto.TAG_RANGE_INCHES, aim.range);
        telemetry.addData("Bearing goal / actual degrees", "%.1f / %.1f", HiveAim.OFFSET_DEGREES, aim.bearing);
        telemetry.addData("Tag phase / travel inches", "%s / %.2f", tagDrive.stage(), tagDrive.translationInches());
    }

    @Override
    public void stop() {
        try {
            if (tagDrive != null) {
                tagDrive.stop();
            }
            robot.stop();
        } finally {
            if (aim != null) {
                aim.close();
            }
        }
    }
}
