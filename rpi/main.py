import os
import comm
import datetime
import time
import threading
import json
from Queue import Queue
import snoreclassifier as sc
import sleepstageclssifier as ssclassifier

alarmTime = 0 # 0 means disabled

# compare current time with alarm time
def isTimeToWakeUp():
	if (alarmTime == 0): # alarm is disabled
		return False
	currTime = calendar.timegm(time.gmtime())
	if (currTime >= alarmTime):
		return True
	else:
		return False

# send JSON indicating that it's time to wake up
def triggerAlarmClock():
		jsonStr = '{ "type":"alarmTime", "alarmTime":0}'
		comm.send(jsonStr)

# send JSON requesting to vibrate
def triggerVibration():
		jsonStr = '{ "type":"vibrate"}'
		comm.send(jsonStr)

# file names
dateTimeStr = datetime.datetime.today().strftime('%Y-%m-%d-%H:%M:%S')
ACC_FILE_NAME = dateTimeStr + "_acc.csv"
HR_FILE_NAME = dateTimeStr + "_hr.csv"

# stores acceleration data in csv format
def storeAccData(jsonObj):
	timestamp = jsonObj['timestamp']
	accX = jsonObj['accX']
	accY = jsonObj['accY']
	accZ = jsonObj['accZ']

	file = open(ACC_FILE_NAME, 'a+')
	for idx, val in enumerate(timestamp):
		file.write(str(timestamp[idx]) + ", " + str(accX[idx]) + ", " + str(accY[idx]) + ", " + str(accZ[idx]) + "\n")
	file.close

# stores heart rate data in csv format
def storeHRData(jsonObj):
	timestamp = jsonObj['timestamp']
	accuracy = jsonObj['accuracy']
	heartRate = jsonObj['heartRate']

	file = open(HR_FILE_NAME, 'a+')
	for idx, val in enumerate(timestamp):
		file.write(str(timestamp[idx]) + ", " + str(accuracy[idx]) + ", " + str(heartRate[idx]) + "\n")
	file.close

# queue for thread communication
q = Queue(16)

# start message reception thread
commThread = threading.Thread(target=comm.receive, args=(5555, q))
commThread.daemon = True
commThread.start()

snoreCheck = sc.SnoreClassifier()
snoreCheck.initialiseData()

# reception test
while 1:
	while q.empty():
		time.sleep(0.1)

	jsonObj = q.get()
	if type(jsonObj) is dict:
		print "-------------------------------------------"
		print jsonObj
	# execute appropiate action according to 'type'
	if 'type' in jsonObj:
		msgType = jsonObj['type']

	if (msgType == 'accData'):
		storeAccData(jsonObj)
		sleepStage = ssclassifier.process(jsonObj)
		if sleepStage != "deepSleep" and isTimeToWakeUp():
		  triggerAlarmClock()
	elif (msgType == 'hrData'):
		storeHRData(jsonObj)
	elif (msgType == 'audioFileFrame'):
		# convert audio file to WAV
		filename = (jsonObj['name'])[:-4]
		command = "mplayer -ao pcm:file=./" + filename + ".wav " + filename + ".m4a" # mplayer -ao pcm:file=./<file_name>.wav <file_name>.m4a
		os.system(command)
		print "command: " + command
		if(snoreCheck.isSnore(filename)):
		  triggerVibration()
		  print(" Snoring ")
		else:
		  print(" Not Snoring ")
	elif (msgType == 'alarmTime'):
		alarmTime = (jsonObj['alarmTime']/1000) - 30*60 # convert to seconds (epoch time) and subtract 30 min
	else:
		print "Warning: Unknown message 'type'!!!"

