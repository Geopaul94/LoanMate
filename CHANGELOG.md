# Changelog

## 1.0.0 — 2026-08-09
First Play Store release.

- Track all loan types (personal, car, home, gold, education, credit-card EMI,
  KSFE chitty, LIC, business, custom) with live EMI calculation.
- Dashboard: debt-free countdown, payment streak, summary cards, loan progress.
- EMI reminders (7/3/1-day + due-day) via WorkManager; notification deep-links
  to the loan.
- EMI calendar, missed-payment detection, milestone celebrations, achievements.
- Calculators: prepayment, foreclosure, and Avalanche-vs-Snowball payoff plan.
- PDF export, local JSON backup/restore, optional Google Drive sync.
- Onboarding, undo-delete, bottom navigation.
- Emerald + gold brand: committed palette, Plus Jakarta Sans / Inter type,
  custom app icon, branded splash, personality-filled empty states.
- 57 unit tests across the calculation and detection logic.
- Fix: "Add Loan" button on the dashboard was overlapping the Monthly EMI card
  for any user with 3+ active loans; switched to a compact icon-only button.
