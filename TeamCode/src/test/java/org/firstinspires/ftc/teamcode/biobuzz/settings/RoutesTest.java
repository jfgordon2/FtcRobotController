package org.firstinspires.ftc.teamcode.biobuzz.settings;

import org.junit.Test;
import static org.junit.Assert.*;

public class RoutesTest {
    @Test
    public void eachSelectionHasItsOwnRoute() {
        String[] clusters={"RED SCORING","RED AUDIENCE","BLUE SCORING","BLUE AUDIENCE"};
        java.util.Set<Routes.Step[]> routes=new java.util.HashSet<>();
        for(String name:clusters) for(boolean b:new boolean[]{false,true}) {
            Routes.Step[] route=Routes.select(name,b); assertNotNull(route); routes.add(route);
            // Students edit the steps, so only check that there is at least one and none is missing.
            assertTrue(route.length>0);
            for(Routes.Step step:route) { assertNotNull(step); assertNotNull(step.toString()); }
        }
        assertEquals(8,routes.size()); assertNull(Routes.select("UNSET",false));
    }

    @Test
    public void theThreeKindsOfStep() {
        assertEquals(Routes.Kind.DRIVE,Routes.drive(24).kind); assertEquals(24,Routes.drive(24).amount,0);
        assertEquals(Routes.Kind.TURN,Routes.turn(-90).kind); assertEquals(-90,Routes.turn(-90).amount,0);
        assertEquals(Routes.Kind.AIM_AND_SHOOT,Routes.aimAndShoot().kind);
    }
}
