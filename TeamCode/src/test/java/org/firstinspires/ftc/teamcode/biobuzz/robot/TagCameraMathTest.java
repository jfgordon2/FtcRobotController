package org.firstinspires.ftc.teamcode.biobuzz.robot;

import org.junit.Test;
import static org.junit.Assert.*;

public class TagCameraMathTest {
    @Test
    public void turnRequestsHaveCorrectDirectionAndLimits() {
        assertTrue("left target requires left turn", TagCamera.turnPower(10, 0) < 0);
        assertTrue("right target requires right turn", TagCamera.turnPower(-10, 0) > 0);
        assertTrue("saturation", TagCamera.turnPower(100, 0) == -TagCamera.MAXIMUM_TURN_POWER);
        assertTrue("camera offset", TagCamera.turnPower(5, 5) == 0);
        assertTrue("nonfinite pose", TagCamera.turnPower(Double.NaN, 0) == 0);
    }

    @Test
    public void cameraMeasurementsMustBeFiniteAndRecent() {
        assertTrue("fresh valid target", TagCamera.usable(0, 30, .1));
        assertTrue("stale frame rejected", !TagCamera.usable(0, 30, .251));
        assertTrue("future timestamp rejected", !TagCamera.usable(0, 30, -.1));
        assertTrue("invalid range rejected", !TagCamera.usable(0, Double.NaN, .1));
        assertTrue("invalid bearing rejected", !TagCamera.usable(Double.POSITIVE_INFINITY, 30, .1));
    }
}
