"""Compatibility entry point: retired Bedrock model import.

The user selected the original smooth Pixelmon models. Re-running an old
handoff command must never overwrite those resources with the rejected cubes.
"""
from complete_za_variant_data import run

if __name__ == "__main__":
    run()
