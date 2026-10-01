package org.firstinspires.ftc.teamcode.biobuzz;

/**
 * The one reading StarterDrive needs from a heading sensor: clockwise-positive degrees.
 * An interface is a promise that an object provides this method.
 * HubHeading reads the real IMU (inertial measurement unit, our heading sensor); tests provide a fake reading through the same interface.
 * Return NaN (Not a Number) when the heading is missing, stale, or unusable.
 */
public interface HeadingSource {
    double degrees();
}
