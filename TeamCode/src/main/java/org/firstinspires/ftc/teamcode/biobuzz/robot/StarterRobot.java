package org.firstinspires.ftc.teamcode.biobuzz.robot;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.hardware.VoltageSensor;

/**
 * Connects Java names to the robot's motors and servos and applies basic outputs.
 * Each OpMode creates one StarterRobot and calls init() during INIT.
 * This class has no gamepad or autonomous decisions; the OpMode chooses what to do.
 * Hardware setup is adapted from goBILDA's and REV's example code (licence notice at the end of this file).
 */
public final class StarterRobot {
    // WHICH ROBOT IS THIS CODE RUNNING ON? Change the one word after "Robot." below.
    public enum Robot {
        BIOBUZZ_GOBILDA,   // the 2026-2027 goBILDA StarterBot: intake, corner rollers, windmill feeder
        PRACTICE_GOBILDA,  // last season's goBILDA StarterBot: two feeder servos, no intake
        PRACTICE_REV       // last season's REV Starter Bot: Core Hex feeder motor and agitator servo, no intake
    }
    public static final Robot ROBOT = Robot.PRACTICE_GOBILDA;

    // THE MENTOR'S SETUP SWITCHES. Programs that move the robot by themselves, or feed balls by
    // themselves, check these first. Leave them false until the matching check has been done.
    // Allows automatic driving and turning. Set after the drive-direction check.
    public static final boolean MOTION_ENABLED = false;
    // Allows the camera-aimed programs to run the feeder. Set after shooting has been tested by hand.
    public static final boolean FEED_ENABLED = false;

    // NUMBERS THAT DEPEND ON THE ROBOT AND ARE USED BY MORE THAN ONE CLASS. Each line lists the value for
    // BIOBUZZ_GOBILDA, PRACTICE_GOBILDA, PRACTICE_REV in that order.
    // They are the starting values from each vendor's example code.

    // The launcher's normal speed, in encoder ticks per second.
    public static final double LAUNCHER_SPEED = pick(1250, 1125, 1300);
    // The wheel counts as "at speed" within this many ticks/second of the requested speed.
    public static final double LAUNCHER_SPEED_TOLERANCE = pick(50, 50, 100);
    // PIDF = proportional, integral, derivative, and feedforward control.
    // The launcher uses its encoder to hold a requested speed.
    // F supplies an expected output for the requested speed before any correction.
    // P adds correction in proportion to the speed error. We leave I and D at zero.
    // NaN (Not a Number) means "keep the Hub's built-in values", which is what REV's code does.
    // Meeting 13 tunes these. They use different scaling from our drive/aim gains.
    public static final double LAUNCHER_P = pick(40, 300, Double.NaN);
    public static final double LAUNCHER_F = pick(12.5, 10, Double.NaN);

    static double pick(double biobuzzGobilda, double practiceGobilda, double practiceRev) {
        switch (ROBOT) {
            case PRACTICE_GOBILDA:
                return practiceGobilda;
            case PRACTICE_REV:
                return practiceRev;
            default:
                return biobuzzGobilda;
        }
    }

    // Public so reusable subsystems can receive the specific hardware they need.
    // SDK (software development kit) types: DcMotor = direct-current motor;
    // DcMotorEx adds extended controls such as encoder-based velocity requests.
    public DcMotor leftDrive;
    public DcMotor rightDrive;
    public DcMotor intake;
    public DcMotorEx launcher;
    // CRServo = continuous-rotation servo: power controls speed/direction, not an angle.
    public CRServo leftIntakeServo;
    public CRServo rightIntakeServo;
    public CRServo windmillServo;
    // Last season's goBILDA robot feeds with two servos instead of a windmill. It has no intake.
    public CRServo leftFeeder;
    public CRServo rightFeeder;
    // Last season's REV robot feeds with a Core Hex motor, helped by an agitator servo. No intake.
    public DcMotor feederMotor;
    public CRServo agitator;
    private HardwareMap hardware;

