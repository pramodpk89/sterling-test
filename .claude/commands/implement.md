Read `context/sterling-knowledge.md` fully before writing any code.

Arguments: $ARGUMENTS
Expected: path to a design doc (e.g. `designs/implement-order-hold-service.md`)

If no path is provided, stop and respond:
"Usage: /implement <design-doc-path>  (e.g. /implement designs/implement-order-hold-service.md)"

---

Execute these steps in order. Do not skip any step.

**Step 1 — Read the design doc**
Read the file at the path given in the arguments. Understand it completely before writing any code. Note:
- The Type (Custom API / User Exit / Agent / Service)
- Every row in the "Components to Create" table — these are the files you must create
- The Input XML and Output XML structure
- Every business rule in the "Business Logic" section
- The fail strategy (fail-open or fail-closed) from "Error Handling"
- All error codes from the "Error Handling" table
- All Sterling API dependencies from the "Dependencies" table

**Step 2 — Implement each component**
For each file in "Components to Create":

a. Apply the correct pattern from Section B of `context/sterling-knowledge.md`:
   - User Exit → User Exit Pattern
   - Custom API → Custom API Pattern
   - Agent → Agent pattern (`YFSAbstractTask`)
   - Service → Custom API Pattern (services are also `YIFCustomApi`-based)

b. Sterling JARs are not present — use `org.w3c.dom` for working code. Show the real YFCDocument pattern as inline comments above every XML operation:
   ```java
   // In real Sterling: YFCDocument inDoc = YFCDocument.getDocumentFor(inputDoc);
   // YFCElement root = inDoc.getDocumentElement();
   // String val = root.getAttribute("Attr"); // returns "" not null
   Element root = inputDoc.getDocumentElement(); // stub
   String val = SterlingAPIHelper.safeGetAttribute(root, "Attr");
   ```

c. Use `SterlingAPIHelper` from `com.mycompany.sterling.util` for all attribute reads.

d. Implement ALL business logic from the design doc — this is not a stub. Rules, conditions, and decision tables must all be coded.

e. Use the exact error codes from the design doc's "Error Handling" table.

f. Apply the fail strategy stated in the design doc. For UEs: fail-open (catch Exception, log SEVERE, swallow). Match exactly what the design doc says for other types.

g. Include the registration snippet (api_list.xml / services.xml / agent_criteria.xml) as a Javadoc `<pre>` comment on the class.

h. Place files at the correct paths using the package from the "Components to Create" table:
   - `com.mycompany.sterling.ue` → `src/main/java/com/mycompany/sterling/ue/`
   - `com.mycompany.sterling.api` → `src/main/java/com/mycompany/sterling/api/`
   - `com.mycompany.sterling.service` → `src/main/java/com/mycompany/sterling/service/`
   - `com.mycompany.sterling.agent` → `src/main/java/com/mycompany/sterling/agent/`

**Step 3 — Compile**
Run `mvn compile` from the project root.

**Step 4 — Fix errors**
If `mvn compile` reports errors, read every error line, fix the root cause in the generated files, and run `mvn compile` again. Repeat until it compiles clean. Do not suppress errors with casts or empty catch blocks.

**Step 5 — Report**
When compilation succeeds, print:
1. List of files created (with relative paths)
2. Registration snippets ready to paste (api_list.xml / services.xml entries)
3. Properties to add to `yfs.properties` (from the design doc's Dependencies section)
