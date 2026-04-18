#!/usr/bin/env python3
"""
AIDS (Artificial Intelligence & Data Science) Year 4 Sem 8 timetable.
Reading directly from the provided screenshot.

Faculty:
  MVG = Dr. Megha V Gupta      | SSW = Suhas S. Waghmare
  ARM = Dr. Anu Malhan          | GVK = Dr. Geetanjali Kale
  SLB = Ms. Swati Bhangale     | PPS = Dr. Prasenkumar Saklecha
  PDD = Mr. Pratap Deshmukh

Batches: B1 Roll 1-21, B2 Roll 22-42, B3 Roll 43-63

Layout (days=rows, periods=cols):
  P1 9:15-10:15 | P2 10:15-11:15 | TEA | P3 11:30-12:30 | P4 12:30-1:30 | LUNCH | P5-7 2-5pm
"""
import json, urllib.request, urllib.error

BASE = "http://localhost:8080/api"
DEPT, YEAR, SEM, SEC = "AIDS", 4, 8, "A"

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
    # P1: ILOC electives (PM/DBM/EM) — same faculty as CSD, different rooms
    s("MONDAY","09:15","10:15","PM","Project Management","Ms. Swati Bhangale","SF215"),
    s("MONDAY","09:15","10:15","DBM","Digital Business Management","Dr. Prasenkumar Saklecha","FF102"),
    s("MONDAY","09:15","10:15","EM","Environmental Management","Mr. Pratap Deshmukh","FF103"),
    # P2: AIFB theory
    s("MONDAY","10:15","11:15","AIFB","AI for Financial & Banking Application","Suhas S. Waghmare","FF101"),
    # P3-P4: Labs — SMA(B1) | AIFB(B2) | AAI(B3)
    s("MONDAY","11:30","12:30","SMA","Social Media Analytics","Dr. Geetanjali Kale","SF202","LAB","B1"),
    s("MONDAY","12:30","13:30","SMA","Social Media Analytics","Dr. Geetanjali Kale","SF202","LAB","B1"),
    s("MONDAY","11:30","12:30","AIFB","AI for Financial & Banking Application","Suhas S. Waghmare","SF203","LAB","B2"),
    s("MONDAY","12:30","13:30","AIFB","AI for Financial & Banking Application","Suhas S. Waghmare","SF203","LAB","B2"),
    s("MONDAY","11:30","12:30","AAI","Advanced Artificial Intelligence","Dr. Megha V Gupta","SF201","LAB","B3"),
    s("MONDAY","12:30","13:30","AAI","Advanced Artificial Intelligence","Dr. Megha V Gupta","SF201","LAB","B3"),

    # ── TUESDAY ─────────────────────────────────────────────────────────────
    # P1: ILOC electives
    s("TUESDAY","09:15","10:15","PM","Project Management","Ms. Swati Bhangale","SF215"),
    s("TUESDAY","09:15","10:15","DBM","Digital Business Management","Dr. Prasenkumar Saklecha","FF102"),
    s("TUESDAY","09:15","10:15","EM","Environmental Management","Mr. Pratap Deshmukh","FF103"),
    # P2: AAI theory
    s("TUESDAY","10:15","11:15","AAI","Advanced Artificial Intelligence","Dr. Megha V Gupta","FF101"),
    # P3-P4: Labs — AAI(B1) | SMA(B2) | AIFB(B3)
    s("TUESDAY","11:30","12:30","AAI","Advanced Artificial Intelligence","Dr. Megha V Gupta","SF201","LAB","B1"),
    s("TUESDAY","12:30","13:30","AAI","Advanced Artificial Intelligence","Dr. Megha V Gupta","SF201","LAB","B1"),
    s("TUESDAY","11:30","12:30","SMA","Social Media Analytics","Dr. Geetanjali Kale","SF202","LAB","B2"),
    s("TUESDAY","12:30","13:30","SMA","Social Media Analytics","Dr. Geetanjali Kale","SF202","LAB","B2"),
    s("TUESDAY","11:30","12:30","AIFB","AI for Financial & Banking Application","Suhas S. Waghmare","SF203","LAB","B3"),
    s("TUESDAY","12:30","13:30","AIFB","AI for Financial & Banking Application","Suhas S. Waghmare","SF203","LAB","B3"),
    # P5-P7: RS lab + SMA lab (parallel)
    s("TUESDAY","14:00","15:00","RS","Recommendation Systems","Dr. Anu Malhan","FF101","LAB"),
    s("TUESDAY","15:00","16:00","RS","Recommendation Systems","Dr. Anu Malhan","FF101","LAB"),
    s("TUESDAY","16:00","17:00","RS","Recommendation Systems","Dr. Anu Malhan","FF101","LAB"),
    s("TUESDAY","14:00","15:00","SMA","Social Media Analytics","Dr. Geetanjali Kale","FF110","LAB"),
    s("TUESDAY","15:00","16:00","SMA","Social Media Analytics","Dr. Geetanjali Kale","FF110","LAB"),
    s("TUESDAY","16:00","17:00","SMA","Social Media Analytics","Dr. Geetanjali Kale","FF110","LAB"),

    # ── WEDNESDAY ───────────────────────────────────────────────────────────
    # P1: ILOC electives
    s("WEDNESDAY","09:15","10:15","PM","Project Management","Ms. Swati Bhangale","SF215"),
    s("WEDNESDAY","09:15","10:15","DBM","Digital Business Management","Dr. Prasenkumar Saklecha","FF102"),
    s("WEDNESDAY","09:15","10:15","EM","Environmental Management","Mr. Pratap Deshmukh","FF103"),
    # P2: RS + SMA (theory — parallel optional courses)
    s("WEDNESDAY","10:15","11:15","RS","Recommendation Systems","Dr. Anu Malhan","FF111"),
    s("WEDNESDAY","10:15","11:15","SMA","Social Media Analytics","Dr. Geetanjali Kale","FF101"),
    # P3: AAI theory
    s("WEDNESDAY","11:30","12:30","AAI","Advanced Artificial Intelligence","Dr. Megha V Gupta","FF113"),
    # P4: AIFB theory
    s("WEDNESDAY","12:30","13:30","AIFB","AI for Financial & Banking Application","Suhas S. Waghmare","FF113"),

    # ── THURSDAY ────────────────────────────────────────────────────────────
    # P1: AIFB theory
    s("THURSDAY","09:15","10:15","AIFB","AI for Financial & Banking Application","Suhas S. Waghmare","FF113"),
    # P2: AAI theory
    s("THURSDAY","10:15","11:15","AAI","Advanced Artificial Intelligence","Dr. Megha V Gupta","FF113"),
    # P3-P4: Labs — AIFB(B1) | AAI(B2) | RS(B3) | SMA(B3)
    s("THURSDAY","11:30","12:30","AIFB","AI for Financial & Banking Application","Suhas S. Waghmare","SF203","LAB","B1"),
    s("THURSDAY","12:30","13:30","AIFB","AI for Financial & Banking Application","Suhas S. Waghmare","SF203","LAB","B1"),
    s("THURSDAY","11:30","12:30","AAI","Advanced Artificial Intelligence","Dr. Megha V Gupta","SF201","LAB","B2"),
    s("THURSDAY","12:30","13:30","AAI","Advanced Artificial Intelligence","Dr. Megha V Gupta","SF201","LAB","B2"),
    s("THURSDAY","11:30","12:30","RS","Recommendation Systems","Dr. Anu Malhan","SF202","LAB","B3"),
    s("THURSDAY","12:30","13:30","RS","Recommendation Systems","Dr. Anu Malhan","SF202","LAB","B3"),
    s("THURSDAY","11:30","12:30","SMA","Social Media Analytics","Dr. Geetanjali Kale","SF204","LAB","B3"),
    s("THURSDAY","12:30","13:30","SMA","Social Media Analytics","Dr. Geetanjali Kale","SF204","LAB","B3"),
    # P5-P7: RS + SMA labs (parallel)
    s("THURSDAY","14:00","15:00","RS","Recommendation Systems","Dr. Anu Malhan","FF101","LAB"),
    s("THURSDAY","15:00","16:00","RS","Recommendation Systems","Dr. Anu Malhan","FF101","LAB"),
    s("THURSDAY","16:00","17:00","RS","Recommendation Systems","Dr. Anu Malhan","FF101","LAB"),
    s("THURSDAY","14:00","15:00","SMA","Social Media Analytics","Dr. Geetanjali Kale","FF102","LAB"),
    s("THURSDAY","15:00","16:00","SMA","Social Media Analytics","Dr. Geetanjali Kale","FF102","LAB"),
    s("THURSDAY","16:00","17:00","SMA","Social Media Analytics","Dr. Geetanjali Kale","FF102","LAB"),

    # ── FRIDAY ──────────────────────────────────────────────────────────────
    s("FRIDAY","09:15","17:00","MAJOR PROJECT 2","Major Project Phase 2","Dr. Megha V Gupta","","SEMINAR"),
]

# Delete existing AIDS Y4 S8 slots first
print("Deleting existing AIDS Y4 S8 slots...")
req = urllib.request.Request(
    f"{BASE}/timetable/schedule/weekly?department={DEPT}&year={YEAR}&semester={SEM}&section={SEC}",
    method="GET"
)
try:
    with urllib.request.urlopen(req, timeout=15) as resp:
        existing = json.loads(resp.read()).get("data", [])
        print(f"Found {len(existing)} existing → deleting...")
        for slot in existing:
            urllib.request.urlopen(
                urllib.request.Request(f"{BASE}/timetable/{slot['id']}", method="DELETE"), timeout=10
            )
        print(f"✅ Deleted {len(existing)}")
except Exception as e:
    print(f"⚠ Delete step: {e}")

# Insert
print(f"\nInserting {len(slots)} AIDS slots...")
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
