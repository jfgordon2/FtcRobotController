# Meeting 5 setup guide: tape practice field, shooter tuning, auto path

For the mentor. Students follow the
Meeting 5 lesson (`lessons/0005-build-and-structure-our-teleop.html` in the ftc-2026 repository) and the
[print packet](../output/pdf/meeting5-shooter-auto-print-pack.pdf). Checked against the
BIOBUZZ manual, Team Update 03, and FTC SDK 12.0.0.

**Robot:** one of last season's practice robots, goBILDA or REV, whichever has the webcam. The BIOBUZZ StarterBot kit has not arrived.
**Field:** none. A tape rectangle on a wall is the hive opening; tape on the floor is the
start line and the parking zone.
**Result:** a measured range-to-speed table and one autonomous path that ends in the box.

## 1. Before the meeting (about 30 minutes)

| Check | Where | What to do |
|---|---|---|
| Which robot | `StarterRobot.java` | Set `ROBOT` to the robot on the table. `PRACTICE_GOBILDA` expects `left_drive`, `right_drive`, `launcher`, `left_feeder`, `right_feeder`. `PRACTICE_REV` expects `leftDrive`, `rightDrive`, `flywheel`, `coreHex`, `servo`. Both also need `imu` and `Webcam 1` in the Driver Station configuration. |
| Hub direction | `Heading.java` | Set `LOGO` and `USB` to the way the Control Hub really faces on the robot, then `MOUNTING_CONFIRMED = true`. In INIT, turning the robot right by hand must make the heading go up. |
| Drive direction | `StarterRobot.java` | Wheels off the floor, run **Tune Shooter**, push the left stick forward. Both sides roll forward. Then `MOTION_ENABLED = true`. |
| Launcher | `ShooterCalibration.java` | No balls. Hold right bumper; measured speed should reach the requested speed. `MAXIMUM_SPEED` (1800 ticks/s) is the cap for the tuner and the table; lower it if the room needs it. |
| Camera | webcam mount | Tilt it up, about 45 degrees to start. The tags will be about 50 inches up the wall and the robot is 2 to 3 feet away. |

Three programs are used and all three appear on the Driver Station:
**Tune Shooter**, **Tune Drive**, **Autonomous Route**.
The last two refuse to move until `MOUNTING_CONFIRMED` and `MOTION_ENABLED` are true.

This code assumes the standard two-drive-motor version of either practice robot. A
mecanum version has four drive motors and needs its own drive code.

