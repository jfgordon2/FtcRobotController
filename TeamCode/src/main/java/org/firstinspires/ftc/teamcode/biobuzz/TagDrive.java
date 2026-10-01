package org.firstinspires.ftc.teamcode.biobuzz;

import java.util.ArrayList;

/**
 * Coordinates a camera-guided approach and the measured return to its starting pose.
 * TagApproach decides forward/turn requests; StarterDrive applies them to the motors.
 * This class records each phase, checks wheel progress, and reverses the phases on return.
 * An OpMode starts each action once and calls its matching update method every loop.
 * While this class owns motion, call its stop() to cancel both its state and motor outputs.
 */
public final class TagDrive {
    // Checkpoints are numbered from zero, like positions in a Java array.
    // 0 = before the first turn; 1 = before driving straight; 2 = before the final turn.
    private static final int STRAIGHT_PHASE_INDEX = 1;
    private final StarterDrive drive;
    private final ArrayList<DriveCheckpoint> checkpoints = new ArrayList<>();
    private TagApproach approach;
    private DriveCheckpoint previous;
    private DriveCheckpoint progress;
    private DriveCheckpoint returnEnd;
    private int returnIndex;
    private double translationInches;
    private double wheelTravelInches;
    private double lastProgress;
    private boolean returnSafe;
    private boolean returning;
    private boolean approaching;
    private StarterDrive.Result result = StarterDrive.Result.DONE;
    private String reason = "";

    public TagDrive(StarterDrive drive) {
        this.drive = drive;
    }

    /** Start a turn/straight/turn approach to one selected tag cluster. */
    public void startApproach(String destination, double rangeInches, double bearingDegrees, double now) {
        stop();
        approach = new TagApproach(destination, rangeInches, bearingDegrees, now);
        checkpoints.clear();
        checkpoints.add(drive.checkpoint());
        previous = drive.checkpoint();
        progress = previous;
        translationInches = 0;
        wheelTravelInches = 0;
        lastProgress = now;
        returnSafe = true;
        approaching = true;
        result = StarterDrive.Result.RUNNING;
        reason = "";
    }

    /** Supply the latest camera observation on each loop; missing/stale data stops motion. */
    public StarterDrive.Result updateApproach(double now, String destination, boolean fresh,
                                              double range, double bearing) {
        if (!approaching) {
            return result;
        }
        drive.observePose();
        if (!Double.isFinite(drive.headingDegrees())) {
            stop();
            returnSafe = false;
            result = StarterDrive.Result.FAULT;
            reason = "Heading sensor (IMU) missing/stale during tag approach";
            return result;
        }
        if (approach.stage() == TagApproach.Stage.FAULT) {
            result = StarterDrive.Result.FAULT;
            reason = approach.reason();
            return result;
        }

        DriveCheckpoint current = drive.checkpoint();
        double leftTravelInches = (current.leftTicks - previous.leftTicks) / StarterDrive.TICKS_PER_INCH;
        double rightTravelInches = (current.rightTicks - previous.rightTicks) / StarterDrive.TICKS_PER_INCH;
        translationInches += Math.abs((leftTravelInches + rightTravelInches) / 2);
        wheelTravelInches += Math.max(Math.abs(leftTravelInches), Math.abs(rightTravelInches));
        previous = current;

        // If powered but either wheel fails to advance, do not trust a return path.
        boolean bothWheelsMoved = Math.abs(current.leftTicks - progress.leftTicks) >= 2
                && Math.abs(current.rightTicks - progress.rightTicks) >= 2;
        if (!drive.wheelsPowered() || bothWheelsMoved) {
            lastProgress = now;
            progress = current;
        } else if (now - lastProgress > 0.75) {
            stop();
            result = StarterDrive.Result.FAULT;
            approach = null;
            returnSafe = false;
            reason = "Tag motion stalled; return path unsafe";
            return result;
        }

        TagApproach.Stage before = approach.stage();
        TagApproach.Stage stage = approach.update(now, destination, fresh, range, bearing,
                translationInches, wheelTravelInches);
        boolean enteredStraightPhase = stage == TagApproach.Stage.RANGE;
        boolean enteredFinalTurn = stage == TagApproach.Stage.FINAL_ALIGN;
        if (before != stage && (enteredStraightPhase || enteredFinalTurn)) {
            checkpoints.add(drive.checkpoint());
        }

        double turnPower = approach.turn;
        if (approach.forward != 0) {
            DriveCheckpoint straightStart = checkpoints.get(checkpoints.size() - 1);
            turnPower = HeadingMath.correction(straightStart.headingDegrees,
                    drive.headingDegrees(), StarterDrive.HEADING_HOLD_PROPORTIONAL_GAIN, 0.10);
        }
        drive.arcade(approach.forward, turnPower);

        if (stage == TagApproach.Stage.FAULT) {
            result = StarterDrive.Result.FAULT;
            reason = approach.reason();
        } else if (stage == TagApproach.Stage.DONE) {
            result = StarterDrive.Result.DONE;
        } else {
            result = StarterDrive.Result.RUNNING;
        }
        approaching = result == StarterDrive.Result.RUNNING;
        return result;
    }

