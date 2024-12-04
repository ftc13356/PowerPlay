package org.firstinspires.ftc.teamcode.Components.RFModules.Devices;

import static org.firstinspires.ftc.teamcode.Robots.BasicRobot.logger;
import static org.firstinspires.ftc.teamcode.Robots.BasicRobot.op;
import static org.firstinspires.ftc.teamcode.Robots.BasicRobot.packet;

import static java.lang.Double.NaN;
import static java.lang.Double.max;
import static java.lang.Double.min;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.signum;

import com.acmerobotics.dashboard.config.Config;
import com.outoftheboxrobotics.photoncore.Photon;
import com.outoftheboxrobotics.photoncore.hardware.motor.PhotonAdvancedDcMotor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.PIDCoefficients;

@Config
public class DualPIDController {
    DcMotorEx ext, rot;
    public static double  A_OFF = -13, MAX=30.2, MIN=0, ROTMAX = 160, ROTMIN = 0, TICKS_PER_IN = 20./1526, TICKS_PER_DEG = 90/256.*90/135/2.1*90/65*90/88,P=0.2,D=0, rP = 0.019, rP2 =0.02,rD2= 4, rD = .6, rF = .3, G = 0.15,rG = 0.3, rG2 = .6,TEST_LEN = 0;
    boolean mid=true;
    double TICKS_PER_RAD = TICKS_PER_DEG*PI/180;
    double targetExt, targetRot, middle, middleRot, trueTargExt, trueTargRot, lastPower=-0.1, curExt, curRot, vel;
    public DualPIDController() {
        ext = (DcMotorEx) op.hardwareMap.dcMotor.get("extendMotor");
        rot = (DcMotorEx) op.hardwareMap.dcMotor.get("rotateMotor");
        ext.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        ext.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rot.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rot.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rot.setDirection(DcMotorSimple.Direction.REVERSE);
        ext.setDirection(DcMotorSimple.Direction.REVERSE);
        mid=true;
        middle=0;
        middleRot = 0;
        curExt =0;
        curRot = 0;
        vel =0;
    }

    public void goTo(double extension, double rotation){
        extension = min(max(extension,MIN),MAX);
        rotation = min(max(rotation,ROTMIN),ROTMAX);
        targetExt = extension;
        targetRot = rotation;
        curExt = ext.getCurrentPosition() + (rot.getCurrentPosition()*TICKS_PER_DEG)/80/TICKS_PER_IN;
        curRot = rot.getCurrentPosition();
        double err = extension - curExt*TICKS_PER_IN;
        double d = ext.getVelocity()*TICKS_PER_IN;
        vel = d;
        ext.setPower(P*err+D*d+G*Math.sin(curRot*TICKS_PER_RAD));
        double rErr = rotation - curRot*TICKS_PER_DEG;
        double rd = -rot.getVelocity()*TICKS_PER_DEG;
        double r = curExt*TICKS_PER_IN/MAX;
        double power = (rP+rP2*r*r)*rErr+.001*(rD+rD2*r*r)*rd+Math.cos(curRot*TICKS_PER_RAD+(A_OFF+6*r)*PI/180)*(rG+ rG2*r);
        if(abs(rd)<5 && abs(rErr)>1.5 && targetRot>3 && curRot<90){
            power+=rF*signum(rErr);
        }
        if(abs(rErr)<10&&rd>-1&&targetRot<3 || (targetRot<3 && lastPower==0))
            power=0;
        rot.setPower(power);
        lastPower = power;
        if(power ==0 && abs(rd)<1 && targetRot <3 && abs(getRot())>1) {
            rot.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            rot.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
        packet.put("powa",power);
        packet.put("rD", rd);
        packet.put("rGcostheta", rG*Math.cos(curRot*TICKS_PER_RAD));
    }
    public void goTo(double extension, double rotation, double middle){
        if(middle != this.middle || targetExt != min(max(extension,MIN),MAX) || targetRot != min(max(rotation,ROTMIN),ROTMAX))
            mid=false;
        if(!mid && abs(getExt() - middle) < 3)
            mid=true;
        if(!mid) {
            extension = middle;
            this.middle = middle;
        }
        goTo(extension,rotation);
    }
    public void goTo(double extension, double rotation, double middle, double middleRot){
        if(middle != this.middle || middleRot != this.middleRot || targetExt != min(max(extension,MIN),MAX) || targetRot != min(max(rotation,ROTMIN),ROTMAX))
            mid=false;
        if(!mid && abs(getExt() - middle) < 3 && abs(getRot()-middleRot)<10)
            mid=true;
        trueTargExt = extension;
        trueTargRot = rotation;
        if(!mid) {
            extension = middle;
            rotation = middleRot;
            this.middle = middle;
            this.middleRot = middleRot;

        }
        goTo(extension,rotation);
    }
    public double getRot(){
        return curRot*TICKS_PER_DEG;
    }
    public double getExt(){
        return curExt*TICKS_PER_IN;
    }
    public double getTargetExt(){
        return targetExt;
    }
    public double getTargetRot(){
        return targetRot;
    }
    public double getTrueTargExt(){return trueTargExt;}

    public double getTrueTargRot() {
        return trueTargRot;
    }

    public double getMiddle(){return middle;}
    public double getMiddleRot(){return  middleRot;}
    public boolean isMid(){return mid;}

    public double getVel(){return vel;}
    public double getExtPosition(){
        return ext.getCurrentPosition();
    }

    public double getRotPosition(){
        return rot.getCurrentPosition();
    }

}
