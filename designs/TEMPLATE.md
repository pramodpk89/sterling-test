# Design Doc: [Feature Name]

> Copy this template for every new feature. Fill in all sections before implementation begins.
> Delete sections that don't apply, but keep the headings so the AI agent can orient itself.

---

## Overview

<!-- 2-4 sentences: what this feature does and why it exists. -->

**Ticket / Jira:** [TICKET-000]
**Author:** [name]
**Date:** [YYYY-MM-DD]
**Status:** Draft | Review | Approved | Implemented

---

## Type

<!-- Select one or more: -->
- [ ] Custom API (extends `YIFCustomApi`)
- [ ] User Exit (implements `YFS*UE`)
- [ ] Agent (extends `YFSAbstractTask`)
- [ ] Service / Pipeline
- [ ] Configuration only
- [ ] Other: _______________

---

## Components to Create

| File | Package | Type | Purpose |
|------|---------|------|---------|
| `ExampleService.java` | `com.mycompany.sterling.service` | Custom API | Short description |
| `BeforeExampleUE.java` | `com.mycompany.sterling.ue` | User Exit | Short description |

---

## Components to Modify

| File | Change Summary |
|------|---------------|
| `api_list.xml` | Register new Custom API |
| `services.xml` | Add service configuration |
| `yfs.properties` | Add feature-flag property |

---

## Input XML

```xml
<!-- Describe the XML document passed to the entry point.
     Include all mandatory and important optional attributes. -->
<Order
    OrderNo="ORD-001"
    EnterpriseCode="ENTERPRISE"
    BuyerOrganizationCode="BUYER_ORG"
    OrderTotal="1500.00">
  <OrderLines>
    <OrderLine OrderLineKey="OLK-001" Quantity="2.0">
      <Item ItemID="ITEM-001" UnitOfMeasure="EACH"/>
    </OrderLine>
  </OrderLines>
</Order>
```

---

## Output XML

```xml
<!-- Describe the XML document returned / emitted.
     Mark optional elements with an XML comment. -->
<Order
    OrderNo="ORD-001"
    Status="SUCCESS">
  <!-- Optional: present only when a hold was applied -->
  <OrderHoldTypes>
    <OrderHoldType HoldType="HIGH_VALUE_HOLD" Status="1"/>
  </OrderHoldTypes>
</Order>
```

---

## Business Logic

<!-- Number each rule so the AI agent can reference them in code comments. -->

1. **Rule 1 — [Name]:** Description of the rule and its conditions.
2. **Rule 2 — [Name]:** Description of the rule and its conditions.
3. **Rule 3 — [Name]:** Description of the rule and its conditions.

### Decision Table (optional)

| Condition | Action |
|-----------|--------|
| `OrderTotal > X` | Apply hold type `Y` |
| `CustomerType == "FLAGGED"` | Reject / log warning |

---

## Error Handling

- **Fail-open / Fail-closed?** [choose one and explain why]
- **Error codes used:**
  - `MYCO_<MODULE>_001` — [description]
  - `MYCO_<MODULE>_002` — [description]
- **Logging level:** ERROR for unexpected exceptions; WARN for business rule violations; DEBUG for trace.

---

## Dependencies

| Dependency | Type | Notes |
|------------|------|-------|
| `getOrderDetails` | Sterling API | Read order attributes |
| `changeOrder` | Sterling API | Apply holds |
| `yfs.properties: mycompany.module.threshold` | Config | Configurable threshold value |
| `CustomerRiskService` | Internal class | Reused from another module |

---

## Testing Notes

- [ ] Unit test with mock `YFSEnvironment` and `YFCDocument`
- [ ] Integration test: create order via Sterling sandbox, verify hold applied
- [ ] Test matrix:
  - Threshold boundary values (at, above, below)
  - Missing / null attributes
  - API call failures (verify fail-open behavior)
  - Large order XML (performance baseline)
- [ ] Verify no duplicate holds on retry

---

## Additional Notes

<!-- Architecture diagrams, links to Confluence, open questions, etc. -->
