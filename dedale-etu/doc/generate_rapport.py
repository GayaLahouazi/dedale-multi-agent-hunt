# -*- coding: utf-8 -*-
"""
Génère le rapport PDF du projet FoSyMa 2025-2026.
Auteur : Gaya LAHOUAZI - 21308739
Structure imposée :
  1 Introduction
  2 Architecture générale + UML
  3 Choix : exploration, communication, coordination, collecte
  4 Analyse des algorithmes : forces, limites, complexité, optimalité, arrêt
  5 Conclusion : synthèse, critique, extensions
"""
import os
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import cm, mm
from reportlab.lib.enums import TA_LEFT, TA_CENTER, TA_JUSTIFY
from reportlab.lib.colors import HexColor, black, grey, white
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Image, PageBreak,
    Table, TableStyle, ListFlowable, ListItem, Preformatted, KeepTogether,
)
from reportlab.graphics.shapes import (
    Drawing, Rect, String, Line, Polygon,
)

# --- Couleurs ----------------------------------------------------------------
SU_BLUE = HexColor('#1E2D58')
SU_RED  = HexColor('#E1303B')
LIGHT_GREY = HexColor('#777777')
CODE_BG = HexColor('#F4F4F4')
TABLE_HDR_BG = HexColor('#E5E9F2')
BOX_AGENT = HexColor('#D9E2F3')
BOX_BEHAV = HexColor('#FFE9CB')
BOX_KNOW  = HexColor('#D9F0D9')
BOX_ABST  = HexColor('#F0F0F0')

LOGO_PATH = r"C:/SPB_Data/git/dedale-etu/dedale-etu/doc/rapport_assets/logo_sorbonne_p0_0.png"
OUTPUT_PATH = r"C:/Users/Test/Downloads/Rapport_FoSyMa_Lahouazi.pdf"
RUNNING_HEADER = "Projet FoSyMa 2025–2026"
RUNNING_HEADER_RIGHT = "Patrouille Multi-agent dans Dedale"


# --- Styles ------------------------------------------------------------------
def make_styles():
    base = getSampleStyleSheet()
    s = {}
    s['CoverTitle'] = ParagraphStyle('CoverTitle', parent=base['Title'], fontName='Helvetica-Bold',
                                     fontSize=22, leading=28, textColor=SU_BLUE,
                                     alignment=TA_CENTER, spaceBefore=10, spaceAfter=8)
    s['CoverSubtitle'] = ParagraphStyle('CoverSubtitle', parent=base['Title'], fontName='Helvetica',
                                        fontSize=15, leading=20, textColor=black,
                                        alignment=TA_CENTER, spaceBefore=4, spaceAfter=20)
    s['CoverProject'] = ParagraphStyle('CoverProject', parent=base['Normal'], fontName='Helvetica',
                                       fontSize=13, textColor=LIGHT_GREY, alignment=TA_CENTER,
                                       spaceAfter=4)
    s['CoverAuthor'] = ParagraphStyle('CoverAuthor', parent=base['Normal'], fontName='Helvetica-Bold',
                                      fontSize=13, alignment=TA_CENTER, textColor=black,
                                      spaceAfter=2)
    s['CoverInfo'] = ParagraphStyle('CoverInfo', parent=base['Normal'], fontName='Helvetica',
                                    fontSize=11, alignment=TA_CENTER, textColor=black, spaceAfter=2)
    s['CoverDate'] = ParagraphStyle('CoverDate', parent=base['Normal'], fontName='Helvetica-Oblique',
                                    fontSize=10, alignment=TA_CENTER, textColor=LIGHT_GREY)
    s['H1'] = ParagraphStyle('H1', parent=base['Heading1'], fontName='Helvetica-Bold',
                             fontSize=14, leading=18, textColor=SU_BLUE,
                             spaceBefore=14, spaceAfter=8, keepWithNext=True)
    s['H2'] = ParagraphStyle('H2', parent=base['Heading2'], fontName='Helvetica-Bold',
                             fontSize=11.5, leading=15, textColor=SU_BLUE,
                             spaceBefore=8, spaceAfter=4, keepWithNext=True)
    s['H3'] = ParagraphStyle('H3', parent=base['Heading3'], fontName='Helvetica-Bold',
                             fontSize=10.5, leading=14, textColor=HexColor('#3A4870'),
                             spaceBefore=6, spaceAfter=3, keepWithNext=True)
    s['Body'] = ParagraphStyle('Body', parent=base['BodyText'], fontName='Helvetica',
                               fontSize=10, leading=13.5, alignment=TA_JUSTIFY, spaceAfter=6)
    s['Bullet'] = ParagraphStyle('Bullet', parent=s['Body'], leftIndent=14, bulletIndent=4,
                                 spaceAfter=2)
    s['Code'] = ParagraphStyle('Code', parent=base['Code'], fontName='Courier',
                               fontSize=8.5, leading=11, backColor=CODE_BG,
                               leftIndent=8, rightIndent=8, spaceBefore=4, spaceAfter=8,
                               textColor=HexColor('#222222'))
    s['Caption'] = ParagraphStyle('Caption', parent=base['Italic'], fontName='Helvetica-Oblique',
                                  fontSize=9, leading=12, alignment=TA_CENTER,
                                  textColor=LIGHT_GREY, spaceAfter=8)
    s['Reference'] = ParagraphStyle('Reference', parent=base['BodyText'], fontName='Helvetica',
                                    fontSize=9.5, leading=12, leftIndent=20,
                                    firstLineIndent=-20, spaceAfter=3)
    s['TocItem'] = ParagraphStyle('TocItem', parent=base['Normal'], fontName='Helvetica',
                                  fontSize=10.5, leading=15)
    s['TocSub'] = ParagraphStyle('TocSub', parent=base['Normal'], fontName='Helvetica',
                                 fontSize=10, leading=14, leftIndent=14, textColor=HexColor('#444444'))
    s['MathBlock'] = ParagraphStyle('MathBlock', parent=base['BodyText'], fontName='Helvetica-Oblique',
                                    fontSize=10, leading=14, alignment=TA_CENTER,
                                    spaceBefore=4, spaceAfter=8, textColor=HexColor('#222222'))
    return s


# --- En-têtes ---------------------------------------------------------------
def header_footer(canvas, doc):
    canvas.saveState()
    page = canvas.getPageNumber()
    if page > 1:
        canvas.setFont('Helvetica', 8.5)
        canvas.setFillColor(LIGHT_GREY)
        canvas.drawString(2.0 * cm, A4[1] - 1.4 * cm, RUNNING_HEADER)
        canvas.drawRightString(A4[0] - 2.0 * cm, A4[1] - 1.4 * cm, RUNNING_HEADER_RIGHT)
        canvas.setStrokeColor(LIGHT_GREY)
        canvas.setLineWidth(0.4)
        canvas.line(2.0 * cm, A4[1] - 1.55 * cm, A4[0] - 2.0 * cm, A4[1] - 1.55 * cm)
    canvas.setFont('Helvetica', 9)
    canvas.setFillColor(LIGHT_GREY)
    canvas.drawCentredString(A4[0] / 2.0, 1.2 * cm, str(page))
    canvas.restoreState()


