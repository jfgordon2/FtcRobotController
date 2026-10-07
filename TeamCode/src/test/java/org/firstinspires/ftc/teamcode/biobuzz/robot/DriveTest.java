package org.firstinspires.ftc.teamcode.biobuzz.robot;

import org.junit.Test;
import static org.junit.Assert.*;

public class DriveTest {
    @Test
    public void turnsUseHeadingAndSensorLossStopsMotion() {
        TestHardware.Motor l=new TestHardware.Motor(),r=new TestHardware.Motor();TestHardware.FakeHeading h=new TestHardware.FakeHeading();
        Drive d=new Drive(TestHardware.robot(l,r),h);
        d.turnDegrees(90,0);d.update(.1);assertTrue(l.power>0&&r.power<0);
        // Encoders alone cannot complete an IMU turn.
        l.position=500;r.position=-500;assertTrue(d.update(.2)==Drive.Result.RUNNING);
        h.value=90;d.update(.3);assertTrue(d.update(.6)==Drive.Result.DONE&&l.power==0&&r.power==0);
        h.value=179;d.turnDegrees(5,1);h.value=-179;d.update(1.1);assertTrue(l.power>0&&r.power<0);
        d.stop();h.value=0;d.driveInches(12,2);h.value=5;d.update(2.1);assertTrue(l.power<r.power);
        d.stop();h.value=0;d.driveInches(-12,3);h.value=5;d.update(3.1);assertTrue(l.power<r.power);
        h.value=Double.NaN;assertTrue(d.update(3.2)==Drive.Result.FAULT&&l.power==0&&r.power==0);
        // turnToHeading goes the short way round to an exact heading.
        h.value=170;d.turnToHeading(-170,5);d.update(5.1);assertTrue("short way is clockwise",l.power>0&&r.power<0);
        h.value=-170;d.update(5.2);assertTrue(d.update(5.5)==Drive.Result.DONE&&l.power==0);
    }

    @Test
    public void positionEstimateUsesTravelAndHeading() {
        Heading.Pose pose=new Heading.Pose();pose.reset(0);pose.update(10,0);assertTrue(pose.x==10&&pose.y==0);
        pose.update(0,90);pose.update(5,90);assertTrue(Math.abs(pose.x-10)<1e-8&&Math.abs(pose.y-5)<1e-8);
        assertTrue(Heading.wrap(-358)==2&&Heading.wrap(358)==-2);
    }
}
