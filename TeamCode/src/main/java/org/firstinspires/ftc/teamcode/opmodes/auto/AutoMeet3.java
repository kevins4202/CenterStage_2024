package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.arcrobotics.ftclib.controller.PIDController;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.VoltageSensor;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.teamcode.roadrunner.drive.CogchampDrive;
import org.firstinspires.ftc.teamcode.roadrunner.trajectorysequence.TrajectorySequence;
import org.firstinspires.ftc.teamcode.utils.Other.Datalogger;
import org.firstinspires.ftc.teamcode.utils.detection.AllianceHelper;
import org.firstinspires.ftc.teamcode.utils.detection.AprilTagAlignment;
import org.firstinspires.ftc.teamcode.utils.detection.ColorDetectionPipeline;
import org.firstinspires.ftc.teamcode.utils.hardware.Intake;
import org.firstinspires.ftc.teamcode.utils.hardware.Outtake;
import org.firstinspires.ftc.teamcode.utils.hardware.Slide;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;

@Autonomous(name = "Auto Meet 3", preselectTeleOp = "MainTeleop", group = "a")
public class AutoMeet3 extends LinearOpMode {
    //Define and declare Robot Starting Locations
    private enum START_POSITION {
        BLUE_CLOSE,
        BLUE_FAR,
        RED_FAR,
        RED_CLOSE,
        UNKNOWN
    }

    private static START_POSITION startPosition = START_POSITION.UNKNOWN;

    private Datalog datalog;
    private AprilTagAlignment aprilTagAlignment;
    private ColorDetectionPipeline colorDetectionPipeline;
    private ColorDetectionPipeline.StartingPosition purplePixelPath = ColorDetectionPipeline.StartingPosition.CENTER;
    private CogchampDrive drive;
    private Intake intake;
    private Outtake outtake;
    private Slide slide;
    private OpenCvCamera frontCamera;
    private WebcamName backCamera;
    private WebcamName frontCameraName;
    private VoltageSensor battery;

    public static double xP = 0.06;
    public static double xI = 0.03;
    public static double xD = 0.0006;
    public static double yP = 0.04;
    public static double yI = 0.04;
    public static double yD = 0.0009;
    public static double headingP = 0.02;
    public static double headingI = 0.035;
    public static double headingD = 0.001;
    private PIDController xPID;
    private PIDController yPID;
    private PIDController headingPID;
    private boolean positionFound = false;

    private TrajectorySequence pathInitToBackboard;
    private TrajectorySequence pathBackboardToWhite;
    private TrajectorySequence pathWhiteToBackboard;
    private TrajectorySequence pathBackboardToPark;

    private Pose2d initPose;
    private Pose2d purplePose;
    private Pose2d aprilTagPose;
    private Pose2d backboardPose;
    private double initTangent;
    private double purpleTangent;
    private double centerBackup = 0;
    private Pose2d whiteDetectionPose;
    private Pose2d preWhitePose;
    private Pose2d whitePixelStackPose;
    private Pose2d postLowerWhitePose;
    private double preLowerWhiteTangent;


    @Override
    public void runOpMode() throws InterruptedException {
        initialize();

        selectStartingPosition();

        colorDetectionPipeline = new ColorDetectionPipeline(AllianceHelper.Companion.getAlliance());

        while(!isStarted()) {
            initLoop();
        }

        waitForStart();

        startAuto();

        while (opModeIsActive()) {
            autoLoop();
        }
    }

    public void initialize() {
        drive = new CogchampDrive(hardwareMap);
        slide = new Slide(hardwareMap);
        intake = new Intake(hardwareMap);
        outtake = new Outtake(hardwareMap, slide);

        xPID = new PIDController(xP, xI, xD);
        yPID = new PIDController(yP, yI, yD);
        headingPID = new PIDController(headingP, headingI, headingD);

        battery = hardwareMap.voltageSensor.get("Control Hub");

        datalog = new Datalog("AutoDatalogger");

        datalog.opModeStatus.set("INIT");
        datalog.battery.set(battery.getVoltage());
        datalog.writeLine();

        outtake.setGateClosed(true);
        outtake.setOuttakeProcedureTarget(Outtake.OuttakePositions.INSIDE);
        outtake.update();

        initColorDetection();

    }

    public void initLoop() {
        detectPurplePath();
        telemetry.addData("Selected Auto: ", startPosition.toString());
        telemetry.update();
    }

