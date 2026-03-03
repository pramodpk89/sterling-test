Read `context/sterling-knowledge.md` fully, paying close attention to the Agent section in Section A and the `agent_criteria.xml` format in Section D.

Arguments: $ARGUMENTS
Expected format: `<ClassName>`
Example: `/new-agent HoldReview`

If arguments are missing, stop and respond:
"Usage: /new-agent <ClassName>  (e.g. /new-agent HoldReview)"

---

Create ONE file: `src/main/java/com/mycompany/sterling/agent/<ClassName>Agent.java`

Follow these rules exactly — do not deviate:

1. **Package**: `com.mycompany.sterling.agent`

2. **Class declaration**: `public class <ClassName>Agent` with NO `extends` clause.
   Add this comment on the line above the class: `// In real Sterling: extends YFSAbstractTask`

3. **Entry method**:
   ```java
   // In real Sterling: @Override
   public void executeTask(/* YFSEnvironment env, */ Document criteriaDoc) {
       // In real Sterling: throws YFSException
   ```

4. **Imports**: Only these — no others:
   ```
   import com.mycompany.sterling.util.SterlingAPIHelper;
   import org.w3c.dom.Document;
   import org.w3c.dom.Element;
   import java.util.logging.Level;
   import java.util.logging.Logger;
   ```

5. **Logger**: `private static final Logger LOG = Logger.getLogger(<ClassName>Agent.class.getName());`
   Add comment: `// In real Sterling: YFCLogCategory LOG = YFCLogCategory.instance(<ClassName>Agent.class);`

6. **Null guard**: If criteriaDoc is null, log WARNING with `MYCO_AGENT_001` and return.

7. **Top-level try/catch**: Wrap the entire method body (after null guard) in `try { ... } catch (Exception e) { LOG.log(Level.SEVERE, "MYCO_AGENT_002 - ...", e); }` — swallow, never rethrow.
   Agents run outside the API transaction, so exceptions must never escape.

8. **XML comment pattern**: Inside the try block, add these comments:
   ```java
   // In real Sterling:
   // YFCDocument criteria = YFCDocument.getDocumentFor(criteriaDoc);
   // YFCElement root = criteria.getDocumentElement();
   // String val = root.getAttribute("SomeAttr"); // returns "" not null
   ```

9. **Method body**: A single line after the comments: `// TODO: implement <ClassName> agent logic here`

10. **Class-level Javadoc**: Include the `agent_criteria.xml` registration snippet in a `<pre>` block using the format from Section D of the knowledge file. Use `MYCO_<CLASSNAME_UPPER>_AGENT` as the `AgentCriteriaId`.

---

After writing the file, print the `agent_criteria.xml` registration snippet as plain text for easy copying.
