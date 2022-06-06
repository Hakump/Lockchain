# Server
import socket


if __name__ == '__main__':
    # next create a socket object
    s = socket.socket()
    print("Socket successfully created")
    port = 6969  # Nice

    # Next bind to the port as listener
    s.bind(('', port))
    print("socket bound to %s" % port)
    s.listen(5)

    while True:
        # Establish connection with client.
        c, addr = s.accept()
        print('Got connection from', addr)

        print(c.recv(1024).decode())

        # send a thank you message to the client. encoding to send byte type.
        c.send('Thank you for connecting'.encode())

        # Close the connection with the client
        c.close()

        # Breaking once connection closed
        break
