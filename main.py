#!/usr/bin/python

import markdown
import re
import os
import stat
import json

def parseMarkdownFile(path):
    f = open(path, 'r', encoding='utf-8')
    html = markdown.markdown(f.read(), extensions=['markdown.extensions.tables'])

    pattern = r'<tr>\n<td>([^<]+)</td>\n<td>([^<]+)</td>\n<td>.*?</td>\n<td>.*?</td>\n</tr>'
    result = [(x.group(1), x.group(2)) for x in re.finditer(pattern, html, re.MULTILINE)]

    # Check result
    bad = [x[0] for x in result if not re.fullmatch(r'[a-z0-9.\-\u0020]+', x[0])]
    if bad:
        raise ValueError('Bad tags', str(bad))

    return result

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

    files = ('artist.md', 'character.md', 'female.md', 'group.md', 'language.md', 'male.md', 'misc.md', 'parody.md')
    tags = [x for f in files for x in parseMarkdownFile(os.path.join('EhTagTranslator.wiki', 'database', f))]

    result = {'type': 'tags', 'lang': 'zh-rCN', 'content': tags}
    with open('json/tags-zh-rCN.json', 'w', encoding='utf-8') as f:
        f.write(json.dumps(result))

    removeMarkdownFiles()
