package org.firstinspires.ftc.teamcode.biobuzz;

/**
 * Decides the next phase of one timed shot: spin up, feed, finish, or fault.
 * StarterLauncher calls update() each loop and applies the returned phase to hardware.
 * This class only tracks time and permission; it has no motor or camera dependencies.
 * A finished or failed shot never restarts itself.
 */
public final class AutoShot {
    public enum State {
        SPINUP, FEED, DONE, FAULT
    }

    public static final double SPEED_DWELL_SECONDS = 0.3;
    public static final double SPINUP_TIMEOUT_SECONDS = 4.0;
    // Trial duration: measure with actual POLLEN.
    public static final double FEED_SECONDS = 0.6;
    private State state = State.SPINUP;
    private final double started;
    // NaN (Not a Number) means the speed has not begun a continuous period inside the allowed band.
    private double stableSince = Double.NaN;
    private double feedStarted;
    private String reason = "";

    public AutoShot(double now) {
        started = now;
    }

    public State update(double now, boolean targetReady, boolean speedReady) {
        if (state == State.DONE || state == State.FAULT) {
            return state;
        }
        if (!Double.isFinite(now) || now < started) {
            return fail("Invalid clock");
        }
        if (!targetReady) {
            return fail("Target, alignment, range, or feed permission lost");
        }
        if (state == State.SPINUP) {
            if (now - started >= SPINUP_TIMEOUT_SECONDS) {
                return fail("Launcher spin-up timeout");
            }
            if (!speedReady) {
                stableSince = Double.NaN;
            } else if (Double.isNaN(stableSince)) {
                stableSince = now;
            }
            if (speedReady && now - stableSince >= SPEED_DWELL_SECONDS) {
                state = State.FEED;
                feedStarted = now;
            }
        } else if (!speedReady) {
            return fail("Launcher speed left qualified band during feed");
        } else if (now - feedStarted >= FEED_SECONDS) {
            state = State.DONE;
        }
        return state;
    }

    private State fail(String message) {
        reason = message;
        state = State.FAULT;
        return state;
    }

    public String reason() {
        return reason;
    }
}
