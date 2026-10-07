package org.firstinspires.ftc.teamcode.biobuzz.robot;

/**
 * Runs one shot from start to finish: spin up, wait for speed, feed, stop feeding.
 * The OpMode calls startShot() once, then updateShot() every loop.
 * ShotTimer decides the phase; StarterRobot moves the launcher, feeder and intake.
 * Speed values are encoder ticks/second. Timing does not count balls.
 */
public final class Launcher {
    private final StarterRobot robot;
    private ShotTimer shot;
    private double shotSpeed = StarterRobot.LAUNCHER_SPEED;

    public Launcher(StarterRobot robot) {
        this.robot = robot;
    }

    /**
     * Start one shot. This is the only way to start one.
     *   ticksPerSecond  the wheel speed to shoot at
     *   feedSeconds     how long the feeder may run once the wheel is at speed
     * A speed that is not a real positive number (for example, from outside our table) is refused.
     */
    public void startShot(double now, double ticksPerSecond, double feedSeconds) {
        shot = null;
        robot.setFeederRunning(false);
        robot.setIntakePower(0);
        if (Double.isFinite(ticksPerSecond) && ticksPerSecond > 0) {
            shotSpeed = ticksPerSecond;
            shot = new ShotTimer(now, feedSeconds);
        }
    }

    /** True from startShot() until the shot is DONE or has a FAULT. */
    public boolean shooting() {
        return shot != null && !shot.finished();
    }

    public ShotTimer.State updateShot(double now, boolean targetReady) {
        if (shot == null) {
            stop();
            return ShotTimer.State.FAULT;
        }
        ShotTimer.State state = shot.update(now, targetReady, robot.launcherAtSpeed(shotSpeed));
        if (state == ShotTimer.State.FAULT) {
            robot.setLauncherSpeed(0);
        } else {
            // The wheel keeps spinning after DONE so another shot can follow. stop() ends it.
            robot.setLauncherSpeed(shotSpeed);
        }
        robot.setFeederRunning(shot.feeding());
        robot.setIntakePower(shot.feeding() ? 0.5 : 0);
        return state;
    }

    public String reason() {
        return shot == null ? "No shot started" : shot.reason();
    }

    public double speed() {
        return robot.launcher.getVelocity();
    }

    /** Stop the wheel, feeder and intake, and forget any shot in progress. */
    public void stop() {
        robot.setLauncherSpeed(0);
        robot.setFeederRunning(false);
        robot.setIntakePower(0);
        shot = null;
    }
}
