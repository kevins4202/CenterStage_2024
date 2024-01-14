package org.firstinspires.ftc.teamcode.testers;

import com.arcrobotics.ftclib.controller.PIDController;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.teamcode.utils.detection.AprilTagAlignment;
import org.firstinspires.ftc.teamcode.utils.hardware.Drivetrain;

import java.util.Arrays;
import java.util.Collections;

@TeleOp(name = "AprilTagTester")
public class AprilTagAlignmentTester extends LinearOpMode {
    private Drivetrain drivetrain;
    private WebcamName webcam;
    private AprilTagAlignment aprilTagAlignment;
    private boolean dpadLeftToggle = false;
    private boolean dpadRightToggle = false;

    @Override
    public void runOpMode() throws InterruptedException {

        drivetrain = new Drivetrain(hardwareMap);
        webcam = hardwareMap.get(WebcamName.class, "Webcam 1");

        aprilTagAlignment = new AprilTagAlignment(webcam, drivetrain, 0.0, 12.0, 0.0,
            (new PIDController(0.0174, 0.0, 0.0)), //x PID controller
            (new PIDController(0.0174, 0.0, 0.0)), //y PID controller
            (new PIDController(0.0174, 0.0, 0.0))); //heading PID controller

        waitForStart();

        while (opModeIsActive()) {
            aprilTagAlignment.update();

            if(gamepad1.left_bumper) {
                aprilTagAlignment.alignRobot();
            }
            else
                driveNormal();

            if(gamepad1.dpad_left && !dpadLeftToggle && aprilTagAlignment.getTargetTagID() > 1) {
                aprilTagAlignment.setTargetTagID(aprilTagAlignment.getTargetTagID()-1);
                dpadLeftToggle = true;
            }
            else if(!gamepad1.dpad_left && dpadLeftToggle)
                dpadLeftToggle = false;

            if(gamepad1.dpad_right && !dpadRightToggle && aprilTagAlignment.getTargetTagID() < 6) {
                aprilTagAlignment.setTargetTagID(aprilTagAlignment.getTargetTagID()+1);
                dpadRightToggle = true;
            }
            else if(!gamepad1.dpad_right && dpadRightToggle)
                dpadRightToggle = false;

            telemetry.addData("targetTagID", aprilTagAlignment.getTargetTagID());
            telemetry.addData("targetFound", aprilTagAlignment.getTargetFound());
            telemetry.addData("x error","%5.1f inches", aprilTagAlignment.getXError());
            telemetry.addData("y error","%5.1f inches", aprilTagAlignment.getYError());
            telemetry.addData("heading error","%3.0f degrees", aprilTagAlignment.getHeadingError());
            telemetry.addData("drivetrain power", Collections.max(Arrays.asList(drivetrain.getMotorPower())));
            telemetry.update();
        }
    }
    private void driveNormal() {
        double speedMult = .7+0.3 * gamepad1.right_trigger-0.3*gamepad1.left_trigger;

        gamepad1.rumble(gamepad1.left_trigger>0.5?(gamepad1.left_trigger-0.5)/.4:0.0,gamepad1.right_trigger>0.4?(gamepad1.right_trigger-0.4)/0.8:0.0,50);

        double forwardMult = 1;
        double turnMult = .75;
        double strafeMult = 1;

        double forward = gamepad1.left_stick_y * forwardMult * speedMult;
        double turn = -gamepad1.right_stick_x * turnMult * speedMult;
        double strafe = -gamepad1.left_stick_x * strafeMult * speedMult;

        drivetrain.move(forward, strafe, turn);
    }
}
