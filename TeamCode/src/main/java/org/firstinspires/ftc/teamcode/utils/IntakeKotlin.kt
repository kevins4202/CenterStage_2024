package org.firstinspires.ftc.teamcode.utils

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.ServoImplEx
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

class IntakeKotlin(hardwareMap: HardwareMap){
    private var intakeServo: ServoImplEx = hardwareMap.get("is") as ServoImplEx //control hub: 5
    private var intakeMotor: DcMotorEx = hardwareMap.get("im") as DcMotorEx  //expansion hub: 2

    private var transfer = false
    private var currentPosition = IntakePositions.INIT

    private var updateTick = false

    private var t: Thread? = null

    enum class IntakePositions {
        INIT, INTAKE, TRANSFER, FIVE, DRIVE, MANUAL //INIT for init, INTAKE for intaking, TRANSFER for transferring, FIVE for 5 stack, DRIVE for driving, OTHER for custom values
    }
    private val intakePositionMap = mapOf(
            IntakePositions.INIT to 0.5,
            IntakePositions.INTAKE to 1.0,
            IntakePositions.TRANSFER to 0.6994,
            IntakePositions.FIVE to 0.8, //TODO
            IntakePositions.DRIVE to 0.85)

    var motorMode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
    var servoPosition = IntakePositions.INIT
    var motorTargetPosition = 0
    var motorPower = 0.0
    private var motorIsBusy = false
    private var manualPosition = 0.0

    init {
        intakeServo.position = intakePositionMap[servoPosition]!!
        intakeMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        intakeMotor.mode = motorMode
    }
    private fun intakeServo(intakePosition: IntakePositions) {
        //if switching off of transfer, make sure can switch back
        if(intakePosition != IntakePositions.TRANSFER)
            transfer = false
        if(intakePosition == IntakePositions.MANUAL) {
            intakeServo.position = manualPosition
        }
        else
            intakeServo.position = intakePositionMap[intakePosition]!!
        currentPosition = intakePosition
    }


    fun changeIntakeServo(power: Double){
        manualPosition = intakeServo.position
        servoPosition = IntakePositions.MANUAL
        manualPosition -= power* 0.05
        transfer = false
    }

    fun intake (power: Double) { //if intaking, make sure the intake is out
        if(abs(power) > 0.2 && servoPosition != IntakePositions.INTAKE) {
            servoPosition = IntakePositions.INTAKE
        }
        motorPower = power
    }
    fun update() {
        intakeMotor.targetPosition = motorTargetPosition
        intakeMotor.mode = motorMode
        intakeServo(servoPosition)
        intakeMotor.power = motorPower
        motorIsBusy = intakeMotor.isBusy
        updateTick = true
    } //griddy griddy on the haters - Charlie Jakymiw 2023

    @OptIn(DelicateCoroutinesApi::class)
    fun transfer() {
        GlobalScope.launch {
            if (intakeServo.position != intakePositionMap[IntakePositions.TRANSFER]!! && !transfer) {
                motorMode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
                motorTargetPosition = -240 //set value for how much motor needs to outtake to transfer
                waitForTick()
                motorMode = DcMotor.RunMode.RUN_TO_POSITION
                waitForTick()
                motorPower = 0.8
                while (motorIsBusy) { //wait for it to finish
                    motorPower = 0.8
                }
                servoPosition = IntakePositions.TRANSFER
                waitForTick()
                var currentTime = System.currentTimeMillis()
                while(System.currentTimeMillis() - currentTime < 500) {
                    motorPower = 0.0
                }
                motorMode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
                waitForTick()
                transfer = true
                waitForTick()
                currentTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - currentTime < 500) {
                    motorPower = 1.0
                }
            }
        }
    }
    suspend fun waitForTick() {
        while (!updateTick) {
            delay(10)
        }
        updateTick = false
    }
    fun getPosition(): Int = intakeMotor.currentPosition
    fun getIntakeMotor(): DcMotorEx = intakeMotor
    fun getIntakeServo(): ServoImplEx = intakeServo
    fun getIntakePos():Double = intakeServo.position
}