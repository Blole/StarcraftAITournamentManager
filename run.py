#!/usr/bin/env python2
import yaml
import shutil
import itertools
import subprocess
import os
import glob
from os.path import basename, splitext

class Game:
	def __init__(self, round, map, type, bots):
		self.round = round
		self.map = map
		self.type = type
		self.bots = list(bots)
	def write(self, path):
		with open(path, 'w') as f:
			f.write(yaml.dump({'round': self.round},	default_flow_style=False))
			f.write(yaml.dump({'map': self.map},		default_flow_style=False))
			f.write(yaml.dump({'type': self.type},		default_flow_style=False))
			f.write(yaml.dump({'bots': self.bots},		default_flow_style=False))

bots = [
	'main-debug',
]

maps = [
	'1m.scx',
]

verbose = True;

rounds = 1
id = 0

def cp(fr, to):
	shutil.copy2(fr, to)
	if verbose:
		print('copied '+basename(fr))

def rm(f):
	subprocess.call('rm '+('-v ' if verbose else '')+f, shell=True)


cp('C:/tm/bwapi/bwapi/build/Release/bin/BWAPI.dll', 'build/server/data/bwapiversions/4.1/')
cp('C:/tm/bwapi/bwapi/build/Release/bin/BWAPI.pdb', 'build/server/data/bwapiversions/4.1/')
cp('C:/tm/bwapi/bwapi/build/Release/bin/SNP_DirectIP.snp', 'build/server/data/starcraft/')
cp('C:/tm/bwapi/bwapi/build/Release/bin/SNP_DirectIP.pdb', 'build/server/data/starcraft/')
cp('tournamentmodule/build/Release/bin/tournamentmodule-4.1.dll', 'build/server/data/bwapiversions/4.1/tournamentmodule.dll')

for dll in glob.iglob('C:/Dropbox/kth/ex/starcraft-micro/build/Release/bin/main-*.dll'):
	name = splitext(basename(dll))[0]
	cp(dll, 'build/server/data/bots/'+name+'/')

rm('build/server/game_queue/*.yaml')
rm('build/server/game_results/* -r')

for round in range(rounds):
	for bot in itertools.permutations(bots,1):
		for map in maps:
			Game(round, map, "USE_MAP_SETTINGS", bot).write('build/server/game_queue/'+str(id)+'.yaml')
			id+=1

if verbose:
	subprocess.call('cat build/server/game_queue/*', shell=True)

server = subprocess.Popen('java -jar server-1.1-SNAPSHOT-run.jar server.yaml'.split(), cwd='build/server')
client = subprocess.Popen('java -jar client-1.1-SNAPSHOT-run.jar client.yaml'.split(), cwd='build/client')


client.wait()
server.wait()















