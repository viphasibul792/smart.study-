import sys, zipfile, struct, zlib

def align(src, dst, alignment=4):
    zin = zipfile.ZipFile(src)
    out = open(dst, 'wb')
    infos = zin.infolist()
    recs = []
    for zi in infos:
        data = zin.read(zi.filename)
        name = zi.filename.encode('utf-8')
        stored = zi.compress_type == zipfile.ZIP_STORED
        if stored:
            payload = data
        else:
            co = zlib.compressobj(9, zlib.DEFLATED, -15)
            payload = co.compress(data) + co.flush()
        csize = len(payload)
        extra = b''
        if stored:
            a = 4096 if zi.filename.endswith('.so') else alignment
            pos = out.tell() + 30 + len(name)
            extra = b'\x00' * ((a - (pos % a)) % a)
        off = out.tell()
        out.write(struct.pack('<IHHHHHIIIHH', 0x04034b50, 20, zi.flag_bits & ~0x08,
                              zi.compress_type, 0, 0, zi.CRC, csize, len(data),
                              len(name), len(extra)))
        out.write(name); out.write(extra); out.write(payload)
        recs.append((zi, off, csize, len(data), name))
    cd = out.tell()
    for zi, off, csize, usize, name in recs:
        out.write(struct.pack('<IHHHHHHIIIHHHHHII', 0x02014b50, 20, 20,
                              zi.flag_bits & ~0x08, zi.compress_type, 0, 0, zi.CRC,
                              csize, usize, len(name), 0, 0, 0, 0,
                              zi.external_attr, off))
        out.write(name)
    end = out.tell()
    out.write(struct.pack('<IHHHHIIH', 0x06054b50, 0, 0, len(recs), len(recs),
                          end - cd, cd, 0))
    out.close(); zin.close()

align(sys.argv[1], sys.argv[2])
