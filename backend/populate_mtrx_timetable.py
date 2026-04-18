#!/usr/bin/env python3
"""
MECHATRONICS Year 4 Sem 8 timetable — from screenshot.

Faculty:
  SVB = Dr. Shrikant Baste (IAI theory) / Swara Bambade (RMV lab)
  PPS = Ms. Preeti Samdani (AI&ML)
  RA  = Ramesh Adokar (EME)
  SLB = Swati Lalit Bhangle (PM elective)

Only B1 batch (Roll 1-15).

MONDAY:   P1=PM/EM/DBM(electives)  P2=EME/RA/TF619  P3-P4=RMV-B1-lab/TF610
TUESDAY:  P1=electives  P2=AI&ML/PPS/TF612  P3=EME/RA/TF618  P4=IAI/SVB/TF618
WEDNESDAY:P1=electives  P2=IAI/SVB/TF613  P3=EME/RA/TF609  P4=AI&ML/PPS/TF612
THURSDAY: P1=IAI(B1)lab/SVB/TF610  P3=AI&ML/PPS/TF612  P4=IAI/SVB/TF613
FRIDAY:   Full day MAJOR PROJECT II
"""
import json, urllib.request, urllib.error

BASE = "http://localhost:8080/api"
DEPT, YEAR, SEM, SEC = "MECHATRONICS", 4, 8, "A"

def s(day, st, et, code, name, fac, room, t="LECTURE", batch=None):
    return {
        "department": DEPT, "year": YEAR, "semester": SEM, "section": SEC,
        "dayOfWeek": day,
        "startTime": st + ":00", "endTime": et + ":00",
        "subjectCode": code, "subjectName": name,
        "faculty": fac, "classroom": room, "type": t, "batch": batch
    }

slots = [
    # ── MONDAY ──────────────────────────────────────────────────────────────
    # P1: ILOC electives
    s("MONDAY","09:15","10:15","PM","Project Management","Ms. Swati Lalit Bhangle","FF101"),
    s("MONDAY","09:15","10:15","EM","Environmental Management","Dr. Shrikant Baste","FF104"),
    s("MONDAY","09:15","10:15","DBM","Digital Business Management","Ms. Preeti Samdani","FF102"),
    # P2: EME theory
    s("MONDAY","10:15","11:15","EME","Engineering Management & Economics","Ramesh Adokar","TF619"),
    # P3-P4: RMV Lab (B1)
    s("MONDAY","11:30","12:30","RMV","Robotics & Machine Vision Lab","Swara Bambade","TF610","LAB","B1"),
    s("MONDAY","12:30","13:30","RMV","Robotics & Machine Vision Lab","Swara Bambade","TF610","LAB","B1"),

    # ── TUESDAY ─────────────────────────────────────────────────────────────
    # P1: ILOC electives
    s("TUESDAY","09:15","10:15","PM","Project Management","Ms. Swati Lalit Bhangle","FF101"),
    s("TUESDAY","09:15","10:15","EM","Environmental Management","Dr. Shrikant Baste","FF104"),
    s("TUESDAY","09:15","10:15","DBM","Digital Business Management","Ms. Preeti Samdani","FF102"),
    # P2: AI&ML theory
    s("TUESDAY","10:15","11:15","AI&ML","AI and Machine Learning","Ms. Preeti Samdani","TF612"),
    # P3: EME theory
    s("TUESDAY","11:30","12:30","EME","Engineering Management & Economics","Ramesh Adokar","TF618"),
    # P4: IAI theory
    s("TUESDAY","12:30","13:30","IAI","Industrial Automation & Industry 4.0","Dr. Shrikant Baste","TF618"),

    # ── WEDNESDAY ───────────────────────────────────────────────────────────
    # P1: ILOC electives
    s("WEDNESDAY","09:15","10:15","PM","Project Management","Ms. Swati Lalit Bhangle","FF101"),
    s("WEDNESDAY","09:15","10:15","EM","Environmental Management","Dr. Shrikant Baste","FF104"),
    s("WEDNESDAY","09:15","10:15","DBM","Digital Business Management","Ms. Preeti Samdani","FF102"),
    # P2: IAI theory
    s("WEDNESDAY","10:15","11:15","IAI","Industrial Automation & Industry 4.0","Dr. Shrikant Baste","TF613"),
    # P3: EME theory
    s("WEDNESDAY","11:30","12:30","EME","Engineering Management & Economics","Ramesh Adokar","TF609"),
    # P4: AI&ML theory
    s("WEDNESDAY","12:30","13:30","AI&ML","AI and Machine Learning","Ms. Preeti Samdani","TF612"),

    # ── THURSDAY ────────────────────────────────────────────────────────────
    # P1: IAI Lab (B1)
    s("THURSDAY","09:15","10:15","IAI","Industrial Automation & Industry 4.0 Lab","Dr. Shrikant Baste","TF610","LAB","B1"),
    # P3: AI&ML theory
    s("THURSDAY","11:30","12:30","AI&ML","AI and Machine Learning","Ms. Preeti Samdani","TF612"),
    # P4: IAI theory
    s("THURSDAY","12:30","13:30","IAI","Industrial Automation & Industry 4.0","Dr. Shrikant Baste","TF613"),

    # ── FRIDAY ──────────────────────────────────────────────────────────────
    s("FRIDAY","09:15","17:00","MAJOR PROJECT II","Major Project Phase 2","Ms. Anupriya Gandhewar","","SEMINAR"),
]

# Delete existing
print("Deleting existing MTRX Y4 S8 slots...")
req = urllib.request.Request(
    f"{BASE}/timetable/schedule/weekly?department={DEPT}&year={YEAR}&semester={SEM}&section={SEC}",
    method="GET"
)
try:
    with urllib.request.urlopen(req, timeout=15) as resp:
        existing = json.loads(resp.read()).get("data", [])
        print(f"Found {len(existing)} → deleting...")
        for slot in existing:
            urllib.request.urlopen(
                urllib.request.Request(f"{BASE}/timetable/{slot['id']}", method="DELETE"), timeout=10)
        print(f"✅ Deleted {len(existing)}")
except Exception as e:
    print(f"⚠ {e}")

# Insert
print(f"\nInserting {len(slots)} MTRX slots...")
data = json.dumps(slots).encode("utf-8")
req = urllib.request.Request(f"{BASE}/timetable/bulk", data=data,
    headers={"Content-Type": "application/json"}, method="POST")
try:
    with urllib.request.urlopen(req, timeout=20) as resp:
        body = json.loads(resp.read())
        print(f"✅ Inserted: {body.get('count','?')} slots")
except urllib.error.HTTPError as e:
    print(f"❌ HTTP {e.code}: {e.read().decode()}")
except Exception as e:
    print(f"❌ Error: {e}")
