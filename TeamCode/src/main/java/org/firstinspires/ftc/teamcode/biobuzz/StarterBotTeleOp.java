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

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * First student reading: translate gamepad inputs into robot actions each loop.
 * StarterRobot handles hardware setup and shared output calculations.
 * This baseline has no camera: right bumper spins AND feeds once the launcher is fast enough.
 */
@TeleOp(name = "BIOBUZZ Standard Baseline", group = "BIOBUZZ Drafts")
@Disabled
public class StarterBotTeleOp extends OpMode {
    private final StarterRobot robot = new StarterRobot();

    // The FTC SDK (software development kit) calls init() once when the driver presses INIT.
    @Override
    public void init() {
        robot.init(hardwareMap);
        telemetry.addData("Status", "Initialized");
    }

    // The SDK repeats loop() from START until STOP. Each pass reads current inputs.
    @Override
    public void loop() {
        // Pushing the left stick forward gives negative Y, so negate it.
        double forward = -gamepad1.left_stick_y;
        double rotate = gamepad1.right_stick_x;
        robot.arcadeDrive(forward, rotate);

        // Right trigger pulls balls in; left trigger reverses. Equal pressures cancel.
        double intakePower = gamepad1.right_trigger - gamepad1.left_trigger;
        robot.setLauncherRunning(gamepad1.right_bumper);

        // Encoder speed is measured in ticks/second, not motor power or RPM.
        boolean fastEnough = robot.launcher.getVelocity() > StarterLauncher.MINIMUM_TICKS_PER_SECOND;
        boolean feed = gamepad1.right_bumper && fastEnough;
        robot.setFeederRunning(feed);
        if (feed) {
            intakePower += 0.5;
        }
        robot.setIntakePower(intakePower);

        telemetry.addData("Motors", "left (%.2f), right (%.2f)",
                robot.leftDrive.getPower(), robot.rightDrive.getPower());
    }

    @Override
    public void stop() {
        robot.stop();
    }
}
