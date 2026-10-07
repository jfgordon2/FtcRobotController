"""Build the Meeting 5 handout and append FIRST's unscaled Letter targets.

Requires reportlab and pypdf. Run from any directory. See the notes in
assets/print for where the target sheets come from. Do not replace the official target artwork with
screenshots or redrawings. The source PDF uses optional-content layers: clone
its document catalog, including /OCProperties, before inserting the handout.
"""

from io import BytesIO
from pathlib import Path

from pypdf import PdfReader, PdfWriter
from pypdf.generic import BooleanObject, DictionaryObject, NameObject
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfgen import canvas
from reportlab.platypus import Paragraph, Table, TableStyle


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "assets/print/biobuzz-apriltags-letter-v1.pdf"
OUTPUT = ROOT / "output/pdf/meeting5-shooter-auto-print-pack.pdf"
INK = colors.HexColor("#153344")
BLUE = colors.HexColor("#176275")
PALE = colors.HexColor("#eaf2f4")
GRAY = colors.HexColor("#52616b")
WIDTH = 516
BODY = ParagraphStyle("body", fontName="Helvetica", fontSize=10.5,
                      leading=14, textColor=INK, spaceAfter=0)
SMALL = ParagraphStyle("small", parent=BODY, fontSize=9.2, leading=12)
CELL = ParagraphStyle("cell", parent=BODY, fontSize=9.5, leading=12)
HEAD = ParagraphStyle("head", parent=CELL, fontName="Helvetica-Bold",
                      textColor=colors.white)
SUBHEAD = ParagraphStyle("subhead", parent=BODY, fontName="Helvetica-Bold",
                         fontSize=12.5, leading=16, textColor=BLUE)


HANDOUT_PAGES = 4
TOTAL_PAGES = HANDOUT_PAGES + 8


