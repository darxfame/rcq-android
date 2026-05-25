# RCQ Android App - Complete TODO List

## Authentication Flow
- [x] Welcome screen with nickname input
- [x] Registration with key generation
- [x] Recovery phrase generation and display
- [x] Recovery phrase auto-copy to clipboard
- [x] Auth token storage in DataStore
- [x] Auth interceptor for API requests
- [x] Logout functionality
- [ ] Account recovery flow (restore from phrase)
- [ ] Biometric/PIN unlock

## Navigation
- [x] Bottom nav: Chats, Contacts, Settings
- [x] Chat detail screen
- [x] Profile screen
- [x] Add contact screen
- [ ] Create group screen
- [ ] Audio rooms screen
- [ ] Stories screen
- [ ] Games screen
- [ ] Marketplace screen

## Contacts
- [x] Display contacts list
- [x] Search contacts locally
- [x] Add contact by UIN (search on server)
- [x] Contact requests - accept/decline
- [x] Remove contact
- [x] Block/unblock contact
- [ ] Edit contact nickname
- [ ] Favorite contacts

## Chats
- [x] Display chats list
- [x] Load messages from database
- [x] Send message
- [x] Edit message
- [x] Delete message
- [ ] Reply to message
- [ ] Forward message
- [ ] Real-time messaging (WebSocket)
- [ ] Push notifications

## Settings
- [x] Display current user info (UIN, nickname)
- [x] View recovery phrase
- [x] Logout button
- [ ] Profile editing
- [ ] Privacy settings
- [ ] Notification settings
- [ ] Appearance/theme settings
- [ ] Storage settings
- [ ] About/help screens

## API Integration
- [x] Auth: register, login
- [x] Users: get, search
- [x] Contacts: list, add, remove, block, unblock
- [x] Contact requests: list, accept, decline
- [x] Chats: list, create
- [x] Messages: send, edit, delete
- [ ] Messages: real-time via WebSocket

## Database (Room)
- [x] Users table
- [x] Contacts table
- [x] Chats table
- [x] Messages table
- [ ] Groups table
- [ ] Stories table
- [ ] Calls table

## Core Features
- [ ] Voice/video calls
- [ ] Audio rooms
- [ ] Stories
- [ ] Games
- [ ] Marketplace/Pets