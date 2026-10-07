package org.firstinspires.ftc.teamcode.biobuzz.programs;

import org.firstinspires.ftc.teamcode.biobuzz.settings.*;
import org.firstinspires.ftc.teamcode.biobuzz.robot.*;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * Meeting 13 exercise: see how the launcher's F and P numbers change the way it holds speed.
 * Hold right bumper to spin. Tap Y for one short shot and watch the speed dip and recover.
 * D-pad up/down changes F. D-pad left/right changes P. Press B to go back to the starting numbers.
 * Nothing here is saved: to keep new numbers, type them into LAUNCHER_P and LAUNCHER_F in StarterRobot.java.
 */
@TeleOp(name = "Tune Launcher PIDF", group = "BIOBUZZ Tuning")
public class TuneLauncherPidf extends OpMode {
    static final double F_STEP = 0.5;

    private final StarterRobot robot = new StarterRobot();
    private final ElapsedTime clock = new ElapsedTime();
    private final Launcher launcher = new Launcher(robot);
    private final double target = StarterRobot.LAUNCHER_SPEED;
    // The P and F the launcher had when this program started. B goes back to these.
    private double startP, startF;
    // One press changes P by a tenth of its starting value.
    private double pStep;
    private double p, f;
    private boolean wasUp, wasDown, wasLeft, wasRight, wasB, wasY, wasSpinning, wasFeeding;
    // Measurements for the worksheet. NaN (Not a Number) means "not measured yet".
    private double spinStarted, spinUpSeconds = Double.NaN;
    private double feedStarted, lowestDuringShot = Double.NaN, recoverSeconds = Double.NaN;
    private boolean timingSpinUp, timingRecovery;

    @Override
    public void init() {
        robot.init(hardwareMap);
        telemetry.addData("Battery", robot.batteryStatus());
        // Read the numbers the Hub is using right now, whichever robot this is.
        PIDFCoefficients current = robot.launcher.getPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER);
        startP = current.p;
        startF = current.f;
        p = startP;
        f = startF;
        pStep = Math.max(0.5, startP / 10);
        telemetry.addData("Starting P / F", "%.1f / %.2f", startP, startF);
        telemetry.addLine("Hold RB to spin. Tap Y to feed. D-pad UP/DOWN = F, LEFT/RIGHT = P, B = reset.");
    }

    @Override
    public void loop() {
        double now = clock.seconds();
        boolean spinning = gamepad1.right_bumper;

        // 1. Change one number per button press, only while the wheel is stopped.
        if (!spinning) {
            if (gamepad1.dpad_up && !wasUp) f += F_STEP;
            if (gamepad1.dpad_down && !wasDown) f = Math.max(0, f - F_STEP);
            if (gamepad1.dpad_right && !wasRight) p += pStep;
            if (gamepad1.dpad_left && !wasLeft) p = Math.max(0, p - pStep);
            if (gamepad1.b && !wasB) {
                p = startP;
                f = startF;
            }
        }
        wasUp = gamepad1.dpad_up;
        wasDown = gamepad1.dpad_down;
        wasLeft = gamepad1.dpad_left;
        wasRight = gamepad1.dpad_right;
        wasB = gamepad1.b;

        // 2. Spin at the robot's normal speed with the numbers on the screen.
        if (spinning && !wasSpinning) {
            robot.setLauncherGains(p, f);
            spinStarted = now;
            spinUpSeconds = Double.NaN;
            timingSpinUp = true;
        }
        wasSpinning = spinning;
        boolean yPressed = gamepad1.y && !wasY;
        wasY = gamepad1.y;
        if (!spinning) {
            launcher.stop();
        } else {
            if (yPressed) {
                launcher.startShot(now, target, ShooterCalibration.FEED_PULSE_SECONDS);
            }
            if (launcher.shooting()) {
                launcher.updateShot(now, true);
            } else {
                robot.setLauncherSpeed(target);
            }
        }

        // 3. Measure: how long to reach speed, how far it dips on a shot, how long to recover.
        double measured = robot.launcher.getVelocity();
        boolean atSpeed = robot.launcherAtSpeed(target);
        if (timingSpinUp && atSpeed) {
            spinUpSeconds = now - spinStarted;
            timingSpinUp = false;
        }
        // "feeding" covers the whole shot, from the tap of Y until the feeder has finished.
        boolean feeding = launcher.shooting();
        if (feeding && !wasFeeding) {
            feedStarted = now;
            lowestDuringShot = measured;
            recoverSeconds = Double.NaN;
            timingRecovery = true;
        }
        wasFeeding = feeding;
        if (timingRecovery) {
            lowestDuringShot = Math.min(lowestDuringShot, measured);
            // Recovered = back at speed after the shot has finished.
            if (!feeding && atSpeed) {
                recoverSeconds = now - feedStarted;
                timingRecovery = false;
            }
        }
        if (!spinning) {
            timingSpinUp = false;
            timingRecovery = false;
        }

        // 4. Show the numbers to write on the worksheet.
        telemetry.addData("P / F", "%.1f / %.2f", p, f);
        telemetry.addData("Speed target / measured", "%.0f / %.0f", target, measured);
        telemetry.addData("Seconds to reach speed", "%.2f", spinUpSeconds);
        telemetry.addData("Lowest speed during shot", "%.0f", lowestDuringShot);
        telemetry.addData("Seconds to recover after shot", "%.2f", recoverSeconds);
        telemetry.addData("At speed", atSpeed);
    }

    @Override
    public void stop() {
        robot.stop();
    }
}