# --- Helpers ----------------------------------------------------------------
def P(t, st): return Paragraph(t, st)
def section(t, story, st): story.append(P(t, st['H1']))
def subsection(t, story, st): story.append(P(t, st['H2']))


# --- Helpers UML ------------------------------------------------------------
def uml_box(d, x, y, w, h, name, lines=None, fill=BOX_AGENT, stereotype=None):
    """Dessine une boîte UML de classe."""
    d.add(Rect(x, y, w, h, fillColor=fill, strokeColor=SU_BLUE, strokeWidth=1.0))
    title_h = 16
    # ligne séparant titre et corps
    d.add(Line(x, y + h - title_h, x + w, y + h - title_h, strokeColor=SU_BLUE))
    if stereotype:
        d.add(String(x + w / 2, y + h - 8, stereotype,
                     textAnchor='middle', fontName='Helvetica-Oblique', fontSize=7.5,
                     fillColor=HexColor('#333')))
        d.add(String(x + w / 2, y + h - 18, name,
                     textAnchor='middle', fontName='Helvetica-Bold', fontSize=9.5))
        # ajuster la ligne de titre plus bas
        d.add(Line(x, y + h - 22, x + w, y + h - 22, strokeColor=SU_BLUE))
        body_top = y + h - 32
    else:
        d.add(String(x + w / 2, y + h - 11, name,
                     textAnchor='middle', fontName='Helvetica-Bold', fontSize=10))
        body_top = y + h - title_h - 2
    if lines:
        for i, ln in enumerate(lines):
            d.add(String(x + 5, body_top - (i + 1) * 10.5, ln,
                         fontName='Courier', fontSize=7.5, fillColor=HexColor('#222')))


def uml_inheritance(d, x1, y1, x2, y2):
    """Flèche d'héritage (triangle vide à la pointe), pointant vers (x2,y2)."""
    d.add(Line(x1, y1, x2, y2, strokeColor=SU_BLUE, strokeWidth=0.8))
    # triangle blanc
    sz = 6
    if y1 < y2:  # parent au-dessus
        pts = [x2 - sz, y2 - sz, x2 + sz, y2 - sz, x2, y2]
    else:
        pts = [x2 - sz, y2 + sz, x2 + sz, y2 + sz, x2, y2]
    d.add(Polygon(pts, fillColor=white, strokeColor=SU_BLUE, strokeWidth=1))


def uml_assoc(d, x1, y1, x2, y2, label=None, dashed=False):
    """Association (ligne simple ou pointillée avec optionnellement label)."""
    da = (3, 2) if dashed else None
    d.add(Line(x1, y1, x2, y2, strokeColor=HexColor('#444'),
               strokeWidth=0.7, strokeDashArray=da))
    if label:
        d.add(String((x1 + x2) / 2, (y1 + y2) / 2 + 3, label,
                     textAnchor='middle', fontName='Helvetica-Oblique',
                     fontSize=7.5, fillColor=HexColor('#444')))


def caption(text, st):
    return P(f"<b>Figure</b>&nbsp;: {text}", st['Caption'])


# --- Diagramme 1 : agents + behaviours --------------------------------------
def diagram_agents_behaviours(st):
    W, H = 16 * cm, 9.6 * cm
    d = Drawing(W, H)
    # Conventions : axe Y monte vers le haut
    # Couches : abstraites en haut, concrètes en bas
    # Niveau 1 (haut) : AbstractDedaleAgent, SimpleBehaviour, CyclicBehaviour
    uml_box(d, 20,    H - 50, 130, 36, "AbstractDedaleAgent",
            None, fill=BOX_ABST, stereotype="<<JADE Dedale>>")
    uml_box(d, 220,   H - 50, 110, 36, "SimpleBehaviour",
            None, fill=BOX_ABST, stereotype="<<JADE>>")
    uml_box(d, 360,   H - 50, 110, 36, "CyclicBehaviour",
            None, fill=BOX_ABST, stereotype="<<JADE>>")

    # Niveau 2 (milieu) : MultiExploreAgent, MultiExploreBehaviour, ReceiveShareBehaviour
    uml_box(d, 20,    H - 130, 145, 60, "MultiExploreAgent",
            ["- myMap", "- teamIntentions",
             "- terminationVotes",
             "+ getTeammates(): List", "# createBehaviours()"],
            fill=BOX_AGENT)
    uml_box(d, 200,   H - 130, 145, 60, "MultiExploreBehaviour",
            ["- previousIntention",
             "- idleTicks",
             "+ action()",
             "- chooseNextStep()",
             "- handleLocalTermination()"],
            fill=BOX_BEHAV)
    uml_box(d, 380,   H - 130, 130, 60, "ReceiveShareBehaviour",
            ["- template",
             "+ action()",
             "- handleTopo()",
             "- handleIntent()"],
            fill=BOX_BEHAV)

    # Niveau 3 (bas) : HuntAgent, HuntBehaviour, ReceiveHuntBehaviour
    uml_box(d, 20,    H - 220, 145, 70, "HuntAgent",
            ["- tracker: WumpusTracker",
             "- teamPositions",
             "- huntComplete: bool",
             "# createBehaviours() [override]"],
            fill=BOX_AGENT)
    uml_box(d, 200,   H - 220, 145, 70, "HuntBehaviour",
            ["+ action()",
             "- chooseNextStep()  [3 phases]",
             "- computeBlockingAssignment()",
             "- isGolemBlocked()",
             "- onCapture()"],
            fill=BOX_BEHAV)
    uml_box(d, 380,   H - 220, 130, 70, "ReceiveHuntBehaviour",
            ["+ action()",
             "- handleSighting()",
             "- handleCaptured()",
             "  ... (6 protocoles)"],
            fill=BOX_BEHAV)

    # Héritage : enfants pointent vers parents (en haut)
    uml_inheritance(d, 92,  H - 70,  92, H - 50)   # MultiExploreAgent -> AbstractDedaleAgent
    uml_inheritance(d, 92,  H - 150, 92, H - 130)  # HuntAgent -> MultiExploreAgent
    uml_inheritance(d, 272, H - 70,  272, H - 50)  # MultiExploreBehaviour -> SimpleBehaviour
    uml_inheritance(d, 272, H - 150, 272, H - 130) # HuntBehaviour -> SimpleBehaviour (via Multi…)? Non : HuntBehaviour étend SimpleBehaviour directement.
    uml_inheritance(d, 445, H - 70,  445, H - 50)  # ReceiveShareBehaviour -> CyclicBehaviour
    uml_inheritance(d, 445, H - 150, 445, H - 130) # ReceiveHuntBehaviour -> CyclicBehaviour

    # Associations agent ↔ behaviours (pointillées, "uses")
    uml_assoc(d, 165, H - 100, 200, H - 100, label="installs", dashed=True)
    uml_assoc(d, 165, H - 185, 200, H - 185, label="installs", dashed=True)
    uml_assoc(d, 345, H - 100, 380, H - 100, dashed=True)
    uml_assoc(d, 345, H - 185, 380, H - 185, dashed=True)

    return d


