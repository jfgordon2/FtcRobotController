package org.firstinspires.ftc.teamcode.biobuzz;

import org.junit.Test;
import static org.junit.Assert.*;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

/** Exercises the shared outputs with fake devices; does not initialize real hardware. */
public class StarterRobotTest {

    @Test
    public void manualOutputsAreLimitedAndStopZerosEveryDevice() {
        StarterRobot robot = new StarterRobot();
        TestHardware.Motor left = new TestHardware.Motor();
        TestHardware.Motor right = new TestHardware.Motor();
        TestHardware.Motor wheel = new TestHardware.Motor();
        TestHardware.Motor intake = new TestHardware.Motor();
        TestHardware.Motor leftServo = new TestHardware.Motor();
        TestHardware.Motor rightServo = new TestHardware.Motor();
        TestHardware.Motor feeder = new TestHardware.Motor();
        robot.leftDrive = left.device(DcMotor.class);
        robot.rightDrive = right.device(DcMotor.class);
        robot.launcher = wheel.device(DcMotorEx.class);
        robot.intake = intake.device(DcMotor.class);
        robot.leftIntakeServo = leftServo.device(CRServo.class);
        robot.rightIntakeServo = rightServo.device(CRServo.class);
        robot.windmillServo = feeder.device(CRServo.class);

        robot.arcadeDrive(1, 1);
        assertTrue("combined input stays within motor limits", left.power == 1 && right.power == 0);
        robot.arcadeDrive(-1, -1);
        assertTrue("reverse combined input stays within limits", left.power == -1 && right.power == 0);
        robot.arcadeDrive(0, 0.2);
        assertTrue("positive rotation turns right", left.power == 0.2 && right.power == -0.2);
        robot.setIntakePower(1.5);
        assertTrue("all three intake devices share clipped positive power", intake.power == 1 && leftServo.power == 1 && rightServo.power == 1);
        robot.setIntakePower(-2);
        assertTrue("all three intake devices share clipped reverse power", intake.power == -1 && leftServo.power == -1 && rightServo.power == -1);
        robot.setLauncherRunning(true);
        assertTrue("spinning up alone does not feed", wheel.requestedSpeed == StarterLauncher.TARGET_TICKS_PER_SECOND && feeder.power == 0);
        robot.setFeederRunning(true);
        robot.setFeederRunning(false);
        assertTrue("releasing feed stops only the feeder", feeder.power == 0 && wheel.requestedSpeed == StarterLauncher.TARGET_TICKS_PER_SECOND);
        robot.setLauncherRunning(false);
        assertTrue("releasing spin requests zero velocity", wheel.requestedSpeed == 0);
        robot.setLauncherRunning(true);
        robot.setFeederRunning(true);
        robot.stop();
        assertTrue("Stop zeros every output", left.power == 0 && right.power == 0 && wheel.requestedSpeed == 0
                        && intake.power == 0 && leftServo.power == 0 && rightServo.power == 0
                        && feeder.power == 0);

        StarterRobot partlyInitialized = new StarterRobot();
        partlyInitialized.leftDrive = left.device(DcMotor.class);
        left.power = 0.5;
        partlyInitialized.stop();
        assertTrue("Stop works after partial initialization", left.power == 0);
    }
}
