package org.firstinspires.ftc.teamcode.biobuzz.robot;

/**
 * Answers one question: has something been true, without a break, for long enough?
 * The drive uses it for "arrived", the camera for "aimed", and the launcher for "at speed".
 * A single good reading can be luck; a reading that stays good for a moment can be trusted.
 */
public final class HeldFor {
    // NaN (Not a Number) means the condition is not currently true, so no timing has started.
    private double since = Double.NaN;

    /** Call every loop. True once the condition has stayed true for the given seconds. */
    public boolean longEnough(double now, boolean conditionTrue, double seconds) {
        if (!conditionTrue) {
            since = Double.NaN;
            return false;
        }
        if (Double.isNaN(since)) {
            since = now;
        }
        return now - since >= seconds;
    }

    public void reset() {
        since = Double.NaN;
    }
}