# --- Diagramme 2 : knowledge ------------------------------------------------
def diagram_knowledge(st):
    W, H = 16 * cm, 6.5 * cm
    d = Drawing(W, H)
    # 3 classes
    uml_box(d, 20, H - 130, 160, 110, "MapRepresentation",
            ["- g: Graph (GraphStream)",
             "- agentName: String",
             "+ addNode/addEdge()",
             "+ getOpenNodes()",
             "+ hasOpenNode()",
             "+ getNeighbors()",
             "+ getAllNodes()",
             "+ getDistance()",
             "+ getNodesWithinRadius(R)",
             "+ getShortestPath()",
             "+ mergeMap()"],
            fill=BOX_KNOW)
    uml_box(d, 220, H - 130, 160, 110, "WumpusTracker",
            ["- diffusionRadius: int",
             "- possiblePositions: Set",
             "- firstSighting: bool",
             "+ update(S+, S-, map)",
             "+ getPossiblePositions()",
             "+ isLocalised(): bool",
             "+ getRepresentativePosition()",
             "+ candidateCount(): int",
             "+ reset()"],
            fill=BOX_KNOW)
    uml_box(d, 410, H - 130, 160, 90, "HuntSightingMessage",
            ["+ senderPosition: String",
             "+ stenchPositive: Set<String>",
             "+ stenchNegative: Set<String>",
             "+ tick: long",
             "<<Serializable>>"],
            fill=BOX_KNOW)
    uml_assoc(d, 180, H - 75, 220, H - 75, label="utilise", dashed=True)
    uml_assoc(d, 380, H - 75, 410, H - 75, label="payload", dashed=True)
    return d


# --- Diagramme 3 : séquence d'un tick de chasse -----------------------------
def diagram_sequence(st):
    W, H = 16 * cm, 11.5 * cm
    d = Drawing(W, H)
    # Lifelines pour : H1, H2, Env, DF
    cols = [("H1 : HuntAgent", 80), ("H2 : HuntAgent", 220),
            ("Env (Dedale)", 360), ("DF / ACL", 480)]
    top = H - 22
    for name, x in cols:
        d.add(Rect(x - 55, top, 110, 18, fillColor=BOX_AGENT, strokeColor=SU_BLUE))
        d.add(String(x, top + 5, name, textAnchor='middle',
                     fontName='Helvetica-Bold', fontSize=9))
        d.add(Line(x, top - 3, x, 20, strokeColor=HexColor('#888'),
                   strokeDashArray=(2, 2)))

    # Helper pour msg
    def msg(y, x_from, x_to, label, dashed=False, color=HexColor('#222')):
        da = (3, 2) if dashed else None
        d.add(Line(x_from, y, x_to, y, strokeColor=color, strokeWidth=0.8,
                   strokeDashArray=da))
        # flèche
        if x_to > x_from:
            d.add(Polygon([x_to - 5, y + 3, x_to - 5, y - 3, x_to, y],
                          fillColor=color, strokeColor=color))
        else:
            d.add(Polygon([x_to + 5, y + 3, x_to + 5, y - 3, x_to, y],
                          fillColor=color, strokeColor=color))
        d.add(String((x_from + x_to) / 2, y + 3, label,
                     textAnchor='middle', fontName='Helvetica', fontSize=7.5,
                     fillColor=HexColor('#222')))

    def note(y, label):
        d.add(String(20, y, label, fontName='Helvetica-Oblique', fontSize=7.5,
                     fillColor=HexColor('#555')))

    note(top - 12, "1. Cadence + observation")
    msg(top - 25, 80, 360, "observe()")
    msg(top - 38, 360, 80, "lobs (positions, agentNames, Stench)", dashed=True)

    note(top - 55, "2. Maj carte locale + tracker")
    note(top - 67, "    map.addNode/Edge ; tracker.update(S+, S-)")

    note(top - 86, "3. Broadcast")
    msg(top - 99, 80, 480, "MEXPLO-TOPO (carte sérialisée)")
    msg(top - 112, 80, 480, "HUNT-SIGHTING (pos, S+, S-)")
    msg(top - 125, 480, 220, "→ relayé à H2", dashed=True)

    note(top - 144, "4. H2 met à jour son tracker avec le sighting de H1")

    note(top - 163, "5. Choix du nextStep (phase EXPLORE / TRACK / BLOCK)")
    note(top - 175, "    si BLOCK : computeBlockingAssignment(g)")

    note(top - 194, "6. Action")
    msg(top - 207, 80, 360, "moveTo(target)")
    msg(top - 220, 360, 80, "moved: bool", dashed=True)

    note(top - 239, "7. Si tous les voisins de g occupés : capture")
    msg(top - 252, 80, 480, "HUNT-CAPTURED (g)")
    msg(top - 265, 480, 220, "→ H2 stoppe", dashed=True)

    return d


# --- Pages ------------------------------------------------------------------
def cover(story, st):
    story.append(Spacer(1, 1.2 * cm))
    img = Image(LOGO_PATH, width=8.5 * cm, height=8.5 * cm * (264.0 / 952.0))
    img.hAlign = 'CENTER'
    story.append(img)
    story.append(Spacer(1, 1.5 * cm))
    story.append(P("Projet FoSyMa 2025–2026", st['CoverProject']))
    story.append(Spacer(1, 0.2 * cm))
    story.append(P("Patrouille Multi-agent dans Dedale", st['CoverTitle']))
    story.append(P("Exploration coopérative et capture coordonnée de Golems",
                   st['CoverSubtitle']))
    story.append(Spacer(1, 1.6 * cm))
    story.append(P("<b>Gaya LAHOUAZI</b> &mdash; 21308739", st['CoverAuthor']))
    story.append(Spacer(1, 0.4 * cm))
    story.append(P("Encadrant : Cédric Herpson", st['CoverInfo']))
    story.append(Spacer(1, 1.6 * cm))
    story.append(P("Département d'Informatique, Faculté des Sciences et Ingénierie",
                   st['CoverInfo']))
    story.append(P("<b>Sorbonne Université</b>", st['CoverInfo']))
    story.append(P("Master Informatique — AI2D", st['CoverInfo']))
    story.append(Spacer(1, 1.0 * cm))
    story.append(P("Version : 30 avril 2026", st['CoverDate']))
    story.append(PageBreak())


