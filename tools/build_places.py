"""Builds app/src/main/assets/places_ar.tsv, the list the app uses to name the phone's position
when the phone has no place naming service, as on Huawei phones without Google services.

The source is GeoNames (https://www.geonames.org, CC BY 4.0). For each Arab country, download
into one folder from https://download.geonames.org/export/dump/ :

    <CC>.zip                      every feature of the country
    alternatenames/<CC>.zip       saved as alt_<CC>.zip, the names in other languages
    admin1CodesASCII.txt          the regions

Then run:  python -I tools/build_places.py <that folder>

Only places people live in that have an Arabic name are kept. Each line of the result is

    latitude <tab> longitude <tab> name <tab> region <tab> country code <tab> flags

where flags holds "x" for a district of a town (GeoNames PPLX) and "t" for a town of five
thousand people or more.
"""
import io
import os
import sys
import zipfile

COUNTRIES = "SA AE KW QA BH OM YE IQ SY JO LB PS EG SD LY TN DZ MA MR SO DJ KM".split()
TOWN_POPULATION = 5000
OUTPUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets", "places_ar.tsv")


def rows(path, member):
    with zipfile.ZipFile(path) as archive:
        with archive.open(member) as raw:
            for line in io.TextIOWrapper(raw, encoding="utf-8"):
                yield line.rstrip("\n").split("\t")


def clean(name):
    """Some names carry invisible direction marks, which would show as a gap or break a comparison."""
    marks = [(0x200B, 0x200F), (0x202A, 0x202E), (0x2066, 0x2069)]
    return "".join(c for c in name if not any(low <= ord(c) <= high for low, high in marks)).strip()


def arabic_names(folder, country):
    """The Arabic name of each feature: the preferred one, else the first that is in use."""
    best = {}
    for row in rows(os.path.join(folder, "alt_%s.zip" % country), "%s.txt" % country):
        _, feature, language, name, preferred, _short, colloquial, historic = (row + [""] * 8)[:8]
        if language != "ar" or colloquial == "1" or historic == "1":
            continue
        if not any(0x0600 <= ord(c) <= 0x06FF for c in name):
            continue
        rank = 0 if preferred == "1" else 1
        if feature not in best or rank < best[feature][0]:
            best[feature] = (rank, clean(name))
    return {feature: name for feature, (_, name) in best.items()}


def main(folder):
    regions = {}
    with io.open(os.path.join(folder, "admin1CodesASCII.txt"), encoding="utf-8") as codes:
        for line in codes:
            code, _name, _ascii, feature = line.rstrip("\n").split("\t")
            regions[code] = feature
    lines = []
    for country in COUNTRIES:
        names = arabic_names(folder, country)
        kept = 0
        for row in rows(os.path.join(folder, "%s.zip" % country), "%s.txt" % country):
            feature, latitude, longitude, feature_class, feature_code = row[0], row[4], row[5], row[6], row[7]
            # PPLQ and PPLW are abandoned and destroyed places, PPLH a historical one.
            if feature_class != "P" or feature_code in ("PPLQ", "PPLW", "PPLH", "PPLCH") or feature not in names:
                continue
            region = names.get(regions.get("%s.%s" % (country, row[10]), ""), "")
            population = int(row[14] or 0)
            flags = ("x" if feature_code == "PPLX" else "") + ("t" if population >= TOWN_POPULATION else "")
            lines.append("%.4f\t%.4f\t%s\t%s\t%s\t%s" % (float(latitude), float(longitude), names[feature], region, country, flags))
            kept += 1
        print(country, kept)
    lines.sort()
    with io.open(OUTPUT, "w", encoding="utf-8", newline="\n") as output:
        output.write("\n".join(lines) + "\n")
    print("places", len(lines), "bytes", os.path.getsize(OUTPUT))


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main(sys.argv[1])