    /**
     * Find this robot's motors and servos and set them up. Each program calls this once in INIT.
     * The names in quotes must match the Driver Station robot configuration exactly.
     * You only need to read the method for the robot you are using.
     */
    public void init(HardwareMap hardwareMap) {
        hardware = hardwareMap;
        switch (ROBOT) {
            case PRACTICE_GOBILDA:
                initPracticeGobilda(hardwareMap);
                break;
            case PRACTICE_REV:
                initPracticeRev(hardwareMap);
                break;
            default:
                initBiobuzzGobilda(hardwareMap);
                break;
        }
        initDriveAndLauncher();
    }

    /** The 2026-2027 goBILDA StarterBot: intake roller, two corner servos, windmill feeder. */
    private void initBiobuzzGobilda(HardwareMap hardwareMap) {
        leftDrive = hardwareMap.get(DcMotor.class, "left_drive");
        rightDrive = hardwareMap.get(DcMotor.class, "right_drive");
        launcher = hardwareMap.get(DcMotorEx.class, "launcher");
        intake = hardwareMap.get(DcMotor.class, "intake");
        windmillServo = hardwareMap.get(CRServo.class, "windmill");
        leftIntakeServo = hardwareMap.get(CRServo.class, "left_intake_servo");
        rightIntakeServo = hardwareMap.get(CRServo.class, "right_intake_servo");

        // The two sides of the drive are mounted as mirror images, so one side is reversed.
        leftDrive.setDirection(DcMotor.Direction.FORWARD);
        rightDrive.setDirection(DcMotor.Direction.REVERSE);
        rightIntakeServo.setDirection(DcMotorSimple.Direction.REVERSE);
        windmillServo.setDirection(DcMotorSimple.Direction.REVERSE);
        intake.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        intake.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    }

    /** Last season's goBILDA StarterBot: two feeder servos, no intake. */
    private void initPracticeGobilda(HardwareMap hardwareMap) {
        leftDrive = hardwareMap.get(DcMotor.class, "left_drive");
        rightDrive = hardwareMap.get(DcMotor.class, "right_drive");
        launcher = hardwareMap.get(DcMotorEx.class, "launcher");
        leftFeeder = hardwareMap.get(CRServo.class, "left_feeder");
        rightFeeder = hardwareMap.get(CRServo.class, "right_feeder");

        leftDrive.setDirection(DcMotor.Direction.REVERSE);
        rightDrive.setDirection(DcMotor.Direction.FORWARD);
        leftFeeder.setDirection(DcMotorSimple.Direction.REVERSE);
    }

    /** Last season's REV Starter Bot: Core Hex feeder motor and agitator servo, no intake. */
    private void initPracticeRev(HardwareMap hardwareMap) {
        leftDrive = hardwareMap.get(DcMotor.class, "leftDrive");
        rightDrive = hardwareMap.get(DcMotor.class, "rightDrive");
        launcher = hardwareMap.get(DcMotorEx.class, "flywheel");
        feederMotor = hardwareMap.get(DcMotor.class, "coreHex");
        agitator = hardwareMap.get(CRServo.class, "servo");

        leftDrive.setDirection(DcMotor.Direction.REVERSE);
        rightDrive.setDirection(DcMotor.Direction.FORWARD);
        launcher.setDirection(DcMotor.Direction.REVERSE);
        feederMotor.setDirection(DcMotor.Direction.REVERSE);
        // REV's code runs the agitator at -1, so reversing it lets us use +1 for "feed".
        agitator.setDirection(DcMotorSimple.Direction.REVERSE);
    }

    /** The part of the setup that is the same on every robot. */
    private void initDriveAndLauncher() {
        // We set drive POWER ourselves; the drive encoders are only read, to measure distance.
        leftDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rightDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        leftDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // The launcher uses its encoder to hold the SPEED we ask for.
        launcher.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        if (Double.isFinite(LAUNCHER_P) && Double.isFinite(LAUNCHER_F)) {
            setLauncherGains(LAUNCHER_P, LAUNCHER_F);
        }
        // Start with everything stopped.
        stop();
    }

    // BATTERY. Rules of thumb for the 12 V robot battery, read with the motors stopped.
    static final double BATTERY_FRESH_VOLTS = 13.5;
    static final double BATTERY_SWAP_VOLTS = 12.5;
    static final double BATTERY_CHARGE_VOLTS = 12.0;