def toc(story, st):
    story.append(P("Table des matières", st['H1']))
    rows = [
        ("1 Introduction", "3", False),
        ("2 Architecture générale et diagrammes UML", "4", False),
        ("    2.1 Structure du projet", "4", True),
        ("    2.2 Diagramme UML — Agents et comportements", "4", True),
        ("    2.3 Diagramme UML — Module knowledge", "5", True),
        ("    2.4 Diagramme de séquence — protocole de chasse", "6", True),
        ("3 Choix de conception", "7", False),
        ("    3.1 Exploration", "7", True),
        ("    3.2 Communication", "7", True),
        ("    3.3 Coordination", "8", True),
        ("    3.4 Collecte", "9", True),
        ("4 Analyse des algorithmes", "9", False),
        ("    4.1 Exploration coordonnée par priorité lexicographique", "9", True),
        ("    4.2 WumpusTracker — triangulation Bayésienne", "10", True),
        ("    4.3 Stratégie de blocage greedy", "10", True),
        ("5 Conclusion", "11", False),
        ("Références", "11", False),
    ]
    data = [[P(name, st['TocSub' if sub else 'TocItem']),
             P(page, st['TocSub' if sub else 'TocItem'])]
            for name, page, sub in rows]
    t = Table(data, colWidths=[14.0 * cm, 1.5 * cm])
    t.setStyle(TableStyle([
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('ALIGN', (1, 0), (1, -1), 'RIGHT'),
        ('TOPPADDING', (0, 0), (-1, -1), 1),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 1),
    ]))
    story.append(t)
    story.append(PageBreak())


# --- Section 1 : Introduction -----------------------------------------------
def sec_intro(story, st):
    section("1 Introduction", story, st)
    story.append(P(
        "Le projet <b>FoSyMa 2025–2026</b> de l'UE <i>Fondements des Systèmes Multi-Agents</i> "
        "(Master Informatique AI2D) propose de développer, sur la plateforme JADE et "
        "l'environnement Dedale, une variante multi-agent du jeu <i>Hunt the Wumpus</i> "
        "(G. Yob, 1972). Une équipe d'agents coopératifs doit <b>explorer un environnement "
        "inconnu</b> représenté par un graphe non-orienté, puis <b>chasser et bloquer</b> un ou "
        "plusieurs Golems présents sur la carte.",
        st['Body']))
    story.append(P(
        "Le projet présente trois défis multi-agents classiques :", st['Body']))
    bullets = [
        "<b>Coordination décentralisée</b> sans coordinateur central, avec des communications "
        "limitées en portée et hétérogènes entre agents ;",
        "<b>Inférence partielle</b> : la position du Golem n'est pas directement observable. "
        "Sa présence n'est trahie que par les odeurs (<i>Stench</i>) qu'il diffuse dans son "
        "voisinage ;",
        "<b>Garantie de terminaison</b> dans un environnement dynamique : le Golem se déplace "
        "pendant la chasse.",
    ]
    story.append(ListFlowable(
        [ListItem(P(b, st['Bullet']), leftIndent=12) for b in bullets],
        bulletType='bullet', start='•'))
    story.append(P(
        "<b>Périmètre traité.</b> Le rapport couvre l'<b>exploration coopérative</b> "
        "(section 2 du sujet) et la <b>patrouille / capture</b> (section 3 du sujet, scénario "
        "1 Golem sur arbres et graphes ; scénario multi-Golems en best-effort). La "
        "<b>collecte de trésors</b> n'est pas implémentée dans ce périmètre — son intégration "
        "future est discutée en §3.4. La <b>distribution multi-machines</b> (semaine 9) est "
        "explicitement hors périmètre.",
        st['Body']))
    story.append(P(
        "<b>Plan.</b> La section&nbsp;2 décrit l'architecture globale du code et présente trois "
        "diagrammes UML (classes agents et behaviours, classes <i>knowledge</i>, séquence d'un "
        "tick de chasse). La section&nbsp;3 motive les choix de conception sur les quatre axes "
        "<i>exploration, communication, coordination, collecte</i>. La section&nbsp;4 analyse "
        "chaque algorithme : forces, limites, complexité (temps / mémoire / communication), "
        "optimalité et critère d'arrêt. La section&nbsp;5 propose une synthèse, un regard "
        "critique et des pistes d'extension.",
        st['Body']))


# --- Section 2 : Architecture & UML -----------------------------------------
def sec_arch(story, st):
    section("2 Architecture générale et diagrammes UML", story, st)

    subsection("2.1 Structure du projet", story, st)
    story.append(P(
        "Le code utilisateur est organisé en trois packages Java sous "
        "<font face='Courier'>eu.su.mas.dedaleEtu.mas</font>:",
        st['Body']))
    arch_data = [
        [P("<b>Package</b>", st['Body']), P("<b>Rôle</b>", st['Body']),
         P("<b>Fichiers ajoutés</b>", st['Body'])],
        [P("agents", st['Body']),
         P("Cycle de vie, enregistrement DF, état partagé", st['Body']),
         P("MultiExploreAgent, HuntAgent", st['Body'])],
        [P("behaviours", st['Body']),
         P("Boucles de décision et dispatchers ACL", st['Body']),
         P("MultiExploreBehaviour, HuntBehaviour, ReceiveShareBehaviour, ReceiveHuntBehaviour", st['Body'])],
        [P("knowledge", st['Body']),
         P("Modèles de données et inférence", st['Body']),
         P("MapRepresentation (étendue), WumpusTracker, HuntSightingMessage", st['Body'])],
    ]
    t = Table(arch_data, colWidths=[2.5 * cm, 6.0 * cm, 7.5 * cm])
    t.setStyle(TableStyle([
        ('GRID', (0, 0), (-1, -1), 0.4, LIGHT_GREY),
        ('BACKGROUND', (0, 0), (-1, 0), TABLE_HDR_BG),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
        ('FONTSIZE', (0, 0), (-1, -1), 9),
        ('TOPPADDING', (0, 0), (-1, -1), 3),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 3),
    ]))
    story.append(t)
    story.append(Spacer(1, 4))
    story.append(P(
        "Trois fichiers JSON de configuration sont fournis dans <font face='Courier'>resources/"
        "</font> : <font face='Courier'>agent-2hunt.json</font> (2 chasseurs + 1 Golem, R=1), "
        "<font face='Courier'>agent-3hunt.json</font> (3 chasseurs + 1 Golem, R=2), "
        "<font face='Courier'>agent-3hunt-2golems.json</font> (3 chasseurs + 2 Golems).",
        st['Body']))

    subsection("2.2 Diagramme UML — Agents et comportements", story, st)
    story.append(P(
        "La hiérarchie d'héritage suit fidèlement l'API de JADE et de Dedale. Les éléments "
        "abstraits hérités (en gris) ne sont pas modifiés. <font face='Courier'>HuntAgent</font> "
        "étend <font face='Courier'>MultiExploreAgent</font> via une méthode patron "
        "<font face='Courier'>createBehaviours()</font>, ce qui permet de réutiliser tout le "
        "code de coordination (DF, intentions, vote DONE) sans duplication.",
        st['Body']))
    story.append(diagram_agents_behaviours(st))
    story.append(caption(
        "diagramme de classes — agents (bleu) et comportements (orange). Les flèches "
        "à pointe creuse indiquent l'héritage, les pointillés une dépendance « installs ».",
        st))

    subsection("2.3 Diagramme UML — Module knowledge", story, st)
    story.append(P(
        "Les classes du module <font face='Courier'>knowledge</font> sont indépendantes de "
        "JADE : elles encapsulent respectivement la topologie partielle "
        "(<font face='Courier'>MapRepresentation</font>), l'inférence sur la position du "
        "Golem (<font face='Courier'>WumpusTracker</font>) et le payload sérialisable des "
        "messages de sighting. Cette séparation permet de tester le tracker en isolation, "
        "sans démarrer la plateforme.",
        st['Body']))
    story.append(diagram_knowledge(st))
    story.append(caption("diagramme de classes — module knowledge.", st))

    subsection("2.4 Diagramme de séquence — protocole de chasse", story, st)
    story.append(P(
        "Le diagramme ci-après illustre une itération typique côté <font face='Courier'>"
        "HuntBehaviour</font> : observation locale, mise à jour du tracker, broadcast aux "
        "coéquipiers, choix du prochain pas et action. La vérification de capture est faite à "
        "la fin du tick suivant l'observation, ce qui garantit une réponse en au plus 1 tick "
        "après immobilisation effective du Golem.",
        st['Body']))
    story.append(diagram_sequence(st))
    story.append(caption("séquence simplifiée d'un tick de chasse entre H1 et H2.", st))


