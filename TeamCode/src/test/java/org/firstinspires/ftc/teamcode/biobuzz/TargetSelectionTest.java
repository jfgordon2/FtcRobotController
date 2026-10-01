package org.firstinspires.ftc.teamcode.biobuzz;

import org.junit.Test;
import static org.junit.Assert.*;

public class TargetSelectionTest {
    static void release(TargetSelection s) { s.update(false, false, false, false, true); }
    @Test
    public void selectionRequiresExplicitChoicesAndNewPermittedPresses() {
        for (boolean red : new boolean[]{true, false}) {
            for (boolean far : new boolean[]{true, false}) {
                TargetSelection s = new TargetSelection();
                assertTrue("explicit initial selection", !s.ready() && s.clusterName().equals("UNSET"));
                s.update(red, !red, false, false, true);
                assertTrue("alliance alone insufficient", !s.ready());
                s.update(false, false, far, !far, true);
                assertTrue("four cluster mappings", s.clusterName().equals((red ? "RED" : "BLUE") + (far ? " SCORING" : " AUDIENCE")));
                s.lockAlliance(); release(s);
                s.update(!red, red, false, false, true);
                assertTrue("alliance locked", s.allianceLabel().equals(red ? "RED" : "BLUE"));
                release(s);
                s.update(false, false, !far, far, false);
                assertTrue("blocked cell change", s.clusterName().endsWith(far ? "SCORING" : "AUDIENCE"));
                s.update(false, false, !far, far, true);
                assertTrue("held blocked input cannot change later", s.clusterName().endsWith(far ? "SCORING" : "AUDIENCE"));
                release(s); s.update(false, false, !far, far, true);
                assertTrue("released and repressed changes cell", s.clusterName().endsWith(far ? "AUDIENCE" : "SCORING"));
            }
        }
        TargetSelection s = new TargetSelection();
        s.update(true, true, true, true, true);
        assertTrue("contradictory directions ignored", !s.ready());
        release(s); s.lockAlliance(); s.update(true, false, true, false, true);
        assertTrue("unselected start requires restart", !s.ready());
    }
}
