package org.firstinspires.ftc.teamcode.biobuzz;

import org.junit.Test;
import static org.junit.Assert.*;

import com.qualcomm.robotcore.hardware.DcMotor;
public class ImuDriveTest {
    @Test
    public void turnsUseHeadingAndSensorLossStopsMotion() {
        TestHardware.Motor l=new TestHardware.Motor(),r=new TestHardware.Motor();TestHardware.Heading h=new TestHardware.Heading();
        StarterDrive d=new StarterDrive(l.device(DcMotor.class),r.device(DcMotor.class),h);
        d.turnDegrees(90,0);d.update(.1);assertTrue(l.power>0&&r.power<0);
        // Encoders alone cannot complete an IMU turn.
        l.position=500;r.position=-500;assertTrue(d.update(.2)==StarterDrive.Result.RUNNING);
        h.value=90;d.update(.3);assertTrue(d.update(.6)==StarterDrive.Result.DONE&&l.power==0&&r.power==0);
        h.value=179;d.turnDegrees(5,1);h.value=-179;d.update(1.1);assertTrue(l.power>0&&r.power<0);
        d.stop();h.value=0;d.driveInches(12,2);h.value=5;d.update(2.1);assertTrue(l.power<r.power);
        d.stop();h.value=0;d.driveInches(-12,3);h.value=5;d.update(3.1);assertTrue(l.power<r.power);
        h.value=Double.NaN;assertTrue(d.update(3.2)==StarterDrive.Result.FAULT&&l.power==0&&r.power==0);
        TagDrive tags = new TagDrive(d);
        h.value=0;tags.startApproach("RED SCORING",32,0,4);h.value=Double.NaN;
        assertTrue(tags.updateApproach(4.1,"RED SCORING",true,32,0)==StarterDrive.Result.FAULT&&!tags.canReturn());
    }

    @Test
    public void positionEstimateUsesTravelAndHeading() {
        HeadingMath.Pose pose=new HeadingMath.Pose();pose.reset(0);pose.update(10,0);assertTrue(pose.x==10&&pose.y==0);
        pose.update(0,90);pose.update(5,90);assertTrue(Math.abs(pose.x-10)<1e-8&&Math.abs(pose.y-5)<1e-8);
        assertTrue(HeadingMath.wrap(-358)==2&&HeadingMath.wrap(358)==-2);
    }

    @Test
    public void startViewRequiresFreshMeasurementsWithinTolerance() {
        StartTagCheck start=new StartTagCheck(true,48,179);
        assertTrue(start.matches(true,49,-179));assertTrue(!start.matches(false,48,179));
        assertTrue(!start.matches(true,55,179));assertTrue(!start.matches(true,48,170));
        assertTrue(!new StartTagCheck(false,48,0).matches(true,48,0));
    }
}
