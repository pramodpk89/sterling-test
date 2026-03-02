# Design Doc: Order Hold Validation Service

---

## Overview

When an order is submitted through Sterling OMS, a validation service must evaluate
three business rules and place appropriate order holds before the order enters
fulfillment pipelines. This ensures high-risk orders are reviewed by operations
before shipping, flagged customers are intercepted at the earliest point, and
orders destined for restricted shipping locations are quarantined.

The implementation is **fail-open**: if the service itself errors, the order is
allowed to proceed without holds rather than blocking fulfilment entirely.

**Ticket / Jira:** MYCO-4412
**Author:** mycompany-dev-team
**Date:** 2026-03-02
**Status:** Approved

---

## Type

- [x] Custom API (extends `YIFCustomApi`) — `OrderHoldValidationService`
- [x] User Exit (implements `YFSBeforeCreateOrderUE`) — `BeforeCreateOrderHoldUE`

---

## Components to Create

| File | Package | Type | Purpose |
|------|---------|------|---------|
| `OrderHoldValidationService.java` | `com.mycompany.sterling.service` | Custom API | Evaluates all three hold rules and calls `changeOrder` to apply holds |
| `BeforeCreateOrderHoldUE.java` | `com.mycompany.sterling.ue` | User Exit | Wires into `createOrder` UE hook; delegates to `OrderHoldValidationService` |

---

## Components to Modify

| File | Change Summary |
|------|---------------|
| `extensions/global/api_list.xml` | Register `BeforeCreateOrderHoldUE` on `createOrder` |
| `extensions/global/template/api/createOrder_output.xml` | Add `OrderHoldTypes` to output template |
| `resources/yfs.properties` | Add configurable thresholds (see Dependencies) |

---

## Input XML

The input is the standard `createOrder` input document. The service reads the
following attributes and child elements:

```xml
<Order
    OrderNo="ORD-20260302-001"
    EnterpriseCode="MYCO_ENT"
    BuyerOrganizationCode="BUYER_ORG_001"
    CustomerID="CUST-8812"
    OrderTotal="6500.00"
    ShipToID="ADDR-0042">

  <!-- Shipping address — checked against restricted destination list -->
  <PersonInfoShipTo
      State="CA"
      Country="US"
      ZipCode="90210"/>

  <OrderLines>
    <OrderLine
        OrderLineKey="OLK-001"
        Quantity="3.0"
        UnitPrice="2166.67">
      <Item ItemID="WIDGET-X" UnitOfMeasure="EACH"/>
    </OrderLine>
  </OrderLines>
</Order>
```

### `getCustomerList` lookup input (internal)

```xml
<Customer
    CustomerID="CUST-8812"
    OrganizationCode="MYCO_ENT"/>
```

---

## Output XML

The User Exit does not return a value; it modifies the order in-flight using
`changeOrder`. The `changeOrder` input constructed by the service looks like:

```xml
<Order
    OrderHeaderKey="OHK-000001"
    EnterpriseCode="MYCO_ENT">
  <OrderHoldTypes>
    <!-- One <OrderHoldType> element per triggered rule -->
    <OrderHoldType
        HoldType="HIGH_VALUE_HOLD"
        Status="1"
        ReasonText="OrderTotal 6500.00 exceeds threshold 5000.00"/>

    <!-- Optional: present only when customer is flagged -->
    <OrderHoldType
        HoldType="FLAGGED_CUSTOMER_HOLD"
        Status="1"
        ReasonText="CustomerID CUST-8812 is on the restricted customer list"/>

    <!-- Optional: present only when destination is restricted -->
    <OrderHoldType
        HoldType="RESTRICTED_DESTINATION_HOLD"
        Status="1"
        ReasonText="ShipTo ZipCode 90210 matches restricted destination pattern"/>
  </OrderHoldTypes>
</Order>
```

---

## Business Logic

### Rule 1 — High Value Order

**Trigger condition:** `Order/@OrderTotal` parsed as `double` > `mycompany.order.hold.highvalue.threshold`
**Action:** Apply hold type `HIGH_VALUE_HOLD`
**Config property:** `mycompany.order.hold.highvalue.threshold` (default: `5000.00`)

```
if (orderTotal > threshold) → apply HIGH_VALUE_HOLD
```

### Rule 2 — Flagged Customer

**Trigger condition:** A `getCustomerList` call returns a `Customer` element whose
`CustomerFlag` attribute equals `"FLAGGED"` or `"RESTRICTED"`.
**Action:** Apply hold type `FLAGGED_CUSTOMER_HOLD`
**Notes:** If `getCustomerList` throws or returns null the rule is **skipped** (fail-open).

```
customerList = getCustomerList(CustomerID, EnterpriseCode)
if (customerList != null && Customer/@CustomerFlag in {"FLAGGED","RESTRICTED"})
    → apply FLAGGED_CUSTOMER_HOLD
```

### Rule 3 — Restricted Shipping Destination

**Trigger condition:** `PersonInfoShipTo/@State` or `PersonInfoShipTo/@ZipCode`
matches any entry in the comma-separated list `mycompany.order.hold.restricted.states`
or `mycompany.order.hold.restricted.zipcodes`.
**Action:** Apply hold type `RESTRICTED_DESTINATION_HOLD`
**Config properties:**
- `mycompany.order.hold.restricted.states` (e.g. `"AK,HI,PR"`)
- `mycompany.order.hold.restricted.zipcodes` (e.g. `""` — empty means no zip restriction)

