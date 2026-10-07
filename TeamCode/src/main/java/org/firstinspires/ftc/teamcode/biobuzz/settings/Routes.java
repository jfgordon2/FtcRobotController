package org.firstinspires.ftc.teamcode.biobuzz.settings;

/**
 * The autonomous routes. Each route is one list of steps, read from top to bottom.
 * This file only stores the lists. AutoFollowRoute carries them out one step at a time.
 *
 * The three kinds of step:
 *   drive(inches)   positive drives forward, negative drives backward
 *   turn(degrees)   positive turns right (clockwise), negative turns left
 *   aimAndShoot()   turn to face the chosen tags, shoot, then turn back to where it was facing
 *
 * A route with no aimAndShoot() line just drives and parks.
 * Every drive or turn must finish within 4 seconds, so split a long drive into two lines.
 * All numbers below are PRACTICE VALUES until we measure them on the robot and the field.
 */
public final class Routes {
    // There is one route for each alliance, each cell, and each of our two starting marks.
    // Start A and start B are OUR OWN marks from the route worksheet, not official field zones.
    // Each route is separate: changing alliance or cell does not simply mirror another route.
    public static final Step[] RED_AUDIENCE_A = {
            drive(24),
            aimAndShoot(),
            turn(-90),
            drive(24),
    };
    public static final Step[] RED_AUDIENCE_B = {
            drive(18),
            aimAndShoot(),
            turn(90),
            drive(30),
    };
    public static final Step[] RED_SCORING_A = {
            drive(24),
            aimAndShoot(),
            turn(90),
            drive(24),
    };
    public static final Step[] RED_SCORING_B = {
            drive(18),
            aimAndShoot(),
            turn(-90),
            drive(30),
    };
    public static final Step[] BLUE_AUDIENCE_A = {
            drive(24),
            aimAndShoot(),
            turn(90),
            drive(24),
    };
    public static final Step[] BLUE_AUDIENCE_B = {
            drive(18),
            aimAndShoot(),
            turn(-90),
            drive(30),
    };
    public static final Step[] BLUE_SCORING_A = {
            drive(24),
            aimAndShoot(),
            turn(-90),
            drive(24),
    };
    public static final Step[] BLUE_SCORING_B = {
            drive(18),
            aimAndShoot(),
            turn(90),
            drive(30),
    };

    /** The route for the tags chosen in INIT ("RED AUDIENCE", "BLUE SCORING", ...) and the start mark. */
    public static Step[] select(String target, boolean startB) {
        switch (target) {
            case "RED AUDIENCE":
                return startB ? RED_AUDIENCE_B : RED_AUDIENCE_A;
            case "RED SCORING":
                return startB ? RED_SCORING_B : RED_SCORING_A;
            case "BLUE AUDIENCE":
                return startB ? BLUE_AUDIENCE_B : BLUE_AUDIENCE_A;
            case "BLUE SCORING":
                return startB ? BLUE_SCORING_B : BLUE_SCORING_A;
            default:
                return null;
        }
    }

    public enum Kind { DRIVE, TURN, AIM_AND_SHOOT }

    /** One line of a route: what to do and, for a drive or turn, how much. */
    public static final class Step {
        public final Kind kind;
        public final double amount;

        private Step(Kind kind, double amount) {
            this.kind = kind;
            this.amount = amount;
        }

        @Override
        public String toString() {
            switch (kind) {
                case DRIVE:
                    return "drive " + amount + " in";
                case TURN:
                    return "turn " + amount + " deg";
                default:
                    return "aim and shoot";
            }
        }
    }

    static Step drive(double inches) {
        return new Step(Kind.DRIVE, inches);
    }

    static Step turn(double degrees) {
        return new Step(Kind.TURN, degrees);
    }

    static Step aimAndShoot() {
        return new Step(Kind.AIM_AND_SHOOT, 0);
    }

    private Routes() {
    }
}
