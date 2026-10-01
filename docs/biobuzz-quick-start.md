# BIOBUZZ: start here

This guide is for students learning to program the standard six-wheel FTC StarterBot.
The programs are drafts. Their numbers are starting examples that must be measured on
the assembled robot. You can read and trace the code before the kit arrives.

An **OpMode** is one robot program you select on the Driver Station. **Telemetry** is
the information that program displays there. **Calibration** means measuring the robot
and updating its settings to match those measurements.

## Choose one program

Java files are in `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/biobuzz/`.
Start at the top of this table. Add the later programs after the earlier exercises work.

| Driver Station name | Java file | What you practice |
|---|---|---|
| BIOBUZZ Standard Baseline | `StarterBotTeleOp.java` | Drive from the gamepad; operate intake and launcher. No camera needed. |
| BIOBUZZ Measure Drive or Turn | `StarterBotDriveCalibration.java` | Select one distance or turn, run it, then measure it. No camera needed. |
| BIOBUZZ Standard Aim Draft | `StarterBotAimTeleOp.java` | Hold a button to face a selected camera target. |
| BIOBUZZ Measure Tag Approach | `StarterBotTagCalibration.java` | Approach a target, then return. No shooting or parking leg. |
| BIOBUZZ Position and Shoot | `StarterBotPositionAuto.java` | Combine a route, target approach, optional shooting, return, and parking. |

## Prepare, build, and run

