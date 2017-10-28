import socket
import json
from Queue import Queue

def receive(port, q):
  s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  s.bind(('', port))
  print "Socket created", port
  
  accum = ""
  
  while 1:
    s.listen(1)
    conn, addr = s.accept()
    print 'Connection accepted', addr
    
    while 1:
      rx_str = conn.recv(1024)
      if not rx_str: break
      #print "rx_str", rx_str
      
      accum += rx_str
      if accum.startswith('{') and ('}' in accum):
	accum = accum[:accum.find('}')+1] # remove everything after }
	#print "accum", accum
	js = json.loads(accum)
	q.put(js)
      
    conn.close() 
    print 'Connection closed', addr
    
def send(addr, port, msg):
  s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  s.connect((addr, port))
  
  totalsent = 0
  while totalsent < len(msg):
    sent = s.send(msg[totalsent:])
    if sent == 0:
      raise RuntimeError("socket connection broken")
    totalsent = totalsent + sent
    
