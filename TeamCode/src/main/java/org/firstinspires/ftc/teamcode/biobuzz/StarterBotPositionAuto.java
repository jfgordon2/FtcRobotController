package org.firstinspires.ftc.teamcode.biobuzz;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * Coordinates the complete autonomous sequence using the reusable subsystems.
 * Select a route in INIT. At Start, lock choices and begin the first action.
 * Each loop advances only the current state: approach, position, shoot, return, or park.
 * Read the smaller calibration OpModes before tracing this full state machine.
 */
@Disabled
@Autonomous(name = "BIOBUZZ Position and Shoot", group = "BIOBUZZ Drafts")
public class StarterBotPositionAuto extends OpMode {
    private final StarterRobot robot = new StarterRobot();
    static final double TOTAL_TIMEOUT_SECONDS = 28.0;
    static final double TAG_RANGE_INCHES = (HiveAim.MINIMUM_RANGE_INCHES + HiveAim.MAXIMUM_RANGE_INCHES) / 2.0;
    private enum State {
        APPROACH, POSITION_TAG, SHOOT, RETURN_FROM_TAG, PARK, DONE, FAULT
    }

    private State state = State.FAULT;
    private final ElapsedTime total = new ElapsedTime();
    private final TargetSelection selection = new TargetSelection();
    private StarterDrive drive;
    private TagDrive tagDrive;
    private HubHeading heading;
    private StarterLauncher mechanisms;
    private HiveAim aim;
    private AutoRoutes.Route route;
    private AutoRoutes.Mode mode = AutoRoutes.Mode.DRY_RUN;
    private boolean startB;
    private boolean startSelected;
    private boolean wasX;
    private int stepIndex;
    private String reason = "Select route during INIT";

    @Override
    public void init() {
        robot.init(hardwareMap);
        heading = new HubHeading(hardwareMap);
        drive = new StarterDrive(robot.leftDrive, robot.rightDrive, heading);
        tagDrive = new TagDrive(drive);
        mechanisms = new StarterLauncher(robot.launcher, robot.intake,
                robot.leftIntakeServo, robot.rightIntakeServo, robot.windmillServo);
        aim = new HiveAim(hardwareMap);
    }

    @Override
    public void init_loop() {
        telemetry.addData("Hub logo / USB / confirmed", "%s / %s / %s",
                HubHeading.LOGO, HubHeading.USB, HubHeading.MOUNTING_CONFIRMED);
        telemetry.addData("Heading sensor (IMU), clockwise degrees", heading.degrees());
        selection.update(gamepad1.dpad_left, gamepad1.dpad_right,
                gamepad1.dpad_up, gamepad1.dpad_down, true);
        if (gamepad1.a && !gamepad1.b) {
            startB = false;
            startSelected = true;
        }
        if (gamepad1.b && !gamepad1.a) {
            startB = true;
            startSelected = true;
        }
        if (gamepad1.x && !wasX) {
            mode = nextMode(mode);
        }
        wasX = gamepad1.x;
        route = AutoRoutes.select(selection.clusterName(), startB);
        aim.setTarget(selection.clusterName());
        aim.update(false);
        telemetry.addLine("D-pad LEFT/RIGHT: red/blue; UP/DOWN: far/audience cell");
        telemetry.addLine("A: start/park A; B: start/park B; X: cycle mode (release between presses)");
        telemetry.addData("Alliance / cell", "%s / %s", selection.allianceLabel(), selection.cellLabel());
        telemetry.addData("Start selected / mode", "%s / %s", startSelected, mode);
        telemetry.addData("Route", route == null ? "Choose alliance and cell" : route.name);
        if (route != null) {
            telemetry.addData("Assumed start", route.startDescription);
            telemetry.addData("Field qualified", route.qualified);
            telemetry.addData("Start tag check", route.startTag.status(aim.fresh, aim.range, aim.bearing));
            telemetry.addData("Start tag measured range / bearing", "%.1f / %.1f", aim.range, aim.bearing);
            telemetry.addData("Tag goal range / bearing", "%.1f inches / %.1f degrees", TAG_RANGE_INCHES, HiveAim.OFFSET_DEGREES);
            telemetry.addData("Approach", java.util.Arrays.toString(route.approach));
            telemetry.addData("Park", java.util.Arrays.toString(route.park));
        }
        telemetry.addLine("DRY_RUN: EMPTY robot, practice route. Other modes require a qualified route card.");
        telemetry.addLine("All choices lock at Start. Place robot at the matching measured start/heading.");
    }

