package org.firstinspires.ftc.teamcode.biobuzz.robot;

import org.junit.Test;
import static org.junit.Assert.*;

import com.qualcomm.robotcore.hardware.*;

/** Uses FTC interfaces with fake actuator state, not real hardware or an Android event loop. */
public class SubsystemTest {
    @Test
    public void driveCommandsStopOnCompletionCancellationAndFault() {
        TestHardware.Motor l=new TestHardware.Motor(), r=new TestHardware.Motor(); l.position=100; r.position=200;
        TestHardware.FakeHeading h=new TestHardware.FakeHeading();
        Drive d=new Drive(TestHardware.robot(l,r),h);
        d.driveInches(24,0);
        assertTrue(d.leftTargetTicks()-100==d.rightTargetTicks()-200 && d.leftTargetTicks()>100);
        assertTrue(d.update(.1)==Drive.Result.RUNNING && l.power>0 && r.power>0);
        l.position=d.leftTargetTicks();
        assertTrue(d.update(.5)==Drive.Result.RUNNING); // right never arrives
        assertTrue(d.update(4)==Drive.Result.FAULT && l.power==0 && r.power==0);
        assertTrue(d.update(5)==Drive.Result.FAULT); // no automatic restart
        d.turnDegrees(-90,6);
        d.update(6.01); assertTrue(l.power<0 && r.power>0); h.value=-90;
        l.position=d.leftTargetTicks(); r.position=d.rightTargetTicks();
        assertTrue(d.update(6.1)==Drive.Result.RUNNING);
        assertTrue(d.update(6.4)==Drive.Result.DONE && l.power==0 && r.power==0);
        d.driveInches(-12,7); d.update(7.1); d.stop();
        assertTrue(l.power==0 && r.power==0 && d.update(7.2)==Drive.Result.DONE);
        d.driveInches(Double.NaN,8);
        assertTrue(d.update(8.1)==Drive.Result.FAULT && l.power==0 && r.power==0);
    }
}
