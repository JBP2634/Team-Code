package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonFX;
import com.kauailabs.navx.frc.AHRS;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.AnalogPotentiometer;
import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.PneumaticsModuleType;
import edu.wpi.first.wpilibj.SPI;

public class Robot extends TimedRobot {

  //Constants vvvvv
  Timer timer = new Timer();
  final XboxController xBoxCont = new XboxController(0);
  double maxDegree = 360; // if your over 360 then your driving to much
  
  //these are used for swerve vvv
  final double kp = 0.3;
  final double ki = 0.0;
  final double kd = 0.0;

  PIDController pidFrontLeftTurn = new PIDController(kp, ki, kd);
  PIDController pidFrontRightTurn = new PIDController(kp, ki, kd);
  PIDController pidBackLeftTurn = new PIDController(kp, ki, kd);
  PIDController pidBackRightTurn = new PIDController(kp, ki, kd);

  public final WPI_TalonFX frontLeftDrive = new WPI_TalonFX(2);
  public final WPI_TalonFX frontRightDrive = new WPI_TalonFX(4);
  public final WPI_TalonFX backLeftDrive = new WPI_TalonFX(6);
  public final WPI_TalonFX backRightDrive= new WPI_TalonFX(8); 

  public final WPI_TalonFX frontLeftSteer = new WPI_TalonFX(1);
  public final WPI_TalonFX frontRightSteer = new WPI_TalonFX(3);
  public final WPI_TalonFX backLeftSteer = new WPI_TalonFX(5);
  public final WPI_TalonFX backRightSteer = new WPI_TalonFX(7); 
  
  public final double kWheelDiameterMeters = Units.inchesToMeters(3.75);
  public final double kDriveMotorGearRatio = 1 / 8.45;
  public final double kTurningMotorGearRatio = 1.0 / (150.0/7.0); //motor rotations to wheel rotations conversion factor
  public final double kDriveEncoderRot2Meter = kDriveMotorGearRatio * Math.PI * kWheelDiameterMeters;
  public final double kTurningEncoderRot2Rad = kTurningMotorGearRatio * 2 * Math.PI; 
  public final double kTurningEncoderTicksToRad = kTurningEncoderRot2Rad/2048;

  double driveSensitivity = 0.4; //do not change above 1
  double turningSensitivity = 2;
  double maxSpeedMpS = 5; // metres/sec

  Translation2d m_frontLeftLocation = new Translation2d(0.340, 0.285);
  Translation2d m_frontRightLocation = new Translation2d(0.340, -0.285);
  Translation2d m_backLeftLocation = new Translation2d(-0.340, 0.285);
  Translation2d m_backRightLocation = new Translation2d(-0.340, -0.285);

  SwerveDriveKinematics m_kinematics = new SwerveDriveKinematics(
  m_frontLeftLocation, m_frontRightLocation, m_backLeftLocation, m_backRightLocation);

  //these are for the arm vvv
  public final WPI_TalonFX leftArmSide= new WPI_TalonFX(6); 
  public final WPI_TalonFX rightArmSide= new WPI_TalonFX(5);
  private final DifferentialDrive armRotate = new DifferentialDrive(leftArmSide, rightArmSide); 
  final WPI_TalonFX armTalonExtenstion = new WPI_TalonFX(7);
  double maxArmDeg = 60;
  double minArmDeg = -5;
  double armSpeed_Fast =  0.50;
  double armSpeed_Slow = 0.45;

  //Claw and pnuematics vvv  
  private final DoubleSolenoid dSolenoidClaw = new DoubleSolenoid(PneumaticsModuleType.CTREPCM, 3, 0);
  private final Compressor compressor = new Compressor(0, PneumaticsModuleType.CTREPCM);
  private final double Scale = 250, offset = -25;
  private final AnalogPotentiometer potentiometer = new AnalogPotentiometer(0, Scale, offset);

  //navx2 vvv
  final double kp_Pitch = 0.1; 
  final double kp_Yaw = 0.1;
  final double ki_Navx = 0.05;
  final double kd_Navx = 0.05;
  AHRS navx = new AHRS(SPI.Port.kMXP);  
  PIDController pidPitch = new PIDController(kp_Pitch, ki_Navx, kd_Navx);
  PIDController pidYaw = new PIDController(kp_Yaw, ki_Navx, kd_Navx);
  //constants ^^^^^
  
  // our functions vvvvvv

  public void resetEncoders () {
    frontLeftSteer.setSelectedSensorPosition(0);
    frontRightSteer.setSelectedSensorPosition(0);
    backLeftSteer.setSelectedSensorPosition(0);
    backRightSteer.setSelectedSensorPosition(0);
  }

  public void setMotorBreaks () {
    frontLeftSteer.setNeutralMode(NeutralMode.Brake);
    frontRightSteer.setNeutralMode(NeutralMode.Brake);
    backLeftSteer.setNeutralMode(NeutralMode.Brake);
    backRightSteer.setNeutralMode(NeutralMode.Brake);

    frontLeftSteer.setNeutralMode(NeutralMode.Brake);
    frontRightSteer.setNeutralMode(NeutralMode.Brake);
    backLeftSteer.setNeutralMode(NeutralMode.Brake);
    backRightSteer.setNeutralMode(NeutralMode.Brake);
  }

  public void invertMotors () {
    frontLeftSteer.setInverted(true);
    frontRightSteer.setInverted(true);
    backLeftSteer.setInverted(true);
    backRightSteer.setInverted(true);

    frontLeftDrive.setInverted(true);
    frontRightDrive.setInverted(true);
    backLeftDrive.setInverted(true);
    backRightDrive.setInverted(true);
  }

