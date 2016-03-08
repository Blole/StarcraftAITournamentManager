#!/usr/bin/env python2
import yaml
import shutil
import itertools
import subprocess
import os
import re
import glob
from os.path import basename, splitext

def replace_all(repls, str):
    return re.sub('|'.join(re.escape(key) for key in repls.keys()),
                  lambda k: repls[k.group(0)], str)                                     

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

def mv(fr, to):
	subprocess.call('mv '+fr+' '+to, shell=True)
	if verbose:
		print 'moved '+basename(fr)

def cp(fr, to):
	subprocess.call('cp -r '+fr+' '+to, shell=True)
	if verbose:
		print 'copied '+basename(fr)

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

actionlisters = 	['branchonunit']
selecters =			['ucb']
evaluaters = 		['hp', 'sqrthp', 'sqrthp_dps', 'attack_closest']
backpropagaters =	['uct']
terminalcheckers =	['frame500']

contractions = {'branchonunit':'bu', 'attack_closest':'ac', 'frame':'fr'}

bots = []
for setting in itertools.product(actionlisters, selecters, evaluaters, backpropagaters, terminalcheckers):
	(actionlister, selecter, evaluater, backpropagater, terminalchecker) = setting
	botName = 'main-any-'+'-'.join(setting)
	botNameShort = replace_all(contractions, '-'.join(setting))
	botDir = 'build/server/data/bots/'+botName
	
	rm('-r '+botDir)
	cp('build/server/data/bots/main-any', botDir)
	mv(botDir+'/main-any.dll', botDir+'/'+botName+'.dll')
	
	with open(botDir+'/bot.yaml', 'r') as f:
		bot_yaml = yaml.load(f)
	
	bot_yaml['nick'] = botNameShort
	bot_yaml['environmentVariables'] = {
		'BLOLE_MCTS_actionlister': actionlister,
		'BLOLE_MCTS_selecter': selecter,
		'BLOLE_MCTS_evaluater': evaluater,
		'BLOLE_MCTS_backpropagater': backpropagater,
		'BLOLE_MCTS_terminalchecker': terminalchecker,
	}
	
	with open(botDir+'/bot.yaml', 'w') as f:
		yaml.dump(bot_yaml, f, default_flow_style=False)
	
	bots.append(botName)


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















