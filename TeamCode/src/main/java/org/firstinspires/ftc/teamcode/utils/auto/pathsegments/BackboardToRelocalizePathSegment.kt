package org.firstinspires.ftc.teamcode.utils.auto.pathsegments

import org.firstinspires.ftc.teamcode.roadrunner.drive.CogchampDrive
import org.firstinspires.ftc.teamcode.roadrunner.trajectorysequence.TrajectorySequenceBuilder
import org.firstinspires.ftc.teamcode.utils.Robot
import org.firstinspires.ftc.teamcode.utils.auto.PoseHelper
import org.firstinspires.ftc.teamcode.utils.hardware.Intake
import org.firstinspires.ftc.teamcode.utils.hardware.Outtake
import org.firstinspires.ftc.teamcode.utils.hardware.Slide

class BackboardToRelocalizePathSegment(
    override val robot: Robot
) : RoadrunnerPathSegment(robot) {
    override lateinit var trajectorySequenceBuilder: TrajectorySequenceBuilder

    override fun buildPathSegment() {
        trajectorySequenceBuilder = robot.drive.trajectorySequenceBuilder(PoseHelper.currentPose)
            .back(PoseHelper.backboardBackup)
            .addTemporalMarker(this::drop)
            .waitSeconds(0.4)
            .addTemporalMarker(this::outtakeIn)
            .addTemporalMarker { setSlideHeight(-1200) }
            .splineToConstantHeading(PoseHelper.aprilTruss.vec(), Math.toRadians(180.0))
    }
}