    public void startAuto() {
        buildAuto();
        frontCamera.closeCameraDevice();
        initAprilTagDetection();
        datalog.opModeStatus.set("RUNNING");
        drive.setPoseEstimate(initPose);
        if (startPosition == START_POSITION.BLUE_CLOSE || startPosition == START_POSITION.RED_CLOSE) {
            pathInitToBackboard = drive.trajectorySequenceBuilder(initPose)
                    .setTangent(initTangent)
                    .addTemporalMarker(()->{ intake.setServoPosition(Intake.IntakePositions.DRIVE); })
                    .splineToSplineHeading(purplePose, purpleTangent)
                    .back(centerBackup)
                    .addTemporalMarker(this::outtakePurple)
                    .waitSeconds(0.2)
                    .setTangent(0)
                    .splineToLinearHeading(aprilTagPose, Math.toRadians(0))
                    .addTemporalMarker(()->{
                        double currentTime = getRuntime();
                        while(getRuntime() - currentTime < 3) { //TODO find the number of seconds, optimal would be 2 sd over mean time to reach apriltag
                            //TODO might need to disable samplemecanum drive idk tho
                            aprilTagAlignment.alignRobotToBackboard(drive);

                            telemetry.addData("x error","%5.1f inches", aprilTagAlignment.getXError());
                            telemetry.addData("y error","%5.1f inches", aprilTagAlignment.getYError());
                            telemetry.addData("heading error","%3.0f degrees", aprilTagAlignment.getHeadingError());
                            telemetry.addData("drivetrain power", drive.getPoseEstimate());
                            telemetry.update();

                            datalog.xError.set(aprilTagAlignment.getXError());
                            datalog.yError.set(aprilTagAlignment.getYError());
                            datalog.headingError.set(aprilTagAlignment.getHeadingError());
                            datalog.writeLine();

                            if(aprilTagAlignment.getTargetFound()) break;
                        }
                        drive.followTrajectorySequenceAsync(pathBackboardToWhite);
                    })
                    .build();
        } else {
            pathInitToBackboard = drive.trajectorySequenceBuilder(initPose)
                    .setTangent(initTangent)
                    .addTemporalMarker(()->{ intake.setServoPosition(Intake.IntakePositions.DRIVE); })
                    .splineToSplineHeading(purplePose, purpleTangent)
                    .back(centerBackup)
                    .addTemporalMarker(this::outtakePurple)
                    .waitSeconds(0.2)
                    .setTangent(0)
                    .splineToLinearHeading(aprilTagPose, Math.toRadians(0))
                    .addTemporalMarker(()->{
                        double currentTime = getRuntime();
                        while(getRuntime() - currentTime < 3) { //TODO find the number of seconds, optimal would be 2 sd over mean time to reach apriltag
                            //TODO might need to disable samplemecanum drive idk tho
                            aprilTagAlignment.alignRobotToBackboard(drive);

                            telemetry.addData("x error","%5.1f inches", aprilTagAlignment.getXError());
                            telemetry.addData("y error","%5.1f inches", aprilTagAlignment.getYError());
                            telemetry.addData("heading error","%3.0f degrees", aprilTagAlignment.getHeadingError());
                            telemetry.addData("drivetrain power", drive.getPoseEstimate());
                            telemetry.update();

                            datalog.xError.set(aprilTagAlignment.getXError());
                            datalog.yError.set(aprilTagAlignment.getYError());
                            datalog.headingError.set(aprilTagAlignment.getHeadingError());
                            datalog.writeLine();

                            if(aprilTagAlignment.getTargetFound()) break;
                        }
                        drive.followTrajectorySequenceAsync(pathBackboardToWhite);
                    })
                    .build();
        }

        pathBackboardToWhite = drive.trajectorySequenceBuilder(backboardPose)
                .addTemporalMarker(this::outtake)
                .waitSeconds(0.5)
                .addTemporalMarker(()->{ setSlideHeight(-240); })
                .waitSeconds(0.2)
                .back(3.5)
                .waitSeconds(0.3)
                .addTemporalMarker(this::drop)
                .waitSeconds(0.4)
                .addTemporalMarker(this::outtakeIn)
                .forward(10)
                .waitSeconds(0.7)
                .setTangent(preLowerWhiteTangent)
                .addTemporalMarker(()->{ intake.setServoPosition(Intake.IntakePositions.FIVE); })
                .splineToSplineHeading(whiteDetectionPose, Math.toRadians(180))
                .addTemporalMarker(() -> {
                    //TODO add white detection
                    drive.followTrajectorySequenceAsync(pathWhiteToBackboard);
                })
                .build();

        pathWhiteToBackboard = drive.trajectorySequenceBuilder(preWhitePose)
                .addTemporalMarker(this::intake)
                .splineToSplineHeading(whitePixelStackPose, Math.toRadians(180))
                .addTemporalMarker(this::transfer)
                .waitSeconds(.6)
                .back(4)
                .waitSeconds(0.7)
                .addTemporalMarker(this::outtakeIn)
                .setTangent(0)
                .splineToSplineHeading(postLowerWhitePose, 0)
                .splineToSplineHeading(aprilTagPose, Math.toRadians(45))
                .addTemporalMarker(this::outtake)
                .waitSeconds(0.5)
                .addTemporalMarker(()->{ setSlideHeight(-400); })
                .waitSeconds(0.2)
                .back(6)
                .waitSeconds(0.6)
                .addTemporalMarker(this::drop)
                .waitSeconds(0.4)
                .addTemporalMarker(this::outtakeIn)
                .forward(7)
                .build();

        drive.followTrajectorySequenceAsync(pathInitToBackboard);
    }

