package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

@TeleOp(name = "M2 One Motor Test", group = "BIOBUZZ")
@Disabled
public class Meeting2OneMotor extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        // STUDENT: Replace only the text inside these quotation marks.
        DcMotor testMotor = hardwareMap.get(DcMotor.class, "REPLACE_WITH_DRIVER_STATION_NAME");
        testMotor.setPower(0.0);
        
        telemetry.addData("Current mode", testMotor.getMode());
        telemetry.addLine("Ready. Check the supported robot before Start.");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            // gamepad1 is inherited from the FTC SDK's OpMode class.
            if (gamepad1.x) {
                testMotor.setPower(0.25);
            } else {
                testMotor.setPower(0.0);
            }

            telemetry.addData("X held", gamepad1.x);
            telemetry.update();
        }

        testMotor.setPower(0.0);
    }
}