# --- Section 3 : Choix de conception ----------------------------------------
def sec_choix(story, st):
    section("3 Choix de conception", story, st)

    subsection("3.1 Exploration", story, st)
    story.append(P(
        "L'algorithme de base est une <b>DFS coordonnée</b>. Chaque agent ferme son nœud courant "
        "(passage à l'attribut <font face='Courier'>closed</font>) et marque ses voisins non "
        "visités comme <font face='Courier'>open</font>. Pour éviter la convergence en file unique "
        "observée dans <font face='Courier'>ExploreCoopAgent</font>, chaque agent annonce son nœud "
        "cible (<font face='Courier'>MEXPLO-INTENT</font>) et applique une règle d'évitement :",
        st['Body']))
    story.append(P(
        "<i>Soit i un agent et T(j) la cible déclarée d'un coéquipier j. L'agent i évite tout "
        "T(j) tel que name(j) &lt; name(i) lexicographiquement.</i>",
        st['MathBlock']))
    story.append(P(
        "Cette règle est <b>déterministe</b> (deux agents qui partagent la même information "
        "calculent la même décision), <b>asymétrique</b> (l'agent de plus petit nom prend la "
        "priorité) et <b>locale</b> (aucun protocole de consensus n'est nécessaire). Le choix "
        "du prochain pas se fait en trois passes : (1) voisin direct ouvert non-évité, (2) "
        "voisin direct ouvert (avoidance ignorée — anti-deadlock), (3) Dijkstra vers le nœud "
        "ouvert le plus proche parmi les non-évités.",
        st['Body']))

    subsection("3.2 Communication", story, st)
    story.append(P(
        "<b>Découverte par DF.</b> Au <font face='Courier'>setup()</font>, chaque agent "
        "s'enregistre dans le Directory Facilitator de JADE sous le service "
        "<font face='Courier'>\"explo-team\"</font>. La méthode <font face='Courier'>"
        "getTeammates()</font> interroge le DF avec un cache TTL de 5 s, ce qui évite un lookup "
        "à chaque tick tout en restant réactif aux nouveaux arrivants. Aucun nom de coéquipier "
        "n'est codé en dur.",
        st['Body']))
    story.append(P(
        "<b>Six protocoles ACL</b>, tous en performatif INFORM :",
        st['Body']))
    proto_data = [
        [P("<b>Protocole</b>", st['Body']), P("<b>Contenu</b>", st['Body']),
         P("<b>Cadence</b>", st['Body']), P("<b>Phase</b>", st['Body'])],
        [P("MEXPLO-TOPO", st['Body']),
         P("SerializableSimpleGraph (carte)", st['Body']),
         P("Chaque tick", st['Body']), P("Toujours", st['Body'])],
        [P("MEXPLO-INTENT", st['Body']),
         P("String — id du nœud cible", st['Body']),
         P("Sur changement", st['Body']), P("Toujours", st['Body'])],
        [P("MEXPLO-DONE", st['Body']),
         P("(vide)", st['Body']),
         P("Au passage à terminé + toutes 10 idle", st['Body']), P("Exploration", st['Body'])],
        [P("MEXPLO-UNDONE", st['Body']),
         P("(vide)", st['Body']),
         P("Sur rétractation", st['Body']), P("Exploration", st['Body'])],
        [P("HUNT-SIGHTING", st['Body']),
         P("HuntSightingMessage (pos, S+, S−)", st['Body']),
         P("Chaque tick", st['Body']), P("Chasse", st['Body'])],
        [P("HUNT-CAPTURED", st['Body']),
         P("String — nœud de blocage", st['Body']),
         P("Sur détection", st['Body']), P("Chasse", st['Body'])],
    ]
    t = Table(proto_data, colWidths=[3.7 * cm, 4.7 * cm, 4.6 * cm, 2.5 * cm])
    t.setStyle(TableStyle([
        ('GRID', (0, 0), (-1, -1), 0.4, LIGHT_GREY),
        ('BACKGROUND', (0, 0), (-1, 0), TABLE_HDR_BG),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
        ('FONTSIZE', (0, 0), (-1, -1), 9),
        ('TOPPADDING', (0, 0), (-1, -1), 3),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 3),
    ]))
    story.append(t)
    story.append(Spacer(1, 6))
    story.append(P(
        "<b>Réception centralisée par agent.</b> Un unique <font face='Courier'>CyclicBehaviour"
        "</font> (<font face='Courier'>ReceiveShareBehaviour</font> ou <font face='Courier'>"
        "ReceiveHuntBehaviour</font>) filtre tous les protocoles via une <font face='Courier'>"
        "MessageTemplate.or(...)</font> pré-construite et dispatch sur les handlers internes. "
        "Cela évite la prolifération de <i>n</i> CyclicBehaviour concurrents et limite le coût "
        "de filtrage à un parcours unique.",
        st['Body']))

    subsection("3.3 Coordination", story, st)
    story.append(P(
        "<b>Trois mécanismes complémentaires</b> sont utilisés selon la phase :",
        st['Body']))
    bullets = [
        "<b>Évitement par priorité lex</b> (exploration) : décrit en §3.1. Asymétrie "
        "déterministe sans consensus.",
        "<b>Vote 2-phases pour la terminaison</b> (exploration) : un agent émet "
        "<font face='Courier'>DONE</font> dès que sa carte n'a plus de nœud ouvert, et "
        "rétracte (<font face='Courier'>UNDONE</font>) si une carte reçue rouvre des nœuds. "
        "La terminaison globale est confirmée lorsque tous les coéquipiers ont voté.",
        "<b>Assignation greedy déterministe</b> (chasse) : quand le Golem est localisé, chaque "
        "agent calcule la même assignation chasseur→voisin par double tri lexicographique "
        "(chasseurs ordonnés, voisins ordonnés). Le coordinateur est <i>implicite</i> et "
        "réplicable.",
    ]
    story.append(ListFlowable(
        [ListItem(P(b, st['Bullet']), leftIndent=12) for b in bullets],
        bulletType='bullet', start='•'))
    story.append(P(
        "L'absence de leader / coordinateur central est volontaire : elle simplifie "
        "drastiquement le code, supprime le risque de point de défaillance unique, et "
        "rend l'algorithme robuste à l'arrivée ou au départ d'un agent (DF dynamique).",
        st['Body']))

    subsection("3.4 Collecte (non implémentée — discussion)", story, st)
    story.append(P(
        "La collecte de ressources (or, diamant) n'est pas requise pour les sections du sujet "
        "traitées dans ce projet. Elle est néanmoins présente dans l'API Dedale "
        "(<font face='Courier'>pick()</font>, <font face='Courier'>openLock()</font>, "
        "<font face='Courier'>emptyMyBackPack()</font>) et exemplifiée par les agents fournis "
        "<font face='Courier'>DummyCollectorAgent</font> et <font face='Courier'>DummyTankerAgent"
        "</font>. Si on voulait l'intégrer à l'architecture proposée :",
        st['Body']))
    bullets = [
        "Étendre <font face='Courier'>MapRepresentation</font> avec un attribut <i>treasure</i> "
        "par nœud (id, type, quantité) — la sérialisation existante via "
        "<font face='Courier'>SerializableSimpleGraph</font> supporte déjà des attributs "
        "génériques ;",
        "Ajouter un protocole <font face='Courier'>MEXPLO-LOOT</font> qui propage les "
        "découvertes de trésors avec leur position, type et quantité ;",
        "Mettre en place un <i>contract-net protocol</i> entre collecteurs et tanker pour "
        "négocier les rendez-vous d'évacuation (<font face='Courier'>emptyMyBackPack</font>) ;",
        "Étendre l'observable <font face='Courier'>WIND</font> (associé aux puits) à un module "
        "de tracking similaire à <font face='Courier'>WumpusTracker</font> pour cartographier "
        "les zones dangereuses.",
    ]
    story.append(ListFlowable(
        [ListItem(P(b, st['Bullet']), leftIndent=12) for b in bullets],
        bulletType='bullet', start='•'))
    story.append(P(
        "L'ajout serait orthogonal aux mécanismes décrits jusqu'ici : un nouveau "
        "<font face='Courier'>CollectAgent</font> hériterait de <font face='Courier'>"
        "MultiExploreAgent</font> via le hook <font face='Courier'>createBehaviours()</font>, "
        "et un <font face='Courier'>HuntCollectAgent</font> combinerait les deux.",
        st['Body']))


