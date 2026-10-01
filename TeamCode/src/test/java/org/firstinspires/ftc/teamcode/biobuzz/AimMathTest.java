package org.firstinspires.ftc.teamcode.biobuzz;

import org.junit.Test;
import static org.junit.Assert.*;

public class AimMathTest {
    @Test
    public void turnRequestsHaveCorrectDirectionAndLimits() {
        assertTrue("left target requires left turn", AimMath.turn(10, 0, .015, .18) < 0);
        assertTrue("right target requires right turn", AimMath.turn(-10, 0, .015, .18) > 0);
        assertTrue("saturation", AimMath.turn(100, 0, .015, .18) == -.18);
        assertTrue("camera offset", AimMath.turn(5, 5, .015, .18) == 0);
        assertTrue("nonfinite pose", AimMath.turn(Double.NaN, 0, .015, .18) == 0);
    }

    @Test
    public void cameraMeasurementsMustBeFiniteAndRecent() {
        assertTrue("fresh valid target", AimMath.usable(0, 30, .1));
        assertTrue("stale frame rejected", !AimMath.usable(0, 30, .251));
        assertTrue("future timestamp rejected", !AimMath.usable(0, 30, -.1));
        assertTrue("invalid range rejected", !AimMath.usable(0, Double.NaN, .1));
        assertTrue("invalid bearing rejected", !AimMath.usable(Double.POSITIVE_INFINITY, 30, .1));
    }

    @Test
    public void shotWindowMustBeValidAndContainRange() {
        assertTrue("uncalibrated shot disabled", !AimMath.inWindow(30, Double.NaN, Double.NaN));
        assertTrue("reversed window disabled", !AimMath.inWindow(30, 40, 20));
        assertTrue("zero minimum disabled", !AimMath.inWindow(30, 0, 40));
        assertTrue("measured window accepted", AimMath.inWindow(30, 28, 32));
        assertTrue("no extrapolation", !AimMath.inWindow(33, 28, 32));
    }
}
