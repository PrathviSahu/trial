#!/usr/bin/env python3
"""
Fix CSD Sem 8 Year 4 timetable — correct data from screenshot.

Key corrections from screenshot:
MONDAY:  Period 2 = SMA (not ADS), Period 3 = DC (not SMA), no Period 4
TUESDAY: Period 2 = DC, Period 3 = SMA, Period 4 = ADS (correct)
"""
import json, urllib.request, urllib.error

BASE = "http://localhost:8080/api"
DEPT, YEAR, SEM, SEC = "CSD", 4, 8, "A"

def s(day, st, et, code, name, fac, room, t="LECTURE", batch=None):
    return {
        "department": DEPT, "year": YEAR, "semester": SEM, "section": SEC,
        "dayOfWeek": day,
        "startTime": st + ":00", "endTime": et + ":00",
        "subjectCode": code, "subjectName": name,
        "faculty": fac, "classroom": room, "type": t, "batch": batch
    }

# Step 1: Delete all existing CSD Y4 S8 slots
print("Step 1: Getting existing slots...")
req = urllib.request.Request(
    f"{BASE}/timetable/schedule/weekly?department={DEPT}&year={YEAR}&semester={SEM}&section={SEC}",
    headers={"Content-Type": "application/json"},
    method="GET"
)
try:
    with urllib.request.urlopen(req, timeout=15) as resp:
        body = json.loads(resp.read())
        existing = body.get("data", [])
        print(f"Found {len(existing)} existing slots → deleting them...")
        for slot in existing:
            sid = slot["id"]
            del_req = urllib.request.Request(
                f"{BASE}/timetable/{sid}",
                method="DELETE"
            )
            with urllib.request.urlopen(del_req, timeout=10) as dr:
                pass
        print(f"✅ Deleted {len(existing)} slots")
except Exception as e:
    print(f"⚠ Could not delete: {e}")

