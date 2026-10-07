package org.firstinspires.ftc.teamcode.biobuzz.robot;

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
        robot.setLauncherSpeed(StarterRobot.LAUNCHER_SPEED);
        assertTrue("spinning up alone does not feed", wheel.requestedSpeed == StarterRobot.LAUNCHER_SPEED && feeder.power == 0);
        robot.setFeederRunning(true);
        robot.setFeederRunning(false);
        assertTrue("releasing feed stops only the feeder", feeder.power == 0 && wheel.requestedSpeed == StarterRobot.LAUNCHER_SPEED);
        robot.setLauncherSpeed(0);
        assertTrue("releasing spin requests zero velocity", wheel.requestedSpeed == 0);
        robot.setLauncherSpeed(StarterRobot.LAUNCHER_SPEED);
        robot.setFeederRunning(true);
        robot.stop();
        assertTrue("Stop zeros every output", left.power == 0 && right.power == 0 && wheel.requestedSpeed == 0
                        && intake.power == 0 && leftServo.power == 0 && rightServo.power == 0
                        && feeder.power == 0);

        // Last season's practice robot: two feeders, and no intake to run.
        StarterRobot practice = new StarterRobot();
        TestHardware.Motor feederA = new TestHardware.Motor();
        TestHardware.Motor feederB = new TestHardware.Motor();
        practice.launcher = wheel.device(DcMotorEx.class);
        practice.leftFeeder = feederA.device(CRServo.class);
        practice.rightFeeder = feederB.device(CRServo.class);
        practice.setIntakePower(1);
        practice.setLauncherSpeed(900);
        practice.setFeederRunning(true);
        assertTrue("both feeders run together", wheel.requestedSpeed == 900 && feederA.power == 1 && feederB.power == 1);
        practice.stop();
        assertTrue("Stop zeros both feeders", wheel.requestedSpeed == 0 && feederA.power == 0 && feederB.power == 0);

        // Last season's REV practice robot: a feeder motor and an agitator servo.
        StarterRobot rev = new StarterRobot();
        TestHardware.Motor coreHex = new TestHardware.Motor();
        TestHardware.Motor agitator = new TestHardware.Motor();
        rev.launcher = wheel.device(DcMotorEx.class);
        rev.feederMotor = coreHex.device(DcMotor.class);
        rev.agitator = agitator.device(CRServo.class);
        rev.setFeederRunning(true);
        assertTrue("feeder motor and agitator run together", coreHex.power == 1 && agitator.power == 1);
        rev.stop();
        assertTrue("Stop zeros the feeder motor and agitator", coreHex.power == 0 && agitator.power == 0);

        StarterRobot partlyInitialized = new StarterRobot();
        partlyInitialized.leftDrive = left.device(DcMotor.class);
        left.power = 0.5;
        partlyInitialized.stop();
        assertTrue("Stop works after partial initialization", left.power == 0);
    }

    @Test
    public void batteryAdviceFollowsRestingVoltage() {
        assertEquals("GOOD, freshly charged", StarterRobot.batteryAdvice(13.6));
        assertEquals("GOOD", StarterRobot.batteryAdvice(12.9));
        assertEquals("SWAP before measuring or a match", StarterRobot.batteryAdvice(12.2));
        assertEquals("CHARGE this battery", StarterRobot.batteryAdvice(11.5));
    }
}
