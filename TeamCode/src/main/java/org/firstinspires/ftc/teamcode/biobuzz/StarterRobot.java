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


package org.firstinspires.ftc.teamcode.biobuzz;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

/**
 * Connects Java names to the robot's motors and servos and applies basic outputs.
 * Each OpMode creates one StarterRobot and calls init() during INIT.
 * This class has no gamepad or autonomous decisions; the OpMode chooses what to do.
 * Hardware setup is adapted from the goBILDA example retained under vendor/gobilda.
 */
public final class StarterRobot {
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

    public void init(HardwareMap hardwareMap) {
        // These strings must match the Driver Station robot configuration exactly.
        leftDrive = hardwareMap.get(DcMotor.class, "left_drive");
        rightDrive = hardwareMap.get(DcMotor.class, "right_drive");
        intake = hardwareMap.get(DcMotor.class, "intake");
        launcher = hardwareMap.get(DcMotorEx.class, "launcher");
        windmillServo = hardwareMap.get(CRServo.class, "windmill");
        leftIntakeServo = hardwareMap.get(CRServo.class, "left_intake_servo");
        rightIntakeServo = hardwareMap.get(CRServo.class, "right_intake_servo");

        // Opposite sides are mounted in opposite directions. Verify on the actual build.
        leftDrive.setDirection(DcMotor.Direction.FORWARD);
        rightDrive.setDirection(DcMotor.Direction.REVERSE);
        leftDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rightDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        intake.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        leftDrive.setPower(0);
        rightDrive.setPower(0);
        intake.setPower(0);
        leftDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        intake.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // The launcher uses its encoder to hold a requested speed, in ticks/second.
        // PIDF = proportional, integral, derivative, and feedforward control.
        // P responds to current speed error; I to accumulated error; D to changing error.
        // F supplies an expected output for the requested speed before error correction.
        // These vendor starting values belong to this launcher's speed controller.
        // They use different scaling from our drive/aim power-per-degree gains.
        double proportionalGain = 40;
        double integralGain = 0;
        double derivativeGain = 0;
        double feedforwardGain = 12.5;
        launcher.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        launcher.setVelocity(0);
        launcher.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER,
                new PIDFCoefficients(proportionalGain, integralGain,
                        derivativeGain, feedforwardGain));

        leftIntakeServo.setPower(0);
        rightIntakeServo.setPower(0);
        windmillServo.setPower(0);
        rightIntakeServo.setDirection(DcMotorSimple.Direction.REVERSE);
        windmillServo.setDirection(DcMotorSimple.Direction.REVERSE);
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
        double limitedPower = Math.max(-1, Math.min(1, power));
        intake.setPower(limitedPower);
        leftIntakeServo.setPower(limitedPower);
        rightIntakeServo.setPower(limitedPower);
    }

    /** Spinning the launcher does not automatically run the feeder. */
    public void setLauncherRunning(boolean running) {
        if (running) {
            launcher.setVelocity(StarterLauncher.TARGET_TICKS_PER_SECOND);
        } else {
            launcher.setVelocity(0);
        }
    }

    public void setFeederRunning(boolean running) {
        if (running) {
            windmillServo.setPower(1);
        } else {
            windmillServo.setPower(0);
        }
    }

    /** Stop all outputs, including if hardware initialization was only partly completed. */
    public void stop() {
        if (leftDrive != null) {
            leftDrive.setPower(0);
        }
        if (rightDrive != null) {
            rightDrive.setPower(0);
        }
        if (intake != null) {
            intake.setPower(0);
        }
        if (launcher != null) {
            launcher.setVelocity(0);
        }
        if (windmillServo != null) {
            windmillServo.setPower(0);
        }
        if (leftIntakeServo != null) {
            leftIntakeServo.setPower(0);
        }
        if (rightIntakeServo != null) {
            rightIntakeServo.setPower(0);
        }
    }
}
