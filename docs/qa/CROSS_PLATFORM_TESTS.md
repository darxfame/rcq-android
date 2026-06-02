# Cross-Platform Tests

## Setup
- 1x iOS device (reference/ios build)
- 1x Android device
- Both logged in, both are contacts

## Suite 1: Direct Message
- Android → iOS text: iOS displays correctly
- iOS → Android text: Android receives in real time
- Offline send: Android queues, delivers on reconnect

## Suite 2: Group Messaging
- Create group iOS → send from Android → iOS receives
- Create group Android → send from iOS → Android receives

## Suite 3: Crypto
- ECIES: Android encrypts → iOS decrypts ✅
- ECIES: iOS encrypts → Android decrypts ✅
- Session reset: both re-establish cleanly
