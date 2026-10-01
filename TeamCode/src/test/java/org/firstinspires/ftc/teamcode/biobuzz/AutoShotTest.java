package org.firstinspires.ftc.teamcode.biobuzz;

import org.junit.Test;
import static org.junit.Assert.*;

public class AutoShotTest {
    @Test
    public void requiresContinuousSpeedBeforeFeedingAndFinishesOnce() {
        AutoShot a = new AutoShot(0);
        assertEquals(AutoShot.State.SPINUP, a.update(0, true, false));
        assertEquals(AutoShot.State.SPINUP, a.update(.1, true, true));
        assertEquals(AutoShot.State.SPINUP, a.update(.3, true, false));
        assertEquals(AutoShot.State.SPINUP, a.update(.4, true, true));
        assertEquals(AutoShot.State.SPINUP, a.update(.6, true, true));
        assertEquals(AutoShot.State.FEED, a.update(.71, true, true));
        assertEquals(AutoShot.State.FEED, a.update(1.0, true, true));
        assertEquals(AutoShot.State.DONE, a.update(1.32, true, true));
        assertEquals(AutoShot.State.DONE, a.update(2, false, false));
    }

    @Test
    public void faultsOnTimeoutLostPermissionLostSpeedOrInvalidClock() {
        AutoShot a = new AutoShot(0);
        assertEquals(AutoShot.State.FAULT, a.update(4, true, false));
        assertEquals(AutoShot.State.FAULT, a.update(5, true, true));
        a = new AutoShot(0);
        assertEquals(AutoShot.State.FAULT, a.update(0, false, true));
        a = new AutoShot(0); a.update(0, true, true); a.update(.31, true, true);
        assertEquals(AutoShot.State.FAULT, a.update(.4, false, true));
        a = new AutoShot(0); a.update(0, true, true); a.update(.31, true, true);
        assertEquals(AutoShot.State.FAULT, a.update(.4, true, false));
        assertEquals(AutoShot.State.FAULT, new AutoShot(0).update(Double.NaN, true, true));
    }
}
