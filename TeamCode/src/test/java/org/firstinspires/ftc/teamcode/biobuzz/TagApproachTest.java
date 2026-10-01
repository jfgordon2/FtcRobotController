package org.firstinspires.ftc.teamcode.biobuzz;

import org.junit.Test;
import static org.junit.Assert.*;

public class TagApproachTest {
    static TagApproach fresh() { return new TagApproach("RED SCORING",32,3,0); }
    static TagApproach.Stage step(TagApproach c,double t,double range,double bearing) {
        return c.update(t,"RED SCORING",true,range,bearing,0,0);
    }
    @Test
    public void approachPhasesSetBoundedPowersAndRejectInvalidObservations() {
        TagApproach c=fresh(); step(c,0,36,10); assertTrue(c.turn<0 && c.forward==0);
        step(c,.1,36,0); assertTrue(step(c,.31,36,0)==TagApproach.Stage.RANGE);
        step(c,.4,36,0); assertTrue(c.forward>0 && c.forward<=.15 && c.turn==0);
        step(c,.5,30,0); assertTrue(c.forward<0 && c.turn==0);
        step(c,.6,32,0); assertTrue(c.forward==0);
        assertTrue(step(c,.81,32,0)==TagApproach.Stage.FINAL_ALIGN);
        step(c,.9,32,0); assertTrue(c.turn>0 && c.forward==0);
        step(c,1,32,3); assertTrue(step(c,1.31,32,3)==TagApproach.Stage.DONE && c.forward==0 && c.turn==0);
        assertTrue(c.update(2,"wrong",false,0,0,0,0)==TagApproach.Stage.DONE);
        c=fresh(); assertTrue(c.update(.1,"wrong",true,32,0,0,0)==TagApproach.Stage.FAULT && c.turn==0);
        c=fresh(); assertTrue(c.update(.1,"RED SCORING",false,32,0,0,0)==TagApproach.Stage.FAULT);
        c=fresh(); assertTrue(c.update(.1,"RED SCORING",true,32,0,6,6)==TagApproach.Stage.FAULT);
        c=fresh(); assertTrue(c.update(.1,"RED SCORING",true,32,0,0,12)==TagApproach.Stage.FAULT);
        c=fresh(); assertTrue(step(c,4,32,0)==TagApproach.Stage.FAULT);
        assertTrue(step(c,4.1,32,0)==TagApproach.Stage.FAULT);
        assertTrue(new TagApproach("RED SCORING",Double.NaN,0,0).stage()==TagApproach.Stage.FAULT);
        c=fresh(); step(c,0,36,0); step(c,.21,36,0);
        assertTrue(step(c,.3,35,9)==TagApproach.Stage.FAULT && c.forward==0);
        c=fresh(); step(c,0,32,0); step(c,.21,32,0);step(c,.3,32,0);step(c,.51,32,0);
        assertTrue(step(c,.6,34,0)==TagApproach.Stage.FAULT);
    }
}
