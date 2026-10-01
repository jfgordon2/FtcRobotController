# BIOBUZZ standard StarterBot: baseline, aiming, and calibration

Reviewed 2026-09-30 against the FIRST Tech Challenge (FTC) software development kit (SDK) 12.0.0. For the standard six-wheel build.
The kit is ordered and will not be available at Meeting 4. Use the code for paper tracing and test planning; assembly and physical calibration follow delivery.
All new OpModes are disabled drafts; no physical qualification is implied.

## Where to start

Students: begin with the [setup and operating guide](biobuzz-quick-start.md) for program
choices, controls, and the calibration order. Then use the [student reading guide](biobuzz-student-guide.md). It explains
OpMode timing, the start-once/update-every-loop pattern, and the purpose of every class.
For terms such as IMU, PIDF, and Kp, see the student guide’s
[abbreviations](biobuzz-student-guide.md#abbreviations-and-hardware-words) and
[proportional-gain example](biobuzz-student-guide.md#what-did-kp-mean).
Read in this order: manual TeleOp → one drive/turn → camera aiming → shooting phases →
tag approach/return → full autonomous. This longer document is the commissioning reference.

- `StarterRobot.java`: shared hardware initialization, basic outputs, and shutdown. Each OpMode owns one; autonomous and aiming do not inherit a manual TeleOp loop.
- `StarterDrive.java`: basic distance/heading commands and local position estimation.
- `TagDrive.java`: camera approach/return coordination using `TagApproach` decisions and named `DriveCheckpoint` measurements.
- `StarterLauncher.java` / `AutoShot.java`: hardware outputs / timed shot decisions.
- `HiveAim.java` / `TargetSelection.java`: camera observations and permissions / explicit driver target choice.
- `AutoRoutes.java`: route cards. Calibration OpModes test one movement or tag sequence; `StarterBotPositionAuto` combines the subsystems.
- `vendor/gobilda/2026-09-30/`: untouched ZIP and extracted TeleOp/autonomous, outside the Android source set. Both original license headers are retained.
- Existing M2/M3 practice files remain in place. Their hardware qualification does not transfer to this build.

Source: [goBILDA standard-wheel resource guide](https://www.gobilda.com/ftc-starter-bot-resource-guide-2026-2027-season/).
Downloaded [example ZIP](https://www.gobilda.com/content/downloads/3200-2627-0003_example-code.zip) on 2026-09-30.
SHA-256: `0ea889528fb7a785c5f06275f6dabfc9cf9278fcda067e8f90d32f92bf116e07`.
The archive is mutable; this checksum identifies the reviewed copy.

## Code behavior and measurement limits

- The current vendor ZIP includes `StarterBotAuto.java`. It spins/feeds for 10 seconds, then requests a -120 mm drive and a two-second settling period. It does not use AprilTags.
- The team position draft assigns bounded outputs, checks both drive encoders, and stops on timeout or completion. It positions, aligns, and shoots one timed burst. DRY_RUN skips firing and completes the parking path. SHOOT_AND_PARK requires feed enabled and a qualified route.
- The vendor TeleOp's bumper combines spin-up and feeding. The aiming draft splits permission: bumper can spin; A + bumper only feeds when all gates pass. Releasing either stops the feeder. Baseline TeleOp uses bumper-controlled spin-up and feeding.
- `ftcPose` uses degrees/inches as explicitly selected here. Range is camera-frame XY distance, not 3D distance or necessarily floor distance when the camera is pitched. Do not apply `Math.toDegrees` again.
- SDK 12 supplies `AprilTagClusterDetection`, `metadata.name`, and `percentClusterFound`; read clusters from the processor’s detections list. Percent found counts visible members, not a calibrated confidence score. The M3 sample demonstrates the shared pose fields.
- No pose can mean missing tag size/library metadata. Missing lens calibration may produce an inaccurate pose; it does not reliably yield null. Bearing accuracy also depends on camera intrinsics (lens calibration values) and mounting.
- Hive tags move; their SDK field-position placeholders do not provide dependable field localization. The auto is relative encoder travel plus relative aiming, not global positioning. The encoder route ends near the shot location; `TagDrive.startApproach` adjusts within a small bounded area to the selected range and bearing. The robot reverses those measured movements before taking the fixed parking path.
- Establish a qualified shooting range through measured trials. Choose drive distances for a marked start and route, and evaluate LEAVE or PARK from the actual end pose under §10.5.4.
- The bottom of the HIVE is 30.6 inches above the TILES ([Figure 9-10](https://ftc-resources.firstinspires.org/ftc/game/tu-01#page=1), §9.6.2). Measure route clearance using the labeled reference points; measure camera-to-target geometry separately.
- Measure hits, misses, tip completion, and ball counts on the team’s field setup to establish repeatable shooting performance.
- G418 specifies top entry and bottom POLLEN removal. Check the complete current rules before choosing a method.

## Hardware/configuration contract

Match the standard season assembly, not the preseason chassis or mecanum variant.

| Config name | SDK type | Vendor direction / use |
|---|---|---|
| `left_drive` | DcMotor | FORWARD, left drivetrain |
| `right_drive` | DcMotor | REVERSE, right drivetrain |
| `intake` | DcMotor | forward intake roller |
| `launcher` | DcMotorEx | RUN_USING_ENCODER, encoder connected |
| `left_intake_servo` | CRServo | FORWARD |
| `right_intake_servo` | CRServo | REVERSE |
| `windmill` | CRServo | REVERSE, feeder |
| `imu` | IMU | Control Hub internal sensor; set the actual mounting in HubHeading |
| `Webcam 1` | WebcamName | added USB camera and rigid mount |

Record exact motor models, ports, gearing, wheel diameter, direction tests, camera model,
resolution, calibration, mount position/pitch, battery, and code revision. The vendor's
launcher values (1250 target, 1200 minimum ticks/second; PIDF 40/0/0/12.5) are matching-build
starting values, not universal constants. PIDF means proportional, integral, derivative,
and feedforward; these four speed-controller coefficients are named in `StarterRobot.init()`.
They are separate from the power-per-degree or power-per-inch gains in our drive/aim code.
The added upper speed gate is `StarterLauncher.MAXIMUM_TICKS_PER_SECOND` (initially target + 50 ticks/s).
Baseline trigger control is right minus left; only equal pressures cancel.

## Hub orientation, heading, and the assumed starts

The Control Hub's IMU (inertial measurement unit) measures heading; motor encoders measure wheel travel. The drive class
combines them for an **estimated local position**: X is forward along the starting heading,
Y is right, and positive heading turns clockwise. Start is (0, 0, 0°) for whichever route is
selected. These are not FIRST field coordinates. Wheel slip still affects position, and IMU
heading can drift. The code does not claim field localization from hive tags.

### Configure the Hub on the assembled robot

1. Label the front of the robot. With power off, inspect which way the REV logo faces and
   which way the Hub's USB ports point **relative to the robot**, not to the room or alliance.
2. In `HubHeading.java`, set `LOGO` and `USB`. The supplied **logo UP, USB FORWARD** is an
   assumption, not a claim about the final goBILDA assembly. Examples: a flat Hub with USB
   toward the back needs UP/BACKWARD; a vertical Hub needs the actual logo and USB directions.
   Use [FIRST's mounting diagrams](https://ftc-docs.firstinspires.org/en/latest/programming_resources/imu/imu.html#physical-hub-mounting).
   For a tilted/non-orthogonal mounting, use the documented angle/quaternion method with a
   mentor; do not choose the nearest cardinal directions.
3. On the Driver Station, check Configure Robot → Control Hub → I2C bus 0 (Inter-Integrated Circuit sensor bus): the internal IMU
   must have the correct device type and be named `imu`. Preserve the type detected for that Hub.
4. Keep `MOUNTING_CONFIRMED = false`. Build/deploy and INIT **BIOBUZZ Measure Drive or Turn**.
   INIT reports `Hub logo / USB / confirmed` and `Heading sensor (IMU), clockwise degrees` without motor motion.
   With an empty unpowered drivetrain and a clear floor, turn the robot by hand about 90°
   right. Displayed heading should increase about 90°; left should decrease. Crossing ±180°
   wraps the display; for example +179° to −179° is a 2° right turn, not a 358° left turn.
5. If the direction/angle is wrong, check physical mounting and those settings. Do not fix
   a mounting error by reversing motors or negating yaw again. SDK yaw is counterclockwise;
   `HubHeading` already negates it once to match our clockwise-positive drive convention.
6. Once verified, set `MOUNTING_CONFIRMED = true` and rebuild. It gates the autonomous and
   calibration OpModes that use the IMU. TeleOp manual driving remains a separate baseline.
   At Start, the code resets yaw while the robot is on its marked starting heading. Never reset
   yaw midway through a route. Missing/stale IMU data stops controlled motion rather than
   silently switching to encoder-only turning.

### Choose physical starting marks before calibrating the route

Assume **Start A** is on a clear audience-side perimeter section and **Start B** is on a clear
opposite-audience perimeter section, each on our own alliance's side with the front facing
inward. These are candidate team choices; the exact point must be measured and verified for
the full robot footprint, flower clearance, LOADING ZONE exclusion, and alliance partner.
The current route-card distances remain practice values until those marks are established.
[G304 and Figure 11-1](https://ftc-resources.firstinspires.org/ftc/game/cm-html/BIOBUZZ%20Competition%20Manual%20-%20TU02.htm#G304)
show the starting conditions and examples. Do not treat A/B as official start boxes.

Record the perimeter reference, distance along that wall to the same chassis pointer, which
part of the robot contacts the wall, and the forward heading. Use these measurements to place
it repeatedly; do not depend on adding tape to an event field. On our practice field, use tape
marks to learn the placement. Each alliance/cell/start combination has its own route card.

### Use a tag to check placement, without inventing a field position

A useful INIT check is possible when the selected cluster is visible and the hive is in the
same known starting state. The camera mount and robot heading must also match calibration.
The manual locates clusters on the cells and gives reference-hole geometry, so a full
state-dependent field-pose solution would require surveying those locations and the camera
mount. Our simple check compares range/bearing; it does **not** solve or reset X/Y/heading.
See [manual §9.9 and Figure 9-16](https://ftc-resources.firstinspires.org/ftc/game/cm-html/BIOBUZZ%20Competition%20Manual%20-%20TU02.htm#_Ref239285332).

1. Place the robot on the independently measured start mark and heading. Verify the hive's
   initial state by observation. INIT the auto, select alliance/cell/start, and record **Start
   tag measured range / bearing** only while the target is fresh. Take several readings.
2. Reposition the robot from scratch three times. Record the mean range/bearing and spread.
   If readings disagree, fix placement/camera issues before accepting them. If the cluster is
   not visible from that legal start, leave this optional check disabled and use physical marks.
3. After the route declarations in `AutoRoutes.java`, add a static initialization block for
   the exact measured card. This is an example structure; replace the example numbers with
   your recorded mean before setting `true`:

   ```java
   static {
       RED_FAR_A.startTag = new StartTagCheck(true, 48.0, -5.0);
   }
   ```

   Here 48.0 is camera XY inches and −5.0 is camera bearing, not IMU heading. The supplied
   disabled reference of 48/0 is also a placeholder practice value, not a surveyed start.
4. Rebuild and reposition. INIT should say `MATCH`; move a few inches off the mark and verify
   it says `MISMATCH/MISSING`. The initial comparison limits are ±2 inches and ±3°; test whether
   these actually distinguish acceptable placement. Do not widen them to hide a bad placement.
5. When enabled, a failed/missing start check prevents motion at Start. Reposition by hand
   before the countdown, verify the hive state, then INIT again. The robot never adjusts its
   starting position under power during INIT. When disabled, telemetry explicitly says to use
   physical marks. The check's result alone is not proof of a correct start pose: different
   poses can produce similar range/bearing, especially if the cell moved.

A hive tip invalidates the stored view of that cell; the comparison is only for the matching
pre-match state. During AUTO, tags still provide local shot positioning, while encoders and
IMU guide the measured route. Fixed practice tags can help assess repeatability in the lab,
but are not assumed to exist on the competition field.

## Calibration sequence (later powered session, one supervised station)

Meeting 4 is a paper review and build plan. Run these steps only after the corresponding
hardware and control rows are approved. Keep mechanisms empty for drive/vision tests.
Remove `@Disabled` only from the specific OpMode under test. All safety gates remain
independent of that annotation. Record actual results; blank means pending.

1. **Baseline and stop.** Verify the assembly/configuration and directions with wheels supported,
   then low-speed floor driving. Test neutral, simultaneous inputs, release, Stop, and restart.
   Confirm baseline spin-up and feed behavior before enabling any assisted feeding.
2. **Camera and target.** Use M3 telemetry first. Verify camera calibration at **640×480**,
   actual printed size, focus/exposure, and a rigid mount. Single sample tag 584 works for M3;
   this aim draft intentionally accepts only the BIOBUZZ cluster library. Use the official
   full-size cluster geometry on a representative target, not a manual screenshot.
   During INIT, use gamepad 1 D-pad **left = RED**, **right = BLUE**,
   **up = far-side cell (opposite audience)**, **down = audience-side cell**.
   Select both; Driver Station telemetry shows the alliance, cell, SDK cluster, and target freshness.
   Mappings are `RED SCORING` (30–33), `RED AUDIENCE` (34–37),
   `BLUE AUDIENCE` (38–41), and `BLUE SCORING` (42–45).
   Confirm the actual upward-facing cell. Detection alone does not prove which cell is up.
   At Start, autonomous freezes both choices and TeleOp locks alliance.
   In TeleOp, release A and right bumper, then press D-pad up/down to select the next cell after a tip.
   Opposing directions are ignored; a press made while aiming/launching is blocked and must be
   released/repressed. Changing the target clears alignment so it must settle again.
   Starting without both choices leaves targeting unavailable; Stop and INIT again.
   Choices reset on each new OpMode run. Alliance selection does not mirror or change the
   encoder route; that remains a separately calibrated setting. No automatic cell switching.
3. **Bearing sign and loss tests.** Initially leave both enable flags false. Confirm left-of-camera
   target gives positive bearing. Enable `MOTION_ENABLED` with an empty robot after checks:
   A should produce a small left turn that reduces positive error. If error grows, stop and
   correct drive direction/mount assumptions before adjusting gain. Positive vendor rotation
   is right, hence the minus sign in `AimMath.turn`. Cover the cluster, show another cluster,
   disconnect the camera, and release A: held assist must stop when data are absent or older
   than 0.25 s; releasing A restores stick driving. There is no blind search turn.
4. **Tune and align.** Start with the bounded draft gain/cap, changing one value at a time.
   Require stable bearing within the proposed ±2° tolerance for 0.3 s. Reduce gain if oscillating.
   Measure camera/launcher offset and target relationship: zero tag bearing is not automatically
   the correct trajectory into the opening. Fit `OFFSET_DEGREES` at one shot location. A fixed offset
   is valid only over its tested range; different positions may need geometry or a range table.
5. **Qualify one shot window.** With the vendor baseline, record at several measured locations:
   camera XY range, tape reference/distance, bearing offset, cell state, requested/actual ticks/s,
   shots/hits/tips, battery, and misses. Camera pitch changes the relationship between SDK range
   and floor tape distance; use a consistent reference. Do not set intrinsics to make one tape
   reading agree. Fix lens calibration/print geometry first. Choose a narrow successful window
   at 1250 ticks/s; replace the trial values in `MINIMUM_RANGE_INCHES` and `MAXIMUM_RANGE_INCHES`. Do not extrapolate.
   Only then set `FEED_ENABLED`. A + bumper must withhold feed outside that window, without
   alignment, during target loss, or outside the speed band. Test release of each button.
   Drivers still check the upward-facing opening and a clear/legal shot; vision cannot do that.
6. **Encoder travel.** Measure positive forward tick deltas on both wheels over repeated loaded
   straight runs. Compute ticks/inch = ticks / actual inches; investigate wheel disagreement.
   Record wheel diameter and output gearing rather than using 28 ticks/rev as a drive constant.
   Set `StarterDrive.TICKS_PER_INCH` and the signed distances in `AutoRoutes` for marked starts and unobstructed routes.
   The per-wheel command cap is 96 inches, power 0.2, tolerance 0.5 inch, four-second deadline; these are
   draft test bounds, not a route qualification. Test both encoders and an obstructed-wheel
   case: either side failing to arrive must time out to FAULT, never proceed to POSITION_TAG.
7. **Validate end-to-end.** Suggested team acceptance: five consecutive moves within the team's
   chosen tape tolerance, five stable aim trials on each side, and all loss/release/Stop tests
   pass. Repeat after a battery or camera-mount change. Tag positioning has a four-second
   deadline; timeout stops. Test the complete shooting sequence below. Auto does not detect
   obstacles or establish a global field pose. The IMU corrects heading during straight travel. A passed relative move alone earns no guaranteed score.

| Date / robot / code / battery | Target / cell state | Tape reference / distance | SDK range / bearing | Target / actual ticks/s | Hits / shots / tips | Stop/loss result / next change |
|---|---|---|---|---|---|---|
| pending | | | | | | |

## Select a route, shoot preloads, and park

**Before the match countdown**, select **BIOBUZZ Position and Shoot**, then INIT:

| Control during INIT | Choice |
|---|---|
| D-pad left / right | RED / BLUE alliance |
| D-pad up / down | Far-side / audience-side cell |
| A / B | Start A → shot A → park A, or Start B → shot B → park B |
| X, release, press again | Cycle DRY_RUN → SHOOT_AND_PARK → PARK_ONLY |

Read the full route name, segment distances/turns, qualification status, and mode on the
Driver Station. Place the robot at that route's measured start **and heading**. All selections
lock at Start. A/B are team-defined floor references, not official field locations. Each
route bundles its shooting destination and parking destination with its start; there is no
arbitrary destination picker that could select an untested combination.

The eight cards in `AutoRoutes.java` cover two starts for each alliance/cell combination.
Their supplied distances and turns are **empty-floor practice examples**, not routes derived
from field CAD or proven to reach the LOADING ZONE. Each is initially `qualified = false`.
DRY_RUN permits empty-robot measurement; the two match modes refuse unqualified cards.
`MOTION_ENABLED` must still be true, and SHOOT_AND_PARK also requires `FEED_ENABLED`.
The camera is configured in all three modes, even though PARK_ONLY does not use its pose.

### What happens after Start

1. **APPROACH:** execute the card's encoder drive/turn segments to a measured approach pose.
2. **POSITION_TAG:** call `tagDrive.startApproach(selectedCell, rangeGoal, bearingGoal, now)`. Turn to center
   the cluster, creep straight to the range goal, then turn to the shooting offset. The goal is
   the midpoint of the qualified shot window and `HiveAim.OFFSET_DEGREES`. This is local positioning,
   not navigation to arbitrary field coordinates.
3. **SHOOT:** require all shot gates and stable speed; perform one calibrated burst. DRY_RUN
   skips launcher/feed operation. PARK_ONLY skips tag positioning and shooting and takes the
   same measured approach/parking segments.
4. **RETURN_FROM_TAG:** reverse the recorded final turn, straight movement, and initial turn,
   in that order, including a partially completed phase. This returns toward the measured
   approach pose before parking. Merely restoring heading would leave a positional error.
5. **PARK:** execute the fixed parking path, then stop everything. DONE reports completed
   encoder commands; it does not mean a sensor or referee confirmed the parking footprint.

A missing/stale tag, a positioning limit, or a shot failure stops that action and attempts the
measured return before parking. A wheel stall, encoder-command timeout, or return failure stops
all outputs and cancels parking. Driver Station Stop cancels all motion, including return.
The return uses the recorded phase distances and measured IMU headings. Wheel slip is not detected reliably; local encoder/IMU estimates are not global localization.
Validate approach, return, and final footprint on the actual floor.

The 28-second overall deadline leaves a proposed margin before AUTO ends. The reserve is
`park segments × 4 seconds + 3 reverse-tag phases × 2 seconds + 1 second margin`.
The supplied two-segment parking path reserves 15 seconds; tag positioning or shooting is
stopped/skipped at elapsed time 13 seconds. Additional parking segments increase the reserve.
Return/parking can still fail if motion times out; this is a time budget, not a guarantee.

### Reusable classes: what a student calls

| Class / method | Meaning |
|---|---|
| `StarterDrive.driveInches(24, now)` | Begin a 24-inch forward segment; negative distance goes backward. |
| `StarterDrive.turnDegrees(90, now)` | Begin a right turn; negative degrees turn left. |
| `StarterDrive.update(now)` | Advance the active command; returns RUNNING, DONE, or FAULT. |
| `TagDrive.startApproach(cell, rangeInches, bearingDegrees, now)` | Start bounded tag-relative positioning. |
| `TagDrive.updateApproach(now, cell, fresh, range, bearing)` | Supply the latest selected-cluster observation each loop; returns RUNNING, DONE, or FAULT. |
| `TagDrive.startReturn(now)` / `updateReturn(now)` | Start and advance the reverse of recorded positioning segments. |
| `StarterDrive.stop()` | Cancel a basic drive/turn and stop both motors. |
| `TagDrive.stop()` | Cancel tag approach/return and stop its underlying drive. |
| `StarterLauncher.intake(power)` | Set intake roller/corner servos together, clipped to −1…1. |
| `StarterLauncher.spinUp()` | Request the configured wheel speed without starting the feeder. |
| `StarterLauncher.startShot(now)` / `updateShot(now, gate)` | Begin and advance the bounded spin-up/burst sequence. |
| `StarterLauncher.stop()` | Stop wheel, intake, corner servos, and feeder. |

While a tag action is active, use `TagDrive` as the wheel controller and cancel it with
`tagDrive.stop()`. Do not issue a basic drive command until the tag action has ended or been cancelled.

These methods are **nonblocking**: starting a command does not wait until it finishes.
The OpMode calls `update` each loop so Stop, telemetry, and timeouts can keep working.
Do not call `driveInches` again on every loop: that would keep moving the target farther away.
`now` is elapsed time in seconds from the OpMode timer.

### Measure and fill in one route card

1. Print/sketch the field using [G304 and Figure 11-1 starting examples](https://ftc-resources.firstinspires.org/ftc/game/cm-html/BIOBUZZ%20Competition%20Manual%20-%20TU02.htm#G304)
   and [Figure 10-7 parking examples](https://ftc-resources.firstinspires.org/ftc/game/cm-html/BIOBUZZ%20Competition%20Manual%20-%20TU02.htm#_Ref239287294).
   Select a legal perimeter start on your alliance's side, outside the LOADING ZONE.
2. Label three poses: Start A, Shot A, Park A. A **pose** is both a position and a direction.
   Draw the entire robot footprint, not only its center. Put Park A clearly partly in the
   LOADING ZONE and clear of the perimeter so LEAVE and PARK can both be satisfied at AUTO end.
   Coordinate the path and parked footprint with the alliance partner.
3. Choose a straight approach near a qualified shot location, allowing a clear six-inch tag
   adjustment and return area, then a turn and straight drive
   to the parking pose. Measure inches between the same chassis-pointer references and
   degrees between heading lines. If obstacles prevent that shape, use additional measured
   `Step.drive(...)` and `Step.turn(...)` segments; account for their time reserve.
4. In `AutoRoutes.java`, find the exact alliance/cell/start card. A simple example is
   `card("RED start A -> far shot A -> park A", false, 24, 90, 24)`.
   The three numbers mean **approach inches, parking turn degrees, parking inches**.
   Replace them with your measurements. `false` means not yet qualified. Do not copy the
   example numbers and label them field measurements.
5. Use DRY_RUN with an empty robot. The robot will position against the tag, reverse that
   movement, and drive the parking leg. Compare each actual stop/heading with the marks; repeat from both left and
   right initial aim errors and with the target hidden. After any drive fault, stop and inspect.
6. When repeated whole-route tests meet the team's distance/angle tolerances, remain clear
   of obstacles, reach the parking footprint, and finish with time margin, change **only that
   card's** `false` to `true`. Keep written evidence. Test PARK_ONLY, then supervised one-ball
   SHOOT_AND_PARK, and finally the intended preloaded count. Do not mark every card qualified
   because one route worked.

| Card / robot / battery | Start pointer + heading | Approach inches | Shot range/offset | Park turn + inches | Actual park footprint | Elapsed sec | Pass / next change |
|---|---|---|---|---|---|---|---|
| | | | | | | | |

### Settings to measure

| File / setting | Supplied value | Measurement |
|---|---|---|
| `StarterDrive.TICKS_PER_INCH` | Vendor nominal ≈45.29 | Exercise 1: ticks divided by actual tape distance. |
| `StarterDrive.TURN_PROPORTIONAL_GAIN` / `HEADING_HOLD_PROPORTIONAL_GAIN` | 0.015 / 0.02 trial gains | Tune measured heading turns and straight-line correction with the IMU exercise below. |
| `StarterDrive.DISTANCE_PROPORTIONAL_GAIN` | 0.06 trial gain | Scale wheel-distance errors into power; reduce if it overshoots, adjust only after measurement. |
| `StarterDrive.DRIVE_POWER` / `TURN_POWER` | 0.2 / 0.18 trial values | Repeat empty moves/turns; change one power at a time, staying within the four-second command deadline. |
| `AutoRoutes` card numbers | Practice examples | Measure approach, park turn, and park distance from field marks. |
| `HiveAim.TURN_PROPORTIONAL_GAIN`, `MAXIMUM_TURN_POWER`, `OFFSET_DEGREES` | 0.015, 0.18, 0° | Bearing and trajectory exercises below. |
| `HiveAim.MINIMUM_RANGE_INCHES` / `MAXIMUM_RANGE_INCHES` | Unqualified 30–34-inch camera window | Replace with repeated successful SDK ranges, not floor tape readings. |
| `StarterLauncher` target/min/max speed | 1250 / 1200 / 1300 ticks/s | Vendor starting speed with trial upper gate; baseline and assisted TeleOp share the target/min constants. Requalify shots after changes. |
| `StarterBotPositionAuto.TAG_RANGE_INCHES` | Midpoint of shot window | Desired camera XY range; keep its tolerance inside the qualified shot window. |
| `TagApproach.RANGE_PROPORTIONAL_GAIN` / `MAXIMUM_FORWARD_POWER` | 0.035 / 0.15 | Exercise 2B: small forward/backward approaches and repeatable returns. |
| `TagApproach.TURN_PROPORTIONAL_GAIN` / `MAXIMUM_TURN_POWER` | 0.015 / 0.18 | Exercise 2B: tune the initial/final turns separately from TeleOp. |
| `AutoShot.FEED_SECONDS` | 0.6 s trial burst | Observe one-ball feeding, then repeat with intended load; timed feeding does not count balls. |
| `HiveAim` motion/feed flags | Both false | Enable motion for empty calibration, feed after shot tests. DRY_RUN never fires even when feed is enabled. |

### Why this sequence fits the game

G304 requires exactly four starting POLLEN in contact with the robot; §10.3.4 allows them
in/on the robot or on the floor touching it. Load the intended storage before Start for
this strategy; collecting extra balls is not part of these routes. Verify the actual storage
and feeding sequence during assembly and tests. The 0.6-second example burst does not promise
to launch all four. A completed HIVE TIP earns 20 points; LEAVE earns 3 and AUTO PARK earns 5.
LEAVE and PARK are assessed at the end of AUTO. Launch only into the upward-facing cell;
a fixed burst cannot guarantee a tip or detect cell orientation. See
[manual §§10.3.4–10.5.5, G304, G401, G417](https://ftc-resources.firstinspires.org/ftc/game/cm-html).
Do not use the transition period as extra driving/shooting time (G403).

## Beginner lab: how to measure and tune

**Calibration** means measuring what this robot actually does and putting those measurements
into its settings. **Tuning** means changing one setting, repeating the same test, and comparing
results. **Telemetry** is the live text on the Driver Station. An **encoder tick** is one count
from a motor's rotation sensor. A **gain** says how strongly the robot reacts to an error;
a **tolerance** says how close is close enough; a **dwell** is how long it must stay close.

For Meeting 4, work the example calculation and predict the tests on paper. The following
physical exercises are for the assembled robot. Example results below are invented arithmetic
examples, not measurements of our robot.

### Set up a repeatable test

- Gather floor tape, a tape measure, paper/pencil, a calculator, the Driver Station, and a
  full-size target cluster. Shot tests also need the actual cell geometry or a suitable test
  fixture reproducing its opening and height; a flat printed tag cannot establish a shot trajectory.
- Assign a driver to Start/Stop, a recorder to read telemetry, and a measurer who approaches
  only after Stop and after mechanisms stop moving. Keep everyone out of the travel and shot paths.
- Put a small tape pointer on a rigid part of the chassis. Measure from this same pointer
  every time, not from whichever wheel or bumper is easiest to reach.
- Mark the start pointer position and a straight heading line on the floor. Record battery
  reading, target cell, camera mount, and code settings. Return to the marks for every trial.
- Before each run, say the expected action and the reason to Stop. Keep all POLLEN out during
  drive/aim tests. Do not hold a wheel or reach into the launcher to create a fault.

**How to change a setting:** press Stop, open the named Java file under
`TeamCode/src/main/java/org/firstinspires/ftc/teamcode/biobuzz/`, find the constant with
Android Studio's Find command, and change only its number (keep the name and semicolon).
For example, the first distance in an `AutoRoutes.card(...)` selects the approach inches; `StarterDrive.TICKS_PER_INCH` controls the conversion to encoder counts.
Record the before/after values. Build and deploy to the Robot Controller using the team's
Android Studio connection, wait for installation to finish, then select the intended OpMode
again on the Driver Station. Press INIT, select alliance/cell again, check telemetry, then Start.
Editing a file alone does not change the code already running on the robot.

### Exercise 1 — find ticks per inch with a tape measure

Use **BIOBUZZ Measure Drive or Turn**, empty, after removing its `@Disabled` annotation.
After direction checks set `MOTION_ENABLED = true`; leave feed disabled. This OpMode requires
no camera and performs no aiming, shooting, or parking after the measured command.

1. Place the chassis pointer over the start mark, aligned with a straight heading line.
2. INIT, press D-pad up for +24 inches (down requests −24), read the selected command, then Start.
3. After motion finishes, record **Tick change left / right** and **Result** from telemetry,
   then press Stop. The counts are changes since Start; no manual subtraction is needed.
   A FAULT, curve, or slip is a failed trial: investigate rather than calculating from it.
4. Once stopped, measure pointer travel along the heading line in inches. Use actual travel,
   not the requested 24 inches. For the forward test, divide each positive tick change by
   actual positive distance. Keep using the forward test for the following calculation.
5. Repeat three times. If both wheels and repeated runs agree, average their ticks/inch and
   enter that value in `StarterDrive.TICKS_PER_INCH`. If they disagree or the robot curves,
   inspect dragging wheels, directions, assembly, and encoder connections before averaging.
6. Rebuild/deploy and repeat the same +24-inch command to check the correction.

**Worked example:** left = 1080 ticks, right = 1084 ticks, actual travel = 23.0 inches.
Left: 1080 ÷ 23 = 46.96 ticks/inch. Right: 1084 ÷ 23 = 47.13 ticks/inch.
Their mean is 47.04 ticks/inch. If repeated runs agree, enter `TICKS_PER_INCH = 47.04;`.
The next 24-inch request uses about 1129 ticks. Keep the calibration command at +24 inches while checking
this correction; changing both numbers would hide which change helped.

| Trial | Requested in | Left ticks | Right ticks | Actual in | Left ticks/in | Right ticks/in | Straight/no slip? |
|---|---|---|---|---|---|---|---|
| 1 | 24 | | | | | | |
| 2 | 24 | | | | | | |
| 3 | 24 | | | | | | |

### Exercise 1B — verify an IMU-controlled 90-degree turn

Complete the mounting/hand-turn checks above and the straight-distance exercise first. Mark
starting heading and a 90° line using a protractor or right-angle template. Use two chassis
pointers to see its centerline; measure angle, not how far a front corner travels.

1. INIT **BIOBUZZ Measure Drive or Turn**; D-pad right chooses +90° right, left chooses −90°.
   Place the robot on the heading mark. Start establishes IMU zero and runs the selected turn.
2. After it stops, read **Local X forward / Y right / heading**, record the final heading,
   press Stop, and compare the chassis line with the floor angle. Repeat three times each way.
   A correct displayed angle with a wrong physical angle points to mounting/reference problems.
3. If it oscillates past the goal, try reducing `StarterDrive.TURN_PROPORTIONAL_GAIN` from 0.015 to 0.012.
   If it approaches slowly without oscillation, try 0.018. Change one setting, rebuild, repeat.
   Keep `TURN_POWER = 0.18` initially. The target must settle within ±2° for 0.25 seconds.
   An IMU turn finishes from measured heading, not a wheel-spacing calculation.
4. For straight +24-inch and −24-inch tests, record heading deviation and lateral endpoint
   error. `HEADING_HOLD_PROPORTIONAL_GAIN` corrects a rightward error by commanding leftward steering even when backing.
   If correction oscillates, try 0.016 instead of 0.02. If it is weak, try 0.024 after verifying
   signs/mounting. Do not change ticks/inch to compensate for a wrong angle.
5. Every move/turn still has a four-second deadline. A stale/invalid IMU stops it. Stationary
   encoder counts while an IMU turn is incomplete must not be accepted as successful motion.

| Direction | Requested degrees | IMU final degrees | Protractor angle | Settle time / fault | Next single change |
|---|---|---|---|---|---|
| Right | 90 | | | | |
| Left | −90 | | | | |

### Exercise 2 — make the turn settle instead of wiggle

Use **BIOBUZZ Standard Aim Draft**, empty, with feed disabled. Select a visible cluster
in INIT. Read **Bearing deg / camera XY range in**, **Fresh / aligned**, and **Frame age sec**.
Bearing is the target's angle relative to the camera; range is the SDK's camera measurement.

1. With motion disabled, place the target slightly left, centered, then right of the camera.
   Write down bearing at each location: left should be positive, right negative. If the cluster
   is not fresh, fix visibility/selection before testing motion.
2. After direction checks enable motion. Start with the robot aimed a little to one side,
   target still visible, sticks centered and right bumper released. Hold A briefly. The absolute
   difference **bearing − OFFSET_DEGREES** should get smaller. If it grows, Stop; this is a
   direction/mount problem, not a reason to increase gain.
3. Repeat from the same start, holding A until **aligned** is true. Time roughly how long
   it takes and note whether it crosses back and forth over the desired direction.
4. If it repeatedly overshoots, try reducing `TURN_PROPORTIONAL_GAIN` from 0.015 to 0.012. If it approaches very
   slowly without overshoot, try 0.018. These are small trial changes, not guaranteed settings.
   Rebuild and repeat three times from each side after each change.
5. Keep `MAXIMUM_TURN_POWER` at 0.18 initially. It limits turn power even with a large error. If turning
   is too abrupt, try 0.15 separately. Do not change gain, cap, and tolerance together.

For example, a 10° error with gain 0.015 requests turn magnitude 0.15. A 20° error would
request 0.30, but the 0.18 cap limits it. The current alignment check requires error within
±2° continuously for 0.3 seconds. Widening that tolerance to make “aligned” appear does not
prove the launcher aims accurately.

### Exercise 2B — approach a tag, then return to the starting mark

Use **BIOBUZZ Measure Tag Approach** with an empty robot, a fixed full-size cluster, a clear
floor area, and the camera mounted as it will be on the robot. This program never spins the
launcher or feeds balls. It approaches the target and automatically reverses the measured
approach movements; it does not run a parking route. Remove its `@Disabled` only for testing.

1. Qualify straight travel and turns first. Set `MOTION_ENABLED = true`; keep feed disabled.
   Mark the chassis pointer position and heading line. Start close to the desired range,
   for example about two SDK inches farther away, with the target near the camera center.
   The default range goal is the midpoint of the shot window (32 inches for a 30–34 window),
   defined by `StarterBotPositionAuto.TAG_RANGE_INCHES`. These are camera-XY inches, not floor distance.
2. INIT, select alliance/cell with the D-pad, and check **Target / fresh**. Read **Range goal /
   actual inches** and **Bearing goal / actual degrees**. If the target is not fresh, fix the
   selection, lighting, print size, or visibility before Start. Do not use an actual moving hive
   for the first positioning tests.
3. Say the expected motion: turn to center the cluster, creep forward because actual range
   exceeds the goal, then turn to the shooting bearing. Clear the entire approach and return
   area, then Start. Record **Tag phase / travel inches** and the final **State / reason**.
   At the goal, it immediately begins its return; a recorder can video telemetry to read the
   final range/bearing. No one should approach it just because it paused between phases.
4. Once STOPPED, press Driver Station Stop and measure how far the pointer and heading are
   from the starting marks. Repeat three times. Then start about two SDK inches too close:
   it should back away, then return. Finally start with a small left or right aiming error.
5. If forward/backward motion oscillates around the goal, try reducing `TagApproach.RANGE_PROPORTIONAL_GAIN`
   from 0.035 to 0.028. If it moves slowly but does not oscillate, try 0.042. Change only one
   value, rebuild, and repeat the same start. Keep `MAXIMUM_FORWARD_POWER = 0.15` initially. Example:
   a +2-inch range error requests +0.07 forward power at gain 0.035; a −2-inch error requests
   −0.07. A smaller error requests less power; if it cannot overcome friction, investigate
   that behavior rather than increasing the travel limit.
6. `TagApproach.TURN_PROPORTIONAL_GAIN` and `MAXIMUM_TURN_POWER` control the two turning phases; use the same small-change
   method as Exercise 2. They are separate from the held TeleOp tuning in `HiveAim`.
   `BEARING_TOLERANCE_DEGREES` is initially 2°, and `RANGE_TOLERANCE_INCHES` is 0.5 inch.
   Final pose must remain settled for 0.3 seconds; the earlier phases settle for 0.2 seconds.
7. Hide the target before Start: expect no positioning motion. During an empty approach,
   cover the camera from a safe position outside the path: positioning must stop and the
   program should reverse its measured movement. Driver Station Stop cancels immediately;
   it never initiates an automatic return. Do not disconnect motors or hold wheels to simulate
   a stall; the software tests cover that logic.

The controller stops on any missing/stale pose, after four seconds, after six inches of
accumulated translation, or twelve inches of accumulated maximum wheel travel (including
turns). Either encoder failing to advance while powered for about 0.75 seconds causes a
stall fault and cancels return. These are proposed commissioning bounds, not collision sensors.
During the straight phase, bearing beyond ±8° means the robot needs a better starting pose;
it stops instead of following a curved path. If range changes during final alignment, it
refuses the shot rather than chasing the target again.

Each reversed segment has a two-second deadline and uses its recorded IMU heading. If return times out, the robot stops and
cancels parking. It uses recorded travel and IMU heading, so slip can leave it off the starting marks
even when the code reports completion. Measure that error; do not qualify a route until
both the approach and its return are repeatable within the team's chosen tolerances.

| Trial | Start SDK range/bearing | Expected direction | Final SDK range/bearing | Return pointer error / heading error | State/reason | One next change |
|---|---|---|---|---|---|---|
| Farther | | Forward | | | | |
| Closer | | Backward | | | | |
| Left/right | | Turn first | | | | |
| Hidden tag | | No approach | | | | |

### Exercise 3 — measure the camera-to-launcher aiming offset

A camera centered on a tag may still leave the launcher aimed beside the opening.
`OFFSET_DEGREES` is the bearing we want to see when the robot's actual shot is correctly aimed.

1. Choose one fixed shooting location. Mark pointer position and heading. With baseline
   TeleOp, run supervised one-ball trials at the same launcher speed, adjusting only heading
   in small steps until shots repeatedly enter the intended opening. Stop before repositioning
   or loading by hand. Baseline right bumper can feed automatically when speed rises; it is not spin-only.
2. Leave the robot in that successful orientation. Switch to Aim Draft, select the same cluster,
   and record bearing with sticks neutral, A and right bumper released. Do not press A and overwrite the
   orientation you are trying to measure. Record several fresh readings.
3. Use their average as a candidate `OFFSET_DEGREES`. Example: readings +3.0°, +3.2°, +2.8°
   give a candidate +3.0°. Preserve the sign; do not guess it from which way a ball missed.
4. Empty the robot, rebuild, and test held A from both sides. It should settle near that bearing.
   Recheck actual shots at the same location before accepting the value.

If no heading produces repeatable hits, investigate trajectory, wheel speed, mounting, and
mechanical consistency first. An angular offset cannot fix a shot that is always too low.

### Exercise 4 — find a range window from actual hits

Keep launcher speed and offset fixed. Use floor marks about two inches apart along a line
toward/away from the target, covering a small region around a successful shooting location.

1. At each mark, use Aim Draft with no shot requested to read the camera XY range. Record
   it alongside the tape distance from the chassis pointer to one named, fixed field reference.
   These are different measurements; a tilted camera need not report the floor tape distance.
2. Use the baseline to test five individually observed shots at that mark, returning to the
   same pose and comparable cell condition. Record hits, misses, and tips separately. A hit
   means the ball entered the intended opening; a tip is a separate observation.
3. As a proposed classroom criterion, require five hits out of five at each accepted mark.
   Test between accepted marks too; do not bridge across a failed location. Repeat on another
   run before trusting the interval. This small sample is a starting check, not proof of reliability.
4. Put the smallest and largest **SDK ranges in the tested successful interval** into
   `MINIMUM_RANGE_INCHES` and `MAXIMUM_RANGE_INCHES`. Example only: repeated successful tests spanning SDK
   readings 31–33 inches suggest a trial window of 31–33, not the supplied 30–34.
5. Enable assisted feed only after those trials. Test A + right bumper at a qualified location and verify
   that outside-window or missing-target conditions withhold feed. Begin with an empty robot
   for refusal tests; do not aim loaded shots outside the test area.

If all shots miss, there is no qualified window yet. Do not enlarge the window just to make
feeding start. If launcher speed changes, repeat the range and offset exercises.

| Floor mark / tape reference | SDK range in | Bearing deg | Launcher ticks/s | Hits / 5 | Tips | Miss direction / next test |
|---|---|---|---|---|---|---|
| | | | | | | |

### Exercise 5 — observe spin-up and choose a burst duration

First use Aim Draft with an **empty** mechanism: keep A released, triggers neutral, hold right bumper,
and watch **Launcher ticks/s**. Record speed readings while it rises and settles. Release right bumper
and confirm speed falls. The auto currently requires 1200–1300 ticks/s continuously for
0.3 seconds; **SPINUP** means the wheel is getting ready, **FEED** means the feeder is running.
If speed never stabilizes, inspect encoder wiring, battery, and mechanism drag with power off;
do not bypass the speed check or start changing PIDF numbers at random.

For a supervised loaded test, first qualify the route with empty DRY_RUN trials. Then select
that card and SHOOT_AND_PARK, using its marked start and cell. Enable feeding only after the
range/offset work. Load one ball before Start and clear both the shot area and parking route.
This is a practice test; a match starts with four POLLEN. The robot will park after the shot.

1. Start and observe the displayed shot phase. Record whether the ball exits, the hit result,
   and the final reason. A recorder can use a video to estimate time from feeder start to exit;
   no one needs to approach the running launcher.
2. The supplied `FEED_SECONDS = 0.6` is a trial. If the run completes but the ball has not
   exited, Stop and inspect for a jam. If the mechanism is clear and observation shows it needs
   more time, try 0.7 seconds. Repeat several one-ball trials before increasing the load.
3. If the run faults for speed loss, increasing feed time will not help: the speed gate stopped
   it. Record the lowest observed speed and inspect the mechanism. Change a speed limit only
   with evidence that shots at that speed still work; never widen it merely to remove the fault.
4. Keep the shortest duration that repeats successfully with a modest measured allowance.
   Record actual ball count; 0.6 seconds does not mean one ball. Re-test if ball loading changes.

### Exercise 6 — combine the route and shot

Use the route-card procedure above: mark start, shooting, and parking poses and measure
both distances and heading changes. First run empty in DRY_RUN. Record whether the final
robot footprint is clearly in the LOADING ZONE and off the wall; record elapsed time too.

If straight travel is wrong, revisit ticks/inch; if turns are wrong, revisit IMU mounting and heading gains. If both commands work but the destination is wrong, revise the route measurements.
Do not widen the shot window to hide a route that stops at the wrong shooting location.

Repeat five times, including aiming from both sides and a hidden-target case. Test PARK_ONLY,
then supervised SHOOT_AND_PARK from the same marks. Observe the reverse tag movements: the robot
must return to the approach position and heading before beginning the parking turn. A failed drive stops
the run; target loss may skip shooting and still park. Record the final state/reason, actual
end footprint, time, hits, and tips. Validate each alliance/cell/start card independently.

## Source checks

- [Current manual and updates](https://ftc-resources.firstinspires.org/ftc/game), TU02, checked 2026-09-30; §§9.6–9.9, 10.4–10.5, G410, G417–G418, R102/R105.
- [Figure 9-10: HIVE clearance diagram (TU01)](https://ftc-resources.firstinspires.org/ftc/game/tu-01#page=1).
- SDK source in the resolved `org.firstinspires.ftc:Vision:12.0.0` sources JAR:
  `AprilTagGameDatabase`, `AprilTagClusterDetection`, `AprilTagClusterMetadata`,
  `AprilTagDetection`, `AprilTagProcessor`, and `AprilTagPoseFtc`.

## Run the current software checks

Local Java tests live under
`TeamCode/src/test/java/org/firstinspires/ftc/teamcode/biobuzz/`, beside the matching
production package in `src/main/java`. They use JUnit 4, a Java test framework that
Gradle and Android Studio discover through `@Test` methods. Test code is not packaged
in the robot application. `docs/` contains guides, not executable tests.

In Android Studio, sync the Gradle project after changing dependencies, then use the
Run arrow next to a test class or `@Test` method. Each named test appears in the test
results. Android Studio uses the Java Development Kit (JDK) configured for the project.

To run the same tests from a terminal, open the repository root (the folder containing
`gradlew` and `gradlew.bat`). Use the command for your operating system:

**Windows PowerShell:**

```powershell
.\gradlew.bat :TeamCode:testDebugUnitTest
```

**macOS or Linux:**

```sh
./gradlew :TeamCode:testDebugUnitTest
```

Terminal commands need a compatible JDK configured on that computer. If Java cannot be
found or Gradle reports an incompatible Java version, use Android Studio to run the tests
and ask a mentor to help configure the terminal. Do not copy another computer's JDK path.

The first run may download JUnit and other test dependencies. Once dependencies are
cached, add `--offline` to run without network access. There is no separate shell runner,
manual `javac` step, or RobotCore JAR lookup.

To run just one class from the terminal, add `--tests` and its full package/class name.
For example, on macOS or Linux (use `.\gradlew.bat` instead of `./gradlew` in Windows PowerShell):

```sh
./gradlew :TeamCode:testDebugUnitTest --tests 'org.firstinspires.ftc.teamcode.biobuzz.TagDriveTest'
```

Gradle writes the HTML report to
`TeamCode/build/reports/tests/testDebugUnitTest/index.html` and machine-readable XML
results to `TeamCode/build/test-results/testDebugUnitTest/`.

| Test class | What it verifies |
|---|---|
| `AimMathTest` | Turn direction/limits, valid camera readings, and shot range windows. |
| `AutoShotTest` | Speed settling, feed timing, target/speed loss, timeout, and terminal states. |
| `DriveRoutesTest` | Wheel geometry, completion settling, and all eight independent route cards. |
| `ImuDriveTest` | Heading-controlled motion, sensor loss, local position math, and start-tag checks. |
| `SubsystemTest` | Drive and launcher outputs on completion, cancellation, and faults. |
| `TagApproachTest` | Camera approach phases, bounded powers, settling, and refusal conditions. |
| `TagDriveTest` | Recorded return phases, partial travel, stalls, timeouts, and cancellation. |
| `TargetSelectionTest` | Explicit selection, alliance locking, and allowed button presses. |
| `StarterRobotTest` | Shared manual outputs, power limits, and complete/partial-init shutdown. |

`TestHardware` supplies fake motors, servos, and heading readings. The tests run on the
computer's Java Virtual Machine (JVM), without an Android device or robot. They do not
simulate camera timing, tire slip, drivetrain dynamics, or physical ball feeding.
Tests that actually require Android would belong in `TeamCode/src/androidTest/java/`;
none of these tests need that source folder.

The unit-test task also compiles the production Java code. For compilation without tests, use the command below on macOS or Linux, or replace
`./gradlew` with `.\gradlew.bat` in Windows PowerShell:

```sh
./gradlew :TeamCode:compileDebugJavaWithJavac --offline
```

Physical calibration remains pending. Software test success does not qualify mounting,
starting positions, camera measurements, launcher performance, or route accuracy.
