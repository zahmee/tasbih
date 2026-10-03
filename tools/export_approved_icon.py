"""Convert the approved artwork's silhouette to native Android vector resources.

The approved store PNG is copied unchanged. Pillow is only used to read the
source artwork; the Android and SVG outputs contain paths rather than a newly
generated raster image.
"""

from collections import defaultdict
from hashlib import sha256
from pathlib import Path
import json
import math
import shutil

import numpy as np
from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs/icon-concepts-2026-10-03/01-alif-noon-tasbih-v2.png"
STORE_SOURCE = ROOT / "store-assets/icon-concepts-2026-10-03/01-alif-noon-tasbih-v2-512.png"
EVIDENCE = ROOT / "docs/approved-icon-2026-10-03"
RES = ROOT / "app/src/main/res"
SCALE = 0.885


def simplify(points, tolerance=0.9):
    if len(points) < 3:
        return points
    start, end = np.asarray(points[0]), np.asarray(points[-1])
    line = end - start
    distances = (
        np.abs(line[0] * (start[1] - np.asarray(points)[:, 1])
               - line[1] * (start[0] - np.asarray(points)[:, 0]))
        / np.linalg.norm(line)
        if np.any(line) else np.linalg.norm(np.asarray(points) - start, axis=1)
    )
    split = int(np.argmax(distances))
    if distances[split] <= tolerance:
        return [points[0], points[-1]]
    return simplify(points[:split + 1], tolerance)[:-1] + simplify(points[split:], tolerance)


def contours(mask):
    edges = defaultdict(list)
    padded = np.pad(mask, 1)
    neighbors = (
        (~padded[:-2, 1:-1], lambda x, y: ((x, y), (x + 1, y))),
        (~padded[1:-1, 2:], lambda x, y: ((x + 1, y), (x + 1, y + 1))),
        (~padded[2:, 1:-1], lambda x, y: ((x + 1, y + 1), (x, y + 1))),
        (~padded[1:-1, :-2], lambda x, y: ((x, y + 1), (x, y))),
    )
    for missing_neighbor, edge in neighbors:
        ys, xs = np.nonzero(mask & missing_neighbor)
        for x, y in zip(xs.tolist(), ys.tolist()):
            start, end = edge(x, y)
            edges[start].append(end)
    result = []
    while edges:
        start = next(iter(edges))
        current = start
        points = [start]
        while True:
            following = edges[current].pop()
            if not edges[current]:
                del edges[current]
            points.append(following)
            current = following
            if current == start:
                break
        area = abs(sum(a[0] * b[1] - b[0] * a[1] for a, b in zip(points, points[1:]))) / 2
        if area >= 80:
            # Split the closed contour to keep the simplification well defined.
            halfway = len(points) // 2
            result.append(simplify(points[:halfway + 1])[:-1] + simplify(points[halfway:])[:-1])
    return result


def path_data(points):
    return "M" + " L".join(f"{x},{y}" for x, y in points) + " Z"


def color_median(pixels):
    return "#" + "".join(f"{int(value):02X}" for value in np.median(pixels, axis=0))


def main():
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    previous = EVIDENCE / "previous"
    previous.mkdir(exist_ok=True)
    for current in [RES / "drawable/ic_launcher_foreground.xml",
                    RES / "drawable/ic_launcher_monochrome.xml",
                    RES / "values/launcher_colors.xml",
                    ROOT / "store-assets/app-icon.png", ROOT / "store-assets/app-icon.svg"]:
        backup = previous / current.name
        if not backup.exists():
            shutil.copy2(current, backup)

    artwork = np.asarray(Image.open(SOURCE).convert("RGB"), dtype=np.int16)
    height, width, _ = artwork.shape
    saturated = artwork.max(axis=2) - artwork.min(axis=2) > 50
    green = saturated & (artwork[:, :, 1] > artwork[:, :, 0])
    copper = saturated & (artwork[:, :, 0] > artwork[:, :, 1])
    background = color_median(artwork[~saturated])
    groups = [(color_median(artwork[green]), contours(green)),
              (color_median(artwork[copper]), contours(copper))]
    vector_scale = 108 / width * SCALE
    translate = 54 * (1 - SCALE)
    for monochrome, name in [(False, "ic_launcher_foreground"), (True, "ic_launcher_monochrome")]:
        paths = "\n".join(
            f'        <path android:fillColor="{"#000000" if monochrome else color}" android:pathData="{path_data(points)}" />'
            for color, shapes in groups for points in shapes
        )
        xml = (
            '<?xml version="1.0" encoding="utf-8"?>\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp" android:height="108dp"\n'
            '    android:viewportWidth="108" android:viewportHeight="108">\n'
            f'    <group android:scaleX="{vector_scale:.9f}" android:scaleY="{vector_scale:.9f}"\n'
            f'        android:translateX="{translate:.2f}" android:translateY="{translate:.2f}">\n'
            f'{paths}\n    </group>\n</vector>\n'
        )
        (RES / f"drawable/{name}.xml").write_text(xml, encoding="utf-8")
    (RES / "values/launcher_colors.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
        f'    <color name="launcher_background">{background}</color>\n</resources>\n', encoding="utf-8"
    )
    svg_paths = "\n".join(f'<path fill="{color}" d="{path_data(points)}"/>'
                          for color, shapes in groups for points in shapes)
    (ROOT / "store-assets/app-icon.svg").write_text(
        f'<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 {width} {height}">\n'
        f'<rect width="{width}" height="{height}" fill="{background}"/>\n{svg_paths}\n</svg>\n',
        encoding="utf-8"
    )
    shutil.copy2(STORE_SOURCE, ROOT / "store-assets/app-icon.png")
    all_points = [point for _, shapes in groups for points in shapes for point in points]
    radius = max(math.hypot(x - width / 2, y - height / 2) for x, y in all_points) * vector_scale
    store_image = Image.open(STORE_SOURCE).convert("RGBA")
    verification = {
        "approved_source": str(SOURCE.relative_to(ROOT)),
        "store_png_sha256": sha256(STORE_SOURCE.read_bytes()).hexdigest(),
        "canonical_png_matches_approved_export": (ROOT / "store-assets/app-icon.png").read_bytes() == STORE_SOURCE.read_bytes(),
        "store_dimensions": list(store_image.size),
        "store_alpha_extrema": list(store_image.getchannel("A").getextrema()),
        "vector_colors": {"green": groups[0][0], "copper": groups[1][0], "background": background},
        "contour_counts": [len(shapes) for _, shapes in groups],
        "path_vertices": len(all_points),
        "adaptive_scale": SCALE,
        "maximum_logo_radius_dp": radius,
        "inside_66dp_safe_circle": radius <= 33,
        "adaptive_design_reference": "https://developer.android.com/develop/ui/compose/system/icon_design_adaptive",
    }
    assert store_image.size == (512, 512) and store_image.getchannel("A").getextrema() == (255, 255)
    # Two pairs of the lower beads touch in the approved raster artwork.
    assert len(groups[0][1]) == 2 and len(groups[1][1]) == 7
    assert radius <= 33
    (EVIDENCE / "asset-verification.json").write_text(json.dumps(verification, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(verification, indent=2))


if __name__ == "__main__":
    main()
