# What is this
This is a course project for CS 230, distributed system, at the University of California, Irvine. The project aims to implement a distributed search engine for the monthly Wikipedia dump file using Lucene Java. Aside from using Lucene for basic searching, we built the distributed indexing/searching, ranking, and caching system in-house.
You may find a demo instance of the project [here](https://wiki.javacafe.dev).
The wonderful React frontend of this project it at [wiki-go](https://github.com/chrismar303/wiki-go), and a simple backend used for load balancing is at [wiki-go-backend](https://github.com/chrismar303/wiki-go-backend).

## Why not Elastic search/Solr
This is clearly not a production ready implementation of a search engine. Both Elastic search and Apache Solr are far more mature than this project. We try to learn more about distributed system and information retrevial by rebuilding the wheel. Hopefully, the future audience of this project are interested at these topics as we do. 

# Project Overview

This project integrates multiple technologies including Java (with Maven), TypeScript, Python (with pip), and others to create a fuzzy search system for Wiki documents. The system is designed with a modular architecture and consists of three main modules: `FuzzyWikiApp`, `libFuzzyWiki`, and a parser script.

## Modules

### FuzzyWikiApp

The `FuzzyWikiApp` module provides the application layer for executing and testing search queries. Key functions include:

- **Server Initialization**: Contains a `Server` class which initializes the searcher components and connects the application with backend services.

### libFuzzyWiki

The `libFuzzyWiki` module contains the core search logic and low-level operations. It is responsible for:

- **Search Processing**: Implementing the low-level fuzzy search mechanisms, merging of search results, and document ranking based on relevancy.
- **Distributed indexing**: Utilizing Hadoop’s Map-Reduce framework to index the preprocessed JSONL file into several shards of Lucene indexes.
- **Distributed searching**: A home made re-implentatin of Lucene's BM25 to achieve distributed searching and reranking the query results.

#### Java Packages in libFuzzyWiki

- **Searcher Class**  
  This class is at the heart of the search functionalities. It handles:
  - Initializing the Lucene index readers and searchers.
  - Executing queries (both fuzzy and exact) using a custom similarity mechanism.
  - Parsing and formatting search results for further processing.

- **Other Supporting Classes**  
  Additional helper classes provide functions such as calculating term frequencies and managing search results, ensuring that each document’s relevancy score is correctly computed and merged.

### Parser Script

The parser script (typically a Python script) is utilized for reading, extracting, and preprocessing the compressed Wiki dump file into a JSONL file. Its main functions are:

- **Data Extraction**: Reading Wiki documents and extracting relevant text and metadata. 
- **Preprocessing**: Cleansing and preparing textual data for indexing and search operations.
- **Integration**: Providing processed data to the Java modules for search and ranking, ensuring the Searcher operates on up-to-date content.

# API Documentation
## Overview
This document provides details on the available API endpoints for the search functionality. The APIs facilitate distributed search queries with configurable forwarding behavior and allow retrieval of document content based on search results.

## Endpoints

### 1. Search API
**Endpoint:**
```
<url>/search?query=<query>&forwarding=[true|false]
```

**Description:**
The primary endpoint for executing search queries. The `query` parameter specifies the search term and may include special characters or spaces encoded as `%20`. The `forwarding` parameter determines whether the search request is propagated to other distributed search instances.

- If `forwarding=true`, the request is initially processed by the specified host, which retrieves local results and then forwards the query to the most relevant peer servers. The aggregated results are ranked using the BM25 algorithm and returned as a sorted JSON array.
- If `forwarding=false`, only the requested node processes the search query, without propagating it to other instances.

**Example Requests & Responses:**

**Request:**
```
<url>/search?query=panda&forwarding=true
```
**Response:**
```json
[
    {
        "score": 13.14291,
        "text": "The Fiat Panda is a city car manufactured and marketed by Fiat since 1980, currently in its third generation.",
        "title": "Fiat Panda"
    },
    ...
]
```

In this case, the response contains ranked search results, where:
- `score`: The combined relevance score across all distributed search instances.
- `text`: A truncated preview of the document's content.
- `title`: The document title.

**Request:**
```
<url>/search?query=panda&forwarding=false
```
**Response:**
```json
{
    "textMap": {
        "Fiat Panda": "The Fiat Panda is a city car manufactured and marketed by Fiat since 1980..."
    },
    "infoMap": {
        "Panda, go panda": {
            "text:big panda": [6.6032476, 0],
            "title:panda": [11.300013, 0.6224865],
            "title:big": [7.47812, 0],
            "text:big": [0.75260127, 0],
            "text:panda": [3.6495636, 0],
            "title:big panda": [25.606544, 0]
        }
    },
    "weightMap": {
        "text:big panda": 6.6032476,
        "title:panda": 11.300013,
        "title:big": 7.47812,
        "text:big": 0.75260127,
        "text:panda": 3.6495636,
        "title:big panda": 25.606544
    }
}
```

In this scenario, the response includes detailed internal search data:
- `textMap`: A mapping of document titles to their full text.
- `infoMap`: A mapping of term weights in various document fields.
- `weightMap`: Frequency and importance of search terms within the index.

The weights in `weightMap` follow the TF-IDF and BM25 principles (see [Okapi BM25](https://en.wikipedia.org/wiki/Okapi_BM25) for more details).

### 2. Document Retrieval API
**Endpoint:**
```
<url>/document/<document_title>
```

**Description:**
Retrieves the full text of a document given its title. The `document_title` parameter is case-sensitive, and spaces must be encoded as `%20`.

**Example Request & Response:**

**Request:**
```
<url>/document/Fiat%20Panda
```
**Response:**
```json
{
    "text": "The Fiat Panda is a city car manufactured and marketed by Fiat since 1980, currently in its third generation.",
    "title": "Fiat Panda"
}
```

## Notes
- The `forwarding=true` search API is intended for distributed search scenarios and may introduce additional latency due to inter-node communication.
- The `forwarding=false` API is primarily for debugging and internal use.
- The document retrieval API should be used only with titles obtained from prior search results.

This documentation provides a detailed overview of the available APIs, including their intended use, parameters, and expected responses. If you have any questions or require further clarifications, please refer to the project's technical documentation or contact the development team.

