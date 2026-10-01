package org.firstinspires.ftc.teamcode.biobuzz;

/**
 * Stores named route cards and their ordered drive/turn steps; it does not move the robot.
 * StarterBotPositionAuto selects a card and sends each step to StarterDrive.
 * All supplied distances are PRACTICE VALUES until measured on the robot and field.
 */
public final class AutoRoutes {
    public enum Mode {
        DRY_RUN, SHOOT_AND_PARK, PARK_ONLY
    }

    /** One requested movement: either inches of travel or degrees of turning. */
    public static final class Step {
        public final boolean turn;
        public final double value;

        private Step(boolean turn, double value) {
            this.turn = turn;
            this.value = value;
        }

        public static Step drive(double inches) {
            return new Step(false, inches);
        }

        public static Step turn(double degrees) {
            return new Step(true, degrees);
        }

        @Override
        public String toString() {
            if (turn) {
                return "Turn degrees " + value;
            }
            return "Drive inches " + value;
        }
    }

    /** Ordered steps and calibration information for one chosen start and destination. */
    public static final class Route {
        public final String name;
        public final String startDescription;
        public final boolean qualified;
        public final Step[] approach;
        public final Step[] park;
        // Optional: replace with a measured snapshot for THIS card before enabling.
        public StartTagCheck startTag = new StartTagCheck(false, 48, 0);

        public Route(String name, boolean qualified, Step[] approach, Step[] park) {
            this.name = name;
            if (name.contains("start B")) {
                startDescription = "Candidate B: opposite-audience perimeter, front inward; verify exact legal mark";
            } else {
                startDescription = "Candidate A: audience perimeter, front inward; verify exact legal mark";
            }
            this.qualified = qualified;
            this.approach = approach;
            this.park = park;
        }

        public double parkReserveSeconds() {
            // Three bounded reverse tag phases, then the fixed parking segments and a margin.
            return park.length * DriveMath.COMMAND_TIMEOUT_SECONDS
                    + 3 * TagApproach.RECOVERY_STEP_SECONDS + 1.0;
        }
    }

    // Independent cards: do not assume changing alliance or cell mirrors a route correctly.
    // Start A/B and Park A/B are TEAM marks to define on the route worksheet, not official zones.
    // card(name, qualified, approachInches, parkingTurnDegrees, parkingInches)
    // Example: false, 24, 90, 24 means an unqualified route: forward 24 inches,
    // then (after the tag action and return) turn right 90 degrees and drive 24 inches.
    public static final Route RED_FAR_A = card("RED start A -> far shot A -> park A", false, 24, 90, 24);
    public static final Route RED_FAR_B = card("RED start B -> far shot B -> park B", false, 18, -90, 30);
    public static final Route RED_AUDIENCE_A = card("RED start A -> audience shot A -> park A", false, 24, -90, 24);
    public static final Route RED_AUDIENCE_B = card("RED start B -> audience shot B -> park B", false, 18, 90, 30);
    public static final Route BLUE_FAR_A = card("BLUE start A -> far shot A -> park A", false, 24, -90, 24);
    public static final Route BLUE_FAR_B = card("BLUE start B -> far shot B -> park B", false, 18, 90, 30);
    public static final Route BLUE_AUDIENCE_A = card("BLUE start A -> audience shot A -> park A", false, 24, 90, 24);
    public static final Route BLUE_AUDIENCE_B = card("BLUE start B -> audience shot B -> park B", false, 18, -90, 30);

    private static Route card(String name, boolean qualified, double approachInches,
                              double parkingTurnDegrees, double parkingInches) {
        // An array holds steps in the order the robot will carry them out.
        Step[] approachSteps = {Step.drive(approachInches)};
        Step[] parkingSteps = {Step.turn(parkingTurnDegrees), Step.drive(parkingInches)};
        return new Route(name, qualified, approachSteps, parkingSteps);
    }

    public static Route select(String cluster, boolean startB) {
        switch (cluster) {
            case "RED SCORING":
                if (startB) {
                    return RED_FAR_B;
                }
                return RED_FAR_A;
            case "RED AUDIENCE":
                if (startB) {
                    return RED_AUDIENCE_B;
                }
                return RED_AUDIENCE_A;
            case "BLUE SCORING":
                if (startB) {
                    return BLUE_FAR_B;
                }
                return BLUE_FAR_A;
            case "BLUE AUDIENCE":
                if (startB) {
                    return BLUE_AUDIENCE_B;
                }
                return BLUE_AUDIENCE_A;
            default:
                return null;
        }
    }

    private AutoRoutes() {
    }
}