    @Override
    public void start() {
        if (!heading.ready()) {
            fail("Heading sensor (IMU) mounting unconfirmed or heading unavailable");
            return;
        }
        heading.resetAtStart();
        drive.resetPoseAtStart();
        total.reset();
        selection.lockAlliance();
        if (!selection.ready() || !startSelected || route == null) {
            fail("Select alliance, cell, and start A/B in INIT");
            return;
        }
        if (!HiveAim.MOTION_ENABLED) {
            fail("Motion commissioning disabled");
            return;
        }
        if (mode != AutoRoutes.Mode.DRY_RUN && !route.qualified) {
            fail("Measure/qualify this route card first; use empty DRY_RUN for calibration");
            return;
        }
        if (mode == AutoRoutes.Mode.SHOOT_AND_PARK && !HiveAim.FEED_ENABLED) {
            fail("Feeding not qualified/enabled; choose DRY_RUN");
            return;
        }
        aim.setTarget(selection.clusterName());
        aim.update(false);
        if (route.startTag.enabled && !route.startTag.matches(aim.fresh, aim.range, aim.bearing)) {
            fail("Start tag check failed: verify physical marks and unchanged hive state");
            return;
        }
        state = State.APPROACH;
        reason = "";
        stepIndex = 0;
        startStep(route.approach);
    }

    // Keep the mode cycle visible: dry practice, shooting, then parking only.
    private AutoRoutes.Mode nextMode(AutoRoutes.Mode current) {
        switch (current) {
            case DRY_RUN:
                return AutoRoutes.Mode.SHOOT_AND_PARK;
            case SHOOT_AND_PARK:
                return AutoRoutes.Mode.PARK_ONLY;
            default:
                return AutoRoutes.Mode.DRY_RUN;
        }
    }

    private void startStep(AutoRoutes.Step[] steps) {
        if (stepIndex >= steps.length) {
            fail("Route must contain at least one segment");
            return;
        }
        AutoRoutes.Step step = steps[stepIndex];
        if (step.turn) {
            drive.turnDegrees(step.value, total.seconds());
        } else {
            drive.driveInches(step.value, total.seconds());
        }
    }

    private boolean updatePath(AutoRoutes.Step[] steps) {
        StarterDrive.Result result = drive.update(total.seconds());
        if (result == StarterDrive.Result.FAULT) {
            fail(drive.reason());
            return false;
        }
        if (result != StarterDrive.Result.DONE) {
            return false;
        }
        stepIndex++;
        if (stepIndex == steps.length) {
            return true;
        }
        startStep(steps);
        return false;
    }