class Handout:
    def __init__(self):
        self.stream = BytesIO()
        self.c = canvas.Canvas(self.stream, pagesize=(612, 792), invariant=1)
        self.c.setTitle("Meeting 5: tune the shooter and build an auto path")
        self.page = 0
        self.y = 0

    def begin(self, title, subtitle):
        if self.page:
            self.c.showPage()
        self.page += 1
        self.c.setFillColor(BLUE)
        self.c.rect(0, 752, 612, 40, fill=1, stroke=0)
        self.c.setFillColor(colors.white)
        self.c.setFont("Helvetica-Bold", 10)
        self.c.drawString(48, 767, "BIOBUZZ  |  MEETING 5")
        self.c.setFillColor(INK)
        self.c.setFont("Helvetica-Bold", 20)
        self.c.drawString(48, 719, title)
        self.y = 697
        self.p(subtitle, SMALL, after=12)
        self.c.setStrokeColor(colors.HexColor("#bccbd1"))
        self.c.line(48, 43, 564, 43)
        self.c.setFont("Helvetica", 8)
        self.c.setFillColor(GRAY)
        self.c.drawString(48, 29, "Print Letter, Actual Size / 100%, single-sided. "
                                  f"AprilTag sheets: pages {HANDOUT_PAGES + 1}-{TOTAL_PAGES}.")
        self.c.drawRightString(564, 29, f"{self.page} / {TOTAL_PAGES}")

    def p(self, text, style=BODY, after=8):
        item = Paragraph(text, style)
        _, h = item.wrap(WIDTH, 700)
        if self.y - h < 55:
            raise ValueError(f"Page {self.page} overflows: {text[:70]}")
        item.drawOn(self.c, 48, self.y - h)
        self.y -= h + after

    def heading(self, text):
        self.p(text, SUBHEAD, after=5)

    def table(self, rows, widths, heights=None, header=True):
        data = []
        for r, row in enumerate(rows):
            style = HEAD if header and r == 0 else CELL
            data.append([Paragraph(str(value), style) for value in row])
        item = Table(data, colWidths=widths, rowHeights=heights)
        commands = [("VALIGN", (0, 0), (-1, -1), "TOP"),
                    ("LEFTPADDING", (0, 0), (-1, -1), 6),
                    ("RIGHTPADDING", (0, 0), (-1, -1), 6),
                    ("TOPPADDING", (0, 0), (-1, -1), 5),
                    ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
                    ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#b4c3c9"))]
        if header:
            commands.append(("BACKGROUND", (0, 0), (-1, 0), BLUE))
        item.setStyle(TableStyle(commands))
        _, h = item.wrap(WIDTH, 700)
        if self.y - h < 55:
            raise ValueError(f"Table overflows page {self.page}")
        item.drawOn(self.c, 48, self.y - h)
        self.y -= h + 9

    def field_diagram(self):
        """Schematic of the tape layout. Not to scale; use the written distances."""
        c = self.c
        top = self.y - 6
        height = 158
        base = top - height
        c.setStrokeColor(BLUE)
        c.setFillColor(INK)
        # Wall, front view.
        c.setLineWidth(1)
        c.line(52, base + 11, 262, base + 11)
        c.setLineWidth(1.4)
        c.rect(100, base + 102, 110, 40, fill=0, stroke=1)
        c.setFillColor(PALE)
        c.rect(120, base + 80, 70, 15, fill=1, stroke=1)
        c.setFillColor(INK)
        c.setFont("Helvetica-Bold", 9)
        c.drawCentredString(155, base + 120, "HIVE OPENING")
        c.setFont("Helvetica", 8.5)
        c.drawCentredString(155, base + 146, "20 in wide")
        c.drawCentredString(155, base + 85, "AprilTags")
        c.drawString(214, base + 138, "top 65.5 in")
        c.drawString(214, base + 102, "bottom 53.5 in")
        c.drawString(194, base + 83, "2 in below tape")
        c.setDash(3, 3)
        c.line(155, base + 78, 155, base + 11)
        c.setDash()
        c.drawCentredString(155, base + 2, "WALL (front view)")
        # Floor, top view.
        c.setLineWidth(3)
        c.line(300, base + 144, 560, base + 144)
        c.setLineWidth(1.2)
        c.drawString(300, base + 148, "wall with target")
        c.setDash(3, 3)
        c.line(390, base + 144, 390, base + 18)
        c.setDash()
        for inches, y in ((18, 142), (26, 116), (34, 90)):
            c.line(378, base + y * 0.8, 402, base + y * 0.8)
            c.drawString(408, base + y * 0.8 - 3, f"{inches} in mark")
        c.line(318, base + 32, 470, base + 32)
        c.drawString(476, base + 29, "start line: 52 in")
        c.rect(500, base + 56, 36, 61, fill=0, stroke=1)
        c.drawString(486, base + 122, "parking box")
        c.drawString(490, base + 46, "23 x 11 in")
        c.drawCentredString(430, base + 2, "FLOOR (top view)")
        self.y = base - 12

    def finish(self):
        assert self.page == HANDOUT_PAGES
        self.c.save()
        self.stream.seek(0)
        return PdfReader(self.stream)


def guide():
    doc = Handout()
    doc.begin("Tune the shooter. Build an auto path.",
              "Robot: one of last season's practice robots. Field: tape on a wall and on the floor.")
    doc.heading("Goal")
    doc.p("Leave with a launcher speed table we measured ourselves and one autonomous "
          "path that ends inside a taped parking box.")
    doc.heading("Safety")
    doc.p("Stand behind the robot whenever the launcher can spin. One student drives; "
          "a different student keeps a hand near <b>Stop</b> and may press it at any time. "
          "Press Stop and wait for the wheel to stop before loading, measuring or touching "
          "the robot. Clear a jam only with the robot switched off.")
    doc.heading("Plan")
    doc.table([
        ["Time", "What we do"],
        ["0:00-0:10", "Goal, safety, first jobs: driver, Stop, recorder, measurer. Swap often."],
        ["0:10-0:45", "Build the practice field (page 2). Check the camera sees the tags."],
        ["0:45-1:30", "Tune the shooter at three distances (page 3)."],
        ["1:30-1:40", "Break. Press Stop first."],
        ["1:40-2:05", "Measure a 24-inch drive and a 90-degree turn (page 4)."],
        ["2:05-2:45", "Build the auto path and run it empty (page 4)."],
        ["2:45-3:00", "Type the numbers into the code, save, photograph the tape layout."],
    ], [80, 436])
    doc.heading("Controls")
    doc.table([
        ["Program", "Controls"],
        ["Tune Shooter",
         "INIT: D-pad LEFT then DOWN picks RED AUDIENCE. After Start: sticks drive slowly; "
         "hold A to face the tags; D-pad UP/DOWN changes speed by 25; hold right bumper to "
         "spin; tap Y for one short shot once the screen says At speed."],
        ["Tune Drive",
         "INIT: D-pad UP/DOWN picks 24 in forward/back; LEFT/RIGHT picks a 90-degree turn. "
         "Start runs it once."],
        ["Autonomous Route",
         "INIT: D-pad LEFT then DOWN picks RED AUDIENCE; A picks Start A; X switches dry run on/off. "
         "A dry run never spins or feeds."],
    ], [150, 366])
    doc.p("One robot runs at a time. Everyone else tapes the route, graphs the results, "
          "or reads the code for the next step.", SMALL)

    doc.begin("Build the practice field",
              "The wall stands in for our hive. The sizes come from the game manual.")
    doc.field_diagram()
    doc.table([
        ["Tape this", "Measurement", "It stands for"],
        ["Wall rectangle", "20 in wide. Bottom edge 53.5 in above the floor, top edge 65.5 in.",
         "The upward CELL opening, seen from in front."],
        ["AprilTags", "Centered under the rectangle. Top of the black squares 2 in below the bottom tape.",
         "The tags under the CELL."],
        ["Center line", "Straight out from the wall, under the middle of the rectangle.",
         "Lining up on our hive."],
        ["Start line", "Across the center line, 52 in from the wall.",
         "The field wall we start against (our estimate)."],
        ["Shooting marks", "Front bumper 18, 26 and 34 in from the wall.",
         "The room between hive and field wall."],
        ["Parking box", "23 in by 11 in, off to one side.",
         "The LOADING ZONE at its real size."],
    ], [92, 262, 162])
    doc.heading("AprilTag sheets")
    doc.table([
        ["Target (choose in INIT)", "Tag IDs", "Packet pages: right / left"],
        ["BLUE AUDIENCE", "38, 39, 40, 41", "5 / 6"],
        ["BLUE SCORING (far side)", "42, 43, 44, 45", "7 / 8"],
        ["<b>RED AUDIENCE - use this one</b>", "34, 35, 36, 37", "<b>9 / 10</b>"],
        ["RED SCORING (far side)", "30, 31, 32, 33", "11 / 12"],
    ], [230, 130, 156])
    doc.p("Print at Actual Size / 100%. A black square measures <b>3.25 in</b>. Tape the "
          "LEFT sheet to the left of the RIGHT sheet so the printed center marks meet; the "
          "labels go below the tags. Tilt the webcam up until INIT shows <b>Tag seen: true</b> "
          "from the 34-inch mark. Use last season's balls. If the launcher cannot reach the "
          "rectangle, lower it, keep its size, and write the new height on page 3.", SMALL)

    doc.begin("Shooting worksheet",
              "Program: Tune Shooter. Shoot five, count, change ONE thing, repeat.")
    doc.p("<b>1.</b> Front bumper on the mark, on the center line. Hold A to face the tags. "
          "<b>2.</b> Hold right bumper; when At speed, tap Y once per ball. Shoot five. "
          "<b>3.</b> Write one row. <b>4.</b> Mostly low: speed up one step. Mostly high: "
          "speed down one step. Left or right: aim again, do not change speed. "
          "<b>5.</b> At four or more hits, shoot five more to check, then circle the row. "
          "<b>6.</b> Do the 34, 26 and 18 inch marks.")
    doc.p("Rectangle bottom edge: ________ in &nbsp;&nbsp; Target printed: ______________ "
          "&nbsp;&nbsp; Top speed allowed: ________ &nbsp;&nbsp; Battery volts at rest: ______ "
          "(below 12.5: swap, then re-shoot one row)", SMALL)
    rows = [["Mark (in<br/>from wall)", "Camera<br/>range (in)", "Speed<br/>(ticks/s)",
             "Hits<br/>out of 5", "Misses: low, high,<br/>left, right", "Next change"]]
    rows += [["", "", "", "", "", ""] for _ in range(12)]
    doc.table(rows, [70, 70, 70, 56, 130, 120], heights=[34] + [27] * 12)
    doc.heading("Put the circled rows in the code")
    doc.p("In ShooterCalibration.java, type the circled rows into RANGE_INCHES and "
          "SPEED_TICKS_PER_SECOND, closest range first. Between two rows the robot picks a "
          "speed in between. Outside the rows it refuses to shoot.", SMALL)
    doc.p("Row 1: range ______ speed ______ &nbsp;&nbsp; Row 2: range ______ speed ______ "
          "&nbsp;&nbsp; Row 3: range ______ speed ______", SMALL)

    doc.begin("Drive, turn and auto path worksheet",
              "Take the balls out for everything on this page.")
    doc.heading("Measure a drive and a turn  (Tune Drive)")
    doc.p("Run it, write the ticks from the screen, press Stop, then measure with a tape. "
          "Ticks per inch = average ticks / real inches. The code starts at about 45.", SMALL)
    doc.table([
        ["Asked for", "Left ticks", "Right ticks", "Measured", "Ticks per inch", "Next change"],
        ["24 in forward", "", "", "", "", ""],
        ["24 in forward", "", "", "", "", ""],
        ["90 degrees right", "", "", "", "not used", ""],
        ["90 degrees left", "", "", "", "not used", ""],
    ], [96, 70, 70, 80, 84, 116], heights=[24, 26, 26, 26, 26])
    doc.heading("Build the auto path  (Autonomous Route)")
    doc.p("A route is a list of steps in Routes.java: <b>drive(inches)</b>, "
          "<b>turn(degrees)</b> and <b>aimAndShoot()</b>, which faces the tags, shoots if "
          "switched on, and turns back. Start with the back bumper on the start line, on "
          "the center line, facing the wall. Type your numbers into the RED_AUDIENCE_A "
          "list: first drive, aimAndShoot, park turn, park drive. Change one number per "
          "run. Three runs in a row that end in the box means done.", SMALL)
    rows = [["Run", "First drive<br/>(in)", "Range at<br/>stop (in)", "Park turn<br/>(degrees)",
             "Park drive<br/>(in)", "Ended in<br/>box?", "Seconds", "Next change"]]
    rows += [[str(i), "", "", "", "", "", "", ""] for i in range(1, 6)]
    doc.table(rows, [34, 62, 62, 62, 62, 58, 52, 124], heights=[34] + [25] * 5)
    doc.heading("If it stops early")
    doc.table([
        ["The screen says", "Try this"],
        ["Not moving: ...", "Ask the mentor: a setup switch is still off."],
        ["Shot skipped: tags not lined up in time", "Check camera tilt, lighting, and the target you chose."],
        ["Shot skipped: speed table has no rows ...", "The first drive stopped outside your measured ranges."],
        ["Drive/turn timeout", "Something blocked a wheel, or the distance is too long for 4 seconds."],
    ], [200, 316])
    doc.p("In a match: leaving the field wall in AUTO is 3 points, ending AUTO partly in "
          "the LOADING ZONE is 5, and tipping the hive is 20.", SMALL)
    return doc.finish()


def build():
    targets = PdfReader(SOURCE)
    assert len(targets.pages) == 8
    assert all(tuple(p.mediabox) == (0, 0, 792, 612) for p in targets.pages)
    # FIRST's source bookmarks reference absent page objects (148-151). Rebuild
    # valid packet bookmarks; modifying the in-memory reader leaves the file intact.
    targets.trailer["/Root"].pop(NameObject("/Outlines"), None)
    handout = guide()
    writer = PdfWriter()
    writer.clone_document_from_reader(targets)
    writer.merge(0, handout, import_outline=False)
    writer.add_metadata({"/Title": "Meeting 5: Shooter Tuning, Auto Path and BIOBUZZ Targets",
                         "/Author": "BIOBUZZ programming mentorship; target artwork by FIRST",
                         "/Subject": "Four-page handout plus eight official Letter target sheets"})
    writer.root_object[NameObject("/ViewerPreferences")] = DictionaryObject({
        NameObject("/PrintScaling"): NameObject("/None"),
        NameObject("/Duplex"): NameObject("/Simplex"),
        NameObject("/PickTrayByPDFSize"): BooleanObject(True),
    })
    for title, index in [("Plan, safety and controls", 0), ("Build the practice field", 1),
                         ("Shooting worksheet", 2), ("Drive, turn and auto path worksheet", 3),
                         ("BLUE AUDIENCE: right, then left", 4),
                         ("BLUE SCORING: right, then left", 6),
                         ("RED AUDIENCE: right, then left", 8),
                         ("RED SCORING: right, then left", 10)]:
        writer.add_outline_item(title, index)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with OUTPUT.open("wb") as stream:
        writer.write(stream)
    result = PdfReader(OUTPUT)
    assert len(result.pages) == TOTAL_PAGES
    assert len(result.trailer["/Root"]["/OCProperties"]["/OCGs"]) == 24
    assert len(result.trailer["/Root"]["/OCProperties"]["/D"]["/OFF"]) == 16
    for i, original in enumerate(targets.pages):
        appended = result.pages[i + HANDOUT_PAGES]
        assert appended.mediabox == original.mediabox
        assert appended.get_contents().get_data() == original.get_contents().get_data()
    print(f"Created {OUTPUT}: {TOTAL_PAGES} pages; original target pages unchanged")


if __name__ == "__main__":
    build()