# --- Section 4 : Analyse algorithmes ----------------------------------------
def sec_algos(story, st):
    section("4 Analyse des algorithmes", story, st)

    # -- 4.1 Exploration
    subsection("4.1 Exploration coordonnée par priorité lexicographique", story, st)
    story.append(P(
        "<b>Description.</b> Chaque agent maintient une <font face='Courier'>"
        "MapRepresentation</font> ; à chaque tick, il ferme son nœud, ouvre les voisins "
        "inconnus, broadcast la carte et choisit le prochain pas selon la stratégie en 3 "
        "passes (§3.1). La règle d'avoidance lex assure une répartition implicite du travail.",
        st['Body']))
    story.append(P("<b>Forces</b>", st['H3']))
    bullets = [
        "Décentralisé, sans leader ni protocole de consensus ;",
        "Robuste à l'arrivée/départ d'un agent (DF dynamique) et aux pertes de message "
        "(décision basée sur l'information localement disponible) ;",
        "Repli en cas d'avoidance vide : pas d'interblocage statique (passe 2).",
    ]
    story.append(ListFlowable(
        [ListItem(P(b, st['Bullet']), leftIndent=12) for b in bullets],
        bulletType='bullet', start='•'))
    story.append(P("<b>Limites</b>", st['H3']))
    bullets = [
        "Diffusion <i>full-map</i> à chaque tick : la taille du message croît linéairement "
        "avec |V|, ce qui pénalise les très grands graphes (à optimiser par delta-share) ;",
        "La priorité lex n'est <b>pas équitable</b> : l'agent de plus grand nom fait "
        "systématiquement les trajets « second choix » ;",
        "Sur deux composantes connexes disjointes, le vote DONE ne converge jamais "
        "(hypothèse de connexité du sujet).",
    ]
    story.append(ListFlowable(
        [ListItem(P(b, st['Bullet']), leftIndent=12) for b in bullets],
        bulletType='bullet', start='•'))
    story.append(P("<b>Complexité</b>", st['H3']))
    cplx = [
        [P("<b>Ressource</b>", st['Body']), P("<b>Coût par tick / par agent</b>", st['Body']),
         P("<b>Coût total</b>", st['Body'])],
        [P("Temps", st['Body']),
         P("O(d) observation + O(|V| log|V|) Dijkstra", st['Body']),
         P("O(|V|² log|V|) jusqu'à carte complète", st['Body'])],
        [P("Mémoire", st['Body']),
         P("O(|V| + |E|) carte + O(|H|) intentions", st['Body']),
         P("O(|V| + |E|) par agent", st['Body'])],
        [P("Communication", st['Body']),
         P("O(|H| · |V|) octets par tick (broadcast carte)", st['Body']),
         P("O(|V|² · |H|) jusqu'à terminaison", st['Body'])],
    ]
    t = Table(cplx, colWidths=[2.5 * cm, 6.5 * cm, 7.0 * cm])
    t.setStyle(TableStyle([
        ('GRID', (0, 0), (-1, -1), 0.4, LIGHT_GREY),
        ('BACKGROUND', (0, 0), (-1, 0), TABLE_HDR_BG),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
        ('FONTSIZE', (0, 0), (-1, -1), 9),
        ('TOPPADDING', (0, 0), (-1, -1), 3),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 3),
    ]))
    story.append(t)
    story.append(Spacer(1, 4))
    story.append(P(
        "<b>Optimalité.</b> L'algorithme <i>n'est pas optimal</i> : un partitionnement explicite "
        "(par exemple Voronoi sur les positions initiales) donnerait moins de visites en doublon. "
        "Cependant la priorité lex offre un bon compromis simplicité / efficacité, sans "
        "négociation préalable. <b>Critère d'arrêt</b> : l'agent vote DONE quand "
        "<font face='Courier'>!myMap.hasOpenNode()</font> ; la terminaison globale est confirmée "
        "quand <font face='Courier'>terminationVotes</font> contient tous les coéquipiers (vote "
        "2-phases avec rétractation).",
        st['Body']))

    # -- 4.2 WumpusTracker
    subsection("4.2 WumpusTracker — triangulation Bayésienne", story, st)
    story.append(P(
        "<b>Description.</b> Maintien d'un ensemble <i>P</i> de positions possibles du Golem. "
        "À chaque mise à jour, on étend <i>P</i> d'un saut (modèle de mouvement), puis on "
        "intersecte avec les boules de rayon <i>R</i> autour des nœuds stench-positifs et on "
        "soustrait celles autour des stench-négatifs.",
        st['Body']))
    story.append(P(
        "P ← (P ∪ N(P)) ∩ ⋂<sub>x ∈ S<sup>+</sup></sub> B(x, R) \\ ⋃<sub>y ∈ S<sup>−</sup>"
        "</sub> B(y, R)",
        st['MathBlock']))
    story.append(P("<b>Forces</b>", st['H3']))
    bullets = [
        "<b>Cohérence</b> : la position réelle reste toujours dans <i>P</i> (preuve par "
        "récurrence sur l'invariant : intersection et différence préservent l'appartenance) ;",
        "<b>Décroissance monotone</b> à information fixe : <i>|P|</i> ne grandit jamais entre "
        "deux observations consécutives ;",
        "Modulaire et testable en isolation (aucune dépendance JADE).",
    ]
    story.append(ListFlowable(
        [ListItem(P(b, st['Bullet']), leftIndent=12) for b in bullets],
        bulletType='bullet', start='•'))
    story.append(P("<b>Limites</b>", st['H3']))
    bullets = [
        "Suppose <b>1 seul Golem</b> : avec plusieurs Golems, S<sup>+</sup> grandit, "
        "l'intersection devient vide et le tracker se réinitialise — solution proposée : "
        "clusterisation spatiale, <font face='Courier'>MultiWumpusTracker</font> ;",
        "Le rayon <i>R</i> doit être connu (paramètre <font face='Courier'>stenchRadius</font>) ; "
        "une mauvaise estimation conduit à des incohérences ;",
        "Si le Golem disparaît du voisinage observé, l'expansion par 1 saut peut faire grossir "
        "<i>P</i> à chaque tick : à long terme |P|→|V| (perte de localisation).",
    ]
    story.append(ListFlowable(
        [ListItem(P(b, st['Bullet']), leftIndent=12) for b in bullets],
        bulletType='bullet', start='•'))
    story.append(P("<b>Complexité</b>", st['H3']))
    cplx2 = [
        [P("<b>Ressource</b>", st['Body']), P("<b>Coût par update</b>", st['Body'])],
        [P("Temps", st['Body']),
         P("O(|S<sup>±</sup>| · d<sup>R</sup>) (BFS rayon R)", st['Body'])],
        [P("Mémoire", st['Body']),
         P("O(|P|) ≤ O(|V|)", st['Body'])],
        [P("Communication", st['Body']),
         P("Chaque sighting : O(|S<sup>±</sup>|) octets, broadcast à O(|H|)", st['Body'])],
    ]
    t = Table(cplx2, colWidths=[3.0 * cm, 13.0 * cm])
    t.setStyle(TableStyle([
        ('GRID', (0, 0), (-1, -1), 0.4, LIGHT_GREY),
        ('BACKGROUND', (0, 0), (-1, 0), TABLE_HDR_BG),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
        ('FONTSIZE', (0, 0), (-1, -1), 9),
        ('TOPPADDING', (0, 0), (-1, -1), 3),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 3),
    ]))
    story.append(t)
    story.append(Spacer(1, 4))
    story.append(P(
        "<b>Optimalité.</b> Le tracker est <i>optimal au sens de l'information</i> : il calcule "
        "exactement l'ensemble des positions compatibles avec les observations cumulées, sans "
        "approximation. Une approche probabiliste (filtre Bayésien à distribution) donnerait "
        "une information plus fine mais nécessiterait une connaissance du modèle de mouvement "
        "du Golem (non disponible dans Dedale). <b>Critère d'arrêt</b> de l'inférence : "
        "<font face='Courier'>isLocalised()</font> retourne vrai dès que <i>|P|</i> = 1 ; le "
        "tracker est ensuite figé jusqu'au prochain mouvement.",
        st['Body']))

    # -- 4.3 Blocage
    subsection("4.3 Stratégie de blocage greedy", story, st)
    story.append(P(
        "<b>Description.</b> Quand le Golem est localisé en <i>g</i>, on calcule l'assignation "
        "chasseur→voisin par double tri lex (déterministe), puis chaque agent va vers son voisin "
        "via Dijkstra. La capture est confirmée dès que tous les voisins de <i>g</i> sont "
        "occupés par des chasseurs (les positions sont connues via le piggyback "
        "<font face='Courier'>senderPosition</font> des sightings).",
        st['Body']))
    story.append(P("<b>Forces</b>", st['H3']))
    bullets = [
        "<b>Déterministe</b> : tous les chasseurs calculent la même assignation à partir de la "
        "même information — pas de protocole de validation à 2 phases ;",
        "Pas de point de défaillance : si un agent crashe, les autres continuent (DF retire "
        "automatiquement, le cache TTL le rafraîchit en ≤ 5 s) ;",
        "Simple à raisonner et à étendre.",
    ]
    story.append(ListFlowable(
        [ListItem(P(b, st['Bullet']), leftIndent=12) for b in bullets],
        bulletType='bullet', start='•'))
    story.append(P("<b>Limites</b>", st['H3']))
    bullets = [
        "<b>Non-optimale</b> : une affectation par algorithme hongrois (matching de coût "
        "minimal) donnerait une somme totale de distances optimale ;",
        "<b>Sous-effectif</b> : si <i>|H|</i> &lt; deg(<i>g</i>), le Golem peut s'échapper "
        "indéfiniment ; sur un arbre, une stratégie <i>push-to-leaf</i> (non implémentée) "
        "ramène le besoin à 1 chasseur ;",
        "Pas de mécanisme de réassignation rapide si un chasseur est bloqué (collision "
        "répétée) : il finira par changer de cible au tick suivant via le <font face='Courier'>"
        "previousIntention=null</font>, mais ça ajoute des allers-retours.",
    ]
    story.append(ListFlowable(
        [ListItem(P(b, st['Bullet']), leftIndent=12) for b in bullets],
        bulletType='bullet', start='•'))
    story.append(P("<b>Complexité</b>", st['H3']))
    cplx3 = [
        [P("<b>Ressource</b>", st['Body']), P("<b>Coût par tick</b>", st['Body']),
         P("<b>Coût jusqu'à capture</b>", st['Body'])],
        [P("Temps", st['Body']),
         P("O(|H| · |V| log|V|) (Dijkstra par paire)", st['Body']),
         P("O(diam(G) · |V|² · |H|)", st['Body'])],
        [P("Mémoire", st['Body']),
         P("O(|H|) assignation + O(|H|) positions", st['Body']),
         P("O(|V| + |H|)", st['Body'])],
        [P("Communication", st['Body']),
         P("O(|H|) — sightings continuent", st['Body']),
         P("O(diam(G) · |H|²)", st['Body'])],
    ]
    t = Table(cplx3, colWidths=[2.5 * cm, 7.0 * cm, 6.5 * cm])
    t.setStyle(TableStyle([
        ('GRID', (0, 0), (-1, -1), 0.4, LIGHT_GREY),
        ('BACKGROUND', (0, 0), (-1, 0), TABLE_HDR_BG),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
        ('FONTSIZE', (0, 0), (-1, -1), 9),
        ('TOPPADDING', (0, 0), (-1, -1), 3),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 3),
    ]))
    story.append(t)
    story.append(Spacer(1, 4))
    story.append(P(
        "<b>Optimalité.</b> Greedy lex est <i>déterministe</i> et <i>cohérent</i>, mais pas "
        "optimal au sens du coût total. Sur le critère du <b>nombre minimal de chasseurs</b>, "
        "la version implémentée requiert deg(<i>g</i>) ; la version optimale connue (push-to-leaf "
        "sur arbre) ramène à 1. <b>Critère d'arrêt</b> de la chasse : "
        "<font face='Courier'>isGolemBlocked(g)</font> renvoie vrai (tous les voisins de <i>g</i> "
        "sont des positions de chasseurs), un <font face='Courier'>HUNT-CAPTURED</font> est "
        "diffusé, et chaque agent vérifie <font face='Courier'>isHuntComplete()</font> au tick "
        "suivant pour terminer son comportement.",
        st['Body']))


