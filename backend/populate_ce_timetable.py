#!/usr/bin/env python3
"""
CE (Computer Engineering) Year 4 Sem 8 timetable — from screenshot.

Faculty:
  PB  = Ms. Priyanka Bhoir (DC)
  SNS = Dr. Sanjay N Sharma (ADS)
  GVK = Dr. Geetanjali Kale (SMA)
  PPU = Mr. Pratyush P Urade (SMA lab)
  SVB = Ms. Swara V Bambade (EM elective)
  SLB = Ms. Swati L Bhangle (PM elective)
  PPS = Dr. Prasenkumar Saklecha (DBM elective)
  HSA = Ms. Hetal S Amrutia (Major Project)

Batches: B1=Roll 1-20, B2=Roll 21-40, B3=Roll 41-59

MONDAY:    P1=ILOC(PM/DBM/EM)  P2=SMA  P3=ADS  P4=DC
TUESDAY:   P1=ILOC  P2=SMA  P3+P4=Labs(SMA-B1/ADS-B2/DC-B3)
WEDNESDAY: P1=ILOC  P2=ADS  P3=DC  P4=SMA  P5+P6+P7=Labs(ADS-B1/DC-B2/SMA-B3)
THURSDAY:  P1+P2=Labs(DC-B1/SMA-B2/ADS-B3)  P3=ADS
FRIDAY:    Full day MAJOR PROJECT
"""
import json, urllib.request, urllib.error

