# Internet Computer
# Defines the methods necessary to interact with the Internet Computer web-canister via
# Dfinity SDK & Subprocess
import subprocess

def getEncKey():
    process = subprocess.run(f'dfx canister --network ic call oc46y-raaaa-aaaai-aa7rq-cai getEncKey',
                     stdout=subprocess.PIPE, shell=True)
    return process.stdout.decode('UTF-8').strip()[5:-4]
    

def setEncKey(CANISTER_ID, key):
    subprocess.Popen(f'dfx canister --network ic call {CANISTER_ID} setEncKey \'(\"{key}\")\'', shell=True, stdout=subprocess.PIPE).stdout.read()


if __name__ == "__main__":
    print(getEncKey())
