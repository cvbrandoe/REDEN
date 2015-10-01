import codecs
import re
import sys
#import collections
#from lxml import etree
#from lxml.etree import tostring, strip_tags
import os
from os import listdir
import commands

if len(sys.argv) > 1:
	# Usage: python main.py input
	input_file = sys.argv[1]
else:
	print "Pass Geonames dump file: allCountries.txt"

print "##############################"
print "INPUT FILE WAS " + input_file
print 

f = open(input_file, "r")
target = open("geonamesDico.txt", "w");
for line in f:
	#main place name
	fields = re.split(r'\t+', line);
	target.write(fields[1] + "\t" +fields[1]+ "\thttp://sws.geonames.org/"+fields[0]); 
	target.write("\n");

	#alternative place names
	if fields[3] != fields[1] and not fields[3].replace(".", "", 1).isdigit(): #avoid name duplicates
		names = re.split(r',', fields[3]);
		for name in names:		
			target.write(name + "\t" + fields[1]+"\thttp://sws.geonames.org/"+fields[0]); 
			target.write("\n");
	#else:
	#	print("ARE DUPLICATES "+fields[3] +" AND " + fields[1]);
			
target.close();
f.close();
print "the end";
 
		




