# BIOBUZZ robot code: reference

For mentors and students who want the whole picture in one place. Students should begin
with [start here](biobuzz-start-here.md). The step-by-step measuring and tuning exercises
are in the lessons (Meetings 5 to 13) in the `ftc-2026` repository.

The code is in three folders under `TeamCode/.../teamcode/biobuzz/`: `programs` (what
appears on the Driver Station), `settings` (`Routes`, `ShooterCalibration`) and `robot`
(everything else named on this page).

Written for FTC SDK 12.0.0. Nothing here has been tuned on our robots yet.

## One job, one place

| Job | The only place it happens |
|---|---|
| Naming the hardware and powering any motor or servo | `StarterRobot` |
| Driving: every program, manual or automatic, ends in `StarterRobot.arcadeDrive(forward, rotate)` | `StarterRobot` |
| Driving a set distance or turning a set angle | `Drive` works out forward and rotate, then calls `arcadeDrive` |
| Turning to face the tags | `TagCamera.turn()` gives the turn power; the program passes it to `arcadeDrive` |
| Spinning the launcher, feeding, running the intake | `StarterRobot.setLauncherSpeed`, `setFeederRunning`, `setIntakePower` |
| One whole shot: spin up, wait for speed, feed, stop feeding | `Launcher.startShot(now, speed, feedSeconds)`, the only way to start one (timing in `ShotTimer`) |
| Reading the camera and deciding "aimed" | `TagCamera` |
| "Has this been true long enough?" (arrived, aimed, at speed) | `HeldFor` |
| Which range needs which launcher speed | The table in `ShooterCalibration` |
| Autonomous routes | The step lists in `Routes` |

## Which robot

`StarterRobot.ROBOT` chooses the hardware. Names must match the Driver Station
configuration exactly.

| | `BIOBUZZ_GOBILDA` | `PRACTICE_GOBILDA` | `PRACTICE_REV` |
|---|---|---|---|
| Robot | 2026-2027 goBILDA StarterBot | 2025-2026 goBILDA StarterBot | 2025-2026 REV Starter Bot |
| Drive motors | `left_drive` (forward), `right_drive` (reverse) | `left_drive` (reverse), `right_drive` (forward) | `leftDrive` (reverse), `rightDrive` (forward) |
| Launcher | `launcher` | `launcher` | `flywheel` (reverse) |
| Feeder | `windmill` servo (reverse) | `left_feeder` (reverse), `right_feeder` servos | `coreHex` motor (reverse) and `servo` agitator |
| Intake | `intake` motor, `left_intake_servo`, `right_intake_servo` (reverse) | none | none |
| Launcher speed, ticks/s | 1250 | 1125 | 1300 |
| "At speed" means within | 50 | 50 | 100 |
| Launcher P / F | 40 / 12.5 | 300 / 10 | the Hub's built-in values |
| Ticks per inch to start from | 45.3 | 45.3 | 44.6 |