BASE = "http://localhost:8080/api"
DEPT, YEAR, SEM, SEC = "CE", 4, 8, "A"

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
    s("MONDAY","09:15","10:15","PM","Project Management","Ms. Swati Bhangle","SF215"),
    s("MONDAY","09:15","10:15","DBM","Digital Business Management","Dr. Prasenkumar Saklecha","FF102"),
    s("MONDAY","09:15","10:15","EM","Environmental Management","Ms. Swara Bambade","FF104"),
    # P2: SMA theory
    s("MONDAY","10:15","11:15","SMA","Social Media Analytics","Dr. Geetanjali Kale","FF104"),
    # P3: ADS theory
    s("MONDAY","11:30","12:30","ADS","Applied Data Science","Dr. Sanjay N Sharma","FF104"),
    # P4: DC theory
    s("MONDAY","12:30","13:30","DC","Distributed Computing","Ms. Priyanka Bhoir","FF104"),

    # ── TUESDAY ─────────────────────────────────────────────────────────────
    # P1: ILOC electives
    s("TUESDAY","09:15","10:15","PM","Project Management","Ms. Swati Bhangle","SF215"),
    s("TUESDAY","09:15","10:15","DBM","Digital Business Management","Dr. Prasenkumar Saklecha","FF102"),
    s("TUESDAY","09:15","10:15","EM","Environmental Management","Ms. Swara Bambade","FF104"),
    # P2: SMA theory
    s("TUESDAY","10:15","11:15","SMA","Social Media Analytics","Dr. Geetanjali Kale","FF104"),
    # P3+P4: Labs — SMA(B1)/ADS(B2)/DC(B3)
    s("TUESDAY","11:30","12:30","SMA","Social Media Analytics","Mr. Pratyush P Urade","FF124","LAB","B1"),
    s("TUESDAY","12:30","13:30","SMA","Social Media Analytics","Mr. Pratyush P Urade","FF124","LAB","B1"),
    s("TUESDAY","11:30","12:30","ADS","Applied Data Science","Dr. Sanjay N Sharma","FF119","LAB","B2"),
    s("TUESDAY","12:30","13:30","ADS","Applied Data Science","Dr. Sanjay N Sharma","FF119","LAB","B2"),
    s("TUESDAY","11:30","12:30","DC","Distributed Computing","Ms. Priyanka Bhoir","FF121","LAB","B3"),
    s("TUESDAY","12:30","13:30","DC","Distributed Computing","Ms. Priyanka Bhoir","FF121","LAB","B3"),

    # ── WEDNESDAY ───────────────────────────────────────────────────────────
    # P1: ILOC electives
    s("WEDNESDAY","09:15","10:15","PM","Project Management","Ms. Swati Bhangle","SF215"),
    s("WEDNESDAY","09:15","10:15","DBM","Digital Business Management","Dr. Prasenkumar Saklecha","FF102"),
    s("WEDNESDAY","09:15","10:15","EM","Environmental Management","Ms. Swara Bambade","FF104"),
    # P2: ADS theory
    s("WEDNESDAY","10:15","11:15","ADS","Applied Data Science","Dr. Sanjay N Sharma","FF104"),
    # P3: DC theory
    s("WEDNESDAY","11:30","12:30","DC","Distributed Computing","Ms. Priyanka Bhoir","FF104"),
    # P4: SMA theory
    s("WEDNESDAY","12:30","13:30","SMA","Social Media Analytics","Dr. Geetanjali Kale","FF104"),
    # P5+P6+P7: Labs — ADS(B1)/DC(B2)/SMA(B3)
    s("WEDNESDAY","14:00","15:00","ADS","Applied Data Science","Dr. Sanjay N Sharma","FF119","LAB","B1"),
    s("WEDNESDAY","15:00","16:00","ADS","Applied Data Science","Dr. Sanjay N Sharma","FF119","LAB","B1"),
    s("WEDNESDAY","16:00","17:00","ADS","Applied Data Science","Dr. Sanjay N Sharma","FF119","LAB","B1"),
    s("WEDNESDAY","14:00","15:00","DC","Distributed Computing","Ms. Priyanka Bhoir","FF121","LAB","B2"),
    s("WEDNESDAY","15:00","16:00","DC","Distributed Computing","Ms. Priyanka Bhoir","FF121","LAB","B2"),
    s("WEDNESDAY","16:00","17:00","DC","Distributed Computing","Ms. Priyanka Bhoir","FF121","LAB","B2"),
    s("WEDNESDAY","14:00","15:00","SMA","Social Media Analytics","Dr. Geetanjali Kale","FF120","LAB","B3"),
    s("WEDNESDAY","15:00","16:00","SMA","Social Media Analytics","Dr. Geetanjali Kale","FF120","LAB","B3"),
    s("WEDNESDAY","16:00","17:00","SMA","Social Media Analytics","Dr. Geetanjali Kale","FF120","LAB","B3"),

    # ── THURSDAY ────────────────────────────────────────────────────────────
    # P1+P2: Labs — DC(B1)/SMA(B2)/ADS(B3)
    s("THURSDAY","09:15","10:15","DC","Distributed Computing","Ms. Priyanka Bhoir","FF121","LAB","B1"),
    s("THURSDAY","10:15","11:15","DC","Distributed Computing","Ms. Priyanka Bhoir","FF121","LAB","B1"),
    s("THURSDAY","09:15","10:15","SMA","Social Media Analytics","Dr. Geetanjali Kale","FF120","LAB","B2"),
    s("THURSDAY","10:15","11:15","SMA","Social Media Analytics","Dr. Geetanjali Kale","FF120","LAB","B2"),
    s("THURSDAY","09:15","10:15","ADS","Applied Data Science","Dr. Sanjay N Sharma","FF119","LAB","B3"),
    s("THURSDAY","10:15","11:15","ADS","Applied Data Science","Dr. Sanjay N Sharma","FF119","LAB","B3"),
    # P3: ADS theory
    s("THURSDAY","11:30","12:30","ADS","Applied Data Science","Dr. Sanjay N Sharma","FF104"),

    # ── FRIDAY ──────────────────────────────────────────────────────────────
    s("FRIDAY","09:15","17:00","MAJOR PROJECT","Major Project Phase 2","Ms. Hetal S Amrutia","","SEMINAR"),
]

# Delete existing CE Y4 S8 slots
print("Deleting existing CE Y4 S8 slots...")
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
print(f"\nInserting {len(slots)} CE slots...")
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
