package org.firstinspires.ftc.teamcode.biobuzz.robot;

import com.qualcomm.robotcore.hardware.Gamepad;

/**
 * Remembers the driver's alliance and cell choices for aiming or autonomous.
 * The OpMode supplies D-pad inputs to update(); targetName() names the target, for example "RED SCORING".
 * Lock the alliance at Start. A cell change requires a new permitted button press.
 * No hardware is read or moved here.
 */
public final class TargetSelection {
    private String alliance;
    private String cell;
    private boolean locked;
    // Remember last loop so holding a button does not count as repeated presses.
    private boolean wasLeft;
    private boolean wasRight;
    private boolean wasUp;
    private boolean wasDown;
    // D-pad left/right: RED/BLUE. Up/down: the SCORING cell (the side away from the audience) / the AUDIENCE cell.
    // These are FIRST's own names for the two groups of tags on a hive.
    // Contradictory directions are ignored. Only new presses change selections.
    public void update(boolean left, boolean right, boolean up, boolean down, boolean cellAllowed) {
        if (!locked) {
            if (left && !wasLeft && !right) {
                alliance = "RED";
            }
            if (right && !wasRight && !left) {
                alliance = "BLUE";
            }
        }
        if (cellAllowed && (!locked || ready())) {
            if (up && !wasUp && !down) {
                cell = "SCORING";
            }
            if (down && !wasDown && !up) {
                cell = "AUDIENCE";
            }
        }
        wasLeft = left;
        wasRight = right;
        wasUp = up;
        wasDown = down;
    }

    /** The usual way to call update(): read the four D-pad buttons from a gamepad. */
    public void update(Gamepad pad, boolean cellAllowed) {
        update(pad.dpad_left, pad.dpad_right, pad.dpad_up, pad.dpad_down, cellAllowed);
    }

    public void lockAlliance() {
        locked = true;
    }

    public boolean ready() {
        return alliance != null && cell != null;
    }

    public String allianceLabel() {
        if (alliance == null) {
            return "NOT SELECTED";
        }
        return alliance;
    }

    public String cellLabel() {
        if (cell == null) {
            return "NOT SELECTED";
        }
        // SCORING is FIRST's name for the tags on the far side of the hive, away from the audience.
        if (cell.equals("SCORING")) {
            return "SCORING (far side)";
        }
        return cell;
    }

    public String targetName() {
        if (!ready()) {
            return "UNSET";
        }
        return alliance + " " + cell;
    }
}
