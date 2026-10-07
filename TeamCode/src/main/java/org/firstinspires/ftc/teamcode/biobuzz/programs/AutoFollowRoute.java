package org.firstinspires.ftc.teamcode.biobuzz.programs;

import org.firstinspires.ftc.teamcode.biobuzz.robot.*;
import org.firstinspires.ftc.teamcode.biobuzz.settings.*;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * The full autonomous. It is AutoOneAction with a LIST of steps instead of one drive.
 * The list is a route in Routes.java. In INIT the drive team chooses which route.
 * start() begins the first step. loop() checks on the current step and, when it is DONE,
 * begins the next one.
 *
 * A drive or turn step uses the MOVE state, exactly as in Meeting 9.
 * An aimAndShoot() step uses AIM (turn to face the tags), then SHOOT, then a MOVE that turns
 * the robot back to the way it was facing, so the rest of the route still lines up.
 * If the tags cannot be seen, or the shot is not allowed, the robot skips the shot and carries on.
 */
@Autonomous(name = "Autonomous Route", group = "BIOBUZZ")
public class AutoFollowRoute extends OpMode {
    // Stop everything at this time, whatever is happening. AUTO lasts 30 seconds.
    static final double TOTAL_TIMEOUT_SECONDS = 28.0;
    // How long an aimAndShoot() step may spend turning to face the tags before it gives up.
    static final double AIM_SECONDS = 3.0;

    // The program is always in exactly one of these states.
    private enum State { MOVE, AIM, SHOOT, DONE, FAULT }

    private final StarterRobot robot = new StarterRobot();
    private final ElapsedTime timer = new ElapsedTime();
    private Heading.Hub heading;
    private Drive drive;
    private Launcher launcher;
    private TagCamera aim;
    private State state = State.FAULT;
    private String note = "Choose a route in INIT";

    // What the drive team chose in INIT.
    private final TargetSelection selection = new TargetSelection();
    private boolean startB;
    private boolean startChosen;
    private boolean dryRun = true;
    private boolean wasX;

    // Where we are in the route.
    private Routes.Step[] route;
    private int stepNumber;
    private double aimStartedSeconds;
    private double headingBeforeAim;

    @Override
    public void init() {
        robot.init(hardwareMap);
        heading = new Heading.Hub(hardwareMap);
        drive = new Drive(robot, heading);
        launcher = new Launcher(robot);
        aim = new TagCamera(hardwareMap);
    }

    @Override
    public void init_loop() {
        telemetry.addData("Battery", robot.batteryStatus());
        chooseRoute();
        showChoices();
    }

    @Override
    public void start() {
        // The same checks as Meeting 9, plus one: a route must have been chosen.
        if (!heading.ready()) {
            fail("Not moving: Hub direction is not confirmed in Heading.java");
            return;
        }
        if (!StarterRobot.MOTION_ENABLED) {
            fail("Not moving: MOTION_ENABLED is false in StarterRobot.java");
            return;
        }
        if (route == null || !startChosen) {
            fail("Not moving: choose alliance, cell and start A or B in INIT");
            return;
        }
        heading.resetAtStart();
        drive.resetPoseAtStart();
        timer.reset();
        note = "";
        stepNumber = 0;
        beginStep();
    }

    @Override
    public void loop() {
        double now = timer.seconds();
        if (state != State.DONE && state != State.FAULT && now >= TOTAL_TIMEOUT_SECONDS) {
            fail("Out of time at 28 seconds");
        }
        // Every loop: do the work for the state we are in. Changing state decides the NEXT loop.
        switch (state) {
            case MOVE:
                // Ask the drive how the command is going, exactly as in Meeting 9.
                Drive.Result result = drive.update(now);
                if (result == Drive.Result.DONE) {
                    stepNumber++;
                    beginStep();
                } else if (result == Drive.Result.FAULT) {
                    fail(drive.reason());
                }
                break;
            case AIM:
                aim.update(true);
                robot.arcadeDrive(0, aim.turn());
                if (aim.aligned) {
                    robot.arcadeDrive(0, 0);
                    String whyNot = whyNotShoot();
                    if (whyNot.isEmpty()) {
                        // Look up the launcher speed for the range the camera reads right now.
                        launcher.startShot(now, ShooterCalibration.speedFor(aim.range), ShotTimer.FEED_SECONDS);
                        state = State.SHOOT;
                    } else {
                        turnBack(whyNot);
                    }
                } else if (now - aimStartedSeconds >= AIM_SECONDS) {
                    turnBack("Shot skipped: tags not lined up in time");
                }
                break;
            case SHOOT:
                aim.update(true);
                ShotTimer.State shot = launcher.updateShot(now, aim.fresh && aim.mayFeed());
                if (shot == ShotTimer.State.DONE) {
                    turnBack("Shot finished");
                } else if (shot == ShotTimer.State.FAULT) {
                    turnBack("Shot stopped: " + launcher.reason());
                }
                break;
            default:
                // DONE or FAULT: nothing left to do. The robot stays stopped.
                break;
        }
        telemetry.addData("State", state);
        telemetry.addData("Note", note);
        telemetry.addData("Step", "%d of %d: %s", Math.min(stepNumber + 1, route == null ? 0 : route.length),
                route == null ? 0 : route.length,
                route != null && stepNumber < route.length ? route[stepNumber] : "-");
        telemetry.addData("Seconds", "%.1f", now);
        telemetry.addData("Ticks left / right", "%d / %d",
                robot.leftDrive.getCurrentPosition(), robot.rightDrive.getCurrentPosition());
        telemetry.addData("Tag range inches / bearing degrees", "%.1f / %.1f", aim.range, aim.bearing);
        telemetry.addData("Launcher ticks/second", launcher.speed());
    }

