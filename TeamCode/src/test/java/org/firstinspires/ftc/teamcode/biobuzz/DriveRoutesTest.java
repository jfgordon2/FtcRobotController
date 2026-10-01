package org.firstinspires.ftc.teamcode.biobuzz;

import org.junit.Test;
import static org.junit.Assert.*;

public class DriveRoutesTest {
    @Test
    public void wheelGeometryPreservesTurnDirection() {
        double wheel = DriveMath.turnWheelInches(90, 16);
        assertEquals(Math.PI*4, wheel, 1e-8);
        assertEquals(-wheel, DriveMath.turnWheelInches(-90,16), 1e-8);
        assertEquals(90, DriveMath.headingDegrees(wheel*45,-wheel*45,45,16), 1e-8);
        assertEquals(0, DriveMath.headingDegrees(100,100,45,16), 1e-8);
        // A camera-induced turn must be undone before the park path starts.
        double approach = 40, aimed = 57;
        assertEquals(-DriveMath.turnWheelInches(17,16), DriveMath.turnWheelInches(approach-aimed,16), 1e-8);
    }

    @Test
    public void completionRequiresContinuousTimeWithinTolerance() {
        DriveMath.Completion c = new DriveMath.Completion();
        assertTrue(!c.settledForRequiredTime(0,false)); // Both wheels must arrive.
        assertTrue(!c.settledForRequiredTime(.1,true));
        assertTrue(!c.settledForRequiredTime(.2,false)); // Leaving tolerance resets dwell.
        assertTrue(!c.settledForRequiredTime(.3,true));
        assertTrue(!c.settledForRequiredTime(.5,true));
        assertTrue(c.settledForRequiredTime(.56,true));
        assertTrue(!c.settledForRequiredTime(.6,false));
    }

    @Test
    public void eachSelectionHasAnIndependentUnqualifiedRoute() {
        String[] clusters={"RED SCORING","RED AUDIENCE","BLUE SCORING","BLUE AUDIENCE"};
        java.util.Set<AutoRoutes.Route> routes=new java.util.HashSet<>();
        for(String name:clusters) for(boolean b:new boolean[]{false,true}) {
            AutoRoutes.Route r=AutoRoutes.select(name,b); assertTrue(r!=null); routes.add(r);
            assertTrue(!r.qualified); assertTrue(r.approach.length>0 && r.park.length>0);
            assertEquals(15, r.parkReserveSeconds(), 1e-8);
            assertTrue(r.name.startsWith(name.split(" ")[0]));
            assertTrue(r.name.contains(b?"start B":"start A"));
        }
        assertTrue(routes.size()==8); assertTrue(AutoRoutes.select("UNSET",false)==null);
    }
}
