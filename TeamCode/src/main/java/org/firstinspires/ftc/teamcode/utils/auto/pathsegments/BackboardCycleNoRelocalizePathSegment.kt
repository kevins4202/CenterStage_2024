package org.firstinspires.ftc.teamcode.utils.auto.pathsegments

import org.firstinspires.ftc.teamcode.roadrunner.trajectorysequence.TrajectorySequenceBuilder
import org.firstinspires.ftc.teamcode.utils.Robot
import org.firstinspires.ftc.teamcode.utils.auto.PoseHelper
import org.firstinspires.ftc.teamcode.utils.detection.AllianceHelper
import org.firstinspires.ftc.teamcode.utils.hardware.Intake

class BackboardCycleNoRelocalizePathSegment(
    override val robot: Robot,
    val cycle: Int
) : RoadrunnerPathSegment(robot) {
    override lateinit var trajectorySequenceBuilder: TrajectorySequenceBuilder

    override fun buildPathSegment() {
        trajectorySequenceBuilder = robot.drive.trajectorySequenceBuilder(PoseHelper.currentPose)
            .back(PoseHelper.backboardBackup)
            .addTemporalMarker(this::drop)
            .waitSeconds(0.4)
            .addTemporalMarker(this::outtakeIn)
            .addTemporalMarker { setSlideHeight(-1200) }
            .splineToConstantHeading(PoseHelper.boardTruss.vec(), Math.toRadians(180.0))
            .addTemporalMarker {
                if (PoseHelper.path == PoseHelper.Path.OUTSIDE)
                    robot.intake.servoPosition = Intake.IntakePositions.THREE
                else if(cycle == 0)
                    robot.intake.servoPosition = Intake.IntakePositions.FOUR
                else
                    robot.intake.servoPosition = Intake.IntakePositions.TWO
                robot.intake.update();
            }
            .splineToConstantHeading(PoseHelper.wingTruss.vec(), Math.toRadians(180.0))
            .addTemporalMarker(::intake)
            .splineToSplineHeading(
                if(PoseHelper.path == PoseHelper.Path.OUTSIDE) PoseHelper.stackPose.plus(PoseHelper.stackOffset.times(PoseHelper.allianceAngleMultiplier)) else PoseHelper.stackPose,
                Math.toRadians(if (PoseHelper.path == PoseHelper.Path.INSIDE) 180.0 else 120.0 * PoseHelper.allianceAngleMultiplier)
            )
            //TO WHITE
            .setVelConstraint(PoseHelper.toBackboardVelocityConstraint)
            .setAccelConstraint(PoseHelper.toBackboardAccelerationConstraint)
            .addTemporalMarker(this::outtakeTransfer)
            .forward(1.5)
            .back(3.0)
            //TO BACKBOARD
            .addTemporalMarker(this::stopIntake).addTemporalMarker(this::transfer)
            .setTangent(Math.toRadians(0.0))
            .splineToConstantHeading(PoseHelper.wingTruss.vec(), Math.toRadians(0.0))
            .splineToConstantHeading(PoseHelper.boardTruss.vec(), Math.toRadians(0.0))
            .addTemporalMarker(this::outtake).addTemporalMarker { setSlideHeight(-1500) }
            .splineToSplineHeading(
                if (AllianceHelper.alliance == AllianceHelper.Alliance.RED) PoseHelper.backboardCenterRed else PoseHelper.backboardCenterBlue,
                Math.toRadians(if (PoseHelper.path == PoseHelper.Path.OUTSIDE) 30.0 else -30.0 * PoseHelper.allianceAngleMultiplier)
            )
            .addTemporalMarker {
                if (PoseHelper.path == PoseHelper.Path.INSIDE && cycle == 0) {
                    if (PoseHelper.backboardPose == PoseHelper.backboardLeftRed || PoseHelper.backboardPose == PoseHelper.backboardRightBlue) {
                        if (AllianceHelper.alliance == AllianceHelper.Alliance.RED) PoseHelper.backboardPose =
                            PoseHelper.backboardCenterRed
                        else PoseHelper.backboardPose = PoseHelper.backboardCenterBlue
                    } else {
                        if (AllianceHelper.alliance == AllianceHelper.Alliance.RED) PoseHelper.backboardPose =
                            PoseHelper.backboardLeftRed
                        else PoseHelper.backboardPose = PoseHelper.backboardRightBlue
                    }
                }
            }
    }

    override fun toString() = "BackboardToRelocalizePathSegment"
}