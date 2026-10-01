package org.firstinspires.ftc.teamcode.biobuzz;

/**
 * Remembers the driver's alliance and cell choices for aiming or autonomous.
 * The OpMode supplies D-pad inputs to update(); clusterName() supplies the camera target.
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
    // D-pad left/right: red/blue. Up/down: far-side/audience-side cell.
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
        if (cell.equals("SCORING")) {
            return "FAR SIDE (opposite audience)";
        }
        return "AUDIENCE SIDE";
    }

    public String clusterName() {
        if (!ready()) {
            return "UNSET";
        }
        return alliance + " " + cell;
    }
}
