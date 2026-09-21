"""Report ELF LOAD alignment of the packaged native libraries (no third-party modules)."""
import struct
import sys
import zipfile

with zipfile.ZipFile(sys.argv[1]) as apk:
    for name in apk.namelist():
        if not name.endswith('.so'):
            continue
        data = apk.read(name)
        is64 = data[4] == 2
        endian = '<' if data[5] == 1 else '>'
        offset = struct.unpack_from(endian + ('Q' if is64 else 'I'), data, 32 if is64 else 28)[0]
        size, count = struct.unpack_from(endian + 'HH', data, 54 if is64 else 42)
        alignments = []
        for i in range(count):
            header = offset + i * size
            if struct.unpack_from(endian + 'I', data, header)[0] == 1:
                alignments.append(struct.unpack_from(endian + ('Q' if is64 else 'I'), data, header + (48 if is64 else 28))[0])
        print(name, 'LOAD alignment:', alignments)
        if is64 and min(alignments) < 16384:
            raise SystemExit('64-bit library does not support 16 KB pages')
