package org.firstinspires.ftc.teamcode.utils

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.ServoImplEx

class IntakeKotlin (hardwareMap: HardwareMap){


    private var intakeServo: ServoImplEx = hardwareMap.get("is") as ServoImplEx //control hub: 0
    private var outtakeServo: ServoImplEx = hardwareMap.get("os") as ServoImplEx //control hub: 1
    private var armServo: ServoImplEx = hardwareMap.get("as") as ServoImplEx //control hub: 2
    private var intakeMotor: DcMotorEx = hardwareMap.get("im") as DcMotorEx  //expansion hub: 2

    private var crossPressed = false;
    private var trianglePressed = false;

    private var intakeStart: Double = 0.4
    private var intakeEnd: Double = 0.0
    private var intakePositions: Array<Double> = arrayOf<Double>(0.0,0.2,0.4,0.6,0.8); //array of positions for the intake servo to go to

    private var outtakeClosed: Double = 1.0 //closed position
    private var outtakeOpen: Double = 0.91 //open position
    private var outtake: Boolean = true;

    private var armOut: Double = 1.0
    private var armIn: Double = 0.0
    private var arm: Boolean = false;

    init {
        intakeServo.position = intakeStart
        outtakeServo.position = outtakeOpen
        armServo.position = armOut
        intakeMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        intakeMotor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
    }

    fun intakeServo(position: Int) {
        intakeServo.position = intakePositions[position]
    }
    fun outtake (){ //toggles between open and closed positions
        if (outtake) {
            outtake = false
            outtakeServo.position = outtakeClosed
        } else {
            outtake = true
            outtakeServo.position = outtakeOpen
        }
    }
    fun arm() {
        if(arm) {
            arm = false
            armServo.position = armIn
        } else {
            arm = true
            armServo.position = armOut
        }
    }

    fun intakeServo (position: Double){
        intakeServo.position = position
    }

    fun armServo(position:Double){
        armServo.position = position;
    }

    fun intake (power: Double) {
        intakeMotor.power = power
    }

    fun getIntakePos():Double{
        return intakeServo.position;
    }

    fun getOuttakePos():Double{
        return armServo.position;
    }

    fun getArmPos():Double{
        return armServo.position;
    }
}