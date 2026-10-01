package org.firstinspires.ftc.teamcode.biobuzz;

import org.junit.Test;
import static org.junit.Assert.*;

import com.qualcomm.robotcore.hardware.*;

/** Uses FTC interfaces with fake actuator state, not real hardware or an Android event loop. */
public class SubsystemTest {
    @Test
    public void driveCommandsStopOnCompletionCancellationAndFault() {
        TestHardware.Motor l=new TestHardware.Motor(), r=new TestHardware.Motor(); l.position=100; r.position=200;
        TestHardware.Heading h=new TestHardware.Heading();
        StarterDrive d=new StarterDrive(l.device(DcMotor.class),r.device(DcMotor.class),h);
        d.driveInches(24,0);
        assertTrue(l.target-100==r.target-200 && l.target>100);
        assertTrue(d.update(.1)==StarterDrive.Result.RUNNING && l.power>0 && r.power>0);
        l.position=l.target;
        assertTrue(d.update(.5)==StarterDrive.Result.RUNNING); // right never arrives
        assertTrue(d.update(4)==StarterDrive.Result.FAULT && l.power==0 && r.power==0);
        assertTrue(d.update(5)==StarterDrive.Result.FAULT); // no automatic restart
        d.turnDegrees(-90,6);
        d.update(6.01); assertTrue(l.power<0 && r.power>0); h.value=-90;
        l.position=l.target; r.position=r.target;
        assertTrue(d.update(6.1)==StarterDrive.Result.RUNNING);
        assertTrue(d.update(6.4)==StarterDrive.Result.DONE && l.power==0 && r.power==0);
        d.driveInches(-12,7); d.update(7.1); d.stop();
        assertTrue(l.power==0 && r.power==0 && d.update(7.2)==StarterDrive.Result.DONE);
        d.driveInches(Double.NaN,8);
        assertTrue(d.update(8.1)==StarterDrive.Result.FAULT && l.power==0 && r.power==0);
    }

    @Test
    public void launcherStopsAfterCompletionGateLossTimeoutAndCancellation() {
        TestHardware.Motor wheel=new TestHardware.Motor(), intake=new TestHardware.Motor(), a=new TestHardware.Motor(), b=new TestHardware.Motor(), feeder=new TestHardware.Motor();
        StarterLauncher s=new StarterLauncher(wheel.device(DcMotorEx.class),intake.device(DcMotor.class),
                a.device(CRServo.class),b.device(CRServo.class),feeder.device(CRServo.class));
        s.intake(4); assertTrue(intake.power==1 && a.power==1 && b.power==1); s.stop();
        s.startShot(0);
        assertTrue(s.updateShot(0,true)==AutoShot.State.SPINUP && feeder.power==0 && wheel.requestedSpeed==1250);
        wheel.measuredSpeed=1250; s.updateShot(.1,true);
        assertTrue(s.updateShot(.41,true)==AutoShot.State.FEED && feeder.power==1 && intake.power==.5);
        assertTrue(s.updateShot(.5,false)==AutoShot.State.FAULT);
        assertTrue(wheel.requestedSpeed==0 && intake.power==0 && a.power==0 && b.power==0 && feeder.power==0);
        assertTrue(s.updateShot(.6,true)==AutoShot.State.FAULT && feeder.power==0);
        s.startShot(1); s.updateShot(1,true); s.updateShot(1.31,true);
        assertTrue(s.updateShot(1.92,true)==AutoShot.State.DONE && wheel.requestedSpeed==0 && feeder.power==0);
        s.startShot(2); wheel.measuredSpeed=0; s.updateShot(2,true);
        assertTrue(s.updateShot(6,true)==AutoShot.State.FAULT && wheel.requestedSpeed==0);
        s.startShot(7); wheel.measuredSpeed=1250; s.updateShot(7,true); s.updateShot(7.31,true); s.stop();
        assertTrue(s.updateShot(7.4,true)==AutoShot.State.FAULT && wheel.requestedSpeed==0 && feeder.power==0);
    }
}