All three also use `imu` (the Control Hub's heading sensor) and, for camera programs,
`Webcam 1`. These are each vendor's starting values, not measurements of our robots.
goBILDA's example code is kept unchanged under `vendor/gobilda/`.

## Settings and where they live

A value used by one class lives in that class. A value several classes need lives in
`StarterRobot`.

| Setting | File | Set it when |
|---|---|---|
| `ROBOT` | `StarterRobot` | You change robots. |
| `MOTION_ENABLED` | `StarterRobot` | The drive-direction check passes. Programs that move by themselves stay still until then. |
| `FEED_ENABLED` | `StarterRobot` | Shooting has been tested by hand. Camera-aimed feeding is off until then. |
| `LAUNCHER_SPEED`, `LAUNCHER_SPEED_TOLERANCE`, `LAUNCHER_P`, `LAUNCHER_F` | `StarterRobot` | Meeting 13, or if the vendor values are clearly wrong. |
| `LOGO`, `USB`, `MOUNTING_CONFIRMED` | `Heading` | The Hub's real mounting has been checked (below). Re-check after changing robots. |
| `TICKS_PER_INCH`, drive and turn gains, `DRIVE_POWER`, `TURN_POWER`, `COMMAND_TIMEOUT_SECONDS` | `Drive` | Meetings 5 and 10. |
| Aim gain, maximum turn power, tolerance, `OFFSET_DEGREES` | `TagCamera` | Aiming wobbles, is slow, or shots land to one side. |
| `RANGE_INCHES`, `SPEED_TICKS_PER_SECOND`, tuner step, cap and tap length, `USE_CAMERA` | `ShooterCalibration` | Meeting 5, and again for each new robot or target. |
| `FEED_SECONDS` | `ShotTimer` | Meeting 12. |
| The routes | `Routes` | Meetings 5 and 11. |
| `AIM_SECONDS`, `TOTAL_TIMEOUT_SECONDS` | `AutoFollowRoute` | Aiming gives up too soon, or the route needs a different cut-off. |

## Check the Hub direction before any automatic driving

Turns and straight driving use the Hub's heading, so the code has to know how the Hub
is mounted.

1. Label the front of the robot. With power off, see which way the REV logo faces and
   which way the Hub's USB ports point, relative to the robot.
2. Set `LOGO` and `USB` in `Heading.java` to match. The starting guess is logo up,
   USB forward. See [FIRST's mounting diagrams](https://ftc-docs.firstinspires.org/en/latest/programming_resources/imu/imu.html#physical-hub-mounting).
3. Run **Tune Drive** and press INIT only. Turn the robot about 90
   degrees to the right by hand. The heading on the screen should go up by about 90.
   To the left it should go down.
4. If it is wrong, fix `LOGO` and `USB`. Do not reverse motors to make up for it.
5. Set `MOUNTING_CONFIRMED = true`.

## How the autonomous works

A route is one list of steps in `Routes.java`, read from top to bottom:

```java
public static final Step[] RED_AUDIENCE_A = {
        drive(24),
        aimAndShoot(),
        turn(-90),
        drive(24),
};
```

| Step | What the robot does |
|---|---|
| `drive(inches)` | Drives that far. Negative is backward. |
| `turn(degrees)` | Turns that much. Positive is right (clockwise). |
| `aimAndShoot()` | Turns to face the chosen tags, looks up the launcher speed for the range the camera reads, shoots for `FEED_SECONDS`, then turns back to the way it was facing. |

There are eight routes: red or blue, audience-side or far-side cell, start A or B. In
INIT the D-pad chooses alliance and cell, A or B chooses the start, and X switches dry
run on or off. The screen shows the chosen list.

**Autonomous Route** is the Meeting 9 one-action program with a list. It
begins a step once, checks on it every loop, and begins the next when it is DONE.

Rules worth knowing:

- **Dry run is on by default.** The robot aims but does not spin or feed.
- **Shooting needs three things:** dry run off, `FEED_ENABLED = true`, and a speed table
  with rows on both sides of the camera range. If one is missing, the screen says which,
  the shot is skipped, and the route carries on.
- **The robot does not correct its distance.** If the drive before `aimAndShoot()` ends
  outside the table's ranges, change that drive or measure more table rows.
- **If the tags are not lined up within 3 seconds,** the shot is skipped and the route carries on.
- **Each drive or turn must finish within 4 seconds** or the run stops with a FAULT,
  because the robot no longer knows where it is. Split long drives into two lines.
- **The program stops itself at 28 seconds.** Budget up to 4 seconds per drive or turn
  and about 12 for an `aimAndShoot()`.
- Hive tags move when the hive tips, so the code uses them only to aim, never to work
  out where the robot is on the field.
- Camera range is measured in the camera's own frame. It is not the same as a tape
  measure on the floor, so always use the number on the screen.

## The order to measure things

| What | Program | Lesson |
|---|---|---|
| Hub direction and drive direction | Tune Drive, Tune Shooter | Meeting 5 setup guide |
| Launcher speed for each range | Tune Shooter | Meetings 5 and 12 |
| Ticks per inch and turns | Tune Drive | Meetings 5 and 10 |
| One-action autonomous | Autonomous One Action | Meeting 9 |
| A route | Autonomous Route, dry run on | Meetings 5 and 11 |
| Shooting in autonomous, feed time | Autonomous Route, dry run off | Meeting 12 |
| Launcher P and F | Tune Launcher PIDF | Meeting 13 |

Change one number at a time, and measure again after any change to the robot.

## Checking the code without a robot

The tests in `TeamCode/src/test/java` use fake motors. Run them with
`./gradlew :TeamCode:testDebugUnitTest`. They pass whichever robot `ROBOT` names, and
they do not depend on the numbers students tune.
