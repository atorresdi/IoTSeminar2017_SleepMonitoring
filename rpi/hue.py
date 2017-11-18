import httplib
import time

DELTA_TIME = 7
IP = "10.42.0.10"
PATH = "/api/pY5xq4YUguxLqvEwqeyXRThbl0IX0kml39FzrV0K/lights/1/state"

# send request to the hub
def httpPUT(ip, path, body):
  conn = httplib.HTTPConnection(ip)
  conn.request("PUT", path, body)
  response = conn.getresponse()
  print response.status, response.reason

# set brightness and color of the light bulb
def setLight(bri, hue):
  if bri > 0:
    BODY = '{"on":true, "sat":255, "bri":' + str(bri) + ',"hue":' + str(hue) + '}'
  else:
    BODY = '{"on":false}'
  httpPUT(IP, PATH, BODY)

# gradually increase brightness and change color
def wakeUp(hue, deltaTime):
  for i in range(1,255):
    setLight(i, hue)
    hue+=50
    time.sleep(deltaTime)
    
def fallAsleep(hue, deltaTime):
  for i in xrange(255, 0, -1):
    setLight(i, hue)
    time.sleep(deltaTime)
    
def startWakeUpThread():
  # start wakeup thread
  wakeUpThread = threading.Thread(target=wakeUp, args=(0, DELTA_TIME))
  wakeUpThread.daemon = True
  wakeUpThread.start()



  
  