    /** Battery voltage right now, as the Hub measures it. It dips while motors are working. */
    public double batteryVolts() {
        if (hardware == null) {
            return Double.NaN;
        }
        for (VoltageSensor sensor : hardware.voltageSensor) {
            double volts = sensor.getVoltage();
            if (volts > 0) {
                return volts;
            }
        }
        return Double.NaN;
    }

    /**
     * The battery line every program shows on the Driver Station during INIT: the voltage
     * and what to do. INIT is the right time to read it because the motors are stopped.
     */
    public String batteryStatus() {
        double volts = batteryVolts();
        if (!Double.isFinite(volts)) {
            return "no reading";
        }
        return String.format("%.1f V: %s", volts, batteryAdvice(volts));
    }

    /** What to do about a battery that reads this many volts with the motors stopped. */
    static String batteryAdvice(double restingVolts) {
        if (restingVolts >= BATTERY_FRESH_VOLTS) {
            return "GOOD, freshly charged";
        } else if (restingVolts >= BATTERY_SWAP_VOLTS) {
            return "GOOD";
        } else if (restingVolts >= BATTERY_CHARGE_VOLTS) {
            return "SWAP before measuring or a match";
        }
        return "CHARGE this battery";
    }

    /** Give the launcher's speed controller new P and F numbers. I and D stay at zero. */
    public void setLauncherGains(double proportionalGain, double feedforwardGain) {
        launcher.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER,
                new PIDFCoefficients(proportionalGain, 0, 0, feedforwardGain));
    }

    /** Positive forward drives ahead; positive rotate turns right (clockwise). */
    public void arcadeDrive(double forward, double rotate) {
        // Scale both sides together so neither exceeds the allowed -1 to +1 power.
        double scale = Math.max(1, Math.abs(forward) + Math.abs(rotate));
        double leftPower = (forward + rotate) / scale;
        double rightPower = (forward - rotate) / scale;
        leftDrive.setPower(leftPower);
        rightDrive.setPower(rightPower);
    }

    /** Run the roller and both corner servos together. Negative power reverses them. */
    public void setIntakePower(double power) {
        // Last season's practice robots have no intake, so there is nothing to run.
        if (intake == null) {
            return;
        }
        double limitedPower = Math.max(-1, Math.min(1, power));
        intake.setPower(limitedPower);
        leftIntakeServo.setPower(limitedPower);
        rightIntakeServo.setPower(limitedPower);
    }

    /** True when the wheel's measured speed is close enough to the speed we asked for. */
    public boolean launcherAtSpeed(double wantedTicksPerSecond) {
        double measured = launcher.getVelocity();
        return Double.isFinite(measured)
                && Math.abs(measured - wantedTicksPerSecond) <= LAUNCHER_SPEED_TOLERANCE;
    }

    /** Ask the launcher wheel to hold this speed, in encoder ticks per second. Zero stops it.
     *  Spinning the launcher does not run the feeder. */
    public void setLauncherSpeed(double ticksPerSecond) {
        launcher.setVelocity(ticksPerSecond);
    }

    /** Push balls into the launcher wheel with whichever feeder parts this robot has. */
    public void setFeederRunning(boolean running) {
        double power = 0;
        if (running) {
            power = 1;
        }
        setPower(windmillServo, power);
        setPower(leftFeeder, power);
        setPower(rightFeeder, power);
        setPower(feederMotor, power);
        setPower(agitator, power);
    }

    /** Stop every motor and servo this robot has. */
    public void stop() {
        setPower(leftDrive, 0);
        setPower(rightDrive, 0);
        setPower(intake, 0);
        setPower(leftIntakeServo, 0);
        setPower(rightIntakeServo, 0);
        if (launcher != null) {
            launcher.setVelocity(0);
        }
        setFeederRunning(false);
    }

    /** Set a motor's or servo's power, skipping parts this robot does not have (they are null). */
    private static void setPower(DcMotorSimple device, double power) {
        if (device != null) {
            device.setPower(power);
        }
    }
}

// The hardware setup in this file is adapted from goBILDA's StarterBot example code, which
// carries the licence notice below. The original is kept unchanged under vendor/gobilda/.
/*   MIT License
 *   Copyright (c) [2026] [Base 10 Assets, LLC]
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:

 *   The above copyright notice and this permission notice shall be included in all
 *   copies or substantial portions of the Software.

 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *   SOFTWARE.
 */
