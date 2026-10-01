package org.firstinspires.ftc.teamcode.biobuzz;

import org.junit.Test;
import static org.junit.Assert.*;

import com.qualcomm.robotcore.hardware.DcMotor;
public class TagDriveTest {
    static StarterDrive.Result update(TagDrive d,double t,double range,double bearing) {
        return d.updateApproach(t,"RED SCORING",true,range,bearing);
    }
    static void finish(TagDrive d,TestHardware.Motor l,TestHardware.Motor r,double now) {
        l.position=l.target; r.position=r.target;
        d.updateReturn(now); d.updateReturn(now+.3);
    }
    @Test
    public void measuredReturnHandlesPartialMotionFaultsAndCancellation() {
        TestHardware.Motor l=new TestHardware.Motor(), r=new TestHardware.Motor();
        l.position=r.position=100;
        TestHardware.Heading h=new TestHardware.Heading();
        TagDrive d=new TagDrive(new StarterDrive(l.device(DcMotor.class),r.device(DcMotor.class),h));
        d.startApproach("RED SCORING",32,3,0);
        update(d,0,34,6); assertTrue(l.power<0 && r.power>0);
        l.position=90;r.position=110;h.value=-5; update(d,.1,34,0);update(d,.31,34,0);
        update(d,.32,34,0);assertTrue(l.power>0 && r.power>0);
        l.position=180;r.position=200;update(d,.5,32,0);update(d,.71,32,0);
        update(d,.72,32,0);assertTrue(l.power>0 && r.power<0);
        l.position=190;r.position=190;h.value=-2;update(d,.8,32,3);
        assertTrue(update(d,1.11,32,3)==StarterDrive.Result.DONE && l.power==0 && r.power==0);
        d.startReturn(1.2);d.updateReturn(1.21);assertTrue(l.power<0 && r.power>0);
        h.value=-5;l.position=180;r.position=200;finish(d,l,r,1.3);
        assertTrue(l.target<l.position && r.target<r.position); // reverse straight leg
        finish(d,l,r,1.7);d.updateReturn(2.01);assertTrue(l.power>0 && r.power<0);
        h.value=0;d.updateReturn(2.1);assertTrue(d.updateReturn(2.4)==StarterDrive.Result.DONE);
        assertTrue(l.power==0 && r.power==0);
        l.position=r.position=100;
        d.startApproach("RED SCORING",32,0,3);update(d,3,36,0);update(d,3.21,36,0);update(d,3.22,36,0);
        l.position+=20;r.position+=20;
        assertTrue(d.updateApproach(3.4,"RED SCORING",false,0,0)==StarterDrive.Result.FAULT && l.power==0);
        assertTrue(d.canReturn());d.startReturn(3.5);assertTrue(l.target==100 && r.target==100);
        assertTrue(d.updateReturn(5.5)==StarterDrive.Result.FAULT && l.power==0 && r.power==0);
        d.startApproach("RED SCORING",32,0,6);update(d,6,36,8);
        l.position-=10; // only one encoder advances
        assertTrue(update(d,6.8,36,8)==StarterDrive.Result.FAULT && !d.canReturn());
        d.startApproach("RED SCORING",32,0,7);update(d,7,36,8);d.stop();
        update(d,7.1,36,8);assertTrue(l.power==0 && r.power==0);
        // A cancelled return must not restart motors on a later update.
        d.startApproach("RED SCORING",32,0,8);update(d,8,36,0);update(d,8.21,36,0);
        update(d,8.22,36,0);l.position+=90;r.position+=90;
        update(d,8.3,35,0);d.startReturn(8.4);d.updateReturn(8.41);
        assertTrue(l.power<0 && r.power<0);
        d.stop();
        assertTrue(d.updateReturn(8.5)==StarterDrive.Result.FAULT && l.power==0 && r.power==0);
    }
}
