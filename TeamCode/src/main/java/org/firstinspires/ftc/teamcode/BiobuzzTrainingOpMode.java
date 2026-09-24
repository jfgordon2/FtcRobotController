package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name="BIOBUZZ Training", group="Training")
@Disabled
public class BiobuzzTrainingOpMode extends LinearOpMode {
    @Override
    public void runOpMode() {
        telemetry.addData("Status", "Initialized - waiting for Start");
        telemetry.addData("Output", "0 - no actuator commands");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            telemetry.addData("Pair", "PAIR_NAME");
            telemetry.addData("Output", "0 - no actuator commands");
            telemetry.update();
            idle();
        }
    }
}