    @Override
    public void loop() {
        drive.observePose();
        telemetry.addData("Local X forward / Y right / heading", "%.1f / %.1f / %.1f",
                drive.xInches(), drive.yInches(), drive.headingDegrees());
        double now = total.seconds();
        if (state != State.DONE && state != State.FAULT && now >= TOTAL_TIMEOUT_SECONDS) {
            fail("28-second deadline; all outputs stopped");
        }
        // Run only the current phase on this loop. Changing state chooses what
        // the NEXT loop will do; break leaves this switch immediately.
        switch (state) {
            case APPROACH:
                if (updatePath(route.approach)) {
                    if (mode == AutoRoutes.Mode.PARK_ONLY) {
                        beginPark("Park-only route");
                    } else {
                        tagDrive.startApproach(selection.clusterName(), TAG_RANGE_INCHES, HiveAim.OFFSET_DEGREES, now);
                        state = State.POSITION_TAG;
                    }
                }
                break;
            case POSITION_TAG:
                aim.update(true);
                if (timeToBeginParking(now)) {
                    returnToApproach("Parking time reserved; shot skipped");
                } else {
                    StarterDrive.Result positioned = tagDrive.updateApproach(now,
                            aim.targetName(), aim.fresh, aim.range, aim.bearing);
                    if (positioned == StarterDrive.Result.FAULT) {
                        if (tagDrive.canReturn()) {
                            returnToApproach("Tag positioning skipped: " + tagDrive.reason());
                        } else {
                            fail(tagDrive.reason());
                        }
                    } else if (positioned == StarterDrive.Result.DONE) {
                        tagDrive.stop();
                        if (mode == AutoRoutes.Mode.DRY_RUN) {
                            returnToApproach("Dry run; no launcher or feeder");
                        } else if (!aim.mayFeed()) {
                            returnToApproach("Shot gates not ready; shot skipped");
                        } else {
                            mechanisms.startShot(now);
                            state = State.SHOOT;
                        }
                    }
                }
                break;
            case SHOOT:
                aim.update(true);
                if (timeToBeginParking(now)) {
                    returnToApproach("Shot stopped to reserve parking time");
                } else {
                    AutoShot.State shotState = mechanisms.updateShot(now, aim.mayFeed());
                    telemetry.addData("Shot phase", shotState);
                    if (shotState == AutoShot.State.DONE) {
                        returnToApproach("Burst complete; parking");
                    } else if (shotState == AutoShot.State.FAULT) {
                        returnToApproach("Shot stopped: " + mechanisms.reason());
                    }
                }
                break;
            case RETURN_FROM_TAG:
                StarterDrive.Result restored = tagDrive.updateReturn(now);
                if (restored == StarterDrive.Result.FAULT) {
                    fail("Tag-path return failed; parking cancelled");
                } else if (restored == StarterDrive.Result.DONE) {
                    beginPark(reason);
                }
                break;
            case PARK:
                if (updatePath(route.park)) {
                    state = State.DONE;
                    tagDrive.stop();
                    mechanisms.stop();
                }
                break;
            default:
                tagDrive.stop();
                mechanisms.stop();
        }
        telemetry.addData("State / reason", "%s / %s", state, reason);
        telemetry.addData("Route / mode", "%s / %s", route == null ? "UNSET" : route.name, mode);
        telemetry.addData("Segment / seconds", "%d / %.1f", stepIndex + 1, now);
        telemetry.addData("Encoders left / right", "%d / %d",
                robot.leftDrive.getCurrentPosition(), robot.rightDrive.getCurrentPosition());
        telemetry.addData("Target / bearing / range", "%s / %.1f / %.1f", selection.clusterName(), aim.bearing, aim.range);
        telemetry.addData("Tag stage / travel inches", "%s / %.2f", tagDrive.stage(), tagDrive.translationInches());
        telemetry.addData("Launcher ticks/second", mechanisms.speed());
    }

    private boolean timeToBeginParking(double now) {
        // Leave enough time to undo tag movements and drive the parking route.
        // Example: 28 seconds total - 15 reserved = stop aiming/shooting at 13 seconds.
        double shootingDeadlineSeconds = TOTAL_TIMEOUT_SECONDS - route.parkReserveSeconds();
        return now >= shootingDeadlineSeconds;
    }

    private void returnToApproach(String message) {
        mechanisms.stop();
        tagDrive.stop();
        reason = message;
        tagDrive.startReturn(total.seconds());
        state = State.RETURN_FROM_TAG;
    }

    private void beginPark(String message) {
        mechanisms.stop();
        tagDrive.stop();
        reason = message;
        state = State.PARK;
        stepIndex = 0;
        startStep(route.park);
    }

    private void fail(String message) {
        state = State.FAULT;
        reason = message;
        if (tagDrive != null) {
            tagDrive.stop();
        }
        if (mechanisms != null) {
            mechanisms.stop();
        }
        robot.stop();
    }

    @Override
    public void stop() {
        try {
            if (tagDrive != null) {
                tagDrive.stop();
            }
            if (mechanisms != null) {
                mechanisms.stop();
            }
            robot.stop();
        } finally {
            if (aim != null) {
                aim.close();
            }
        }
    }
}