On the REV robot the launcher's normal speed is 1300 ticks per second, the wheel counts
as at speed within 100 (REV's own margin), and the Hub's built-in PIDF values are kept.
Its feeder is a motor, so check how many balls one 0.2-second tap of Y pushes through
and change `FEED_PULSE_SECONDS` in `ShooterCalibration.java` if it is not one.

Use the balls last season's robot was built for. BIOBUZZ POLLEN is about 2.8 inches
across and will not feed through that launcher.

## 2. Tape layout and where each number comes from

| Tape | Measurement | Source |
|---|---|---|
| Wall rectangle | 20 in wide; bottom edge 53.5 in and top edge 65.5 in above the floor | Manual Figure 9-10: the upward CELL opening spans 53.5 to 65.6 in above the TILES. Section 9.6.2: the opening is about 20 in wide by 14 in tall, tilted back 30 degrees, so it looks about 12 in tall from in front. |
| AprilTags | RED AUDIENCE pair, centered under the rectangle, top of the black squares 2 in below the bottom tape | Figure 9-16: the tag row is on the underside of the CELL, about 7 in back from the front edge. With the 30-degree tilt that is about 3.5 in below the opening. |
| Center line | Straight out from the wall under the rectangle's center | Our own line for lining up. |
| Start line | 52 in from the wall | Our estimate from Figures 9-2, 9-9 and 9-10 of the gap between the field wall and the front of the upward CELL. Measure it on a real field when we get to one. |
| Shooting marks | Front bumper 18, 26 and 34 in from the wall | The room a robot has between its hive and the field wall. At 34 in an 18-inch robot's back is on the start line. |
| Parking box | 23 in by 11 in, anywhere convenient | Section 9.3: the LOADING ZONE's real size. Its real position is on the alliance wall on the far side of the hive, so today's park leg is practice. |

**Why RED AUDIENCE:** Figure 10-2 shows the red hive starting the match with its
audience-side CELL facing up. For blue it is the far-side CELL (`BLUE SCORING`). SCORING is
FIRST's name for the tags on the side away from the audience; the INIT screen shows it as `SCORING (far side)`.
In INIT, D-pad LEFT then DOWN selects RED AUDIENCE.

**Printing:** US Letter, Actual Size / 100%, single-sided. A black square must
measure 3.25 in. Join the left and right sheets at the printed center marks.

**If the launcher cannot reach 53.5 in** at the speed cap, lower the rectangle, keep
its size, and write the new height on the worksheet. The method is what matters today.

## 3. How the three exercises work

**Shooter.** `TuneShooter` asks the launcher for a speed in encoder
ticks per second with `setVelocity`, the same way goBILDA's code and our baseline
TeleOp do. D-pad up/down changes the request by 25. A tap of Y starts one
short shot: the feeder runs for a moment once the wheel is at speed. Students record the camera range and the speed that gives at least
four hits in five, then type those rows into `ShooterCalibration.java`.

**Drive and turn.** `TuneDrive` runs one 24-inch drive or one
90-degree turn. Students compare encoder ticks with a tape measure and correct
`Drive.TICKS_PER_INCH` if needed. Turns finish on the IMU heading.

**Auto path.** `AutoFollowRoute` carries out a route: a list of `drive`,
`turn` and `aimAndShoot` steps in `Routes.java`. Students edit one list
(`RED_AUDIENCE_A`). Dry run is on by default and never spins or feeds; X in INIT
switches it. `aimAndShoot()` turns to face the tags, shoots, and turns back. It does
not correct the robot's distance, so the drive before it has to stop at a range the
speed table covers. Shooting also needs `FEED_ENABLED = true` in `StarterRobot.java`.

## 4. Battery

During INIT every program shows a `Battery` line on the Driver Station with the voltage and what to do (GOOD, SWAP or CHARGE). It clears at Start. Read it with the motors
stopped. These are rules of thumb for the standard 12 V NiMH battery, not spec-sheet
numbers; trust your own readings over time.

| Volts at rest | What to do |
|---|---|
| About 13.5 or more | Freshly charged. Good for measuring. |
| 12.5 to 13.5 | Normal. Fine for practice. |
| Below about 12.5 | Swap it before measuring anything you will keep. |
| Below about 12 | Charge it. |

Start on a fresh battery and write the resting voltage on each worksheet. After a
swap, shoot one row again (or repeat the 24-inch drive) to check it still agrees.
To re-check during a session, press Stop and INIT again. If the measured
speed can no longer reach the requested speed, change the battery before changing
anything else. A reading taken straight off the charger is a little high.

## 5. What carries over, and what to measure again

| From today | Later |
|---|---|
| Procedure, worksheets, the three programs, the table format | Reused as they are. Set `ROBOT` to `BIOBUZZ_GOBILDA` for the BIOBUZZ StarterBot. |
| Tape wall and floor layout | Reused to tune the new launcher before we have a hive. |
| Ticks per inch | Starting value. Both robots use the same drive motors and 96 mm wheels. Confirm with one 24-inch run. |
| Launcher speeds and camera ranges | Measure again: different launcher, different balls, different camera mount. |
| Route numbers | Measure again on a real field. |

**First time on a real hive, recheck three things:** the camera range at each
shooting mark (the real tags are tilted, so the reading changes), the speed at each
mark (a ball has to land in the CELL, not pass through a flat rectangle), and whether
enough balls land to tip the hive. A hit on the wall rectangle does not show a tip.

## Sources

- [BIOBUZZ Competition Manual, Team Update 03](https://ftc-resources.firstinspires.org/ftc/game/cm-html/BIOBUZZ%20Competition%20Manual%20-%20TU03.htm): sections 9.3, 9.6, 9.9, 10.3.1, 10.5.4 and rule G304.
- [FIRST field resources](https://ftc-resources.firstinspires.org/ftc/field): printable AprilTag sheets.
- [goBILDA 2025-2026 StarterBot guide](https://www.gobilda.com/ftc-starter-bot-resource-guide-decode/), [goBILDA 2026-2027 StarterBot guide](https://www.gobilda.com/ftc-starter-bot-resource-guide-2026-2027-season/) and [REV 2025-26 Starter Bot programming pages](https://docs.revrobotics.com/ftc-kickoff-concepts/decode-2025-26/programming-teleop): hardware names, drive directions and launcher starting speeds for each robot.
- [FIRST: understanding AprilTag detection values](https://ftc-docs.firstinspires.org/en/latest/apriltag/understanding_apriltag_detection_values/understanding-apriltag-detection-values.html) and [Hub mounting for the IMU](https://ftc-docs.firstinspires.org/en/latest/programming_resources/imu/imu.html#physical-hub-mounting).
