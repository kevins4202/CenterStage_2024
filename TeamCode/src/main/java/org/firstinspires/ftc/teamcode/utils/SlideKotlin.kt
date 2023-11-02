package org.firstinspires.ftc.teamcode.utils

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit

class SlideKotlin (hardwareMap: HardwareMap){
    private var Slide1: DcMotorEx = hardwareMap.get("sa") as DcMotorEx //expansion hub: 0
    private var Slide2: DcMotorEx = hardwareMap.get("sb") as DcMotorEx //expansion hub: 1
    var minSlideHeight = -800;
    var targetSlideHeight = -1000;
    var minArmTimeIn = 600;
    var minOuttakeTime = 400;


    init {
        Slide1.direction = DcMotorSimple.Direction.FORWARD
        Slide2.direction = DcMotorSimple.Direction.FORWARD

        Slide1.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        Slide2.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        Slide1.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        Slide2.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        Slide1.mode = DcMotor.RunMode.RUN_USING_ENCODER
        Slide2.mode = DcMotor.RunMode.RUN_USING_ENCODER
    }

    fun setPower (s: Double) {
        Slide1.power = s
        Slide2.power = s
    }
    fun setMode (mode: DcMotor.RunMode) {
        Slide1.mode = mode
        Slide2.mode = mode
    }
    fun setTargetPosition (position: Int) {
        Slide1.targetPosition = position
        Slide2.targetPosition = position
    }
    fun getPosition(): Array<Int> = arrayOf(Slide1.currentPosition, Slide2.currentPosition);
    fun getCurrent(): Array<Double> = arrayOf(Slide1.getCurrent(CurrentUnit.AMPS), Slide2.getCurrent(CurrentUnit.AMPS))
    fun getPower(): Array<Double> = arrayOf(Slide1.power, Slide2.power)
    fun getMode(): Array<DcMotor.RunMode> = arrayOf(Slide1.mode, Slide2.mode)
}