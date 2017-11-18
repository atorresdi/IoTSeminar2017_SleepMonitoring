import numpy as np
import csv
import matplotlib.pyplot as plt
import sys
import sympy
from sympy.abc import delta

# class defining the possible states
class State:
  STILL = 0
  MOVING = 1
  MOVING_TO_STILL = 2
  
sleepState = "lightSleep"
treshold = 0.2
minDeepSleepTime = 20*60 # 20 minutes
tStill = [0]
tMoving = []
deepSleep = []
totalDeepSleep = 0
maxDeepSleep = 0

# calculate the approximate derivative of acceleration
def calculateJerk(t, acc):
  jerk = []
  jerk.append(0)
  for i in range(1, len(accX)-1):
    jerk.append(abs((accX[i+1] - accX[i-1]) / float(t[i+1] - t[i-1])))
  jerk.append(0)
  return jerk;

# finite state machine for activity detection and sleep stage classification
def runFSM(timestamp, totalJerk):  
  for i in range(0, len(timestamp)):
    if state == State.STILL:
      if time - tStill[-1] >= minDeepSleepTime:
	sleepState = "deepSleep"
      if totalJerk[i] >= treshold:
	time = timestamp[i]
	print "\t\t\tmoving at " + str(time)
	sleepState = "lightSleep"
	state = State.MOVING
	tMoving.append(time)
	if time - tStill[-1] >= minDeepSleepTime:
	  timeDeepSleep = time - tStill[-1]
	  deepSleep.append([tStill[-1], time])
	  totalDeepSleep += timeDeepSleep
	  if timeDeepSleep > maxDeepSleep:
	    maxDeepSleep = timeDeepSleep

    elif state == State.MOVING:
      if totalJerk[i] < treshold:
	time = timestamp[i]
	state = State.MOVING_TO_STILL
	
    elif state == State.MOVING_TO_STILL:
      if totalJerk[i] >= treshold:
	state = State.MOVING
      elif (timestamp[i]-time) > minStillTime:
	state = State.STILL
	print "still at " + str(time)
	tStill.append(time)

# process acceleration data
def process(jsonObj):
  timestamp = jsonObj['timestamp']
  accX = jsonObj['accX']
  accY = jsonObj['accY']
  accZ = jsonObj['accZ']
  
  jerkX = calculateJerk(timestamp, accX)
  jerkY = calculateJerk(timestamp, accY)
  jerkZ = calculateJerk(timestamp, accZ)
  jerkXYZ = jerkX+jerkY+jerkZ
  
  runFSM(timestamp, jerkXYZ)
  return sleepState

