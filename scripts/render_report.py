"""Render the five explicit REPORT.md pages to a portable PDF (optional tool)."""
from pathlib import Path
import html
import re

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, PageBreak

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "output/pdf/REPORT.pdf"


def fonts():
    candidates = [
        (Path("C:/Windows/Fonts"), "arial.ttf", "arialbd.ttf", "consola.ttf"),
        (Path("/usr/share/fonts/truetype/dejavu"), "DejaVuSans.ttf", "DejaVuSans-Bold.ttf", "DejaVuSansMono.ttf"),
    ]
    for directory, normal, bold, mono in candidates:
        if (directory / normal).exists():
            for name, filename in [("Body", normal), ("BodyBold", bold), ("Mono", mono)]:
                pdfmetrics.registerFont(TTFont(name, str(directory / filename)))
            pdfmetrics.registerFontFamily("Body", normal="Body", bold="BodyBold", italic="Body", boldItalic="BodyBold")
            return
    raise RuntimeError("Install Arial or DejaVu Sans to render Greek complexity notation.")


def inline(text):
    text = html.escape(text)
    text = re.sub(r"`([^`]+)`", r'<font name="Mono">\1</font>', text)
    text = re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", text)
    text = re.sub(r"\[([^\]]+)\]\((https?://[^)]+)\)", r'<link href="\2" color="#2168b2">\1</link>', text)
    return text


def main():
    fonts()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    body = ParagraphStyle("body", fontName="Body", fontSize=9, leading=12, spaceAfter=7, textColor=colors.HexColor("#263344"))
    headings = {
        1: ParagraphStyle("title", parent=body, fontName="BodyBold", fontSize=18, leading=22, spaceAfter=10),
        2: ParagraphStyle("h2", parent=body, fontName="BodyBold", fontSize=13, leading=17, spaceBefore=4, spaceAfter=9, keepWithNext=True),
        3: ParagraphStyle("h3", parent=body, fontName="BodyBold", fontSize=10.3, leading=14, spaceBefore=5, spaceAfter=6, keepWithNext=True),
    }
    cell = ParagraphStyle("cell", parent=body, fontSize=7.5, leading=9.8, spaceAfter=0)
    lines = (ROOT / "REPORT.md").read_text(encoding="utf-8").splitlines()
    story = []
    index = 0
    while index < len(lines):
        line = lines[index].strip()
        if not line:
            index += 1
            continue
        if 'class="pagebreak"' in line:
            story.append(PageBreak())
        elif line.startswith("#"):
            level = len(line) - len(line.lstrip("#"))
            story.append(Paragraph(inline(line[level:].strip()), headings[level]))
        elif line.startswith("!["):
            path = ROOT / re.search(r"\]\(([^)]+)\)", line).group(1)
            from PIL import Image as PillowImage
            with PillowImage.open(path) as picture:
                width, height = picture.size
            # Two figures and captions fit on each workload page.
            display_width = 470 if "build_heap" in path.name else 480
            story.append(Image(str(path), width=display_width, height=display_width * height / width))
            story.append(Spacer(1, 6))
        elif line.startswith("|"):
            rows = []
            while index < len(lines) and lines[index].strip().startswith("|"):
                cells = [x.strip() for x in lines[index].strip().strip("|").split("|")]
                if not all(re.fullmatch(r"[-: ]+", x) for x in cells):
                    rows.append([Paragraph(inline(x), cell) for x in cells])
                index += 1
            table = Table(rows, colWidths=[96, 43, 75, 43, 82, 176], repeatRows=1, hAlign="LEFT")
            table.setStyle(TableStyle([
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#e1eaf5")),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#f4f7fa")]),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LINEBELOW", (0, 0), (-1, 0), 0.6, colors.HexColor("#9aafc8")),
                ("LEFTPADDING", (0, 0), (-1, -1), 4),
                ("RIGHTPADDING", (0, 0), (-1, -1), 4),
                ("TOPPADDING", (0, 0), (-1, -1), 4),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
            ]))
            story.extend([table, Spacer(1, 10)])
            continue
        else:
            paragraph = [line]
            while index + 1 < len(lines) and lines[index + 1].strip() and not lines[index + 1].startswith(("#", "|", "![", "<div")):
                index += 1
                paragraph.append(lines[index].strip())
            story.append(Paragraph(inline(" ".join(paragraph)), body))
        index += 1

    def footer(canvas, document):
        canvas.saveState()
        canvas.setStrokeColor(colors.HexColor("#cbd7e5"))
        canvas.line(40, 31, A4[0] - 40, 31)
        canvas.setFont("Body", 8)
        canvas.setFillColor(colors.HexColor("#64748b"))
        canvas.drawString(40, 18, "DAA Assignment 2 | Yerbol Alikhan | SE-2517")
        canvas.drawRightString(A4[0] - 40, 18, str(document.page))
        canvas.restoreState()

    document = SimpleDocTemplate(str(OUTPUT), pagesize=A4, leftMargin=40, rightMargin=40,
                                 topMargin=32, bottomMargin=43, title="DAA Assignment 2 - Data Structures",
                                 author="Yerbol Alikhan, SE-2517")
    document.build(story, onFirstPage=footer, onLaterPages=footer)
    print(OUTPUT)


if __name__ == "__main__":
    main()
