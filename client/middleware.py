from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives import serialization
from os.path import exists
import socket
import icp

# Globals store public/private keys as key objects
PRIVATE_KEY = None
PUBLIC_KEY = None
# Keyflie names
PUBLIC_KEY_FILE = 'public_key.pem'
PRIVATE_KEY_FILE = 'private_key.pem'
# ICP Canister ID
CANISTER_ID = 'oc46y-raaaa-aaaai-aa7rq-cai'


# Generates a public / private key pair
# returned as tuple (public, private)
def generate_keypair():
    private_key = rsa.generate_private_key(
        public_exponent=65537,
        key_size=512,
        backend=default_backend()
    )
    public_key = private_key.public_key()
    return (public_key, private_key)


# Write keys to file
def store_keys(keypair):
    # Write public key
    with open(PUBLIC_KEY_FILE, 'wb') as f:
        f.write(keypair[0].public_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PublicFormat.PKCS1
        ))

    # Write private key
    with open(PRIVATE_KEY_FILE, 'wb') as f:
        f.write(keypair[1].private_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PrivateFormat.PKCS8,
            encryption_algorithm=serialization.NoEncryption()
        ))


# Read exisiting keys saved in .pem format
def read_keys():
    with open(PRIVATE_KEY_FILE, "rb") as key_file:
        private_key = serialization.load_pem_private_key(
            key_file.read(),
            password=None,
            backend=default_backend()
        )
    with open(PUBLIC_KEY_FILE, "rb") as key_file:
        public_key = serialization.load_pem_public_key(
            key_file.read(),
            backend=default_backend()
        )
    return public_key, private_key


def connect_to_server(ip):
    # Create a socket object
    server = socket.socket()
    port = 420  # Nice
    server.connect((ip, port))
    return server


def unlock(server: socket.socket):
    server.send("unlock".encode())

def listen_for_phone():
    s = socket.socket()
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
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
        return (c.recv(1024).decode())
        

if __name__ == '__main__':
    # Default ip
    ip = '192.168.158.64'

    while True:
        
        phone_key = listen_for_phone()

        icp_key = icp.getEncKey()

        if(phone_key == icp_key):
            print("Unlocked the lock")
            s = connect_to_server(ip)
            s.send("unlock".encode())
        else:
            print("Failed")
