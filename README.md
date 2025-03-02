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

