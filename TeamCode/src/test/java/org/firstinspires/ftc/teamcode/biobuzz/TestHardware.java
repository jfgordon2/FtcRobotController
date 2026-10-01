package org.firstinspires.ftc.teamcode.biobuzz;

import com.qualcomm.robotcore.hardware.DcMotor;
import java.lang.reflect.Proxy;

/** Shared test doubles: store requests and let tests supply sensor readings. No robot is used. */
final class TestHardware {
    private TestHardware() { }

    static final class Heading implements HeadingSource {
        double value;

        @Override
        public double degrees() {
            return value;
        }
    }

    /** Fake motor or servo state. Unimplemented SDK calls fail rather than silently succeeding. */
    static final class Motor {
        int position;
        int target;
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
                            case "setTargetPosition":
                                target = (Integer) arguments[0];
                                return null;
                            case "getTargetPosition":
                                return target;
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
