<#-- LLM Code Generation Prompt Template -->
You are a senior Java Spring Boot developer and API integration expert. Your task is to create config, controller, service classes, and their unit tests for a passthrough REST service based on the provided Postman collection and template classes.

1. Scan the methods and authentication details defined in the Postman collection provided below.
2. Using the scanned information, implement the methods in the Controller and Service classes. Base your implementation on the structure and style of the templateController and templateService classes provided. The methods should closely mirror the structure of the requests in the Postman collection, handling variables (auth type, params, body, etc.) similarly.
3. Use the templateController and templateService classes as a reference for coding style, architecture, and best practices.
4. **Do not add any comment lines to the generated Java code.**
5. Write comprehensive JUnit 5 unit tests for the newly created Service and Controller classes using Mockito. Ensure the tests follow the correct package structure.

---

**General Requirements:**
- Design the package and import sections according to standard Java and Spring Boot conventions, based on the main package: `${packageName}`. Controller should be in `.controller`, Service in `.service`, Config (if needed) in `.config`, and tests should mirror this under `src/test/java`.
- Remove authentication information (like the `Authorization` header) from the incoming HTTP request within the Controller class.
- Pass the necessary credentials (e.g., fetched token, basic auth) to all downstream service calls made within the Service methods. Use `RestTemplate` to set appropriate headers (`Authorization`, `Content-Type`, `Accept`) for outgoing requests.
- Follow consistent logging (using SLF4J), exception handling (catching `HttpStatusCodeException` and general `Exception`), and request/response handling patterns as shown in the template classes.
- Return **ONLY** the Java code files within \`\`\`java ... \`\`\` blocks. Provide exactly four files: the fully implemented Controller, the fully implemented Service, the ControllerTest, and the ServiceTest. Do not include any other explanatory text outside the code blocks.

---

**Target Service Class (Implement methods based on Postman collection):**
\`\`\`java
${serviceTemplateContent}
\`\`\`

---

**Target Controller Class (Implement methods based on Postman collection):**
\`\`\`java
${controllerTemplateContent}
\`\`\`

---

**Reference Template Controller Class (Use for style and structure):**
\`\`\`java
${referenceControllerContent}
\`\`\`

---

**Reference Template Service Class (Use for style and structure):**
\`\`\`java
${referenceServiceContent}
\`\`\`

---

**Postman Collection JSON (Analyze this for endpoints, methods, auth, params, body):**
\`\`\`json
${postmanCollectionJson}
\`\`\`