1. With your mentor, match the assembled robot to the
   [hardware configuration table](biobuzz-starterbot.md#hardwareconfiguration-contract).
   Names such as `left_drive` must match the Driver Station configuration exactly.
   Camera programs also need the configured `Webcam 1`.
2. Open the project in Android Studio. Open the Java file for **one** program in the
   table above. Remove its `@Disabled` line when that program is ready for testing.
   This annotation hides the program from the Driver Station list.
3. Follow the calibration steps below for that program. Removing `@Disabled` does not
   complete calibration or enable the separate assisted-motion and feed settings.
4. Build and install the project on the Robot Controller using your team's Android
   Studio connection. Wait for installation to finish. Saving an edit alone does not
   update the robot.
5. Select that program on the Driver Station and press **INIT**. Read telemetry and
   make any selections listed below. INIT connects devices and prepares the program.
6. Place the robot on the exercise's marked start. Keep it empty for drive and camera
   tests. Clear the area, then press **START**. Keep the driver ready to press **STOP**.
7. Record telemetry when motion ends. Press **STOP**, wait for mechanisms to stop, then
   approach the robot to measure. Return to the same start for the next trial.

## Calibrate in this order

Change **one setting at a time**, rebuild/install, and repeat the same exercise.
Record the old value, new value, battery reading, and result. The linked exercises
include detailed steps and blank measurement tables.

| Step | What to do and measure | Where to change settings |
|---|---|---|
| 1. Manual controls | Check wheel/intake directions, neutral inputs, button release, and Stop, starting with supported wheels. Use the [baseline checks](biobuzz-starterbot.md#calibration-sequence-later-powered-session-one-supervised-station). | Hardware names and directions in `StarterRobot.init()`. |
| 2. Heading sensor | Follow the [Hub mounting and hand-turn checks](biobuzz-starterbot.md#configure-the-hub-on-the-assembled-robot). A hand turn to the right should increase the displayed heading. | `LOGO`, `USB`, then `MOUNTING_CONFIRMED` in `HubHeading.java`. |
| 3. Straight distance | After direction checks, enable assisted motion. Run [Exercise 1](biobuzz-starterbot.md#exercise-1--find-ticks-per-inch-with-a-tape-measure): request 24 inches, record both encoder counts, then measure actual travel. | `TICKS_PER_INCH` in `StarterDrive.java`; `MOTION_ENABLED` in `HiveAim.java`. |
| 4. Turns | Run [Exercise 1B](biobuzz-starterbot.md#exercise-1b--verify-an-imu-controlled-90-degree-turn). Compare the displayed heading and actual angle for both left and right turns. | `TURN_PROPORTIONAL_GAIN` and `HEADING_HOLD_PROPORTIONAL_GAIN` in `StarterDrive.java`. |
| 5. Camera aiming | Verify the camera and target using the [camera checks](biobuzz-starterbot.md#calibration-sequence-later-powered-session-one-supervised-station), then [Exercise 2](biobuzz-starterbot.md#exercise-2--make-the-turn-settle-instead-of-wiggle). Check aiming from both sides and stopping when the target is hidden. | Aim gain, maximum power, and `OFFSET_DEGREES` in `HiveAim.java`. |
| 6. Approach and return | Run [Exercise 2B](biobuzz-starterbot.md#exercise-2b--approach-a-tag-then-return-to-the-starting-mark). Measure how closely the robot returns to its original position and heading. | Range/turn gains in `TagApproach.java`; goal range comes from the shot window. |
| 7. Shooting | Follow [shot-window qualification](biobuzz-starterbot.md#calibration-sequence-later-powered-session-one-supervised-station), step 5. Record range, aim offset, wheel speed, and hits/misses with the actual shooting setup. | Shot window and `FEED_ENABLED` in `HiveAim.java`; speed settings in `StarterLauncher.java`. |
| 8. Full route | [Measure one route card](biobuzz-starterbot.md#measure-and-fill-in-one-route-card). Start with an empty DRY_RUN; verify approach, return, final footprint, and time. | Distances/angles and that card's `qualified` value in `AutoRoutes.java`. |

For example, **1080 encoder ticks ÷ 23 actual inches = 46.96 ticks per inch**.
Use repeated measurements from both wheels as explained in Exercise 1. These example
numbers are arithmetic practice, not measurements to copy into your robot.

`MOTION_ENABLED` allows the camera turn and the drive/autonomous exercises.
`FEED_ENABLED` allows assisted feeding after shooting has been tested. Both start false.
**The baseline manual program has its own bumper-controlled feeding and does not use
these flags.** Keep the launcher empty during its initial drive checks.

## Gamepad 1 controls

### Baseline manual program

| Control | Result |
|---|---|
| Left stick up/down | Drive forward/backward. |
| Right stick left/right | Turn left/right. |
| Right trigger / left trigger | Intake / reverse intake. Equal pressures cancel. |
| Hold right bumper | Spin the launcher; feed when it is fast enough. Feeding also adds intake power. |
| Release right bumper | Stop the launcher and feeder. Triggers can still run the intake. |

### Measure Drive or Turn

During INIT, press one D-pad direction: **up = +24 inches**, **down = −24 inches**,
**left = −90°**, **right = +90°**. Check `Selected`, then press START.
The robot performs just that movement. Record `Result` and `Tick change left / right`.
If `Result` says FAULT, read its reason and investigate before repeating.

### Camera programs

During INIT, select **both** an alliance and a cell:
**D-pad left = red**, **right = blue**, **up = far-side cell**, **down = audience-side cell**.
Check the displayed selection and target freshness. The far-side cell is opposite the audience.

In **Standard Aim Draft**, the sticks drive normally until you hold **A**. Holding A
stops forward driving and requests a turn toward the selected target. A missing target
stops the assisted turn. Release A to return to stick driving.
The **right bumper** spins the launcher. **A + right bumper** permits feeding only when
the target is fresh, alignment has settled, range and wheel speed are acceptable,
and assisted feeding is enabled. Release either button to stop feeding.
To change cells after a hive tip, release A and right bumper, then press D-pad up/down.

In **Measure Tag Approach**, START runs the approach and return automatically.
In **Position and Shoot**, INIT also uses **A/B** to select start/park A or B, and **X**
to cycle DRY_RUN → SHOOT_AND_PARK → PARK_ONLY. Release X between presses.
DRY_RUN moves but never fires. PARK_ONLY follows the approach and parking route without
tag positioning or shooting. The two match modes require a qualified route card.

## If something does not work

| What you see | What to check next |
|---|---|
| Program missing from the list | Correct Java file's `@Disabled` removed, build installed, and correct TeleOp/Auto list selected. |
| Hardware name error at INIT | Exact configuration spelling and device type in `StarterRobot.init()`, `HubHeading`, or `HiveAim`. |
| Motion disabled or mounting unconfirmed | Complete the relevant checks above before changing the named flag. |
| Target not fresh / no assisted movement | Alliance and cell selected, correct cluster visible, camera streaming, calibrated camera at 640×480. |
| Drive or turn timeout | Stop and inspect both encoder readings, wheel directions, heading, and obstructions. Do not treat it as a completed move. |
| Launcher spins but assisted feed stays off | A and bumper held, feed enabled, fresh/aligned target, measured range inside the window, and speed inside the allowed band. |

For a guided explanation of the Java, continue with the [student reading guide](biobuzz-student-guide.md).
The [full calibration reference](biobuzz-starterbot.md) contains the measurement worksheets,
route details, and [instructions for running the software tests](biobuzz-starterbot.md#run-the-current-software-checks).
