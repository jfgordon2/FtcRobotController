package org.firstinspires.ftc.teamcode.biobuzz.programs;

import org.firstinspires.ftc.teamcode.biobuzz.settings.*;
import org.firstinspires.ftc.teamcode.biobuzz.robot.*;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * Adds held camera aiming to manual driving. Read the baseline TeleOp first.
 * Each loop reads the selected target, chooses drive outputs, then chooses feed outputs.
 * TagCamera supplies measurements and permission; this OpMode commands StarterRobot.
 */
@TeleOp(name = "TeleOp with Aiming", group = "BIOBUZZ")
public class TeleOpAim extends OpMode {
    private final StarterRobot robot = new StarterRobot();
    private TagCamera aim;
    private final TargetSelection selection = new TargetSelection();

    @Override
    public void init() {
        robot.init(hardwareMap);
        aim = new TagCamera(hardwareMap);
    }

    @Override
    public void init_loop() {
        telemetry.addData("Battery", robot.batteryStatus());
        selection.update(gamepad1, true);
        aim.setTarget(selection.targetName());
        aim.update(false);
        showSelection();
        telemetry.addLine("D-pad LEFT: red | RIGHT: blue | UP: SCORING (far side) | DOWN: AUDIENCE");
        telemetry.addLine(selection.ready() ? "Selection ready. Confirm the cell is upward-facing."
                : "Select BOTH alliance and cell before Start. No assisted motion without selection.");
    }

    @Override
    public void start() {
        selection.lockAlliance();
        aim.setTarget(selection.targetName());
    }

    private void showSelection() {
        telemetry.addData("Alliance", selection.allianceLabel());
        telemetry.addData("Cell", selection.cellLabel());
        telemetry.addData("Target", selection.targetName());
        telemetry.addData("Target visible / fresh", aim.fresh);
    }

    @Override
    public void loop() {
        // Change cells only with aim and launch released. A blocked press must be released/repressed.
        selection.update(gamepad1, !gamepad1.a && !gamepad1.right_bumper);
        aim.setTarget(selection.targetName());
        aim.update(gamepad1.a);
        updateDriving();
        updateLauncherAndIntake();
        showSelection();
        telemetry.addLine("Alliance locked. Release A/right bumper, then D-pad UP/DOWN to choose next cell.");
        if (!selection.ready()) {
            telemetry.addLine("No target selected: Stop and INIT to select alliance/cell.");
        }
        telemetry.addData("Fresh / aligned", "%s / %s", aim.fresh, aim.aligned);
        telemetry.addData("Bearing degrees / camera XY range inches", "%.1f / %.1f", aim.bearing, aim.range);
        telemetry.addData("Frame age seconds", aim.ageSeconds);
        telemetry.addData("Motion / feed enabled", "%s / %s", StarterRobot.MOTION_ENABLED, StarterRobot.FEED_ENABLED);
        telemetry.addLine("A: stationary aim. right bumper: spin. A + right bumper: feed only when all gates pass.");
    }

    private void updateDriving() {
        // Holding A requests stationary aim. A missing target gives a zero turn request.
        if (gamepad1.a) {
            robot.arcadeDrive(0, aim.turn());
        } else {
            robot.arcadeDrive(-gamepad1.left_stick_y, gamepad1.right_stick_x);
        }
        telemetry.addData("Motors", "left (%.2f), right (%.2f)",
                robot.leftDrive.getPower(), robot.rightDrive.getPower());
    }

    private void updateLauncherAndIntake() {
        // While aiming, use the speed our Meeting 5 table gives for this camera range.
        // Without a table row for this range the wheel spins at its normal speed but will not feed.
        double wantedSpeed = StarterRobot.LAUNCHER_SPEED;
        double tableSpeed = ShooterCalibration.speedFor(aim.range);
        if (gamepad1.a && Double.isFinite(tableSpeed)) {
            wantedSpeed = tableSpeed;
        }
        if (gamepad1.right_bumper) {
            robot.setLauncherSpeed(wantedSpeed);
        } else {
            robot.setLauncherSpeed(0);
        }
        double speed = robot.launcher.getVelocity();
        boolean speedReady = robot.launcherAtSpeed(wantedSpeed);
        // Every condition must pass: driver request, camera checks, and wheel speed.
        boolean driverRequestsFeed = gamepad1.a && gamepad1.right_bumper;
        boolean feed = driverRequestsFeed && aim.mayFeed() && speedReady;
        robot.setFeederRunning(feed);
        double intakePower = gamepad1.right_trigger - gamepad1.left_trigger;
        if (gamepad1.a) {
            // During aiming, the intake helps only when feeding is permitted.
            intakePower = 0;
            if (feed) {
                intakePower = 0.5;
            }
        }
        robot.setIntakePower(intakePower);
        telemetry.addData("Feeding", feed);
        telemetry.addData("Launcher ticks/second wanted / measured", "%.0f / %.0f", wantedSpeed, speed);
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
