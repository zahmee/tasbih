"""Generate the original, short, non-verbal prayer alert tones using only the standard library.

They are made here, so they carry no third-party rights. Each lasts about three seconds: long
enough to be noticed, short enough for a notification channel to play it in full.
"""
import math
from pathlib import Path
import struct
import wave

SAMPLE_RATE = 22050
OUTPUT = Path(__file__).resolve().parents[1] / "app/src/main/res/raw"


def write_tone(name, notes, duration):
    """notes: (start, length, frequency, gain); a soft attack and a long, bell-like decay."""
    samples = []
    for index in range(round(duration * SAMPLE_RATE)):
        time = index / SAMPLE_RATE
        sample = 0.0
        for start, length, frequency, gain in notes:
            elapsed = time - start
            if 0 <= elapsed < length:
                attack = min(1.0, elapsed / 0.02)
                release = min(1.0, (length - elapsed) / 0.25)
                envelope = attack * release * math.exp(-1.6 * elapsed / length)
                tone = math.sin(2 * math.pi * frequency * elapsed)
                tone += 0.30 * math.sin(2 * math.pi * frequency * 2 * elapsed)
                tone += 0.12 * math.sin(2 * math.pi * frequency * 3.01 * elapsed)
                sample += gain * envelope * tone
        samples.append(sample)
    peak = max(abs(value) for value in samples)
    samples = [round(value / peak * 0.85 * 32767) for value in samples]
    with wave.open(str(OUTPUT / f"prayer_tone_{name}.wav"), "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(SAMPLE_RATE)
        output.writeframes(struct.pack(f"<{len(samples)}h", *samples))
    print(f"{name}: {duration:.2f}s")


if __name__ == "__main__":
    OUTPUT.mkdir(parents=True, exist_ok=True)
    # Three rising notes of a calm major chord.
    write_tone("chime", [(0, 1.6, 523.25, 1), (0.45, 1.6, 659.25, 0.9), (0.9, 2.1, 783.99, 0.85)], 3.1)
    # One low bell, struck twice.
    write_tone("bell", [(0, 1.5, 392.0, 1), (1.3, 1.8, 392.0, 0.8)], 3.2)
    # A soft falling pair, for those who want the quietest cue.
    write_tone("soft", [(0, 1.4, 587.33, 0.8), (0.6, 2.0, 440.0, 0.7)], 2.8)
