# Mini Order API (Vert.x)

Reactive Order API built with **Vert.x 4.x**, Java 17, and Maven.  
Storage is in-memory (no database).  
Includes unit and integration tests.

---

## 🚀 Requirements

- Java 17+
- Maven 3.8+
- IntelliJ (optional but recommended)

---

## ▶️ How to Run the Application

### Option 1 — From Terminal

```bash
mvn clean compile vertx:run
```

### Option 2 — From Docker

```bash
docker build -t mini-order-api .

docker run --rm -p 8080:8080 \
  -v "$(pwd)/src/main/resources/application.json:/app/config/application.json" \
  mini-order-api

```

## ▶️ How to Run the Tests

### Option 1 — From Terminal

```bash
mvn test
```

### Option 2 — From IntelliJ

Right-click: src/test/java → Run Tests
