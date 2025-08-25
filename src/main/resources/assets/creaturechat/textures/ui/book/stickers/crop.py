#!/usr/bin/env python3
import argparse, os, sys
from PIL import Image

def iter_pngs(paths, recursive):
    for p in paths:
        if os.path.isdir(p):
            if recursive:
                for root, _, files in os.walk(p):
                    for name in files:
                        if name.lower().endswith(".png"):
                            yield os.path.join(root, name)
            else:
                for name in os.listdir(p):
                    full = os.path.join(p, name)
                    if os.path.isfile(full) and name.lower().endswith(".png"):
                        yield full
        else:
            if p.lower().endswith(".png"):
                yield p

def expand_box(bbox, w, h, pad):
    if not bbox or pad <= 0:
        return bbox
    l, t, r, b = bbox
    return (max(0, l - pad), max(0, t - pad), min(w, r + pad), min(h, b + pad))

def crop_png(path, out_path, threshold=0, pad=0, dry_run=False):
    try:
        with Image.open(path) as im:
            # Ensure alpha channel is present (handles P with tRNS too)
            if im.mode != "RGBA":
                im = im.convert("RGBA")
            w, h = im.size
            alpha = im.getchannel("A")
            # Build a binary mask of "visible" pixels by alpha threshold
            if threshold > 0:
                mask = alpha.point(lambda p: 255 if p > threshold else 0, mode="L")
            else:
                mask = alpha
            bbox = mask.getbbox()  # (left, upper, right, lower) or None

            if bbox is None:
                print(f"SKIP (fully transparent): {path}")
                return False

            bbox = expand_box(bbox, w, h, pad)

            if bbox == (0, 0, w, h):
                print(f"OK (already tight): {path}  [{w}x{h}]")
                # Still write if out_path differs (e.g., copying to out dir)
                if out_path and os.path.abspath(out_path) != os.path.abspath(path):
                    if not dry_run:
                        os.makedirs(os.path.dirname(out_path), exist_ok=True)
                        im.save(out_path, format="PNG", optimize=True)
                return False

            cropped = im.crop(bbox)
            cw, ch = cropped.size
            msg = f"{path}  [{w}x{h}] -> [{cw}x{ch}]"
            if dry_run:
                print("DRY RUN:", msg)
                return True

            os.makedirs(os.path.dirname(out_path), exist_ok=True)
            # Save atomically when overwriting
            tmp = out_path + ".tmp"
            cropped.save(tmp, format="PNG", optimize=True)
            os.replace(tmp, out_path)
            print("TRIM:", msg)
            return True
    except Exception as e:
        print(f"ERROR: {path}: {e}", file=sys.stderr)
        return False

def main():
    ap = argparse.ArgumentParser(
        description="Trim transparent margins from PNG files."
    )
    ap.add_argument("paths", nargs="+", help="Files or directories to process")
    ap.add_argument("-r", "--recursive", action="store_true",
                    help="Recurse into subdirectories")
    ap.add_argument("-o", "--out-dir", default=None,
                    help="Write results under this directory instead of overwriting files")
    ap.add_argument("-t", "--threshold", type=int, default=0,
                    help="Alpha threshold 0-255 (default 0: any nonzero alpha is kept)")
    ap.add_argument("-p", "--pad", type=int, default=0,
                    help="Extra padding (pixels) to keep around content")
    ap.add_argument("-n", "--dry-run", action="store_true",
                    help="Show what would change without writing files")
    args = ap.parse_args()

    # If a single directory is provided and an out-dir is used, preserve structure
    base_dir_for_rel = None
    if args.out_dir and len(args.paths) == 1 and os.path.isdir(args.paths[0]):
        base_dir_for_rel = os.path.abspath(args.paths[0])

    changed = 0
    total = 0
    for src in iter_pngs(args.paths, args.recursive):
        total += 1
        if args.out_dir:
            if base_dir_for_rel and os.path.abspath(src).startswith(base_dir_for_rel + os.sep):
                rel = os.path.relpath(src, start=base_dir_for_rel)
                out_path = os.path.join(args.out_dir, rel)
            else:
                out_path = os.path.join(args.out_dir, os.path.basename(src))
        else:
            out_path = src

        if crop_png(src, out_path, threshold=args.threshold, pad=args.pad, dry_run=args.dry_run):
            changed += 1

    print(f"\nDone. Examined {total} file(s). Trimmed {changed} file(s).")

if __name__ == "__main__":
    main()
