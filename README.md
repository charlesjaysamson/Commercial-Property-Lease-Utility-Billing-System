# NEXUS Property Management
Commercial Property Lease & Utility Billing System (console app, Java 17 + SQLite + Maven)

## Project structure
```
src/main/java/com/nexus/
  Main.java                      single entry point
  dao/    DatabaseConnection     opens SQLite, PRAGMA foreign_keys = ON on every connection
          DatabaseInitializer    creates all tables before the menu starts
          UserDao, PropertyDao, TenantDao, LeaseDao, BillingDao, ActivityLogDao
  model/  Java Records (User, Property, Tenant, Lease, Billing, ActivityLog) + Role enum
  ui/     Authentication         register, login, and routing by role
          AdminMenu, PropertyManagerMenu, BillingStaffMenu    one menu per role
          PropertyMenu, TenantMenu, LeaseMenu, BillingMenu, ReportMenu,
          UserAccountMenu, LogMenu, DashboardView, InputHelper
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
