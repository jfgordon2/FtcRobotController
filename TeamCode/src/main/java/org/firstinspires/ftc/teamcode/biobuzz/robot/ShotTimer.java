package org.firstinspires.ftc.teamcode.biobuzz.robot;

/**
 * Decides the next phase of one shot: spin up, feed, finish, or fault.
 * Launcher calls update() each loop and applies the result to the robot.
 * This class only tracks time and permission; it has no motor or camera dependencies.
 * A finished or failed shot never restarts itself.
 */
public final class ShotTimer {
    public enum State {
        SPINUP, FEED, DONE, FAULT
    }

    public static final double SPEED_DWELL_SECONDS = 0.3;
    public static final double SPINUP_TIMEOUT_SECONDS = 4.0;
    // How long the feeder is allowed to run for an autonomous burst. Meeting 12 tunes it
    // with real balls. It is a time, not a ball counter.
    public static final double FEED_SECONDS = 0.6;

    private State state = State.SPINUP;
    private final double started;
    private final double feedSeconds;
    private final HeldFor atSpeed = new HeldFor();
    private double feedStarted;
    private boolean feeding;
    private String reason = "";

    public ShotTimer(double now, double feedSeconds) {
        started = now;
        this.feedSeconds = feedSeconds;
    }

    public State update(double now, boolean targetReady, boolean speedReady) {
        feeding = false;
        if (state == State.DONE || state == State.FAULT) {
            return state;
        }
        if (!Double.isFinite(now) || now < started || !(feedSeconds > 0)) {
            return fail("Invalid clock or feed time");
        }
        if (!targetReady) {
            return fail("Target, alignment, range, or feed permission lost");
        }
        if (state == State.SPINUP) {
            if (now - started >= SPINUP_TIMEOUT_SECONDS) {
                return fail("Launcher spin-up timeout");
            }
            if (atSpeed.longEnough(now, speedReady, SPEED_DWELL_SECONDS)) {
                state = State.FEED;
                feedStarted = now;
                feeding = true;
            }
        } else if (now - feedStarted >= feedSeconds) {
            state = State.DONE;
        } else {
            // A ball slows the wheel. The feeder waits while the wheel catches up.
            feeding = speedReady;
        }
        return state;
    }

    /** True on loops where the feeder should run. */
    public boolean feeding() {
        return feeding;
    }

    public boolean finished() {
        return state == State.DONE || state == State.FAULT;
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
