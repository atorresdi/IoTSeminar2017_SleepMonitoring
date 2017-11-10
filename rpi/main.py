import os
import comm
import datetime
import time
import threading
import json
from Queue import Queue

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
comm_thread = threading.Thread(target=comm.receive, args=(5555, q))
comm_thread.daemon = True
comm_thread.start()

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
      elif (msgType == 'hrData'):
        storeHRData(jsonObj)
      elif (msgType == 'audioFileFrame'):
	# convert audio file to WAV
	filename = (jsonObj['name'])[:-4]
        command = "mplayer -ao pcm:file=./" + filename + ".wav " + filename + ".m4a" # mplayer -ao pcm:file=./<file_name>.wav <file_name>.m4a
        os.system(command)
        print "command: " + command
      else:
        print "Warning: Unknown message 'type'!!!"

