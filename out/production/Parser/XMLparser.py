import mwxml
import json
import argparse

def process_page(dump, page):
    for page in dump:
        try:
            first_rev = next(iter(page))  # Access the first revision
            yield {"id": first_rev.id, "title": first_rev.page.title, "text": first_rev.slots.contents['main'].text} # Yield a dictionary
        except StopIteration:
            print(f"Warning: Page {page.title} has no revisions.") # Handle empty pages

def main():
    parser = argparse.ArgumentParser(description="Process MediaWiki XML dump and write to JSONL file.")
    parser.add_argument("input_file", type=str, help="Path to the input XML dump file.")
    parser.add_argument("output_file", type=str, help="Path to the output JSONL file.")
    parser.add_argument("-n", "--num_processes", type=int, default=1, help="Number of processes to use.")

    args = parser.parse_args()

    with open(args.output_file, "w") as outfile:  # Open in write mode "w" to avoid appending to previous content. If you want to append, use "a".
        for item in mwxml.map(process_page, [args.input_file], args.num_processes):
            json_line = json.dumps(item, ensure_ascii=False) + "\n"  # Ensure UTF-8 encoding
            outfile.write(json_line)

    print(f"Successfully processed {args.input_file} and wrote to {args.output_file}")


if __name__ == "__main__":
    main()
