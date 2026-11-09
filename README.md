# NEXUS Property Management
Commercial Property Lease & Utility Billing System (console app, Java 17 + SQLite + Maven)

## Project structure
```
src/main/java/com/scc/
  Main.java                      single entry point
  dao/    DatabaseConnection     opens SQLite, PRAGMA foreign_keys = ON on every connection
          DatabaseInitializer    creates all tables before the menu starts
          UserDao, PropertyDao, TenantDao, LeaseDao, BillingDao, ActivityLogDao
  model/  Java Records (User, Property, Tenant, Lease, Billing, ActivityLog) + Role enum
  ui/     Authentication         register, login, and routing by role
          AdminMenu, PropertyManagerMenu, BillingStaffMenu    one menu per role
          PropertyMenu, TenantMenu, LeaseMenu, BillingMenu, ReportMenu,
          UserAccountMenu, DashboardView, InputHelper
lease_billing.db                 the SQLite database (kept in the repository)
```

## Startup flow
`Main` -> `DatabaseInitializer.initialize()` -> create ONE `UserDao` and ONE `Scanner` -> `Authentication.start()`

## How to run
1. Install JDK 17 or newer and Maven.
2. In this folder run: `mvn compile exec:java`
3. On a brand-new database the program asks you to create the first Administrator account.

## Roles
- ADMINISTRATOR: everything, plus user accounts and the activity log
- PROPERTY_MANAGER: properties, tenants, leases
- BILLING_STAFF: billing, payments, reports

New accounts start as PENDING until an Administrator approves them.

## Audit log
Table `activity_logs` (id, user_id, action_type, description, timestamp).
`user_id` references `users(id)` with ON DELETE CASCADE. All SQL for it lives in `dao/ActivityLogDao.java`
and every query is a parameterized `PreparedStatement`. An older database is upgraded automatically on
start (`DatabaseInitializer`) and keeps all of its existing log rows.

The 4 required triggers:

| # | Event | Where it is logged | Log text |
|---|-------|--------------------|----------|
| 1 | Login / Logout | `ui/Authentication.java` -> `login()` / `logout()` | `User admin (ID: 1) logged in.` / `... logged out.` |
| 2 | Account approved / role-status edit | `dao/UserDao.java` -> `approveUser()` / `updateUserRoleAndStatus()` | `Administrator [1] approved User [2] (samson)` / `... Role changed to PROPERTY_MANAGER, Status changed to ACTIVE` |
| 3 | Lease creation | `dao/LeaseDao.java` -> `createLease()` | `Administrator [1] created lease: Tech Hub -> Unit A01 (₱25,000.00/month)` |
| 4 | Lease ends (Expired / Terminated) | `dao/LeaseDao.java` -> `endLease()` | `Administrator [1] updated Lease #1 (Tech Hub, Unit A01) status from ACTIVE to EXPIRED` |

Triggers 2-4 are written in the same transaction as the change itself, so a change can never be saved
without its log entry. Other events (register, property, tenant, billing, lease update) are also logged.
The Administrator opens the logs from "View System Audit Logs" (all logs, or filtered by User ID).
