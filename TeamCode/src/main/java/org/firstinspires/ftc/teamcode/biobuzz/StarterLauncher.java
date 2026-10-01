package org.firstinspires.ftc.teamcode.biobuzz;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

/**
 * Controls the intake, launcher wheel, and feeder for one autonomous burst.
 * The OpMode calls startShot() once, then updateShot() every loop.
 * AutoShot decides the phase; this class applies that phase to actual hardware.
 * Speed values are encoder ticks/second. Timing does not count balls.
 */
public final class StarterLauncher {
    public static final int TARGET_TICKS_PER_SECOND = 1250;
    public static final int MINIMUM_TICKS_PER_SECOND = 1200;
    public static final int MAXIMUM_TICKS_PER_SECOND = TARGET_TICKS_PER_SECOND + 50;
    // DcMotorEx is the extended direct-current motor interface; it supports velocity control.
    private final DcMotorEx wheel;
    private final DcMotor intake;
    // CRServo means continuous-rotation servo (speed/direction control).
    private final CRServo left;
    private final CRServo right;
    private final CRServo feeder;
    private AutoShot shot;

    public StarterLauncher(DcMotorEx wheel, DcMotor intake, CRServo left, CRServo right, CRServo feeder) {
        this.wheel = wheel;
        this.intake = intake;
        this.left = left;
        this.right = right;
        this.feeder = feeder;
    }

    public void intake(double power) {
        power = Math.max(-1, Math.min(1, power));
        intake.setPower(power);
        left.setPower(power);
        right.setPower(power);
    }

    public void spinUp() {
        wheel.setVelocity(TARGET_TICKS_PER_SECOND);
    }

    public void startShot(double now) {
        stop();
        shot = new AutoShot(now);
    }

    public AutoShot.State updateShot(double now, boolean targetReady) {
        if (shot == null) {
            stop();
            return AutoShot.State.FAULT;
        }
        double speed = wheel.getVelocity();
        AutoShot.State state = shot.update(now, targetReady, Double.isFinite(speed)
                && speed >= MINIMUM_TICKS_PER_SECOND && speed <= MAXIMUM_TICKS_PER_SECOND);
        if (state == AutoShot.State.DONE || state == AutoShot.State.FAULT) {
            zeroOutputs();
        } else {
            spinUp();
            if (state == AutoShot.State.FEED) {
                intake(0.5);
                feeder.setPower(1);
            } else {
                intake(0);
                feeder.setPower(0);
            }
        }
        return state;
    }

    public String reason() {
        return shot == null ? "No shot started" : shot.reason();
    }

    public double speed() {
        return wheel.getVelocity();
    }

    private void zeroOutputs() {
        wheel.setVelocity(0);
        intake(0);
        feeder.setPower(0);
    }

    public void stop() {
        zeroOutputs();
        shot = null;
    }
}
