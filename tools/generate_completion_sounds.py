"""Generate original, short, non-verbal completion cues using only the standard library."""
import math
from pathlib import Path
import struct
import wave

SAMPLE_RATE = 44100
OUTPUT = Path(__file__).resolve().parents[1] / "app/src/main/res/raw"


def write_cue(name, pulses, duration):
    samples = []
    for index in range(round(duration * SAMPLE_RATE)):
        time = index / SAMPLE_RATE
        sample = 0.0
        for start, length, frequency, gain in pulses:
            elapsed = time - start
            if 0 <= elapsed < length:
                attack = min(1.0, elapsed / 0.008)
                release = min(1.0, (length - elapsed) / 0.035)
                envelope = attack * release * math.exp(-2.2 * elapsed / length)
                fundamental = math.sin(2 * math.pi * frequency * elapsed)
                overtone = 0.23 * math.sin(2 * math.pi * frequency * 2 * elapsed)
                sample += gain * envelope * (fundamental + overtone)
        samples.append(sample)
    peak = max(abs(value) for value in samples)
    samples = [round(value / peak * 0.89 * 32767) for value in samples]
    with wave.open(str(OUTPUT / f"completion_{name}.wav"), "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(SAMPLE_RATE)
        output.writeframes(struct.pack(f"<{len(samples)}h", *samples))
    rms = math.sqrt(sum((value / 32767) ** 2 for value in samples) / len(samples))
    print(f"{name}: {duration:.2f}s, peak=0.89, rms={rms:.3f}")


if __name__ == "__main__":
    OUTPUT.mkdir(parents=True, exist_ok=True)
    write_cue("clear_bell", [(0, 1.1, 1046.5, 1)], 1.15)
    write_cue("double_chime", [(0, .55, 880, 1), (.38, .65, 1174.66, 1)], 1.10)
    write_cue("triple_pulse", [(0, .24, 1318.51, 1), (.34, .24, 1318.51, 1), (.68, .30, 1318.51, 1)], 1.05)
    write_cue("soft_chime", [(0, .85, 659.25, 1), (.16, .9, 987.77, .6)], 1.15)
