package org.firstinspires.ftc.teamcode.utils

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.ServoImplEx

class OuttakeKotlin (hardwareMap: HardwareMap, private var slide: SlideKotlin) {

    private var armServo: ServoImplEx = hardwareMap.get("as") as ServoImplEx //control hub:
    private var wristServo: ServoImplEx = hardwareMap.get("ws") as ServoImplEx //control hub:
    private var gateServo: ServoImplEx = hardwareMap.get("gs") as ServoImplEx //control hub:

    private var armStartAngle = 42.5 //angle of arm at position 0.0 relative to horizontal, positive values ccw, towards outside of robot
    private var armEndAngle = -174.0 //angle of arm at position 1.0
    private var armInAngle = -130.4 //angle of arm when it is in the robot TODO
    private var armOutAngle = -30.8969 //angle of arm when it is out of the robot TODO
    private var currentArmAngle = armInAngle //current arm angle
    private var incrementMultiplier = -2.0 //multiplier for how much the arm angle changes when the outtake angle is adjusted
    private var arm = false //whether the arm is in or out

    private var wristStartAngle = -164.5 //angle of wrist at position 0.0 relative to the arm, positive values flips claw upwards
    private var wristEndAngle = 81.0 //angle of wrist at position 1.0
    private var wristInAngle = 62.6 //angle of wrist when it is in the robot TODO
    private var wristOutAngle = 8.48 //angle of wrist when it is out of the robot
    private var currentWristAngle = wristInAngle //current wrist angle

    private var gateOpen = 0.0 //open position TODO
    private var gateClosed = 0.5 //closed position TODO

    private var outtake = false;

    private var t: Thread? = null

    fun setOuttakeAngle(armAngle: Double, wristAngle: Double, absPos: Boolean) { //set position of arm and wrist servos, absPos is if the angle is absolute or relative to the arm
        armServo.position = (armAngle - armStartAngle) / (armEndAngle - armStartAngle)
        currentArmAngle = armAngle
        wristServo.position = if(absPos) (wristAngle + armAngle - wristStartAngle) / (wristEndAngle - wristStartAngle) else (wristAngle - wristStartAngle) / (wristEndAngle - wristStartAngle)
        currentWristAngle = wristAngle
    }
    fun getOuttakeAngle(): DoubleArray { //get position of arm and wrist servos
        return doubleArrayOf(currentArmAngle, currentWristAngle)
    }

    fun gateToggle() {
        gateServo.position = if (gateServo.position == gateOpen) gateClosed else gateOpen
    }
    fun gateToggle(toggle: Boolean) {
        gateServo.position = if (toggle) gateClosed else gateOpen
    }
    fun armToggle() {
        if (arm) { //if arm out, bring in
            arm = false
            outtake = false
            setOuttakeAngle(armInAngle, wristInAngle, true) //bring arm out and wrist down to correct angle
        } else { //if arm in, bring out
            outtake = true
            arm = true
            setOuttakeAngle(armOutAngle, wristOutAngle, true) //bring arm out and wrist down to correct angle
        }
    }
    fun armToggle(toggle:Boolean) {
        if(toggle!=arm) {
            if (toggle) {
                arm = true
                outtake = true
                setOuttakeAngle(armOutAngle, wristOutAngle, true) //bring arm out and wrist down to correct angle
            } else {
                arm = false
                outtake = false
                setOuttakeAngle(armInAngle, wristInAngle, true) //bring arm out and wrist down to correct angle
            }
        }
    }
    fun outtakeAngleAdjust(armAngleIncrement: Double) {
        if(outtake) {
            currentArmAngle += armAngleIncrement*incrementMultiplier
            setOuttakeAngle(currentArmAngle, wristOutAngle, true)
        }
    }
    fun outtakeProcedure(toggle:Boolean) {
        if(toggle && !outtake) { //makes sure outtake is not already out or currently going out
            outtake = true
            t?.interrupt() //stops any existing threads
            t = Thread { //makes a new thread to run the outtake procedure
                gateToggle(true) //close both claws
                while(true) {
                    if (slide.getPosition()
                            .average() <= slide.minSlideHeight
                    ) {
                        armToggle(true) //bring arm out and wrist down to correct angle
                        break
                    }
                }
                t?.interrupt() //stops any existing threads
            }
            t!!.start()
        }
        else if (!toggle && outtake) { //makes sure outtake is not already in or currently going in
            outtake = false
            t?.interrupt() //stops any existing threads
            t = Thread { //makes a new thread to run the outtake procedure
                gateToggle(false) //open both claws
                while(true) {
                    if (slide.getPosition()
                            .average() <= slide.minSlideHeight
                    ) {
                        armToggle(false) //bring arm in and wrist up to correct angle
                        break
                    }
                }
                t?.interrupt() //stops any existing threads
            }
            t!!.start()
        }
    }
    fun outtakeProcedure() {
        if(!outtake) { //makes sure outtake is not already out or currently going out
            outtakeProcedure(true)
        }
        else { //makes sure outtake is not already in or currently going in
            outtakeProcedure(false)
        }
    }

    init {
        setOuttakeAngle(armInAngle, wristInAngle, true)
        gateServo.position = gateOpen
    }
}