# --- Section 5 : Conclusion -------------------------------------------------
def sec_conclu(story, st):
    section("5 Conclusion", story, st)

    subsection("5.1 Synthèse", story, st)
    story.append(P(
        "Le projet propose une architecture unifiée pour les deux étapes du sujet : un agent "
        "<font face='Courier'>HuntAgent</font> qui hérite d'un <font face='Courier'>"
        "MultiExploreAgent</font> robuste, et y ajoute l'inférence Bayésienne sur les odeurs "
        "ainsi qu'une stratégie d'assignation déterministe pour la capture. La modularité du "
        "code (séparation tracker / strategy / protocol / map) facilite la maintenance et "
        "permet de tester chaque composant en isolation. Les trois algorithmes principaux "
        "(exploration coordonnée, triangulation, blocage greedy) ont chacun des garanties "
        "théoriques explicites et des complexités documentées en Javadoc et en §4.",
        st['Body']))

    subsection("5.2 Regard critique", story, st)
    bullets = [
        "<b>Diffusion full-map</b> à chaque tick : viable sur les topologies d'examen mais "
        "scaling-limité ; un mécanisme de delta-share aurait été préférable dès le début ;",
        "<b>Tracker mono-Golem</b> : choix volontaire pour valider l'inférence sur le cas "
        "simple, mais limite la robustesse multi-Golems. La voie vers <font face='Courier'>"
        "MultiWumpusTracker</font> est claire mais demande encore du temps de développement ;",
        "<b>Stratégie de blocage non-optimale</b> : sur arbres, l'encerclement gaspille des "
        "chasseurs là où push-to-leaf serait optimal. La preuve d'optimalité (1 chasseur "
        "suffit sur un arbre fini) est connue ; l'implémentation est délicate (calcul "
        "d'anti-chemin dynamique en présence d'un Golem qui bouge) ;",
        "<b>Tests automatisés absents</b> : la validation s'est faite à la console, sur un "
        "petit nombre de scénarios. Une suite JUnit sur <font face='Courier'>WumpusTracker"
        "</font> et <font face='Courier'>MapRepresentation</font> aurait sécurisé les "
        "régressions ;",
        "<b>Cache DF à TTL fixe</b> : un mécanisme d'invalidation événementielle (sur "
        "réception d'un message d'un nouvel agent) serait plus réactif.",
    ]
    story.append(ListFlowable(
        [ListItem(P(b, st['Bullet']), leftIndent=12) for b in bullets],
        bulletType='bullet', start='•'))

    subsection("5.3 Extensions et améliorations possibles", story, st)
    bullets = [
        "<b>MultiWumpusTracker</b> : clusterisation spatiale des nœuds stench-positifs en "
        "composantes connexes (avec arêtes ≤ 2R), un tracker par cluster ; assignation des "
        "chasseurs par composante ;",
        "<b>Push-to-leaf sur arbres</b> : algorithme dédié qui réduit le besoin à 1 chasseur ; "
        "calcul d'un anti-chemin qui force le Golem vers la feuille la plus proche ;",
        "<b>Delta-share</b> pour <font face='Courier'>MEXPLO-TOPO</font> : ne transmettre que "
        "les nœuds/arêtes ajoutés depuis le dernier broadcast à chaque destinataire (mémoire "
        "locale par paire) ;",
        "<b>Algorithme hongrois</b> pour l'assignation : remplace le greedy par un matching "
        "de coût minimal (toujours déterministe) ;",
        "<b>Ajout de la collecte</b> : <font face='Courier'>CollectAgent</font> avec "
        "protocole <font face='Courier'>MEXPLO-LOOT</font> et coordination par contract-net "
        "avec un tanker ;",
        "<b>Distribution multi-machines</b> : configuration <font face='Courier'>"
        "PLATFORMisDISTRIBUTED=true</font> et répartition des agents par <font face='Courier'>"
        "agentInitialContainer</font>. L'architecture DF + ACL est nativement compatible.",
    ]
    story.append(ListFlowable(
        [ListItem(P(b, st['Bullet']), leftIndent=12) for b in bullets],
        bulletType='bullet', start='•'))


