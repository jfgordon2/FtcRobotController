package org.firstinspires.ftc.teamcode.biobuzz.robot;

import org.junit.Test;
import static org.junit.Assert.*;

public class HeldForTest {
    @Test public void needsAnUnbrokenStretchOfTrue() {
        HeldFor s = new HeldFor();
        assertFalse(s.longEnough(0, false, .25));
        assertFalse(s.longEnough(.1, true, .25));
        assertFalse(s.longEnough(.2, false, .25)); // a break starts the wait again
        assertFalse(s.longEnough(.3, true, .25));
        assertFalse(s.longEnough(.5, true, .25));
        assertTrue(s.longEnough(.56, true, .25));
        s.reset();
        assertFalse(s.longEnough(.6, true, .25));
    }
}