    /** Reverse the recorded phases, including a partly completed phase after target loss. */
    public void startReturn(double now) {
        stop();
        if (!returnSafe || checkpoints.isEmpty()) {
            result = StarterDrive.Result.FAULT;
            reason = "No trusted tag return path";
            return;
        }
        returnIndex = checkpoints.size() - 1;
        // A phase-end checkpoint equal to our current position adds no return segment.
        while (returnIndex > 0 && atCheckpoint(checkpoints.get(returnIndex))) {
            returnIndex--;
        }
        returnEnd = drive.checkpoint();
        returning = true;
        startReturnStep(now);
    }

    private boolean atCheckpoint(DriveCheckpoint point) {
        DriveCheckpoint current = drive.checkpoint();
        return Math.abs(current.leftTicks - point.leftTicks) <= 1
                && Math.abs(current.rightTicks - point.rightTicks) <= 1
                && Math.abs(HeadingMath.wrap(point.headingDegrees - current.headingDegrees))
                        <= StarterDrive.HEADING_TOLERANCE_DEGREES;
    }

    private void startReturnStep(double now) {
        DriveCheckpoint point = checkpoints.get(returnIndex);
        DriveCheckpoint end = returnEnd;
        if (returnIndex + 1 < checkpoints.size()) {
            end = checkpoints.get(returnIndex + 1);
        }
        // Recorded travel remains useful even if IMU-controlled turns change wheel counts.
        double leftReturnTicks = point.leftTicks - end.leftTicks;
        double rightReturnTicks = point.rightTicks - end.rightTicks;
        double averageReturnTicks = (leftReturnTicks + rightReturnTicks) / 2;
        double inches = averageReturnTicks / StarterDrive.TICKS_PER_INCH;
        // Checkpoint 0: start; 1: after initial turn; 2: after straight travel.
        // Return phase 1 reverses translation; phases 0 and 2 restore measured headings.
        boolean turnOnly = returnIndex != STRAIGHT_PHASE_INDEX;
        result = drive.startRecordedMove(inches, point.headingDegrees, turnOnly,
                TagApproach.RECOVERY_STEP_SECONDS, now);
    }

    public StarterDrive.Result updateReturn(double now) {
        if (!returning) {
            return StarterDrive.Result.FAULT;
        }
        result = drive.update(now);
        if (result == StarterDrive.Result.FAULT) {
            returning = false;
            reason = drive.reason();
        } else if (result == StarterDrive.Result.DONE && returnIndex > 0) {
            returnIndex--;
            startReturnStep(now);
        } else if (result == StarterDrive.Result.DONE) {
            returning = false;
        }
        return result;
    }

    /** Cancel active motion. Keep recorded checkpoints available for an explicit return. */
    public void stop() {
        drive.stop();
        approaching = false;
        returning = false;
        result = StarterDrive.Result.DONE;
    }

    public boolean canReturn() {
        return returnSafe;
    }

    public String stage() {
        if (approach == null) {
            return "NONE";
        }
        return approach.stage().name();
    }

    public double translationInches() {
        return translationInches;
    }

    public String reason() {
        return reason;
    }
}