# Step 2: Insert correct slots
# Reading screenshot carefully:
# ─────────────────────────────────────────────────────────────────────────────
# MONDAY:  P1=PM/DBM/EM  P2=SMA(SSP,FF112)  P3=DC(IPJ,FF112)   P5-7=DC(B1,IPJ,SF214)
# TUESDAY: P1=PM/DBM/EM  P2=DC(IPJ,FF112)   P3=SMA(SSP,FF103)  P4=ADS(PAY,FF103)  P5-7=ADS(B1,SF213)+SMA(B2,SF212)
# WEDNESDAY:P1=PM/DBM/EM P2=DC(IPJ,FF103)   P3+P4=ADS(B2,SF213)+SMA(B3,SF212) labs
# THURSDAY: P1=ADS(PAY,FF102) P2=SMA(SSP,FF102) P3+P4=SMA(B1,SF212)+DC(B2,SF214)+ADS(B3,SF213) P5-7=DC(B3,IPJ,SF214)
# FRIDAY:   All day = MAJOR PROJECT 1
# ─────────────────────────────────────────────────────────────────────────────
slots = [
    # ── MONDAY ──────────────────────────────────────────────────────────────
    s("MONDAY","09:15","10:15","PM","Project Management","Ms. Swati Bhangale","FF101"),
    s("MONDAY","09:15","10:15","DBM","Digital Business Management","Dr. Prasenkumar Saklecha","LG015"),
    s("MONDAY","09:15","10:15","EM","Environmental Management","Mr. Pratap Deshmukh","FF103"),
    # Period 2 = SMA (screenshot: SMA / SSP / FF112)
    s("MONDAY","10:15","11:15","SMA","Social Media Analytics","Dr. Sunanda Pandita","FF112"),
    # Period 3 = DC (screenshot: DC / IPJ / FF112)
    s("MONDAY","11:30","12:30","DC","Distributed Computing","Ms. Indira Joshi","FF112"),
    # Periods 5-7 — DC Lab (B1)
    s("MONDAY","14:00","15:00","DC","Distributed Computing","Ms. Indira Joshi","SF214","LAB","B1"),
    s("MONDAY","15:00","16:00","DC","Distributed Computing","Ms. Indira Joshi","SF214","LAB","B1"),
    s("MONDAY","16:00","17:00","DC","Distributed Computing","Ms. Indira Joshi","SF214","LAB","B1"),

    # ── TUESDAY ─────────────────────────────────────────────────────────────
    s("TUESDAY","09:15","10:15","PM","Project Management","Ms. Swati Bhangale","FF101"),
    s("TUESDAY","09:15","10:15","DBM","Digital Business Management","Dr. Prasenkumar Saklecha","LG015"),
    s("TUESDAY","09:15","10:15","EM","Environmental Management","Mr. Pratap Deshmukh","FF103"),
    # Period 2 = DC
    s("TUESDAY","10:15","11:15","DC","Distributed Computing","Ms. Indira Joshi","FF112"),
    # Period 3 = SMA
    s("TUESDAY","11:30","12:30","SMA","Social Media Analytics","Dr. Sunanda Pandita","FF103"),
    # Period 4 = ADS
    s("TUESDAY","12:30","13:30","ADS","Applied Data Science","Ms. Prajakta Yadav","FF103"),
    # Periods 5-7 — ADS Lab (B1) + SMA Lab (B2)
    s("TUESDAY","14:00","15:00","ADS","Applied Data Science","Ms. Prajakta Yadav","SF213","LAB","B1"),
    s("TUESDAY","15:00","16:00","ADS","Applied Data Science","Ms. Prajakta Yadav","SF213","LAB","B1"),
    s("TUESDAY","16:00","17:00","ADS","Applied Data Science","Ms. Prajakta Yadav","SF213","LAB","B1"),
    s("TUESDAY","14:00","15:00","SMA","Social Media Analytics","Dr. Sunanda Pandita","SF212","LAB","B2"),
    s("TUESDAY","15:00","16:00","SMA","Social Media Analytics","Dr. Sunanda Pandita","SF212","LAB","B2"),
    s("TUESDAY","16:00","17:00","SMA","Social Media Analytics","Dr. Sunanda Pandita","SF212","LAB","B2"),

    # ── WEDNESDAY ───────────────────────────────────────────────────────────
    s("WEDNESDAY","09:15","10:15","PM","Project Management","Ms. Swati Bhangale","FF101"),
    s("WEDNESDAY","09:15","10:15","DBM","Digital Business Management","Dr. Prasenkumar Saklecha","LG015"),
    s("WEDNESDAY","09:15","10:15","EM","Environmental Management","Mr. Pratap Deshmukh","FF103"),
    # Period 2 = DC
    s("WEDNESDAY","10:15","11:15","DC","Distributed Computing","Ms. Indira Joshi","FF103"),
    # Periods 3-4 — ADS Lab (B2) + SMA Lab (B3)
    s("WEDNESDAY","11:30","12:30","ADS","Applied Data Science","Ms. Prajakta Yadav","SF213","LAB","B2"),
    s("WEDNESDAY","12:30","13:30","ADS","Applied Data Science","Ms. Prajakta Yadav","SF213","LAB","B2"),
    s("WEDNESDAY","11:30","12:30","SMA","Social Media Analytics","Dr. Sunanda Pandita","SF212","LAB","B3"),
    s("WEDNESDAY","12:30","13:30","SMA","Social Media Analytics","Dr. Sunanda Pandita","SF212","LAB","B3"),

    # ── THURSDAY ────────────────────────────────────────────────────────────
    s("THURSDAY","09:15","10:15","ADS","Applied Data Science","Ms. Prajakta Yadav","FF102"),
    s("THURSDAY","10:15","11:15","SMA","Social Media Analytics","Dr. Sunanda Pandita","FF102"),
    # Periods 3-4 — SMA(B1) + DC(B2) + ADS(B3) labs
    s("THURSDAY","11:30","12:30","SMA","Social Media Analytics","Dr. Sunanda Pandita","SF212","LAB","B1"),
    s("THURSDAY","12:30","13:30","SMA","Social Media Analytics","Dr. Sunanda Pandita","SF212","LAB","B1"),
    s("THURSDAY","11:30","12:30","DC","Distributed Computing","Ms. Indira Joshi","SF214","LAB","B2"),
    s("THURSDAY","12:30","13:30","DC","Distributed Computing","Ms. Indira Joshi","SF214","LAB","B2"),
    s("THURSDAY","11:30","12:30","ADS","Applied Data Science","Ms. Prajakta Yadav","SF213","LAB","B3"),
    s("THURSDAY","12:30","13:30","ADS","Applied Data Science","Ms. Prajakta Yadav","SF213","LAB","B3"),
    # Periods 5-7 — DC Lab (B3)
    s("THURSDAY","14:00","15:00","DC","Distributed Computing","Ms. Indira Joshi","SF214","LAB","B3"),
    s("THURSDAY","15:00","16:00","DC","Distributed Computing","Ms. Indira Joshi","SF214","LAB","B3"),
    s("THURSDAY","16:00","17:00","DC","Distributed Computing","Ms. Indira Joshi","SF214","LAB","B3"),

    # ── FRIDAY ──────────────────────────────────────────────────────────────
    s("FRIDAY","09:15","17:00","MAJOR PROJECT 1","Major Project Phase 1","Dr. Sunanda Pandita","","SEMINAR"),
]

print(f"Step 2: Inserting {len(slots)} corrected slots...")
data = json.dumps(slots).encode("utf-8")
req = urllib.request.Request(
    f"{BASE}/timetable/bulk",
    data=data,
    headers={"Content-Type": "application/json"},
    method="POST"
)
try:
    with urllib.request.urlopen(req, timeout=15) as resp:
        body = json.loads(resp.read())
        print(f"✅ Inserted: {body.get('count', '?')} slots")
except urllib.error.HTTPError as e:
    print(f"❌ HTTP {e.code}: {e.read().decode()}")
except Exception as e:
    print(f"❌ Error: {e}")
