package org.firstinspires.ftc.teamcode.biobuzz;

/**
 * A snapshot of wheel encoder counts and heading at a boundary between tag movements.
 * TagDrive saves these values to reverse the measured movements after aiming or shooting.
 * This stores measurements only; it does not move hardware or represent a field position.
 */
final class DriveCheckpoint {
    final int leftTicks;
    final int rightTicks;
    final double headingDegrees;

    DriveCheckpoint(int leftTicks, int rightTicks, double headingDegrees) {
        this.leftTicks = leftTicks;
        this.rightTicks = rightTicks;
        this.headingDegrees = headingDegrees;
    }
}
