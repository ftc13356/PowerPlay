package org.firstinspires.ftc.teamcode.roadrunner.drive

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.kinematics.Kinematics
import com.acmerobotics.roadrunner.localization.Localizer
import com.acmerobotics.roadrunner.util.Angle
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.DecompositionSolver
import org.apache.commons.math3.linear.LUDecomposition
import org.apache.commons.math3.linear.MatrixUtils
import org.firstinspires.ftc.teamcode.roadrunner.drive.PoseStorage.currentPOVVelocity
import org.firstinspires.ftc.teamcode.roadrunner.drive.PoseStorage.currentPose
import org.firstinspires.ftc.teamcode.roadrunner.drive.PoseStorage.poseHeadOffset

/**
 * Localizer based on two unpowered tracking omni wheels and an orientation sensor.
 *
 * @param wheelPoses wheel poses relative to the center of the robot (positive X points forward on the robot)
 */
abstract class RFTwoTrackingWheelLocalizer(
    wheelPoses: List<Pose2d>
) : Localizer {
    private var _poseEstimate = Pose2d()
    override var poseEstimate: Pose2d
        get() = currentPose
        set(value) {
            poseHeadOffset = value.heading-currentPose.heading
            currentPose = value
            lastHeading = currentPose.heading

        }
    override var poseVelocity: Pose2d? = null
    private var lastWheelPositions = emptyList<Double>()
    private var lastHeading = Double.NaN

    private val forwardSolver: DecompositionSolver

    init {
        require(wheelPoses.size == 2) { "2 wheel poses must be provided" }

        val inverseMatrix = Array2DRowRealMatrix(3, 3)
        for (i in 0..1) {
            val orientationVector = wheelPoses[i].headingVec()
            val positionVector = wheelPoses[i].vec()
            inverseMatrix.setEntry(i, 0, orientationVector.x)
            inverseMatrix.setEntry(i, 1, orientationVector.y)
            inverseMatrix.setEntry(
                i,
                2,
                positionVector.x * orientationVector.y - positionVector.y * orientationVector.x
            )
        }
        inverseMatrix.setEntry(2, 2, 1.0)

        forwardSolver = LUDecomposition(inverseMatrix).solver

        require(forwardSolver.isNonSingular) { "The specified configuration cannot support full localization" }
    }

    private fun calculatePoseDelta(wheelDeltas: List<Double>, headingDelta: Double): Pose2d {
        val rawPoseDelta = forwardSolver.solve(
            MatrixUtils.createRealMatrix(
                arrayOf((wheelDeltas + headingDelta).toDoubleArray())
            ).transpose()
        )
        return Pose2d(
            rawPoseDelta.getEntry(0, 0),
            rawPoseDelta.getEntry(1, 0),
            rawPoseDelta.getEntry(2, 0)
        )
    }

    override fun update() {
        val wheelPositions = getWheelPositions()
        val heading = getHeading()
        if (lastWheelPositions.isNotEmpty()) {
            val wheelDeltas = wheelPositions
                .zip(lastWheelPositions)
                .map { it.first - it.second }
            val headingDelta = Angle.normDelta(heading - currentPose.heading)
            val robotPoseDelta = calculatePoseDelta(wheelDeltas, headingDelta)
            currentPose = Kinematics.relativeOdometryUpdate(currentPose, robotPoseDelta)

        }

        val wheelVelocities = getWheelVelocities()
        val headingVelocity = getHeadingVelocity()
        if (wheelVelocities != null && headingVelocity != null) {
            currentPOVVelocity = calculatePoseDelta(wheelVelocities, headingVelocity)
            poseVelocity = currentPOVVelocity
            PoseStorage.currentVelocity = Pose2d(currentPOVVelocity.vec().rotated(currentPose.heading),
                currentPOVVelocity.heading)
        }

        lastWheelPositions = wheelPositions
        lastHeading = heading
    }

    /**
     * Returns the positions of the tracking wheels in the desired distance units (not encoder counts!)
     */
    abstract fun getWheelPositions(): List<Double>

    /**
     * Returns the velocities of the tracking wheels in the desired distance units (not encoder counts!)
     */
    open fun getWheelVelocities(): List<Double>? = null

    /**
     * Returns the heading of the robot (usually from a gyroscope or IMU).
     */
    abstract fun getHeading(): Double

    /**
     * Returns the heading of the robot (usually from a gyroscope or IMU).
     */
    open fun getHeadingVelocity(): Double? = null
}
