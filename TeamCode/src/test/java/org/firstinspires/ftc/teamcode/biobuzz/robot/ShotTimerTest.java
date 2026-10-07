package org.firstinspires.ftc.teamcode.biobuzz.robot;

import org.junit.Test;
import static org.junit.Assert.*;

public class ShotTimerTest {
    @Test
    public void requiresContinuousSpeedBeforeFeedingAndFinishesOnce() {
        ShotTimer a = new ShotTimer(0, ShotTimer.FEED_SECONDS);
        assertEquals(ShotTimer.State.SPINUP, a.update(0, true, false));
        assertEquals(ShotTimer.State.SPINUP, a.update(.1, true, true));
        assertEquals(ShotTimer.State.SPINUP, a.update(.3, true, false));
        assertEquals(ShotTimer.State.SPINUP, a.update(.4, true, true));
        assertEquals(ShotTimer.State.SPINUP, a.update(.6, true, true));
        assertEquals(ShotTimer.State.FEED, a.update(.71, true, true));
        // Feeding began at 0.71 and lasts FEED_SECONDS, a number students tune.
        assertEquals(ShotTimer.State.FEED, a.update(.71 + ShotTimer.FEED_SECONDS / 2, true, true));
        assertEquals(ShotTimer.State.DONE, a.update(.72 + ShotTimer.FEED_SECONDS, true, true));
        assertEquals(ShotTimer.State.DONE, a.update(2, false, false));
    }

    @Test
    public void feederWaitsWhileTheWheelIsSlowButTheShotStillEndsOnTime() {
        ShotTimer a = new ShotTimer(0, 1.0);
        a.update(0, true, true);
        assertEquals(ShotTimer.State.FEED, a.update(.31, true, true));
        assertTrue(a.feeding());
        assertEquals(ShotTimer.State.FEED, a.update(.5, true, false));
        assertFalse("a slowed wheel pauses the feeder", a.feeding());
        assertEquals(ShotTimer.State.FEED, a.update(.8, true, true));
        assertTrue(a.feeding());
        assertEquals(ShotTimer.State.DONE, a.update(1.32, true, true));
        assertFalse(a.feeding());
        assertTrue(a.finished());
    }

    @Test
    public void faultsOnTimeoutLostPermissionOrInvalidClock() {
        ShotTimer a = new ShotTimer(0, ShotTimer.FEED_SECONDS);
        assertEquals(ShotTimer.State.FAULT, a.update(4, true, false));
        assertEquals(ShotTimer.State.FAULT, a.update(5, true, true));
        a = new ShotTimer(0, ShotTimer.FEED_SECONDS);
        assertEquals(ShotTimer.State.FAULT, a.update(0, false, true));
        a = new ShotTimer(0, ShotTimer.FEED_SECONDS); a.update(0, true, true); a.update(.31, true, true);
        assertEquals(ShotTimer.State.FAULT, a.update(.4, false, true));
        assertFalse(a.feeding());
        assertEquals(ShotTimer.State.FAULT, new ShotTimer(0, ShotTimer.FEED_SECONDS).update(Double.NaN, true, true));
        assertEquals(ShotTimer.State.FAULT, new ShotTimer(0, 0).update(0, true, true));
    }
}
