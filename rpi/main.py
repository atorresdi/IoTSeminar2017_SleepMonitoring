import comm
import time
import threading
import json
from Queue import Queue

# transmision test
# send JSON every n secs
def tx_test():
  ANDROID_IP = "172.20.90.119"
  ANDROID_PORT = 5555
  js_str = '{ "name":"John", "age":30, "car":null }'
  js = json.loads(js_str)
  while 1:
    time.sleep(2)
    comm.send(ANDROID_IP, ANDROID_PORT, json.dumps(js))

q = Queue(16)

# start message reception thread
comm_thread = threading.Thread(target=comm.receive, args=(5555, q))
comm_thread.daemon = True
comm_thread.start()

# # start message transmission thread
# tx_test_thread = threading.Thread(target=tx_test)
# tx_test_thread.daemon = True
# tx_test_thread.start()

# reception test
while 1:
  while q.empty():
    time.sleep(0.1)
    
  rx_obj = q.get()
  # print "type(rx_obj)", type(rx_obj)
  if type(rx_obj) is dict:
    # print "dict received"
    print "-------------------------------------------"
    print rx_obj
    #comm.send("127.0.0.1",7778, json.dumps(rx_obj))