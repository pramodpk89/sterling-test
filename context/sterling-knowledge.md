# Sterling OMS Domain Knowledge

> This file is the authoritative AI context source for Sterling OMS development on this project.
> Read it fully before writing or modifying any Sterling-related code.

---

## Section A: Sterling Architecture Overview

### What Sterling OMS Is

IBM Sterling Order Management System (OMS) is an enterprise platform for managing
the full lifecycle of an order: capture, promising, fulfillment, and settlement.
It exposes its functionality through a layered architecture of **APIs**, **Services**,
**User Exits**, **Agents**, and **Pipelines**.

### Core Architectural Layers

```
┌─────────────────────────────────────────────────┐
│               External Clients                  │
│     (REST/SOAP adapters, UI, B2B partners)      │
└──────────────────────┬──────────────────────────┘
                       │ XML over HTTP / JMS
┌──────────────────────▼──────────────────────────┐
│                  Sterling APIs                  │
│  (createOrder, changeOrder, getOrderDetails…)   │
│  Entry point: YIFApi.invoke(env, apiName, doc)  │
└──────────┬───────────────────────┬──────────────┘
           │ User Exits (sync)     │ Services (async/sync)
┌──────────▼──────┐      ┌─────────▼────────────┐
│   User Exits    │      │   Sterling Services   │
│ (YFS*UE impls)  │      │  (Pipeline steps,     │
│  Before/After   │      │   Condition evals,    │
│  hooks on APIs  │      │   Transaction mgmt)   │
└──────────┬──────┘      └─────────┬────────────┘
           └──────────┬────────────┘
              ┌───────▼────────┐
              │   Database     │
              │  (YFS schema)  │
              └────────────────┘
                       │
              ┌────────▼────────┐
              │     Agents      │
              │  (scheduled /   │
              │   event-driven) │
              └─────────────────┘
```

### APIs

- Entry point for all business operations.
- Called via `YIFApi api = YIFClientFactory.getInstance().getLocalApi(); api.invoke(env, apiName, doc)`.
- Each API accepts an XML `Document` and returns an XML `Document`.
- APIs fire **User Exits** (before/after hooks) and may call **Services** internally.
- Standard APIs live in `com.yantra.yfs.japi`; Custom APIs extend `YIFCustomApi`.

### Services

- Business logic units registered in `services.xml`.
- Run inside Pipeline **transactions** (begin/commit/rollback managed by Sterling).
- Can be invoked synchronously (inline) or asynchronously (via JMS queue).
- Implement `YIFApi` or extend `YFSAbstractTask` for agent-style services.

### User Exits (UEs)

- Hooks that fire **before** or **after** a standard API executes.
- Registered in `api_list.xml` under the API's `<UEList>`.
- Implement the corresponding `YFS*UE` interface (e.g., `YFSBeforeCreateOrderUE`).
- **Before UEs** can inspect/modify the input document; they run inside the same transaction.
- **After UEs** see the output document; good for cross-system side-effects.
- **Rule**: Never throw unrecoverable exceptions from a UE unless intentionally blocking the API.

### Agents

- Scheduled Java tasks that process queued work (e.g., background scheduling, alerts).
- Extend `YFSAbstractTask` and are configured in `agent_criteria.xml`.
- Each agent run fetches a batch of records, processes them, and updates status.
- Agents run outside the API transaction; they must manage their own error handling.

### Pipelines

- Ordered sequences of Services with conditional branching.
- Defined in `pipeline.xml`; each node is a Service or condition evaluator.
- State transitions (e.g., `CREATED → RELEASED → SCHEDULED`) are driven by pipelines.
- Customise by adding nodes to existing pipelines or creating new pipelines.

### Extension Framework

Sterling uses an **override** pattern for customisation:

| Extension point | Mechanism |
|-----------------|-----------|
| Custom API | Create class extending `YIFCustomApi`; register in `api_list.xml` |
| User Exit | Create class implementing `YFS*UE`; register in `api_list.xml` |
| Custom Service | Create class; register in `services.xml` |
| Custom Agent | Extend `YFSAbstractTask`; register in `agent_criteria.xml` |
| Template override | Copy `template/api/<API>_output.xml` to extensions folder and edit |
| Property override | Add to `extensions/global/properties/yfs.properties` |

All customisation files live under `<STERLING_INSTALL>/extensions/global/`.
The Sterling classloader merges extension JARs with core JARs at startup.

---

## Section B: Coding Patterns & Conventions

### Custom API Pattern

