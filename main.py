#!/usr/bin/python

import markdown
import re
import os
import stat
import json
import collections
import struct
import base64
import hashlib
from bs4 import BeautifulSoup

def removeEmojis(x):
    return ''.join(c for c in x if c <= '\uFFFF')

def parseMarkdownFile(path, prefix):
    f = open(path, 'r', encoding='utf-8')
    html = markdown.markdown(f.read(), extensions=['markdown.extensions.tables'])

    result = []
    soup = BeautifulSoup(html, 'html.parser')
    for tr in soup.find_all('tr'):
        tds = [x for x in tr.find_all('td')]
        if len(tds) == 4:
            x = ''.join(tds[0].strings).strip()
            y = ''.join(tds[1].strings).strip()
            y = removeEmojis(y)
            if len(x) != 0 and len(y) != 0:
                result.append((x, y))

    # Check result
    bad = [x[0] for x in result if not re.fullmatch(r'[a-z0-9.\-\u0020]+', x[0])]
    if bad:
        raise ValueError('Bad tags', str(bad))

    # Add prefix
    if prefix:
        p = prefix + ':'
        result = [(p + x[0], x[1]) for x in result]

    return result

def sha1(path):
    sha1 = hashlib.sha1()
    with open(path, 'rb') as f:
        while True:
            data = f.read(64 * 1024)
            if not data:
                break
            sha1.update(data)
    return sha1.digest()

def saveTags(path, tags):
    tags = sorted(tags)
    with open(path, 'wb') as f:
        # write size placeholder
        f.write(struct.pack('>i', 0))
        # write tags
        for x, y in tags:
            f.write(x.encode())
            f.write(struct.pack('b', ord('\r')))
            f.write(base64.b64encode(y.encode()))
            f.write(struct.pack('b', ord('\n')))
        # get file size
        f.seek(0, 2)
        size = f.tell()
        # write tags size
        f.seek(0, 0)
        f.write(struct.pack('>i', size - 4))

    # Save sha1
    with open(path + ".sha1", 'wb') as f:
        f.write(sha1(path))

def downloadMarkdownFiles():
    if os.system('git clone https://github.com/Mapaler/EhTagTranslator.wiki.git --depth=1'):
        raise ValueError('Failed to git clone')

def rmtree(path):
    for root, dirs, files in os.walk(path, topdown=False):
        for name in files:
            f = os.path.join(root, name)
            os.chmod(f, stat.S_IWUSR)
            os.remove(f)
        for name in dirs:
            os.rmdir(os.path.join(root, name))
    os.rmdir(path)

def removeMarkdownFiles():
    rmtree('EhTagTranslator.wiki')

if __name__ == "__main__":
    if os.path.exists('EhTagTranslator.wiki'):
        removeMarkdownFiles()
    downloadMarkdownFiles()

    files = (
        ('rows.md', 'n'),
        ('artist.md', 'a'),
        ('character.md', 'c'),
        ('female.md', 'f'),
        ('group.md', 'g'),
        ('language.md', 'l'),
        ('male.md', 'm'),
        ('misc.md', None),
        ('parody.md', 'p'),
        ('reclass.md', 'r')
    )
    tags = [x for f, p in files for x in parseMarkdownFile(os.path.join('EhTagTranslator.wiki', 'database', f), p)]
    saveTags('tag-translations/tag-translations-zh-rCN', tags)

    removeMarkdownFiles()
