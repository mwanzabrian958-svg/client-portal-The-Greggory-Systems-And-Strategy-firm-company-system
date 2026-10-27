#!/usr/bin/env python3
"""
Enterprise Milestone & Task Tracking Analytics Engine
The Greggory Systems & Strategy Firm (GSSF)
"""

import json
from datetime import datetime

class MilestoneTracker:
    def __init__(self, milestones_data=None):
        self.milestones = milestones_data or []

    def add_milestone(self, id, title, project, progress, status, priority, due_date, assignee):
        self.milestones.append({
            "id": id,
            "title": title,
            "project": project,
            "progress": progress,  # 0 to 100
            "status": status,      # e.g., 'completed', 'in_progress', 'blocked'
            "priority": priority,  # e.g., 'critical', 'high', 'medium', 'low'
            "due_date": due_date,
            "assignee": assignee
        })

    def generate_analytics_report(self):
        total = len(self.milestones)
        if total == 0:
            return {"total": 0, "message": "No milestones recorded."}

        completed = sum(1 for m in self.milestones if m["progress"] >= 100 or m["status"] == "completed")
        blocked = sum(1 for m in self.milestones if m["status"] == "blocked")
        in_progress = total - completed - blocked

        avg_progress = sum(m["progress"] for m in self.milestones) / total

        critical_items = [m for m in self.milestones if m["priority"].lower() in ["critical", "high"] and m["progress"] < 100]

        report = {
            "timestamp": datetime.now().isoformat(),
            "total_milestones": total,
            "completed": completed,
            "in_progress": in_progress,
            "blocked": blocked,
            "overall_completion_percentage": round(avg_progress, 2),
            "critical_action_items": len(critical_items),
            "milestones": self.milestones
        }
        return report

    def print_executive_summary(self):
        report = self.generate_analytics_report()
        print("==================================================")
        print(" GSSF ENTERPRISE MILESTONE TRACKING ANALYTICS")
        print("==================================================")
        print(f" Timestamp: {report['timestamp']}")
        print(f" Total Milestones: {report['total_milestones']}")
        print(f" Completed: {report['completed']}")
        print(f" In Progress: {report['in_progress']}")
        print(f" Blocked: {report['blocked']}")
        print(f" Overall Completion: {report['overall_completion_percentage']}%")
        print(f" Critical / High Action Items: {report['critical_action_items']}")
        print("--------------------------------------------------")
        print(" DETAILED MILESTONE STATUS:")
        for m in report['milestones']:
            status_symbol = "✅" if m['progress'] >= 100 else ("❌" if m['status'] == 'blocked' else "⏳")
            print(f" {status_symbol} [{m['project']}] {m['title']} - {m['progress']}% ({m['priority'].upper()}) - Assigned: {m['assignee']}")
        print("==================================================")

if __name__ == "__main__":
    # Example usage / demonstration telemetry data
    tracker = MilestoneTracker()
    tracker.add_milestone(1, "API Gateway Architecture Design", "Enterprise Client Portal", 100, "completed", "critical", "2026-03-01", "Lead Architect")
    tracker.add_milestone(2, "AES-256 Data Vault Encryption", "Cybersecurity Shield", 85, "in_progress", "high", "2026-03-10", "SecOps Lead")
    tracker.add_milestone(3, "M-Pesa Daraja Payment Gateway", "FinTech Integration", 40, "in_progress", "critical", "2026-03-15", "Backend Eng")
    tracker.add_milestone(4, "Biometric Gateway hardening", "Client Portal", 0, "blocked", "high", "2026-03-20", "Mobile Lead")

    tracker.print_executive_summary()
