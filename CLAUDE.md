# 🤖 Claude Context Guide

## 🧩 Project Overview
This project is a **Spring Boot (Java 17)** application for TDD practice.
The goal is to implement a **Point Service** that supports:
- Point balance inquiry
- Point charge and usage
- Transaction history

This is part of the `hhplus-week01-tdd` assignment.

---

## 🧪 Development Approach
We are following **Test-Driven Development (TDD)** strictly:
1. Write failing tests (Red)
2. Make minimal code to pass (Green)
3. Refactor for better structure (Refactor)

All implementation should begin with test creation.

---

## 🧱 Architecture Guideline
- Layered Architecture: **Controller → Service → Repository**
- Use in-memory data structures (no external DB)
- Follow **Clean Architecture** and **SOLID principles**
- Log behavior using `org.slf4j.Logger`

---

## 🧰 Dependencies & Tools
- **Spring Boot Starter Web**
- **Lombok** (for boilerplate code)
- **JUnit 5** (for testing)
- **Jacoco** (for coverage reporting)
- **Gradle Kotlin DSL (build.gradle.kts)**

The project runs with:
```bash
./gradlew test