# --- Références -------------------------------------------------------------
def sec_refs(story, st):
    section("Références", story, st)
    refs = [
        "[1] C. Herpson, <i>Sujet de Projet FoSyMa 2025–2026 — Patrouille Multi-agent</i>, "
        "M1 AI2D, Sorbonne Université, 2026.",
        "[2] C. Herpson, <i>TP n°2 — Projet Dedale&nbsp;: Exploration de l'Environnement</i>, "
        "M1 AI2D, Sorbonne Université, 2026.",
        "[3] G. Yob, “Hunt the Wumpus”, <i>Creative Computing</i>, vol. 1, n° 5, 1975, p. 51–54.",
        "[4] FIPA, <i>FIPA ACL Message Structure Specification</i>, document SC00061G, 2002.",
        "[5] F. Bellifemine, G. Caire, D. Greenwood, <i>Developing Multi-Agent Systems with "
        "JADE</i>, Wiley Series in Agent Technology, 2007.",
        "[6] <i>Dedale Documentation</i>, https://dedale.gitlab.io.",
        "[7] <i>GraphStream Project — A Dynamic Graph Library</i>, "
        "http://graphstream-project.org/.",
        "[8] S. Russell et P. Norvig, <i>Artificial Intelligence : A Modern Approach</i>, "
        "3<sup>e</sup> éd., Prentice Hall, 2010.",
    ]
    for r in refs:
        story.append(P(r, st['Reference']))


# --- Build ------------------------------------------------------------------
def main():
    styles = make_styles()
    doc = SimpleDocTemplate(
        OUTPUT_PATH, pagesize=A4,
        leftMargin=2.0 * cm, rightMargin=2.0 * cm,
        topMargin=2.2 * cm, bottomMargin=2.2 * cm,
        title="Rapport projet FoSyMa 2025–2026",
        author="Gaya LAHOUAZI - 21308739",
    )
    story = []
    cover(story, styles)
    toc(story, styles)
    sec_intro(story, styles)
    sec_arch(story, styles)
    sec_choix(story, styles)
    sec_algos(story, styles)
    sec_conclu(story, styles)
    sec_refs(story, styles)
    doc.build(story, onFirstPage=header_footer, onLaterPages=header_footer)
    size = os.path.getsize(OUTPUT_PATH)
    print(f"Generated: {OUTPUT_PATH}  ({size/1024:.1f} KB)")


if __name__ == "__main__":
    main()
