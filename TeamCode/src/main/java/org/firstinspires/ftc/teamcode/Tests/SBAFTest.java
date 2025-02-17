package org.firstinspires.ftc.teamcode.Tests;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.Robots.BasicRobot;
import org.firstinspires.ftc.teamcode.pedroPathing.follower.Follower;
import org.firstinspires.ftc.teamcode.pedroPathing.localization.Pose;
import org.firstinspires.ftc.teamcode.pedroPathing.localization.PoseUpdater;
import org.firstinspires.ftc.teamcode.pedroPathing.pathGeneration.BezierLine;
import org.firstinspires.ftc.teamcode.pedroPathing.pathGeneration.Path;
import org.firstinspires.ftc.teamcode.pedroPathing.pathGeneration.Point;
import org.firstinspires.ftc.teamcode.pedroPathing.util.DashboardPoseTracker;

@Config
public class SBAFTest extends BasicRobot {
    public static double distance = 12;
    private String navigation = "forward";

    private DashboardPoseTracker dashboardPoseTracker;
    private PoseUpdater poseUpdater;
    private Telemetry telemetryA;
    private Follower follower;
    private Path forward, backward;
    private Boolean forwardStatus, backwardStatus;
    private Pose forwardEndPose, backwardEndPose;
    public LinearOpMode op;

    public SBAFTest(LinearOpMode opmode){
        super(opmode,false);
        op = opmode;
        follower = new Follower(op.hardwareMap);
        telemetryA = new MultipleTelemetry(op.telemetry, FtcDashboard.getInstance().getTelemetry());
        poseUpdater = new PoseUpdater(op.hardwareMap);
        dashboardPoseTracker = new DashboardPoseTracker(poseUpdater);
    }

    public void setBackdropGoalPose() {
        forwardEndPose = new Pose(distance, 0, Point.CARTESIAN);
        backwardEndPose = new Pose(-distance, 0, Point.CARTESIAN);
    }

    public void buildPaths(){
        forward = new Path(new BezierLine(
                new Point(0,0,Point.CARTESIAN),
                new Point(forwardEndPose.getX(), forwardEndPose.getY(), Point.CARTESIAN)));
        forward.setConstantHeadingInterpolation(0);

        backward = new Path(new BezierLine(
                new Point(0,0, Point.CARTESIAN),
                new Point(backwardEndPose.getX(), backwardEndPose.getY(), Point.CARTESIAN)));
        backward.setConstantHeadingInterpolation(0);

        follower.followPath(forward);
        packet.put("forward running", forwardStatus);
        update();
    }

    public void updateFollower() {
        poseUpdater.update();
        follower.update();
        if(!follower.isBusy()){
            switch(navigation){
                case "forward":
                    forwardStatus = follower.isBusy();
                    if(!forwardStatus){
                        setNavigation("backward");
                        follower.followPath(backward);
                    }
                    packet.put("forward running", forwardStatus);
                    update();
                    break;
                case "backward":
                    backwardStatus = follower.isBusy();
                    if(!backwardStatus){
                        setNavigation("forward");
                        follower.followPath(forward);
                    }
                    packet.put("backward running", backwardStatus);
                    update();
                    break;
            }
        }
        packet.put("x", poseUpdater.getPose().getX());
        packet.put("y", poseUpdater.getPose().getY());
        packet.put("heading", poseUpdater.getPose().getHeading());
        packet.put("total heading", poseUpdater.getTotalHeading());
        update();
    }

    public void setNavigation(String pathNavigation){
        navigation = pathNavigation;
    }
}