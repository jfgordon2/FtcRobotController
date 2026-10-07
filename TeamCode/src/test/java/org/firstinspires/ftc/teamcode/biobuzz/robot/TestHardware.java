package org.firstinspires.ftc.teamcode.biobuzz.robot;

import com.qualcomm.robotcore.hardware.DcMotor;
import java.lang.reflect.Proxy;

/** Shared test doubles: store requests and let tests supply sensor readings. No robot is used. */
final class TestHardware {
    private TestHardware() { }

    /** A StarterRobot whose drive motors are fakes, for testing code that drives through it. */
    static StarterRobot robot(Motor left, Motor right) {
        StarterRobot robot = new StarterRobot();
        robot.leftDrive = left.device(DcMotor.class);
        robot.rightDrive = right.device(DcMotor.class);
        return robot;
    }

    static final class FakeHeading implements Heading.Source {
        double value;

        @Override
        public double degrees() {
            return value;
        }
    }

    /** Fake motor or servo state. Unimplemented SDK calls fail rather than silently succeeding. */
    static final class Motor {
        int position;
        double power;
        double requestedSpeed;
        double measuredSpeed;
        DcMotor.RunMode mode;

        <T> T device(Class<T> type) {
            return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                    (proxy, method, arguments) -> {
                        switch (method.getName()) {
                            case "setPower":
                                power = (Double) arguments[0];
                                return null;
                            case "getPower":
                                return power;
                            case "setVelocity":
                                requestedSpeed = (Double) arguments[0];
                                return null;
                            case "getVelocity":
                                return measuredSpeed;
                            case "getCurrentPosition":
                                return position;
                            case "setMode":
                                mode = (DcMotor.RunMode) arguments[0];
                                return null;
                            case "getMode":
                                return mode;
                            default:
                                throw new UnsupportedOperationException(method.getName());
                        }
                    }));
        }
    }
}