  public void continouousInput () {
    pidFrontLeftTurn.enableContinuousInput(-Math.PI, Math.PI);
    pidFrontRightTurn.enableContinuousInput(-Math.PI, Math.PI);
    pidBackLeftTurn.enableContinuousInput(-Math.PI, Math.PI);
    pidBackRightTurn.enableContinuousInput(-Math.PI, Math.PI);
  }

  public double removeDeadzone(int axisInput) {
    if (Math.abs(xBoxCont.getRawAxis(axisInput)) < 0.1) {
      return 0;
    }
    return xBoxCont.getRawAxis(axisInput);
  }

  public void swerveDrive(double xSpeed, double ySpeed, double rotSpeed) {
    ChassisSpeeds desiredSpeeds = new ChassisSpeeds(xSpeed, ySpeed, rotSpeed);
    
    //make desiredSpeeds into speeds and angles for each module
    SwerveModuleState[] moduleStates = m_kinematics.toSwerveModuleStates(desiredSpeeds);

    //normalize module values to remove impossible speed values
    SwerveDriveKinematics.desaturateWheelSpeeds(moduleStates, maxSpeedMpS);
    
    SwerveModuleState frontLeftModule = moduleStates[0];
    SwerveModuleState frontRightModule = moduleStates[1];
    SwerveModuleState backLeftModule = moduleStates[2];
    SwerveModuleState backRightModule = moduleStates[3];

    //optimize wheel angles (ex. wheel is at 359deg and needs to go to 1deg. wheel will now go 2deg instead of 358deg)

    double frontLeftSensorPos = frontLeftSteer.getSelectedSensorPosition()*kTurningEncoderTicksToRad;
    double frontRightSensorPos = frontRightSteer.getSelectedSensorPosition()*kTurningEncoderTicksToRad;
    double backLeftSensorPos = backLeftSteer.getSelectedSensorPosition()*kTurningEncoderTicksToRad;
    double backRightSensorPos = backRightSteer.getSelectedSensorPosition()*kTurningEncoderTicksToRad;

    var frontLeftCurrentAngle = new Rotation2d(frontLeftSensorPos);
    var frontRightCurrentAngle = new Rotation2d(frontRightSensorPos);
    var backLeftCurrentAngle = new Rotation2d(backLeftSensorPos);
    var backRightCurrentAngle = new Rotation2d(backRightSensorPos);
    
    var frontLeftOptimized = SwerveModuleState.optimize(frontLeftModule, frontLeftCurrentAngle);
    var frontRightOptimized = SwerveModuleState.optimize(frontRightModule, frontRightCurrentAngle);
    var backLeftOptimized = SwerveModuleState.optimize(backLeftModule, backLeftCurrentAngle);
    var backRightOptimized = SwerveModuleState.optimize(backRightModule, backRightCurrentAngle);
    
    double frontLeftTurnPower = pidFrontLeftTurn.calculate(frontLeftSteer.getSelectedSensorPosition()*kTurningEncoderTicksToRad, frontLeftOptimized.angle.getRadians());
    double frontRightTurnPower = pidFrontRightTurn.calculate(frontRightSteer.getSelectedSensorPosition()*kTurningEncoderTicksToRad, frontRightOptimized.angle.getRadians());
    double backLeftTurnPower = pidBackLeftTurn.calculate(backLeftSteer.getSelectedSensorPosition()*kTurningEncoderTicksToRad, backLeftOptimized.angle.getRadians());
    double backRightTurnPower = pidBackRightTurn.calculate(backRightSteer.getSelectedSensorPosition()*kTurningEncoderTicksToRad, backRightOptimized.angle.getRadians());

    //set steer motor power to the pid output of current position in radians and desired position in radians
    //positive is clockwise (right side up)
    frontLeftSteer.set(frontLeftTurnPower);
    frontRightSteer.set(frontRightTurnPower);
    backLeftSteer.set(backLeftTurnPower);
    backRightSteer.set(backRightTurnPower);

    //set drive power to desired speed div max speed to get value between 0 and 1
    frontLeftDrive.set(frontLeftOptimized.speedMetersPerSecond/maxSpeedMpS);
    frontRightDrive.set(frontRightOptimized.speedMetersPerSecond/maxSpeedMpS);
    backLeftDrive.set(backLeftOptimized.speedMetersPerSecond/maxSpeedMpS);
    backRightDrive.set(backRightOptimized.speedMetersPerSecond/maxSpeedMpS);
  }

  public void limitArmRotation(double getArmDegValue) {
    if (getArmDegValue <= maxArmDeg){
      armRotate.tankDrive(armSpeed_Fast, -armSpeed_Fast);
    }
    if (getArmDegValue >= minArmDeg){ 
      armRotate.tankDrive(-armSpeed_Slow, armSpeed_Slow);
    } 
  }

  //execution Functions vvvvv
  @Override
  public void robotInit() {
    resetEncoders();
    setMotorBreaks();
    invertMotors();
    continouousInput();
  }

  @Override
  public void robotPeriodic() {
    double encoderDegrees_rightArmSide = rightArmSide.getSelectedSensorPosition()/maxDegree;
    //limitArmRotation(encoderDegrees_rightArmSide);  

    //smartDash lines vvv
    SmartDashboard.putNumber("encoderDegrees_rightArmSide", encoderDegrees_rightArmSide);
  }
  
  @Override
  public void autonomousInit() {
  }

  @Override
  public void autonomousPeriodic() {
    
  }

  @Override
  public void teleopInit() {

  }

  @Override
  public void teleopPeriodic() {

  }
}
