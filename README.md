# MiMo-RAG Pro: Enterprise-Grade Knowledge Hub with Self-Healing Capabilities

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-green.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5.32-brightgreen.svg)](https://vuejs.org/)
[![MiMo](https://img.shields.io/badge/Powered_by-MiMo_V2.5-blue.svg)](https://xiaomimimo.com/)

**MiMo-RAG Pro** is a production-ready, full-stack Retrieval-Augmented Generation (RAG) knowledge base system. Unlike standard RAG implementations that rely solely on naive vector chunking, this system introduces **Zero-Loss Long-Context Processing** and a **Knowledge Blind-Spot Self-Healing Loop**, specifically optimized for the Xiaomi MiMo V2.5 & V2.5-Pro models.

## 🌟 Core Highlights

- 🚀 **Non-Blocking Reactive Backend**: Built with Spring WebFlux `WebClient`, handling AI streaming (SSE) and high concurrency without thread pool exhaustion.
- 🧠 **MiMo Long-Context Integration**: Bypasses traditional Embedding chunking for documents under 100k tokens by utilizing MiMo V2.5's 1M context window, achieving "zero-loss" semantic understanding.
- 🕵️ **Strict Answer Traceability**: Every generated response is forced to append source document names, exact text snippets, and cosine similarity scores.
- 🔄 **Self-Healing Knowledge Loop**: Automatically captures low-similarity or missed queries, visualizes them via ECharts, and uses MiMo V2.5-Pro's Agent capabilities to autonomously patch knowledge gaps.

## 🏗️ System Architecture

The system follows a strict 4-layer decoupled architecture:

1. **Presentation Layer**: Vue 3 + Element Plus + Pinia (State Management)
2. **Business Layer**: Spring Boot 3.2.5 (WebFlux for async AI calls, MVC for standard CRUD)
3. **Data Storage Layer**: 
   - MySQL 8.0 (Structured business data)
   - Redis (Multi-turn context caching, JWT sessions, Hot-query caching)
   - Chroma (Embedding vector storage & semantic search)
4. **AI Service Layer**: MiMo V2.5/V2.5-Pro (Generation & Agent), BAAI/bge-large-zh-v1.5 (Embedding)

## ⚙️ Tech Stack

| Component | Technology | Purpose |
| :--- | :--- | :--- |
| **Backend** | Java 17, Spring Boot 3.2.5, Spring WebFlux | RESTful APIs, Reactive async streaming |
| **Frontend** | Vue 3, Vite, Element Plus, ECharts | UI, Markdown rendering, Data visualization |
| **Persistence** | MyBatis 3.5.16, MySQL 8.0 | Structured data ORM |
| **Caching** | Redis 7.x | Session, Context (Latest 3 turns), Hot prompts |
| **Parsing** | Apache Tika 2.9.1 | PDF, DOCX, TXT, Markdown extraction |
| **Vector DB** | Chroma | Semantic vector storage & Top-K retrieval |
| **AI Models** | **MiMo V2.5 / V2.5-Pro**, bge-large-zh-v1.5 | Long-context generation, Agent reasoning, Embedding |

## 🔄 RAG Workflow (MiMo Optimized)

