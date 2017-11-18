import os
import socket
import json
from Queue import Queue

addrPhone = ''
PORT = 5555

RX_BUF_SIZE = 1024 * 32
rxbuf = bytearray(RX_BUF_SIZE)

def getJsonString(byteArray):
  if byteArray[0] != ord('{'): return ""
  for idx, val in enumerate(byteArray):
    if (val == ord('}')):
      jsonString = byteArray[:idx+1].decode("utf-8")
      return jsonString
  return ""

def receive(port, q):
  s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  s.bind(('', port))
  print "Socket created", port
  
  while 1:
    s.listen(1)
    conn, addr = s.accept()
    addrPhone = addr
    print 'Connection accepted', addr
    
    # wait for data
    size = 0
    toread = RX_BUF_SIZE
    view = memoryview(rxbuf)
    while 1:
      nbytes = conn.recv_into(view, toread)
      if nbytes == 0: break
      view = view[nbytes:]
      size += nbytes
      toread -= nbytes

    conn.close() 
    # print 'Connection closed', addr

    jsonString = getJsonString(rxbuf)
    jsonObj = json.loads(jsonString) # parse json string
    # print "jsonString " + jsonString

    if 'type' in jsonObj and jsonObj['type'] == 'audioFileFrame':
      # store audio data
      offset = len(jsonString)
      file = open(jsonObj['name'], 'a+')
      file.write(rxbuf[offset:size])
      file.close()
      fsize = (os.stat(jsonObj['name'])).st_size
      if fsize >= int(jsonObj['totalSize']): q.put(jsonObj) # send to main thread
    else:
      q.put(jsonObj) # send to main thread
    
    
def send(msg):
  s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  s.connect((addrPhone, PORT))
  
  totalsent = 0
  while totalsent < len(msg):
    sent = s.send(msg[totalsent:])
    if sent == 0:
      raise RuntimeError("socket connection broken")
    totalsent = totalsent + sent
    