```java
import com.yantra.yfc.core.YFCIterable;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import com.yantra.interop.japi.YIFCustomApi;
import com.yantra.yif.api.YIFApi;
import com.yantra.yif.api.YIFClientFactory;
import org.w3c.dom.Document;

public class MyCustomAPI extends YIFCustomApi {

    @Override
    public Document invoke(YFSEnvironment env, Document inputDoc) throws YFSException {
        // 1. Parse input
        YFCDocument inDoc = YFCDocument.getDocumentFor(inputDoc);
        YFCElement root = inDoc.getDocumentElement();
        String orderId = root.getAttribute("OrderNo");  // null-safe in YFCElement

        // 2. Call a Sterling API
        YIFApi api = YIFClientFactory.getInstance().getLocalApi();
        YFCDocument apiInput = YFCDocument.createDocument("Order");
        apiInput.getDocumentElement().setAttribute("OrderNo", orderId);
        Document apiOutput = api.invoke(env, "getOrderDetails", apiInput.getDocument());

        // 3. Build output
        YFCDocument outDoc = YFCDocument.createDocument("MyOutput");
        outDoc.getDocumentElement().setAttribute("Status", "SUCCESS");
        return outDoc.getDocument();
    }
}
```

### User Exit Pattern

```java
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import com.yantra.yfs.japi.ue.YFSBeforeCreateOrderUE;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import org.w3c.dom.Document;

public class BeforeCreateOrderHoldUE implements YFSBeforeCreateOrderUE {

    @Override
    public void beforeCreateOrder(YFSEnvironment env, Document inputDoc) throws YFSException {
        if (inputDoc == null) return;

        YFCDocument inDoc = YFCDocument.getDocumentFor(inputDoc);
        YFCElement orderElem = inDoc.getDocumentElement();
        String orderTotal = orderElem.getAttribute("OrderTotal");  // returns "" not null

        // business logic...
    }
}
```

### XML Handling (CRITICAL)

- **Always use `YFCDocument` / `YFCElement`**, never raw `org.w3c.dom.Document` / `Element`.
- `YFCElement.getAttribute(name)` returns `""` (empty string) when absent — never `null`.
  Always check `YFCLogCategory.isEmpty(value)` or `"".equals(value)` before using it.
- To create a document: `YFCDocument doc = YFCDocument.createDocument("RootElement");`
- To wrap an existing Document: `YFCDocument doc = YFCDocument.getDocumentFor(domDoc);`
- To add a child element: `YFCElement child = parent.createChild("ChildElement");`
- To iterate children: `for (YFCElement child : parent.getChildren("ChildTag")) { ... }`

### Error Handling

```java
// Throw a Sterling business exception (visible to caller):
throw new YFSException("MYCO_MODULE_001", "Human-readable message", null);

// Log with Sterling's logger (preferred over java.util.logging in real Sterling):
import com.yantra.yfc.log.YFCLogCategory;
private static final YFCLogCategory LOG = YFCLogCategory.instance(MyClass.class);

LOG.error("Error message", exception);
LOG.warn("Warning message");
LOG.info("Info message");
LOG.debug("Debug message");

// Fail-open pattern:
try {
    Document result = api.invoke(env, "someApi", input);
} catch (YFSException e) {
    LOG.error("MYCO_MODULE_002 - someApi failed; proceeding without result", e);
    // do NOT rethrow — allow calling code to continue
}
```

### Null Safety Rules

1. `YFCElement.getAttribute(name)` → check `"".equals(value)` not `value == null`.
2. `YFCDocument.getDocumentFor(doc)` → safe on any non-null `Document`.
3. Always null-check the `Document` parameter before calling `getDocumentFor`.
4. When calling `YIFApi.invoke`, check the returned `Document` before parsing.
5. Collection results from `getOrderList`, `getCustomerList`, etc. can have 0 rows — always check `getLength()` before accessing index 0.

### Transaction Management

- UEs and Custom APIs fire **inside the calling transaction** — do not create new connections or transactions.
- For side-effects that must survive a rollback (e.g., audit logs), use the Sterling **asynch event framework** or write to a separate audit schema with its own connection.
- Never call `Connection.commit()` or `Connection.rollback()` directly; Sterling manages transactions.

---

## Section C: Common APIs Reference