```
if (shipToState in restrictedStates || shipToZip in restrictedZips)
    → apply RESTRICTED_DESTINATION_HOLD
```

### Decision Table

| OrderTotal | CustomerFlag | ShipTo restricted? | Holds applied |
|---|---|---|---|
| ≤ 5000 | not flagged | no | none |
| > 5000 | not flagged | no | `HIGH_VALUE_HOLD` |
| ≤ 5000 | FLAGGED | no | `FLAGGED_CUSTOMER_HOLD` |
| ≤ 5000 | not flagged | yes | `RESTRICTED_DESTINATION_HOLD` |
| > 5000 | FLAGGED | yes | all three holds |
| any | getCustomerList error | — | Rule 2 skipped; Rules 1 & 3 still evaluated |

---

## Error Handling

**Strategy: Fail-open.** The service must never block an order solely because of
an internal error. Operations staff will review holds; they should not be tasked
with manually releasing every order when the hold service crashes.

| Scenario | Behaviour |
|----------|-----------|
| `getCustomerList` throws `YFSException` | Log ERROR with order/customer details; skip Rule 2; continue with Rules 1 & 3 |
| `changeOrder` throws `YFSException` | Log ERROR with full exception; swallow — holds not applied but order proceeds |
| `OrderTotal` attribute missing | Log WARN; skip Rule 1 |
| `PersonInfoShipTo` element missing | Log WARN; skip Rule 3 |
| Any unexpected `RuntimeException` | Caught at top-level in UE; logged as ERROR; order proceeds |

**Error codes:**

| Code | Meaning |
|------|---------|
| `MYCO_HOLD_001` | `OrderTotal` attribute missing or non-numeric |
| `MYCO_HOLD_002` | `getCustomerList` API call failed |
| `MYCO_HOLD_003` | `changeOrder` (apply holds) failed |
| `MYCO_HOLD_004` | Unexpected exception in UE; fail-open applied |
| `MYCO_HOLD_005` | `PersonInfoShipTo` element missing |

**Logging:** Use `YFCLogCategory` (real Sterling) / `java.util.logging.Logger` (stub).
- `ERROR` for API failures and unexpected exceptions
- `WARN` for missing optional attributes and rule skips
- `INFO` for each hold applied
- `DEBUG` for rule evaluation trace

---

## Dependencies

| Dependency | Type | Notes |
|------------|------|-------|
| `getCustomerList` | Sterling API | Retrieve customer risk flag; fail-open on error |
| `changeOrder` | Sterling API | Apply `OrderHoldTypes`; fail-open on error |
| `mycompany.order.hold.highvalue.threshold` | `yfs.properties` | Default `5000.00` |
| `mycompany.order.hold.restricted.states` | `yfs.properties` | Default `""` (none) |
| `mycompany.order.hold.restricted.zipcodes` | `yfs.properties` | Default `""` (none) |
| `SterlingAPIHelper` | Internal utility | `safeGetAttribute`, `invokeAPIFailOpen` |
| `YFSBeforeCreateOrderUE` | Sterling interface | UE hook on `createOrder` |

---

## Testing Notes

### Unit Tests (`OrderHoldValidationServiceTest`)

- [ ] Rule 1: OrderTotal just below threshold → no HIGH_VALUE_HOLD
- [ ] Rule 1: OrderTotal exactly at threshold → no hold (strictly greater than)
- [ ] Rule 1: OrderTotal above threshold → HIGH_VALUE_HOLD applied
- [ ] Rule 1: OrderTotal attribute missing → WARN logged, no hold, no exception
- [ ] Rule 2: CustomerFlag="FLAGGED" → FLAGGED_CUSTOMER_HOLD applied
- [ ] Rule 2: CustomerFlag="RESTRICTED" → FLAGGED_CUSTOMER_HOLD applied
- [ ] Rule 2: CustomerFlag="PREFERRED" → no hold
- [ ] Rule 2: `getCustomerList` throws → rule skipped, Rules 1 & 3 still run
- [ ] Rule 3: State in restricted list → RESTRICTED_DESTINATION_HOLD applied
- [ ] Rule 3: State not in restricted list → no hold
- [ ] Rule 3: ZipCode in restricted list → RESTRICTED_DESTINATION_HOLD applied
- [ ] Rule 3: `PersonInfoShipTo` element missing → WARN logged, no hold, no exception
- [ ] All 3 rules trigger simultaneously → all 3 holds applied in single `changeOrder` call
- [ ] `changeOrder` throws → ERROR logged, no exception propagated, order proceeds
- [ ] Null `inputDoc` → early return, no exception

### Integration Tests (Sterling sandbox)

- [ ] Create order with `OrderTotal=6000` → verify hold appears in `getOrderDetails` output
- [ ] Create order for `CustomerID` seeded as FLAGGED in test env → verify hold
- [ ] Create order with `State=AK` (if configured as restricted) → verify hold
- [ ] Create order meeting none of the criteria → verify zero holds
- [ ] Simulate `getCustomerList` unavailable → verify order created without Rule 2 hold

### Performance

- Target: < 200 ms added latency per `createOrder` call under 50 TPS load
- `getCustomerList` call is the primary latency risk; add a 2-second timeout via `yfs.properties`
