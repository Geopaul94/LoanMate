# LoanMate Improvement Plan

## 1. Backup & Restore Nudge (Onboarding/First Launch)
- **Goal**: Prevent data loss for returning users and encourage cloud backups.
- **Implementation**:
    - Update `DashboardViewModel` to check if it's the first launch with no data.
    - Add a "Restore from Cloud" nudge card to `DashboardScreen` when the loan list is empty.
    - Improve the "Empty State" to be more helpful regarding backups.

## 2. Document Vault (UI Integration)
- **Goal**: Allow users to store and view loan-related documents (Sanction letters, NOCs).
- **Implementation**:
    - Create `DocumentViewModel` and `DocumentRepository`.
    - Update `LoanDetailsScreen` to include a "Documents" tab or section.
    - Implement a `DocumentListScreen` and `DocumentPicker` integration.
    - Support viewing/deleting stored documents.

## 3. Smart Prepayment Proactive Alerts
- **Goal**: Motivate users to pay off debt faster with real-time insights.
- **Implementation**:
    - Add a "Prepayment Insight" card on the `DashboardScreen`.
    - Use `PrepaymentCalculator` to show a "What-If" scenario (e.g., "Paying ₹2000 extra saves 5 months").
    - Make the card interactive (allow changing the extra amount to see different savings).

## 4. Interactive Amortization Schedule & PDF Export
- **Goal**: Provide a detailed month-by-month breakdown of payments.
- **Implementation**:
    - Create `AmortizationScheduleScreen`.
    - Generate a list of payments with Principal vs. Interest split.
    - Link to this screen from `LoanDetailsScreen`.
    - Enhance `PdfExporter` to support exporting the full schedule.

## 5. Debt-Free "Celebration" & Sharing
- **Goal**: Gamify the experience and celebrate user success.
- **Implementation**:
    - Trigger a celebration (Confetti + Dialog) when a loan status changes to `CLOSED`.
    - Create a "Debt-Free Card" layout that can be captured as an image for sharing.
    - Add a "Share Success" button to the closed loan details.

---

### Step-by-Step Execution
1. [ ] Implement Backup/Restore nudge in Dashboard.
2. [ ] Integrate Document Vault UI in Loan Details.
3. [ ] Add Proactive Prepayment Alerts to Dashboard.
4. [ ] Create Interactive Amortization Schedule Screen.
5. [ ] Implement Debt-Free Celebration and Sharing.
