import express from 'express'
import cors from 'cors'
import axios, { AxiosError } from 'axios'
import cookieParser from 'cookie-parser'
import 'dotenv/config'

const app = express()
const PORT: number | string = process.env.PORT || 3000

app.use(cors({ 
  origin: true, 
  credentials: true 
})) // enable frontend access with credentials
app.use(cookieParser()) // parse cookies

const luceneNodes: string[] = [
  `${process.env.LUCENE_ENDPOINT_1}`,
  `${process.env.LUCENE_ENDPOINT_2}`,
  `${process.env.LUCENE_ENDPOINT_3}`,
  `${process.env.LUCENE_ENDPOINT_4}`
]

/*** Forward request to lucene compute nodes ***/
let currentLuceneNodeIndex = 0

const selectNode = (req: express.Request) => {
  // Check if there's a cookie with a node index
  const cookieNodeIndex = req.cookies.nodeIndex
  
  // If cookie exists and is valid, use that node
  if (cookieNodeIndex !== undefined && 
      Number.isInteger(Number(cookieNodeIndex)) && 
      Number(cookieNodeIndex) >= 0 && 
      Number(cookieNodeIndex) < luceneNodes.length) {
    const selectedLuceneNode = luceneNodes[Number(cookieNodeIndex)]
    console.log(`Using cookie node: ${selectedLuceneNode} (index: ${cookieNodeIndex})`)
    return { node: selectedLuceneNode, index: Number(cookieNodeIndex) }
  }

  // Otherwise, use round-robin
  const selectedLuceneNode = luceneNodes[currentLuceneNodeIndex]
  const selectedIndex = currentLuceneNodeIndex
  currentLuceneNodeIndex = ++currentLuceneNodeIndex % luceneNodes.length
  console.log(`Selected node: ${selectedLuceneNode} (index: ${selectedIndex})`)
  return { node: selectedLuceneNode, index: selectedIndex }
}

app.get('/search', async (req, res) => {
  try {
    // Select node and get both node and index
    const { node: selectedLuceneNode, index: nodeIndex } = selectNode(req)
    
    // Set cookie with the node index (expires in 1 hour)
    res.cookie('nodeIndex', nodeIndex.toString(), { 
      maxAge: 3600000, // 1 hour
      httpOnly: true,
      sameSite: 'strict'
    })

    const response = await axios.get(`${selectedLuceneNode}/search`, {
      params: {
        query: req.query.query,
        forwarding: true
      }
    })
    res.send(response.data)
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    console.error("Error fetching search results:", errorMessage)
    res.status(500).json({ error: "Internal Server Error" })
  }
})

app.get('/article/:title', async (req, res) => {
  try {
    // Use the same node selection logic (will use cookie if available)
    const { node: selectedLuceneNode } = selectNode(req)
    
    const response = await axios.get(
      `${selectedLuceneNode}/document/${req.params.title}`
    )
    res.send(response.data)
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    console.error("Error fetching article:", errorMessage)
    res.status(500).json({ error: "Internal Server Error" })
  }
})

app.listen(PORT, () => {
  console.log(`Listening on Port ${PORT}`)
})