    /** Give the command for the current step ONCE. When the list is finished, we are DONE. */
    private void beginStep() {
        if (stepNumber >= route.length) {
            state = State.DONE;
            robot.stop();
            return;
        }
        Routes.Step step = route[stepNumber];
        double now = timer.seconds();
        switch (step.kind) {
            case DRIVE:
                drive.driveInches(step.amount, now);
                state = State.MOVE;
                break;
            case TURN:
                drive.turnDegrees(step.amount, now);
                state = State.MOVE;
                break;
            default: // AIM_AND_SHOOT
                headingBeforeAim = heading.degrees();
                aimStartedSeconds = now;
                aim.setTarget(selection.targetName());
                state = State.AIM;
                break;
        }
    }

    /** Empty text means the robot may shoot. Otherwise the text says why it will not. */
    private String whyNotShoot() {
        if (dryRun) {
            return "Dry run: aimed, no shot";
        }
        if (!StarterRobot.FEED_ENABLED) {
            return "Shot skipped: FEED_ENABLED is false in StarterRobot.java";
        }
        if (!ShooterCalibration.covers(aim.range)) {
            return String.format("Shot skipped: speed table has no rows around %.0f inches", aim.range);
        }
        return "";
    }

    /** End an aimAndShoot() step: stop the launcher and turn back to where the robot was facing. */
    private void turnBack(String message) {
        note = message;
        launcher.stop();
        drive.turnToHeading(headingBeforeAim, timer.seconds());
        // This turn is an ordinary MOVE. When it is DONE, loop() begins the next step in the list.
        state = State.MOVE;
    }

    /** Stop everything and remember why. */
    private void fail(String message) {
        state = State.FAULT;
        note = message;
        if (launcher != null) {
            launcher.stop();
        }
        robot.stop();
    }

    @Override
    public void stop() {
        try {
            if (launcher != null) {
                launcher.stop();
            }
            robot.stop();
        } finally {
            if (aim != null) {
                aim.close();
            }
        }
    }

    // ---- The INIT menu. Nothing below here moves the robot. ----

    /** Read the gamepad and work out which route the drive team wants. */
    private void chooseRoute() {
        selection.update(gamepad1, true);
        if (gamepad1.a && !gamepad1.b) {
            startB = false;
            startChosen = true;
        }
        if (gamepad1.b && !gamepad1.a) {
            startB = true;
            startChosen = true;
        }
        if (gamepad1.x && !wasX) {
            dryRun = !dryRun;
        }
        wasX = gamepad1.x;
        route = Routes.select(selection.targetName(), startB);
        aim.setTarget(selection.targetName());
        aim.update(false);
    }

    private void showChoices() {
        telemetry.addLine("D-pad LEFT/RIGHT: red/blue.  UP/DOWN: SCORING (far side) / AUDIENCE cell.");
        telemetry.addLine("A: start A.  B: start B.  X: dry run on/off.");
        telemetry.addData("Alliance / cell", "%s / %s", selection.allianceLabel(), selection.cellLabel());
        telemetry.addData("Start", !startChosen ? "NOT SELECTED" : startB ? "B" : "A");
        telemetry.addData("Shooting", dryRun ? "DRY RUN: aims but does not shoot" : "ON");
        telemetry.addData("Route", route == null ? "Choose alliance and cell" : java.util.Arrays.toString(route));
        telemetry.addData("Tag range inches / bearing degrees now", "%.1f / %.1f", aim.range, aim.bearing);
        telemetry.addData("Speed table covers this range", ShooterCalibration.covers(aim.range));
        telemetry.addData("Speed table problem", ShooterCalibration.tableProblem());
        telemetry.addData("Heading (degrees, clockwise)", "%.1f", heading.degrees());
    }
}
