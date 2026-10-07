package org.firstinspires.ftc.teamcode.biobuzz.robot;

import org.firstinspires.ftc.teamcode.biobuzz.settings.*;
import com.qualcomm.robotcore.hardware.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** The launcher sequence, run against fake devices on each kind of robot. */
public class LauncherTest {
    private final TestHardware.Motor wheel=new TestHardware.Motor(), intake=new TestHardware.Motor(),
            left=new TestHardware.Motor(), right=new TestHardware.Motor(), feed=new TestHardware.Motor();

    private StarterRobot biobuzzRobot() {
        StarterRobot robot=new StarterRobot();
        robot.launcher=wheel.device(DcMotorEx.class);
        robot.intake=intake.device(DcMotor.class);
        robot.leftIntakeServo=left.device(CRServo.class);
        robot.rightIntakeServo=right.device(CRServo.class);
        robot.windmillServo=feed.device(CRServo.class);
        return robot;
    }

    @Test public void shotWaitsForSpeedFeedsThenLeavesTheWheelSpinning() {
        Launcher launcher=new Launcher(biobuzzRobot());
        launcher.startShot(0,1050,ShotTimer.FEED_SECONDS);
        wheel.measuredSpeed=1500; // spinning, but not at this row's speed
        assertEquals(ShotTimer.State.SPINUP,launcher.updateShot(0,true));
        assertEquals(1050,wheel.requestedSpeed,1e-9); assertEquals(0,feed.power,0);
        launcher.updateShot(.5,true);
        assertEquals("wrong speed never feeds",0,feed.power,0);
        wheel.measuredSpeed=1040;
        launcher.updateShot(.6,true);
        assertEquals(ShotTimer.State.FEED,launcher.updateShot(.91,true));
        assertTrue(feed.power==1 && intake.power==.5 && launcher.shooting());
        wheel.measuredSpeed=700; // a ball slows the wheel: the feeder waits
        assertEquals(ShotTimer.State.FEED,launcher.updateShot(.95,true));
        assertTrue(feed.power==0 && intake.power==0);
        wheel.measuredSpeed=1050;
        launcher.updateShot(1,true);
        assertEquals(1,feed.power,0);
        assertEquals(ShotTimer.State.DONE,launcher.updateShot(.92+ShotTimer.FEED_SECONDS,true));
        assertTrue("feeder stops, wheel keeps spinning for the next shot",
                feed.power==0 && intake.power==0 && wheel.requestedSpeed==1050 && !launcher.shooting());
        launcher.stop();
        assertEquals(0,wheel.requestedSpeed,0);
    }

    @Test public void lostTargetTimeoutRefusedSpeedAndCancelAllStopEverything() {
        Launcher launcher=new Launcher(biobuzzRobot());
        wheel.measuredSpeed=1000;
        launcher.startShot(0,1000,ShotTimer.FEED_SECONDS); launcher.updateShot(0,true); launcher.updateShot(.31,true);
        assertEquals(1,feed.power,0);
        assertEquals(ShotTimer.State.FAULT,launcher.updateShot(.4,false)); // target lost
        assertTrue(wheel.requestedSpeed==0 && feed.power==0 && intake.power==0);
        assertEquals(ShotTimer.State.FAULT,launcher.updateShot(.5,true)); // no automatic restart
        launcher.startShot(1,1000,ShotTimer.FEED_SECONDS); wheel.measuredSpeed=0; launcher.updateShot(1,true);
        assertEquals(ShotTimer.State.FAULT,launcher.updateShot(5,true)); // never reached speed
        assertEquals(0,wheel.requestedSpeed,0);
        launcher.startShot(6,Double.NaN,ShotTimer.FEED_SECONDS); // a range outside the table gives no speed
        assertEquals(ShotTimer.State.FAULT,launcher.updateShot(6,true));
        wheel.measuredSpeed=1000;
        launcher.startShot(7,1000,ShotTimer.FEED_SECONDS); launcher.updateShot(7,true); launcher.updateShot(7.31,true); launcher.stop();
        assertTrue(wheel.requestedSpeed==0 && feed.power==0);
        assertEquals(ShotTimer.State.FAULT,launcher.updateShot(7.4,true));
    }

    @Test public void shortTunerShotUsesItsOwnFeedTime() {
        Launcher launcher=new Launcher(biobuzzRobot());
        wheel.measuredSpeed=1000;
        launcher.startShot(0,1000,.2); launcher.updateShot(0,true);
        assertEquals(ShotTimer.State.FEED,launcher.updateShot(.31,true));
        assertEquals(ShotTimer.State.DONE,launcher.updateShot(.52,true));
    }

    @Test public void practiceRobotsFeedWithTheirOwnParts() {
        TestHardware.Motor a=new TestHardware.Motor(), b=new TestHardware.Motor();
        StarterRobot gobilda=new StarterRobot();
        gobilda.launcher=wheel.device(DcMotorEx.class);
        gobilda.leftFeeder=a.device(CRServo.class); gobilda.rightFeeder=b.device(CRServo.class);
        StarterRobot rev=new StarterRobot();
        rev.launcher=wheel.device(DcMotorEx.class);
        rev.feederMotor=a.device(DcMotor.class); rev.agitator=b.device(CRServo.class);
        for (StarterRobot robot : new StarterRobot[]{gobilda,rev}) {
            Launcher launcher=new Launcher(robot);
            wheel.measuredSpeed=1000;
            launcher.startShot(0,1000,ShotTimer.FEED_SECONDS); launcher.updateShot(0,true);
            assertEquals(ShotTimer.State.FEED,launcher.updateShot(.31,true));
            assertTrue(a.power==1 && b.power==1); // and no crash from the missing intake
            launcher.stop();
            assertTrue(a.power==0 && b.power==0 && wheel.requestedSpeed==0);
        }
    }
}
