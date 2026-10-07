package org.firstinspires.ftc.teamcode.biobuzz.settings;

import org.junit.Test;
import static org.junit.Assert.*;

public class SpeedTableTest {
    /** A made-up table, so these tests do not depend on the rows the team has typed in. */
    private static final class Table {
        final double[] ranges, speeds;
        Table(double[] ranges, double[] speeds) { this.ranges = ranges; this.speeds = speeds; }
        double speedFor(double range) { return ShooterCalibration.speedFor(range, ranges, speeds, 1800); }
        String problem() { return ShooterCalibration.problem(ranges, speeds, 1800); }
        boolean valid() { return problem().isEmpty(); }
    }
    private Table table(double[] ranges, double[] speeds) {
        return new Table(ranges, speeds);
    }

    @Test public void interpolatesOnlyInsideMeasuredRange() {
        Table t = table(new double[]{30,36,48}, new double[]{1000,1100,1400});
        assertTrue(t.valid());
        assertEquals(1000, t.speedFor(30), 1e-9);
        assertEquals(1050, t.speedFor(33), 1e-9);
        assertEquals(1100, t.speedFor(36), 1e-9);
        assertEquals(1250, t.speedFor(42), 1e-9);
        assertEquals(1400, t.speedFor(48), 1e-9);
        for (double range : new double[]{29,49,Double.NaN,Double.POSITIVE_INFINITY}) {
            assertTrue(Double.isNaN(t.speedFor(range)));
        }
    }

    @Test public void rejectsEmptyIncompleteDuplicateUnsortedAndInvalidRows() {
        assertFalse(table(new double[]{},new double[]{}).valid());
        assertFalse(table(new double[]{30},new double[]{1000}).valid());
        assertFalse(table(new double[]{30,36},new double[]{1000}).valid());
        for (double[] ranges : new double[][]{{30,30},{36,30},{0,36},{30,Double.NaN}}) {
            assertFalse(table(ranges,new double[]{1000,1100}).valid());
        }
        for (double[] speeds : new double[][]{{0,1100},{-1000,1100},{1000,1801},{1000,Double.NaN}}) {
            assertTrue(Double.isNaN(table(new double[]{30,36},speeds).speedFor(33)));
        }
        assertFalse(table(new double[]{},new double[]{}).problem().isEmpty());
    }

    @Test public void theTeamsOwnTableAnswersWithoutCrashing() {
        // It may be empty or filled in. Either way these must return an answer.
        ShooterCalibration.covers(40);
        ShooterCalibration.speedFor(40);
        assertNotNull(ShooterCalibration.tableProblem());
    }
}