    public void autoLoop() {
        drive.update();
        outtake.update();
        intake.update();
        slide.update();
        telemetry.update();
    }

    private void initColorDetection() {
        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        frontCameraName = hardwareMap.get(WebcamName.class, "Webcam 2");
        frontCamera = OpenCvCameraFactory.getInstance().createWebcam(frontCameraName, cameraMonitorViewId);
        frontCamera.setPipeline(colorDetectionPipeline);
        frontCamera.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener() {
            @Override
            public void onOpened() {
                frontCamera.startStreaming(320, 240, OpenCvCameraRotation.UPRIGHT);
            }
            @Override
            public void onError(int errorCode) {}
        });
    }
    private void detectPurplePath() {
        purplePixelPath = colorDetectionPipeline.getPosition();
        telemetry.addData("Purple Pixel Path: ", purplePixelPath.toString());
    }

    private void initAprilTagDetection() {
        backCamera = hardwareMap.get(WebcamName.class, "Webcam 1");
        aprilTagAlignment = new AprilTagAlignment(backCamera, 0.0, 12.0, 0.0, AllianceHelper.Companion.getAlliance(), (xPID), (yPID), (headingPID));
    }

    public void buildAuto() {
        switch (startPosition) {
            case BLUE_CLOSE:
                initPose = new Pose2d(16, 63, Math.toRadians(270));
                purplePose = new Pose2d(-63, -48, Math.toRadians(0));
                purpleTangent = Math.toRadians(210);
                initTangent = Math.toRadians(300);
                whiteDetectionPose = new Pose2d(24,11, Math.toRadians(180));
                whitePixelStackPose = new Pose2d(-57,-6.5, Math.toRadians(180));//This had better be the same every time TY
                postLowerWhitePose = new Pose2d(28, 10, Math.toRadians(180));
                preLowerWhiteTangent = 225;
                switch (purplePixelPath) {
                    case LEFT:
                        purplePose = new Pose2d(11,-32, Math.toRadians(180));
                        aprilTagPose = new Pose2d(-63, -48, Math.toRadians(0)); // TODO see below
                        backboardPose = new Pose2d(-65, -48, Math.toRadians(0)); // TODO see below
                        centerBackup = 1;
                        break;
                    case CENTER:
                        purplePose = new Pose2d(23,-24.2, Math.toRadians(180));
                        aprilTagPose = new Pose2d(-63, -48, Math.toRadians(0)); // TODO see below
                        backboardPose = new Pose2d(-65, -48, Math.toRadians(0)); // TODO see below
                        centerBackup = 1;
                        break;
                    case RIGHT:
                        purplePose = new Pose2d(25,-32, Math.toRadians(180));
                        aprilTagPose = new Pose2d(-63, -48, Math.toRadians(0)); // TODO see below
                        backboardPose = new Pose2d(-65, -48, Math.toRadians(0)); // TODO see below
                        centerBackup = 1;
                        break;
                }
                break;
            case RED_CLOSE:
                initPose = new Pose2d(11, -63, Math.toRadians(90));
                purplePose = new Pose2d(-63, -48, Math.toRadians(0));
                purpleTangent = Math.toRadians(150);
                initTangent = Math.toRadians(90);
                whiteDetectionPose = new Pose2d(24,-10, Math.toRadians(180));
                whitePixelStackPose = new Pose2d(-57,16, Math.toRadians(180));
                preWhitePose = new Pose2d(24, -10, Math.toRadians(180));
                postLowerWhitePose = new Pose2d(28, -10, Math.toRadians(180));
                preLowerWhiteTangent = 135;
                switch (purplePixelPath) {
                    case LEFT:
                        purplePose = new Pose2d(11,-32, Math.toRadians(180));
                        aprilTagPose = new Pose2d(54, -48, Math.toRadians(0)); // *this is below* TODO adjust for april tag estimate to get tag in frame
                        backboardPose = new Pose2d(54, -48, Math.toRadians(0)); // TODO see below
                        centerBackup = 3.5; // FIX THIS POOP
                        break;
                    case CENTER:
                        purplePose = new Pose2d(23,-24.2, Math.toRadians(180));
                        aprilTagPose = new Pose2d(54, -48, Math.toRadians(0)); // TODO adjust for april tag estimate to get tag in frame
                        backboardPose = new Pose2d(54, -48, Math.toRadians(0)); // TODO see below
                        centerBackup = 3.5; // FIX THIS POOP
                        break;
                    case RIGHT:
                        purplePose = new Pose2d(12.5,-30, Math.toRadians(180));
                        aprilTagPose = new Pose2d(54, -48, Math.toRadians(0)); // TODO adjust for april tag estimate to get tag in frame
                        backboardPose = new Pose2d(54, -48, Math.toRadians(0)); // TODO see below
                        centerBackup = 3.5-8; // FIX THIS POOP
                        break;
                }
                break;
            case BLUE_FAR:
                // TODO: Write auto
                break;
            case RED_FAR:
                // TODO: Write auto
                break;
        }
    }

    private void intake() {
        intake.intake(.75);
    }

    private void outtakePurple() {
        intake.setMotorTargetPosition(250);
        intake.setMotorMode(DcMotor.RunMode.RUN_TO_POSITION);
        intake.setMotorPower(0.5);
        intake.update();
    }

    private void outtake() {
        outtake.setOuttakeProcedureTarget(Outtake.OuttakePositions.OUTSIDE);
        setSlideHeight(-1200);
    }

    private void outtakeIn() {
        if(outtake.getOuttakePosition() == Outtake.OuttakePositions.OUTSIDE)
            setSlideHeight(-1200);
        outtake.setOuttakeProcedureTarget(Outtake.OuttakePositions.INSIDE);
    }

    private void drop() {
        outtake.setGateClosed(false);
    }

    private void setSlideHeight(int height) {
        slide.setTargetPosition(height);
        slide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        slide.setPower(1.0);
    }

    private void transfer() {
        intake.transfer();
    }

    //Method to select starting position using dpad on gamepad
    public void selectStartingPosition() {
        telemetry.setAutoClear(true);
        //******select start pose*****
        while (!positionFound) {
            telemetry.addLine("Auto Meet 3 Initialized");
            telemetry.addData("---------------------------------------", "");
            telemetry.addLine("Select Starting Position using DPAD Keys");
            telemetry.addData("    Blue Left   ", "(^)");
            telemetry.addData("    Blue Right ", "(v)");
            telemetry.addData("    Red Left    ", "(<)");
            telemetry.addData("    Red Right  ", "(>)");

            if (gamepad1.dpad_up || gamepad2.dpad_up) {
                startPosition = START_POSITION.BLUE_CLOSE;
                AllianceHelper.alliance = AprilTagAlignment.Alliance.BLUE;
                positionFound = true;
            }
            if (gamepad1.dpad_down || gamepad2.dpad_down) {
                startPosition = START_POSITION.BLUE_FAR;
                AllianceHelper.alliance = AprilTagAlignment.Alliance.BLUE;
                positionFound = true;
            }
            if (gamepad1.dpad_left || gamepad2.dpad_left) {
                startPosition = START_POSITION.RED_FAR;
                AllianceHelper.alliance = AprilTagAlignment.Alliance.BLUE;
                positionFound = true;
            }
            if (gamepad1.dpad_right || gamepad2.dpad_left) {
                startPosition = START_POSITION.RED_CLOSE;
                AllianceHelper.alliance = AprilTagAlignment.Alliance.BLUE;
                positionFound = true;
            }
        }
    }
    public static class Datalog
    {
        // The underlying datalogger object - it cares only about an array of loggable fields
        private final Datalogger datalogger;

        // These are all of the fields that we want in the datalog.
        // Note that order here is NOT important. The order is important in the setFields() call below
        public Datalogger.GenericField opModeStatus = new Datalogger.GenericField("OpModeStatus");
        public Datalogger.GenericField loopCounter  = new Datalogger.GenericField("Loop Counter");
        public Datalogger.GenericField xError       = new Datalogger.GenericField("X Error");
        public Datalogger.GenericField yError       = new Datalogger.GenericField("Y Error");
        public Datalogger.GenericField headingError = new Datalogger.GenericField("Heading Error");
        public Datalogger.GenericField battery      = new Datalogger.GenericField("Battery");

        public Datalog(String name)
        {
            // Build the underlying datalog object
            datalogger = new Datalogger.Builder()

                    // Pass through the filename
                    .setFilename(name)

                    // Request an automatic timestamp field
                    .setAutoTimestamp(Datalogger.AutoTimestamp.DECIMAL_SECONDS)

                    // Tell it about the fields we care to log.
                    // Note that order *IS* important here! The order in which we list
                    // the fields is the order in which they will appear in the log.
                    .setFields(
                            opModeStatus,
                            loopCounter,
                            xError,
                            yError,
                            headingError,
                            battery
                    )
                    .build();
        }

        // Tell the datalogger to gather the values of the fields
        // and write a new line in the log.
        public void writeLine()
        {
            datalogger.writeLine();
        }
    }
}
