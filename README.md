# DMP-Evaluation-Service

Repository with specifications and rulesets developed to support the semi-automated evaluation of Data Management Plans (DMPs). It is designed to support evaluation of both machine-actionable DMPs (maDMPs), i.e. in JSON format and compliant with the DMP Common Standard (DCS), as well as traditional, narrative-style DMPs. It is a key component of the OSTrails project (https://ostrails.eu/) and reflects input from research funders, institutional policy frameworks, and research support needs.

**Authors:** Tomasz Miksa, Elli Papadopoulou, Lukas Arnhold, Andres Mauricio, Maria Kontopidi, Diamantis Tziotzios, Georgios Kakaletris

# Evaluation Dimensions
Evaluation is organised across five core dimensions, each addressing a critical aspect of a high-quality Data Management Plan:
- **Content Completeness:** Verifies whether all required sections of the DMP have been addressed. It checks for presence, adequacy, and consistency of information, ensuring that no essential element is missing.
- **Research Data Management Coverage:** Assesses how thoroughly the DMP addresses key areas of research data management, such as data collection, documentation, storage, access, sharing, and preservation.
- **Openness:** Examines the extent to which the DMP supports open access to data, metadata, and other outputs, including considerations of licensing, embargo periods, and justifications for any access restrictions.
- **FAIRness:** Evaluates the extent to which the DMP aligns with the FAIR Principles (Findable, Accessible, Interoperable, Reusable), including aspects such as metadata richness, licensing, and use of persistent identifiers.
- **Policy alignment:** Measures the degree to which the DMP reflects and adheres to relevant institutional, funder, and legal policies (e.g., GDPR compliance, data sharing mandates).
- **Standards compliance:** Evaluates whether the DMP adheres to recognized structural and content standards (e.g., the DMP Common Standard), supporting interoperability, machine-readability, and alignment with community expectations.

# Exemplar metadata
Compatible with: https://github.com/OSTrails/FAIR_assessment_output_specification 

# Requirements to run the service.
Java 17 or higher Required for Spring Boot 3.x. <br>
MongoDB Any (locally or Docker) Can use Docker to simplify. <br>

# How to run the service.
1. clone the repository
   * git clone https://github.com/your-org/dmp-evaluator-service.git
   * cd dmp-evaluator-service
2. Start MongoDB Using Docker Compose
   * Make sure Docker is running, then start the MongoDB service:
   * docker-compose up -d
3. Build the project
   * mvn clean install
4. Run the Application
   * mvn spring-boot:run
5. Checking th status
   * Access Swagger UI (Optional)
   * http://localhost:8080/swagger-ui.html



