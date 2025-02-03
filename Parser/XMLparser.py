import xml.sax
import multiprocessing as mp
from pathlib import Path
import queue
import threading
import mmap
import io
import os
from typing import Dict, List, Generator
import time
from tqdm import tqdm
import math

class WikiXMLHandler(xml.sax.ContentHandler):
    def __init__(self, chunk_queue: mp.Queue, pbar: tqdm = None):
        super().__init__()
        self.current_tag = ""
        self.title = ""
        self.text = ""
        self.redirect = ""
        self.in_revision = False
        self.in_page = False
        self.buffer = []
        self.chunk_queue = chunk_queue
        self.current_chunk = []
        self.chunk_size = 1000  # Collect this many pages before sending to queue
        self.page_count = 0
        self.pbar = pbar

    def startElement(self, tag, attributes):
        self.current_tag = tag
        if tag == "page":
            self.in_page = True
            self.title = ""
            self.text = ""
            self.redirect = ""
        elif tag == "redirect":
            self.redirect = attributes.get("title", "")
        elif tag == "revision":
            self.in_revision = True
            self.text = ""

    def endElement(self, tag):
        if tag == "page":
            self.in_page = False
            page_data = {
                "title": self.title.strip(),
                "redirect": self.redirect,
                "text": self.text.strip()
            }
            self.current_chunk.append(page_data)
            self.page_count += 1

            if len(self.current_chunk) >= self.chunk_size:
                self.chunk_queue.put(self.current_chunk)
                self.current_chunk = []

            if self.pbar:
                self.pbar.update(1)

        elif tag == "revision":
            self.in_revision = False

    def characters(self, content):
        if self.in_page:
            if self.current_tag == "title":
                self.title += content
            elif self.current_tag == "text" and self.in_revision:
                self.text += content

    def endDocument(self):
        if self.current_chunk:  # Send any remaining pages
            self.chunk_queue.put(self.current_chunk)

def get_total_pages(file_path: str) -> int:
    """Count total number of pages in the XML file."""
    total = 0
    with open(file_path, 'rb') as f:
        for line in f:
            if b'<page>' in line:
                total += 1
    return total

def chunk_file(file_path: str, chunk_size: int = 1024*1024*100) -> Generator[bytes, None, None]:
    """Generate chunks of the file using memory mapping."""
    with open(file_path, 'rb') as f:
        with mmap.mmap(f.fileno(), 0, access=mmap.ACCESS_READ) as mm:
            file_size = len(mm)
            for i in range(0, file_size, chunk_size):
                chunk = mm[i:min(i + chunk_size, file_size)]
                # Ensure we don't split in the middle of a page
                if i + chunk_size < file_size:
                    last_page_end = chunk.rfind(b'</page>')
                    if last_page_end != -1:
                        yield chunk[:last_page_end] + b'</page>'
                        mm.seek(i + last_page_end + 7)  # Skip past </page>
                else:
                    yield chunk

class ChunkWriter:
    def __init__(self, output_dir: str, process_id: int, pbar: tqdm = None):
        self.output_file = Path(output_dir) / f"output_{process_id}.xml"
        self.buffer = []
        self.buffer_size = 5000  # Pages per buffer
        self.file_handle = None
        self.pbar = pbar
        self.initialize_file()

    def initialize_file(self):
        with open(self.output_file, 'w', encoding='utf-8') as f:
            f.write('<?xml version="1.0" encoding="UTF-8"?>\n<wikis>\n')

    def write_chunk(self, pages: List[Dict]):
        output = []
        for page in pages:
            output.extend([
                '  <page>\n',
                f'    <title>{xml.sax.saxutils.escape(page["title"])}</title>\n',
                f'    <redirect>{xml.sax.saxutils.escape(page["redirect"])}</redirect>\n',
                f'    <text>{xml.sax.saxutils.escape(page["text"])}</text>\n',
                '  </page>\n'
            ])

        with open(self.output_file, 'a', encoding='utf-8') as f:
            f.write(''.join(output))

        if self.pbar:
            self.pbar.update(len(pages))

    def finalize(self):
        with open(self.output_file, 'a', encoding='utf-8') as f:
            f.write('</wikis>')

def process_chunks(chunk_queue: mp.Queue, output_dir: str, process_id: int, total_pages: int):
    """Process chunks of pages from the queue."""
    with tqdm(
            total=total_pages,
            desc=f"Writer-{process_id}",
            position=process_id + 1,
            leave=True
    ) as pbar:
        writer = ChunkWriter(output_dir, process_id, pbar)

        while True:
            try:
                chunk = chunk_queue.get(timeout=60)
                writer.write_chunk(chunk)
            except queue.Empty:
                break

        writer.finalize()

def parse_wiki_chunk(chunk: bytes, chunk_queue: mp.Queue, pbar: tqdm = None):
    """Parse a chunk of XML data."""
    handler = WikiXMLHandler(chunk_queue, pbar)
    parser = xml.sax.make_parser()
    parser.setContentHandler(handler)

    # Wrap the chunk in a file-like object
    chunk_file = io.BytesIO(chunk)
    parser.parse(chunk_file)

def process_wiki_dump(input_file: str, output_dir: str, num_processes: int = None):
    """Process Wikipedia XML dump using optimized parsing and processing."""
    if num_processes is None:
        num_processes = mp.cpu_count()

    Path(output_dir).mkdir(parents=True, exist_ok=True)

    # Count total pages first
    print("Counting total pages...")
    total_pages = get_total_pages(input_file)
    pages_per_process = math.ceil(total_pages / num_processes)

    # Create queues
    chunk_queue = mp.Queue(maxsize=num_processes * 2)

    # Start writer processes
    writers = []
    for i in range(num_processes):
        p = mp.Process(
            target=process_chunks,
            args=(chunk_queue, output_dir, i, pages_per_process)
        )
        p.start()
        writers.append(p)

    # Create main progress bar
    with tqdm(total=total_pages, desc="Total Progress", position=0) as main_pbar:
        # Create parser pool
        with mp.Pool(processes=num_processes) as pool:
            # Process file in chunks
            chunk_size = 1024 * 1024 * 100  # 100MB chunks
            chunks = chunk_file(input_file, chunk_size)

            # Process chunks in parallel
            pool.starmap(
                parse_wiki_chunk,
                [(chunk, chunk_queue, main_pbar) for chunk in chunks]
            )

    # Wait for all processes to complete
    for p in writers:
        p.join()

    print("\nProcessing complete!")

if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Process Wikipedia XML dump using optimized SAX parser")
    parser.add_argument("input_file", help="Path to input XML file")
    parser.add_argument("output_dir", help="Directory for output XML files")
    parser.add_argument(
        "--processes",
        type=int,
        help="Number of processes to use (default: number of CPU cores)"
    )

    args = parser.parse_args()

    process_wiki_dump(args.input_file, args.output_dir, args.processes)
