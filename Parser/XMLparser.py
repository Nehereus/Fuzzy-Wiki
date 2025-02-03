import xml.sax
import multiprocessing as mp
from pathlib import Path
import queue
import threading
from typing import Dict, List
import time

class WikiXMLHandler(xml.sax.ContentHandler):
    def __init__(self, queue_size=1000):
        super().__init__()
        self.current_tag = ""
        self.title = ""
        self.text = ""
        self.redirect = ""
        self.in_revision = False
        self.in_page = False
        self.buffer = []
        self.pages_queue = mp.Queue(maxsize=queue_size)
        self.page_count = 0

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
            self.pages_queue.put(page_data)
            self.page_count += 1
            if self.page_count % 10000 == 0:
                print(f"Processed {self.page_count} pages...")
        elif tag == "revision":
            self.in_revision = False

    def characters(self, content):
        if self.in_page:
            if self.current_tag == "title":
                self.title += content
            elif self.current_tag == "text" and self.in_revision:
                self.text += content

def write_pages_to_file(pages: List[Dict], process_id: int, output_dir: str):
    """Write a batch of pages to an XML file."""
    output_file = Path(output_dir) / f"output_{process_id}.xml"

    with open(output_file, 'w', encoding='utf-8') as f:
        f.write('<?xml version="1.0" encoding="UTF-8"?>\n')
        f.write('<wikis>\n')
        for page in pages:
            f.write('  <page>\n')
            f.write(f'    <title>{xml.sax.saxutils.escape(page["title"])}</title>\n')
            f.write(f'    <redirect>{xml.sax.saxutils.escape(page["redirect"])}</redirect>\n')
            f.write(f'    <text>{xml.sax.saxutils.escape(page["text"])}</text>\n')
            f.write('  </page>\n')
        f.write('</wikis>')

def process_queue(pages_queue: mp.Queue, output_dir: str, process_id: int, batch_size: int = 1000):
    """Process pages from the queue and write them to files."""
    pages_batch = []

    while True:
        try:
            page = pages_queue.get(timeout=60)  # 1 minute timeout
            pages_batch.append(page)

            if len(pages_batch) >= batch_size:
                write_pages_to_file(pages_batch, process_id, output_dir)
                pages_batch = []

        except queue.Empty:
            if pages_batch:  # Write remaining pages
                write_pages_to_file(pages_batch, process_id, output_dir)
            break

def process_wiki_dump(input_file: str, output_dir: str, num_processes: int = None):
    """Process Wikipedia XML dump using SAX parser and multiprocessing."""
    if num_processes is None:
        num_processes = mp.cpu_count()

    # Create output directory if it doesn't exist
    Path(output_dir).mkdir(parents=True, exist_ok=True)

    # Create and start parser
    handler = WikiXMLHandler()
    parser = xml.sax.make_parser()
    parser.setContentHandler(handler)

    # Start worker processes
    processes = []
    for i in range(num_processes):
        p = mp.Process(
            target=process_queue,
            args=(handler.pages_queue, output_dir, i)
        )
        p.start()
        processes.append(p)

    # Parse the XML file
    print(f"Starting to parse {input_file}...")
    parser.parse(input_file)

    # Wait for all processes to complete
    for p in processes:
        p.join()

    print("Processing complete!")

if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Process Wikipedia XML dump using SAX parser")
    parser.add_argument("input_file", help="Path to input XML file")
    parser.add_argument("output_dir", help="Directory for output XML files")
    parser.add_argument(
        "--processes",
        type=int,
        help="Number of processes to use (default: number of CPU cores)"
    )

    args = parser.parse_args()

    process_wiki_dump(args.input_file, args.output_dir, args.processes)
