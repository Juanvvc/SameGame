#!/usr/bin/python
# -*- coding: utf-8 -*-
#

import Image, ImageDraw

# Generate the name of the images
images = map(lambda f: '00%02d.png'%f, range(1, 17))
print images
# get the size of a tile
tile = Image.open(images[0])
tileSize = tile.size
print tileSize
# Calculate the cropbix: centered square
cropBox = ((tileSize[0]-tileSize[1])/2, 0, (tileSize[0]+tileSize[1])/2, tileSize[1])
# Generate the final image
im = Image.new('RGB', (len(images)*tileSize[1], tileSize[1]*8))

offset = 0

for i in images:
	tile = Image.open(i)
	for j in range(0, 8):
		im.paste(tile.crop(cropBox), (offset, j*tileSize[1]))
	offset = offset + tileSize[1]


im.save('espera.png', 'PNG')
	