| API Name | Purpose | Key Input Attributes | Key Output Attributes |
|----------|---------|---------------------|----------------------|
| `createOrder` | Create a new order | `Order@EnterpriseCode`, `Order@BuyerOrganizationCode`, `OrderLines/OrderLine` | `Order@OrderHeaderKey`, `Order@OrderNo` |
| `changeOrder` | Modify order attributes, apply holds, cancel lines | `Order@OrderHeaderKey` or `Order@OrderNo+EnterpriseCode`, changed attributes | Updated `Order` element |
| `getOrderDetails` | Retrieve full order document | `Order@OrderHeaderKey` or `Order@OrderNo+EnterpriseCode` | Full `Order` XML with all child elements |
| `getOrderList` | List orders matching criteria | `Order` with filter attributes, `Pagination` element | `OrderList/Order` collection |
| `createOrderInvoice` | Create invoice for an order | `Order@OrderHeaderKey`, `InvoiceDetail` | `Invoice@InvoiceNo` |
| `confirmShipment` | Record that a shipment has been physically shipped | `Shipment@ShipmentKey`, `ContainerList` | Updated `Shipment` |
| `createShipment` | Create a new outbound shipment | `Shipment` with order line details | `Shipment@ShipmentKey` |
| `getShipmentDetails` | Retrieve shipment details | `Shipment@ShipmentKey` | Full `Shipment` XML |
| `receiveOrder` | Receive inbound PO / return | `Receipt`, `ReceiptLines` | `Receipt@ReceiptNo` |
| `scheduleOrder` | Schedule order lines for fulfillment | `Order@OrderHeaderKey`, scheduling template | `Order` with schedule assignments |
| `releaseOrder` | Release order to fulfillment | `Order@OrderHeaderKey`, `ReleaseDetail` | Released `Order` |
| `getItemAvailability` | ATP / promising check | `Item@ItemID`, `UnitOfMeasure`, quantity, `ShipDate` | `Availability/ItemAvailability` collection |
| `manageItem` | Create or modify item/catalog records | `Item@ItemID`, `ItemID@UnitOfMeasure` | Updated `Item` |
| `getCustomerList` | Search customers | `Customer@CustomerID` or `Customer@EMailID` | `CustomerList/Customer` collection |
| `manageCustomer` | Create or modify customer | `Customer@CustomerID`, `BillToAddress` etc. | `Customer@CustomerKey` |
| `createException` | Log a Sterling exception record | `Exception@ExceptionType`, `ExceptionDescription` | `Exception@ExceptionKey` |
| `getOrganizationList` | Look up enterprise / org | `Organization@OrganizationCode` | `OrganizationList/Organization` |
| `invokeUEForAddtionalAttr` | Generic UE invocation for attribute enrichment | API-specific | API-specific |
| `getWorkOrderDetails` | Retrieve value-added service work order | `WorkOrder@WorkOrderKey` | Full `WorkOrder` XML |
| `changeWorkOrder` | Modify work order status | `WorkOrder@WorkOrderKey`, `Status` | Updated `WorkOrder` |

### API Invocation Example (getOrderDetails)

```java
// Build input
YFCDocument inputDoc = YFCDocument.createDocument("Order");
YFCElement orderElem = inputDoc.getDocumentElement();
orderElem.setAttribute("OrderHeaderKey", "OHK-000001");

// Set template to control output fields
Document templateDoc = // load from classpath or build inline

// Invoke
YIFApi api = YIFClientFactory.getInstance().getLocalApi();
Document outputDoc = api.invoke(env, "getOrderDetails", inputDoc.getDocument());

// Parse output
YFCDocument outDoc = YFCDocument.getDocumentFor(outputDoc);
YFCElement order = outDoc.getDocumentElement();
String status = order.getAttribute("Status");
```

---

## Section D: Configuration Reference

### yfs.properties

- Location: `<INSTALL>/properties/yfs.properties` (core); customisations go in
  `<INSTALL>/extensions/global/properties/yfs.properties`.
- Extension properties **override** core properties with the same key.
- Read at runtime via `YFCProperties.getInstance().getProperty("key")` or
  `YFCProperties.getInstance().getProperty("key", "defaultValue")`.
- Changes require a server restart (properties are cached at startup).

**Key core properties:**

```properties
# Database connection
yfs.db.url=jdbc:db2://host:50000/YFSDB
yfs.db.user=yfsuser
yfs.db.password=secret

# Timezone and locale
yfs.timezone=UTC
yfs.locale=en_US_EST

# Transaction timeout (seconds)
yfs.tx.timeout=120
```

### api_list.xml

- Location: `extensions/global/api_list.xml`
- Registers custom APIs and User Exits on standard APIs.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<APIList>

  <!-- Register a custom API -->
  <API Name="myCustomApi"
       Factory="com.mycompany.sterling.api.MyCustomAPI"/>

  <!-- Register a User Exit on createOrder -->
  <API Name="createOrder">
    <UEList>
      <UE Name="YFSBeforeCreateOrderUE"
          Factory="com.mycompany.sterling.ue.BeforeCreateOrderHoldUE"/>
    </UEList>
  </API>

</APIList>
```

### services.xml

- Location: `extensions/global/services.xml`
- Defines custom services and overrides service behaviour.

```xml
<ServiceDefinition>
  <Service Name="mycompany.OrderHoldValidationService"
           FactoryClass="com.mycompany.sterling.service.OrderHoldValidationService"
           TransactionType="REQUIRED"/>
