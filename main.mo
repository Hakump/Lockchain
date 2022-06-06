// Version 3 Final

import Text "mo:base/Text";
import Hash "mo:base/Hash";
import HashMap "mo:base/HashMap";
import Principal "mo:base/Principal";
import Array "mo:base/Array";

// For reference:
//let eqTest: (Nat,Nat) ->Bool = func(x, y) { x == y };
//let keyHashTest: (Nat) -> Hash.Hash = func(x) { Hash.hash x };
//var topics = HashMap.HashMap<Nat, Nat>(8, eqTest, keyHashTest);
//class HashMap<K, V>(initCapacity : Nat, keyEq : (K, K) -> Bool, keyHash : K -> Hash.Hash)
// func encodeUtf8(t : Text) : Blob = (prim "encodeUtf8" : Text -> Blob) t;

// Sets owner: principal to canister initializer
shared({caller}) actor class LockchainV1() {
    stable let root = caller;

    // Public Encryption key of lock
    var encKey: Text = "";
    var userSize: Nat = 0;

    //https://smartcontracts.org/docs/interface-spec/index.html#system-api-inspect-message
    // FIXME Stable vars retain state across version updates

    // Define custom types
    public type Role = {
        #owner;
        #user;
        #lock;
    };

    //FIXMe Async
    func getRole(pal: Principal) : ?Role {
        if (pal == root) {
            ?#owner;
        } else {
            userRoles.get(pal);
        }
    };
    
    // Func used to find keys
    let eq: (Principal, Principal) ->Bool = func(x: Principal, y: Principal) { Principal.equal(x, y)};
    // Function used to take hash of keys
    let keyHash: (Principal) -> Hash.Hash = func(x: Principal) {Principal.hash x};
    // Hash map used to store userRoles as <principals, role>
    var userRoles = HashMap.HashMap<Principal, Role>(10, eq, keyHash);
    // Fash map used to store userKeys
    var userKeys =  HashMap.HashMap<Principal, Text>(10, eq, keyHash);

    // Get the public encryption key.
    public func getEncKey() : async Text {
        return encKey;
    };

    // Set the public encryption key. 
    public shared({caller}) func setEncKey(key : Text) : async Bool {
        //assert(getRole(caller) == ?#lock);
        encKey := key;
	    return true;
    };

    // Removes access control of a user. Should be private to admin
    public shared({caller}) func removeUser(user: Principal) : async (?Role, ?Text){
        //assert(getRole(caller) == ?#owner);
        userSize -= 1;
        return (userRoles.remove(user), userKeys.remove(user));
    };

    // Add user principal to access control. Should be private to admin
    // HOW TO CALL : dfx canister call lockchainV1 addUser '(principal "prin-id", variant {owner/user/lock}, "key")'
    public shared({caller}) func addUser(user: Principal, userRole: Role, key: Text) : async Bool{
        //assert(getRole(caller) == ?#owner);
        userRoles.put(user, userRole);
        userKeys.put(user, key);
        userSize += 1;
        return true;
    };

    // public shared({caller}) func getUsers() : async Iter.Iter<Text> {
    //     //assert(getRole(caller) == ?#lock);
    //     return userKeys.keys();
    // };

    public shared({caller}) func getUsers() : async [Text]{
        //assert(getRole(caller) == ?#lock);
        let temp = Array.init<Text>(userSize, "");
        var i: Nat = 0;
        
        for (key in userKeys.vals()) {  
            temp[i] := key;
            i += 1;
        };
        // Reset remove size
        userSize := 0;

        let toReturn = Array.freeze(temp);

        return toReturn;
    };

    // Asserts callers are authorized. Prevents unauthorized cycle on internet
    public shared({caller}) func canister_inspect_message() : async Bool{
        assert(null != userRoles.get(caller));
        return true;
    };

    // // Test ingore
    // public func test(val : Nat) : async Bool {
    //     assert(1 != val);
	//     return true;
    // };  
};
