package org.firstinspires.ftc.teamcode.biobuzz;

/**
 * Drive calculations for classroom exercises, plus the completion timer used by StarterDrive.
 * The encoder-only turn formulas help explain wheel geometry; actual turns use the IMU.
 * Static calculation methods need no object. Completion needs an object to remember time.
 */
public final class DriveMath {
    public static final double COMMAND_TIMEOUT_SECONDS = 4.0;

    private DriveMath() {
    }

    public static double turnWheelInches(double degrees, double trackInches) {
        return Math.PI * trackInches * degrees / 360.0;
    }

    public static double headingDegrees(double leftTicks, double rightTicks,
            double ticksPerInch, double trackInches) {
        return Math.toDegrees((leftTicks - rightTicks) / ticksPerInch / trackInches);
    }

    public static final class Completion {
        // NaN (Not a Number) marks that no continuous settling period has started.
        private double settled = Double.NaN;
        /** True only after the condition stays true for a full quarter second. */
        public boolean settledForRequiredTime(double now, boolean withinTolerance) {
            if (!withinTolerance) {
                settled = Double.NaN;
                return false;
            }
            if (Double.isNaN(settled)) {
                settled = now;
            }
            return now - settled >= 0.25;
        }
    }
}