</ServiceDefinition>
```

### agent_criteria.xml

- Location: `extensions/global/agent_criteria.xml`
- Configures custom agents and their scheduling.

```xml
<AgentCriteria AgentCriteriaId="MYCO_HOLD_REVIEW_AGENT"
               AgentCode="com.mycompany.sterling.agent.HoldReviewAgent"
               CriteriaId="DEFAULT"
               NumberOfThreads="4"
               MaxRecordsToBuffer="100"/>
```

### Pipeline XML

- Override location: `extensions/global/pipeline/<PipelineKey>.xml`
- Add a custom service node between existing nodes:

```xml
<TransactionType TransactionTypeId="CREATED">
  <DependentTransactionTypes>
    <DependentTransactionType TransactionTypeId="MYCO_HOLD_VALIDATION"
                              ServiceName="mycompany.OrderHoldValidationService"/>
  </DependentTransactionTypes>
</TransactionType>
```

### Registering Custom APIs and Services — Step-by-Step

1. Build your class and include the JAR in `<INSTALL>/extensions/global/lib/`.
2. Add `<API>` or `<UE>` entries to `extensions/global/api_list.xml`.
3. Add `<ServiceDefinition>` entries to `extensions/global/services.xml` if needed.
4. Restart the Sterling application server.
5. Validate by calling the API via the Sterling Interactive Console or a test client.

### Output Templates

- Located at `template/api/<APIName>_output.xml`.
- Control which attributes are returned by an API by default.
- Override by copying to `extensions/global/template/api/<APIName>_output.xml` and adding attributes.

---

## Section E: Project Conventions

### Package Structure

```
com.mycompany.sterling
├── api/          Custom APIs (extend YIFCustomApi)
├── ue/           User Exits (implement YFS*UE interfaces)
├── service/      Service classes (called by pipelines or agents)
├── agent/        Agent tasks (extend YFSAbstractTask)
├── util/         Shared utility classes
└── model/        Value objects / DTOs (plain Java, no Sterling deps)
```

### Naming Conventions

| Component | Suffix | Example |
|-----------|--------|---------|
| Custom API | `*API` | `OrderHoldValidationAPI` |
| User Exit | `*UE` | `BeforeCreateOrderHoldUE` |
| Service class | `*Service` | `OrderHoldValidationService` |
| Agent class | `*Agent` | `HoldReviewAgent` |
| Utility class | `*Helper` or `*Util` | `SterlingAPIHelper` |
| Test class | `*Test` | `OrderHoldValidationServiceTest` |

### Error Codes

Format: `MYCO_<MODULE>_<3-digit-number>`

| Module tag | Area |
|------------|------|
| `ORDER` | Order management (createOrder, changeOrder, etc.) |
| `HOLD` | Order hold validation |
| `SHIP` | Shipment processing |
| `INV` | Inventory and availability |
| `CUST` | Customer management |
| `AGENT` | Background agent tasks |

Examples: `MYCO_HOLD_001`, `MYCO_ORDER_007`, `MYCO_SHIP_003`

### Properties Namespace

All custom properties use the prefix `mycompany.<module>.<property>`:

```properties
mycompany.order.hold.highvalue.threshold=5000.00
mycompany.order.hold.restricted.states=AK,HI,PR
mycompany.order.hold.restricted.zipcodes=
mycompany.ship.carrier.timeout.seconds=10
```

### Logging Conventions

- Use `YFCLogCategory.instance(MyClass.class)` in real Sterling; `java.util.logging.Logger` as stub.
- Always include the error code in WARN/ERROR messages: `"MYCO_HOLD_001 - ..."`.
- Log at `DEBUG` for attribute reads and decision traces.
- Log at `INFO` for significant business events (hold applied, order released, etc.).
- Log at `WARN` for missing optional data and skipped rules.
- Log at `ERROR` for API failures and unexpected exceptions.

### Fail-Open vs Fail-Closed

- **UEs always fail-open** unless the business explicitly requires blocking (e.g., fraud hold must block).
- **Services can fail-closed** for mandatory validations (quantity checks, address validation).
- Document the failure strategy in the design doc before implementation.

### Code Review Checklist

- [ ] No raw DOM usage — all XML via `YFCDocument`/`YFCElement` (or stub pattern with comments)
- [ ] All `getAttribute` results checked for empty string before use
- [ ] `YFSException` error codes follow `MYCO_<MODULE>_<NNN>` format
- [ ] Logging uses appropriate level and includes error code
- [ ] Fail strategy (open/closed) matches design doc
- [ ] No direct DB access — all data access via Sterling APIs
- [ ] Unit tests cover all rule boundary conditions
- [ ] Javadoc on all public methods
